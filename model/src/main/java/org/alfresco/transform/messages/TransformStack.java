/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transform.messages;

import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.MultiStep;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.common.TransformerDebug;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Represents the current state of a top level transform request in terms of its current nested call stack, which is
 * the current transform step being performed and what other transform steps are still to be executed in the current
 * level. This information is encoded in the {@link MultiStep} structure of the
 * internal context passed between T-Router and T-Engines. Ideally we would have changed the structure,
 * but for backward compatibility we are using the existing structure, which allows T-Engines that were developed
 * previously to be used unchanged.<p><br/>
 *
 * Originally the T-Router only allowed pipeline and single step transforms, so it was possible to represent them as a
 * flat list. However the original design was of a nested structure. Pipelines are just one example, with failover
 * transforms being the other. To bring the T-Router up to the same level of functionality as the Repository, it too
 * needs to support nested transforms. <p><br/>
 *
 * <li>@{code transformsToBeDone[0]} holds the original Transformer Request Options as a list of key values pairs.
 *     Needed so that we don't lose values as we walk down the individual transform steps</li>
 * <li>@{code transformsToBeDone[1]} holds the original source reference so that we don't delete the original source
 *     until the whole transform is known to be successful, so that a queue entry may be retried on failure.</li>
 * <li>@{code transformsToBeDone[2]} holds information about the top level transform</li>
 * <li>@{code transformsToBeDone[size()-1]} holds information about the most nested transform being processed</li>
 * <li>Each level contains a list of step transforms. Just one for a singe step transform or a list for pipeline and
 *     failover transforms</li>
 * <li>When a step is processed it will result in the creation of another level if it is a pipeline or failover
 *     transform</li>
 * <li>As steps are completed, they are removed</li>
 * <li>When there are no steps left in a level the level is removed</li>
 *
 * Each level is represented by a String with a pipeline or failover flag @{code 'P'|'F'} followed by a step counter
 * and start time used in debug, a retry count and a sequence of transform steps. Each step is made up of three parts:
 * @{code<transformerName>|<sourceMimetype>|<targetMimetype> . All fields are separated by a @code{'\u23D0'} character.
 * The last step in the sequence is the current transform being performed. The top level transform is a pipeline of
 * one step. Although the source and target mimetypes are always the same for failover transforms, they use the same
 * structure.
 */
public class TransformStack
{
    public static final String PIPELINE_FLAG = "P";
    public static final String FAILOVER_FLAG = "F";

    public static final String SEPARATOR = "\u23D0";
    private static final String SEPARATOR_REGEX = "\u23D0";

    static final int OPTIONS_LEVEL = 0;
    static final int SOURCE_REFERENCE_LEVEL = 1;
    static final int TOP_STACK_LEVEL = 2;

    private static final int FLAG_INDEX = 0;
    private static final int REFERENCE_INDEX = 1;
    private static final int START_INDEX = 2;
    private static final int RETRY_INDEX = 3;

    private static final int FIELDS_IN_HEADER = 4; // flag | counter | retry
    private static final int FIELDS_PER_STEP  = 3; // name | source | target

    public static LevelBuilder levelBuilder(String flag)
    {
        return new LevelBuilder(flag);
    }

    public static class LevelBuilder
    {
        private final ArrayList<String> reverseOrderStepElements = new ArrayList<>();
        private final String flag;

        private LevelBuilder(String flag)
        {
            this.flag = flag;
        }

        public LevelBuilder withStep(String transformerName, String sourceMediaType, String targetMediaType)
        {
            reverseOrderStepElements.add(targetMediaType);
            reverseOrderStepElements.add(sourceMediaType);
            reverseOrderStepElements.add(transformerName);
            return this;
        }

        public String build()
        {
            StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
            stringJoiner.add(flag);
            stringJoiner.add("1");
            stringJoiner.add("0");
            stringJoiner.add("0");
            for (int i=reverseOrderStepElements.size()-1; i>=0; i--)
            {
                stringJoiner.add(reverseOrderStepElements.get(i));
            }
            return stringJoiner.toString();
        }
    }

    public static class Step
    {
        private final String[] step;

        private Step(String stepWithSeparatedFields)
        {
            this.step = stepWithSeparatedFields.split(SEPARATOR_REGEX);
        }

        public String getTransformerName()
        {
            return step[0];
        }

        public String getSourceMediaType()
        {
            return step[1];
        }

        public String getTargetMediaType()
        {
            return step[2];
        }
    }

    public static void setInitialTransformRequestOptions(InternalContext internalContext,
                                                         Map<String, String> transformRequestOptions)
    {
        init(internalContext);
        StringJoiner sj = new StringJoiner(SEPARATOR);
        transformRequestOptions.forEach((key,value)-> sj.add(key).add(value));
        levels(internalContext).set(OPTIONS_LEVEL, sj.toString());
    }

    public static void setInitialSourceReference(InternalContext internalContext, String sourceReference)
    {
        init(internalContext);
        levels(internalContext).set(SOURCE_REFERENCE_LEVEL, sourceReference);
    }

    public static Map<String, String> getInitialTransformRequestOptions(InternalContext internalContext)
    {
        Map<String, String> transformRequestOptions = new HashMap<>();

        // To avoid the case where split() discards the last value, when it is a zero length string, we add and remove
        // a space. None of the keys or value may be null.
        String[] split = (level(internalContext, OPTIONS_LEVEL)+' ').split(SEPARATOR_REGEX);
        String lastValue = split[split.length - 1];
        split[split.length-1] = lastValue.substring(0, lastValue.length()-1);

        for (int i = split.length-2; i >= 0; i-=2)
        {
            transformRequestOptions.put(split[i], split[i+1]);
        }
        return transformRequestOptions;
    }

    public static String getInitialSourceReference(InternalContext internalContext)
    {
        return level(internalContext, SOURCE_REFERENCE_LEVEL);
    }

    private static List<String> levels(InternalContext internalContext)
    {
        return internalContext.getMultiStep().getTransformsToBeDone();
    }

    private static String level(InternalContext internalContext, int i)
    {
        return levels(internalContext).get(i);
    }

    private static void init(InternalContext internalContext)
    {
        while(levels(internalContext).size() < TOP_STACK_LEVEL)
        {
            levels(internalContext).add(null);
        }
    }

    private static String currentLevel(InternalContext internalContext)
    {
        return parentLevel(internalContext, 0);
    }

    private static String parentLevel(InternalContext internalContext, int parentLevels)
    {
        List<String> levels = levels(internalContext);
        int i = levels.size() - 1 - parentLevels;
        return i >= TOP_STACK_LEVEL ? levels.get(i) : null;
    }

    public static boolean isFinished(InternalContext internalContext)
    {

        int levelsLeft = levels(internalContext).size() - TOP_STACK_LEVEL;
        return  levelsLeft <= 0 || // there has been an error, so we have lost the stack
                levelsLeft == 1 && // on top level wrapper level
                isTransformLevelFinished(internalContext); // the one step has been processed (removed)

    }

    public static void addTransformLevel(InternalContext internalContext, LevelBuilder levelBuilder)
    {
        levels(internalContext).add(levelBuilder.build());
    }

    public static void setReference(InternalContext internalContext, String requestCountOrClientRequestId)
    {
        setHeaderField(internalContext, REFERENCE_INDEX, requestCountOrClientRequestId);
    }

    public static void incrementReference(InternalContext internalContext)
    {
        setHeaderField(internalContext, REFERENCE_INDEX, Integer.toString(getReferenceCounter(internalContext)+1));
    }

    public static void resetAttemptedRetries(InternalContext internalContext)
    {
        setHeaderField(internalContext, RETRY_INDEX, 0);
    }

    public static void setStartTime(InternalContext internalContext)
    {
        setHeaderField(internalContext, START_INDEX, System.currentTimeMillis());
    }

    public static void incrementAttemptedRetries(InternalContext internalContext)
    {
        setHeaderField(internalContext, RETRY_INDEX,getAttemptedRetries(internalContext)+1);
    }

    private static void setHeaderField(InternalContext internalContext, int index, long value)
    {
        setHeaderField(internalContext, index, Long.toString(value));
    }

    private static void setHeaderField(InternalContext internalContext, int index, String value)
    {
        List<String> levels = levels(internalContext);
        int size = levels.size();
        String level = levels.get(size-1);
        int j = indexOfField(level, index);
        int k = level.indexOf(SEPARATOR, j+1);
        levels.set(size-1, level.substring(0, j) + value + level.substring(k));
    }

    public static String getReference(InternalContext internalContext)
    {
        StringJoiner ref = new StringJoiner(".");
        List<String> levels = levels(internalContext);
        for (int i=TOP_STACK_LEVEL; i<levels.size(); i++)
        {
            ref.add(getHeaderFieldString(levels.get(i), REFERENCE_INDEX));
        }
        return ref.toString();
    }

    public static void setReferenceInADummyTopLevelIfUnset(InternalContext internalContext, String reference)
    {
        if (!reference.isBlank() && getReference(internalContext).isBlank() ) // When top transform level not set
        {
            init(internalContext);
            addTransformLevel(internalContext, levelBuilder(PIPELINE_FLAG));
            setReference(internalContext, reference);
        }
    }

    public static long getElapsedTime(InternalContext internalContext)
    {
        return System.currentTimeMillis() - getHeaderField(internalContext, START_INDEX).longValue();
    }

    private static int getReferenceCounter(InternalContext internalContext)
    {
        return getHeaderField(internalContext, REFERENCE_INDEX).intValue();
    }

    public static int getAttemptedRetries(InternalContext internalContext)
    {
        return getHeaderField(internalContext, RETRY_INDEX).intValue();
    }

    private static Long getHeaderField(InternalContext internalContext, int index)
    {
        return getHeaderField(currentLevel(internalContext), index);
    }

    private static Long getHeaderField(String level, int index)
    {
        return Long.valueOf(getHeaderFieldString(level, index));
    }

    private static String getHeaderFieldString(String level, int index)
    {
        return level.split(SEPARATOR_REGEX)[index];
    }

    public static void removeTransformLevel(InternalContext internalContext)
    {
        List<String> levels = levels(internalContext);
        levels.remove(levels.size()-1);
    }

    public static void removeRemainingTransformLevels(TransformReply reply, TransformerDebug transformerDebug)
    {
        List<String> levels = levels(reply.getInternalContext());
        if (levels != null)
        {
            while (!TransformStack.isFinished(reply.getInternalContext()))
            {
                removeFailedStep(reply, transformerDebug);
            }
        }
    }

    public static boolean isParentAFailover(InternalContext internalContext)
    {
        String level = currentLevel(internalContext);
        return level != null && level.startsWith(FAILOVER_FLAG);
    }

    public static String getParentName(InternalContext internalContext)
    {
        String level = parentLevel(internalContext, 1);
        return level == null ? null : new Step(level.substring(indexOfNextStep(level))).getTransformerName();
    }

    public static Step currentStep(InternalContext internalContext)
    {
        String level = currentLevel(internalContext);
        return new Step(level.substring(indexOfNextStep(level)));
    }

    public static boolean isLastStepInTransformLevel(InternalContext internalContext)
    {
        return getStepCount(internalContext) == 1;
    }

    private static boolean isTransformLevelFinished(InternalContext internalContext)
    {
        return getStepCount(internalContext) == 0;
    }

    private static int getStepCount(InternalContext internalContext)
    {
        return (StringUtils.countMatches(currentLevel(internalContext), SEPARATOR)+1-FIELDS_IN_HEADER)/FIELDS_PER_STEP;
    }

    public static void removeSuccessfulStep(TransformReply reply, TransformerDebug transformerDebug)
    {
        removeFinishedSteps(reply, true, transformerDebug);
    }

    public static void removeFailedStep(TransformReply reply, TransformerDebug transformerDebug)
    {
        removeFinishedSteps(reply, false, transformerDebug);
    }

    private static void removeFinishedSteps(TransformReply reply, boolean successful,
                                            TransformerDebug transformerDebug)
    {
        TransformStack.removeStep(reply, successful, transformerDebug);

        InternalContext internalContext = reply.getInternalContext();
        while (!TransformStack.isFinished(internalContext) &&
                TransformStack.isTransformLevelFinished(internalContext))
        {
            TransformStack.removeTransformLevel(internalContext);
            // We want to exit if removing steps for a failure and we have a parent that is a failover transform
            if (successful || !TransformStack.isParentAFailover(internalContext))
            {
                TransformStack.removeStep(reply, successful, transformerDebug);
            }
        }
    }

    private static void removeStep(TransformReply reply, boolean successful, TransformerDebug transformerDebug)
    {
        InternalContext internalContext = reply.getInternalContext();
        boolean parentAFailover = isParentAFailover(internalContext);
        boolean successfulFailoverStep = successful && parentAFailover;
        boolean unsuccessfulPipelineStep = !successful && !parentAFailover;

        transformerDebug.popTransform(reply);

        // For a successful failover step, or an unsuccessful pipeline step remove all sibling steps, otherwise just
        // remove one step as it was a successful pipeline step or an unsuccessful failover step
        List<String> levels = levels(internalContext);
        int size = levels.size();
        String level = levels.get(size-1);
        levels.set(size-1, level.substring(0,
                (successfulFailoverStep || unsuccessfulPipelineStep ? indexOfLastStep(level) : indexOfNextStep(level)) - 1));

        if (!isTransformLevelFinished(internalContext))
        {
            TransformStack.incrementReference(internalContext);
        }
    }

    private static int indexOfNextStep(String level)
    {
        int j = level.length()-1;
        for (int i = FIELDS_PER_STEP; i >= 1 && j > 0; i--)
        {
            j = level.lastIndexOf(SEPARATOR, j-1);
        }
        return j+1;
    }

    private static int indexOfLastStep(String level)
    {
        return indexOfField(level, FIELDS_IN_HEADER);
    }

    private static int indexOfField(String level, int n)
    {
        int j = 0;
        for (int i = n; i >= 1; i--)
        {
            j = level.indexOf(SEPARATOR, j+1);
        }
        return j+1;
    }

    public static String checkStructure(InternalContext internalContext, String type)
    {
        // A null value will have been replaced with an empty array, so no need to check for that.
        String errorMessage = levels(internalContext).size() < (TOP_STACK_LEVEL + 1)
                ? type+" InternalContext did not have the Stack set"
                : !validTransformOptions(internalContext)
                ? type+" InternalContext did not have the TransformOptions set correctly"
                : levels(internalContext).size() == 1
                ? type+" InternalContext levels were not set"
                : !validLevels(levels(internalContext))
                ? type+" InternalContext did not have levels set correctly"
                : null;
        return errorMessage;
    }

    private static boolean validTransformOptions(InternalContext internalContext)
    {
        String keysAndValues = level(internalContext, OPTIONS_LEVEL);
        if (keysAndValues == null)
        {
            return false;
        }
        if ("".equals(keysAndValues))
        {
            return true;
        }
        String[] split = keysAndValues.split(SEPARATOR_REGEX);
        if (split.length%2 != 0)
        {
            return false;
        }
        for (int i = split.length-2; i >= 0; i-=2)
        {
            if (split[i].isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    private static boolean validLevels(List<String> levels)
    {
        for (int i=levels.size()-1; i >=TOP_STACK_LEVEL; i--)
        {
            String level = levels.get(i);
            if (!validLevel(level))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean validLevel(String level)
    {
        if (level == null)
        {
            return false;
        }
        String[] split = level.split(SEPARATOR_REGEX);
        if (split.length   <  FIELDS_IN_HEADER+FIELDS_PER_STEP || // must be at least 1 step
            (split.length-FIELDS_IN_HEADER)%FIELDS_PER_STEP != 0 ||
            (!PIPELINE_FLAG.equals(split[FLAG_INDEX]) &&
             !FAILOVER_FLAG.equals(split[FLAG_INDEX])) ||
            !aValidReference(split[REFERENCE_INDEX]) ||
            !aPositiveLong(split[START_INDEX]) ||
            !aPositiveInt(split[RETRY_INDEX]))
        {
            return false;
        }

        for (int i=split.length-1; i>=FIELDS_IN_HEADER; i--)
        {
            if (split[i].isBlank())
            {
                return false;
            }
        }

        return true;
    }

    private static boolean aValidReference(String string)
    {
        string = string.startsWith("e") ? string.substring(1) : string;
        return aPositiveInt(string);
    }

    private static boolean aPositiveInt(String string)
    {
        try
        {
            return Integer.valueOf(string).toString().equals(string) && Integer.valueOf(string) >= 0;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    private static boolean aPositiveLong(String string)
    {
        try
        {
            return Long.valueOf(string).toString().equals(string) && Long.valueOf(string) >= 0;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
