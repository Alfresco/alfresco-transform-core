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
package org.alfresco.transform.client.registry;

import org.alfresco.transform.client.model.config.AddSupported;
import org.alfresco.transform.client.model.config.SupportedDefaults;
import org.alfresco.transform.client.model.config.OverrideSupported;
import org.alfresco.transform.client.model.config.RemoveSupported;
import org.alfresco.transform.client.model.config.SupportedSourceAndTarget;
import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transform.client.model.config.TransformOption;
import org.alfresco.transform.client.model.config.TransformStep;
import org.alfresco.transform.client.model.config.Transformer;
import org.alfresco.transform.client.model.config.TransformerAndTypes;
import org.alfresco.transform.client.model.config.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.alfresco.transform.client.model.config.CoreVersionDecorator.setCoreVersionOnMultiStepTransformers;

/**
 * This class combines one or more T-Engine config and local files and registers them as if they were all in one file.
 * Transform options are shared between all sources.<p><br>
 *
 * The caller should make calls to {@link #addTransformConfig(TransformConfig, String, String, AbstractTransformRegistry)}
 * followed by calls to {@link #combineTransformerConfig(AbstractTransformRegistry)} and then
 * {@link #registerCombinedTransformers(AbstractTransformRegistry)}.<p><br>
 *
 * The helper method {@link #combineAndRegister(TransformConfig, String, String, AbstractTransformRegistry)} may be used
 * when there is only one config.
 *
 * @author adavis
 */
public class CombinedTransformConfig
{
    private static final String MIMETYPE_METADATA_EXTRACT = "alfresco-metadata-extract";
    private static final String MIMETYPE_METADATA_EMBED = "alfresco-metadata-embed";

    private static final String INTERMEDIATE_STEPS_SHOULD_HAVE_A_TARGET_MIMETYPE = "intermediate steps should have a target mimetype";
    private static final String THE_FINAL_STEP_SHOULD_NOT_HAVE_A_TARGET_MIMETYPE = "the final step should not have a target mimetype";

    private final Map<String, Set<TransformOption>> combinedTransformOptions = new HashMap<>();
    private List<Origin<Transformer>> combinedTransformers = new ArrayList<>();
    private final Defaults defaults = new Defaults();

    public static void combineAndRegister(TransformConfig transformConfig, String readFrom, String baseUrl,
                                          AbstractTransformRegistry registry)
    {
        CombinedTransformConfig combinedTransformConfig = new CombinedTransformConfig();
        combinedTransformConfig.addTransformConfig(transformConfig, readFrom, baseUrl, registry);
        combinedTransformConfig.combineTransformerConfig(registry);
        combinedTransformConfig.registerCombinedTransformers(registry);
    }

    public void clear()
    {
        combinedTransformOptions.clear();
        combinedTransformers.clear();
        defaults.clear();
    }

    public void addTransformConfig(List<Origin<TransformConfig>> transformConfigList, AbstractTransformRegistry registry)
    {
        transformConfigList.forEach(tc -> addTransformConfig(tc.get(), tc.getReadFrom(), tc.getBaseUrl(), registry));
    }

    public void addTransformConfig(TransformConfig transformConfig, String readFrom, String baseUrl,
                                   AbstractTransformRegistry registry)
    {
        removeTransformers(transformConfig.getRemoveTransformers(), readFrom, registry);
        supportedDefaults(transformConfig.getSupportedDefaults(), readFrom, registry);

        removeSupported(transformConfig.getRemoveSupported(), readFrom, registry);
        addSupported(transformConfig.getAddSupported(), readFrom, registry);
        overrideSupported(transformConfig.getOverrideSupported(), readFrom, registry);

        // Add transform options and transformers from the new transformConfig
        transformConfig.getTransformOptions().forEach(combinedTransformOptions::put);
        transformConfig.getTransformers().forEach(t -> combinedTransformers.add(new Origin<>(t, baseUrl, readFrom)));
    }

    private void removeTransformers(Set<String> removeTransformersSet, String readFrom, AbstractTransformRegistry registry)
    {
        if (!removeTransformersSet.isEmpty())
        {
            Set<String> leftOver = new HashSet<>(removeTransformersSet);
            combinedTransformers.removeIf(combinedTransformer ->
            {
                String transformerName = combinedTransformer.get().getTransformerName();
                if (removeTransformersSet.contains(transformerName))
                {
                    leftOver.remove(transformerName);
                    return true;
                }
                return false;
            });

            Set<String> quotedLeftOver = leftOver.stream().map(transformerName -> "\""+transformerName+'"').collect(toSet());
            logWarn(quotedLeftOver, readFrom, registry, "removeTransformers");
        }
    }

    private void supportedDefaults(Set<SupportedDefaults> supportedDefaults, String readFrom, AbstractTransformRegistry registry)
    {
        if (!supportedDefaults.isEmpty())
        {
            Set<SupportedDefaults> leftOver = new HashSet<>(supportedDefaults);
            supportedDefaults.stream()
                    .filter(supportedDefault -> supportedDefault.getMaxSourceSizeBytes() != null ||
                                                supportedDefault.getPriority() != null)
                    .forEach(supportedDefault ->
                    {
                        defaults.add(supportedDefault);
                        leftOver.remove(supportedDefault);
                    });

            logWarn(leftOver, readFrom, registry, "supportedDefaults");
        }
    }

    private <T extends TransformerAndTypes> void processSupported(
            Set<T> tSet, String readFrom, AbstractTransformRegistry registry, String elementName,
            ProcessSingleT<T> process)
    {
        if (!tSet.isEmpty())
        {
            Set<T> leftOver = new HashSet<>(tSet);
            tSet.stream()
                    .filter(t -> t.getTransformerName() != null &&
                                 t.getSourceMediaType() != null &&
                                 t.getTargetMediaType() != null)
                    .forEach(t -> process.process(leftOver, t));

            logWarn(leftOver, readFrom, registry, elementName);
        }
    }

    private <T> void logWarn(Set<T> leftOver, String readFrom, AbstractTransformRegistry registry, String elementName)
    {
        if (!leftOver.isEmpty())
        {
            StringJoiner sj = new StringJoiner(", ",
                    "Unable to process \"" + elementName + "\": [", "]. Read from " + readFrom);
            leftOver.forEach(removeSupported -> sj.add(removeSupported.toString()));
            registry.logWarn(sj.toString());
        }
    }

    private interface ProcessSingleT<T>
    {
        void process(Set<T> leftOver, T removeSupported);
    }

    private void removeSupported(Set<RemoveSupported> removeSupportedSet, String readFrom, AbstractTransformRegistry registry)
    {
        processSupported(removeSupportedSet, readFrom, registry, "removeSupported",
                (leftOver, removeSupported) ->
                        combinedTransformers.stream()
                        .map(Origin::get)
                        .forEach(transformer ->
                        {
                            if (transformer.getTransformerName().equals(removeSupported.getTransformerName()) &&
                                    transformer.getSupportedSourceAndTargetList().removeIf(supported ->
                                            supported.getSourceMediaType().equals(removeSupported.getSourceMediaType()) &&
                                            supported.getTargetMediaType().equals(removeSupported.getTargetMediaType())))
                            {
                                leftOver.remove(removeSupported);
                            }
                        }));
    }

    private void addSupported(Set<AddSupported> addSupportedSet, String readFrom, AbstractTransformRegistry registry)
    {
        processSupported(addSupportedSet, readFrom, registry, "addSupported",
                (leftOver, addSupported) ->
                        combinedTransformers.stream()
                                .map(Origin::get)
                                .filter(transformer -> transformer.getTransformerName().equals(addSupported.getTransformerName()))
                                .forEach(transformerWithName ->
                                {
                                    Set<SupportedSourceAndTarget> supportedSourceAndTargetList =
                                            transformerWithName.getSupportedSourceAndTargetList();
                                    SupportedSourceAndTarget existingSupported = getExistingSupported(
                                            supportedSourceAndTargetList,
                                            addSupported.getSourceMediaType(), addSupported.getTargetMediaType());
                                    if (existingSupported == null)
                                    {
                                        SupportedSourceAndTarget newSupportedSourceAndTarget = SupportedSourceAndTarget.builder()
                                                .withSourceMediaType(addSupported.getSourceMediaType())
                                                .withTargetMediaType(addSupported.getTargetMediaType())
                                                .withMaxSourceSizeBytes(addSupported.getMaxSourceSizeBytes())
                                                .withPriority(addSupported.getPriority())
                                                .build();
                                        supportedSourceAndTargetList.add(newSupportedSourceAndTarget);
                                        leftOver.remove(addSupported);
                                    }}));
    }

    private void overrideSupported(Set<OverrideSupported> overrideSupportedSet, String readFrom, AbstractTransformRegistry registry)
    {
        processSupported(overrideSupportedSet, readFrom, registry, "overrideSupported",
                (leftOver, overrideSupported) ->
                        combinedTransformers.stream().
                                map(Origin::get).
                                filter(transformer -> transformer.getTransformerName().equals(overrideSupported.getTransformerName())).
                                forEach(transformerWithName ->
                                {
                                    Set<SupportedSourceAndTarget> supportedSourceAndTargetList =
                                            transformerWithName.getSupportedSourceAndTargetList();
                                    SupportedSourceAndTarget existingSupported = getExistingSupported(
                                            supportedSourceAndTargetList,
                                            overrideSupported.getSourceMediaType(), overrideSupported.getTargetMediaType());
                                    if (existingSupported != null)
                                    {
                                        supportedSourceAndTargetList.remove(existingSupported);
                                        existingSupported.setMaxSourceSizeBytes(overrideSupported.getMaxSourceSizeBytes());
                                        existingSupported.setPriority(overrideSupported.getPriority());
                                        supportedSourceAndTargetList.add(existingSupported);
                                        leftOver.remove(overrideSupported);
                                    }
                                }));
    }

    private SupportedSourceAndTarget getExistingSupported(Set<SupportedSourceAndTarget> supportedSourceAndTargetList,
                                                          String sourceMediaType, String targetMediaType)
    {
        return supportedSourceAndTargetList.stream().filter(supported ->
                        supported.getSourceMediaType().equals(sourceMediaType) &&
                        supported.getTargetMediaType().equals(targetMediaType))
                .findFirst()
                .orElse(null);
    }

    public void combineTransformerConfig(AbstractTransformRegistry registry)
    {
        removeInvalidTransformers(registry);
        sortTransformers(registry);
        applyDefaults();
        addWildcardSupportedSourceAndTarget(registry);
        setCoreVersionOnCombinedMultiStepTransformers();
    }

    public TransformConfig buildTransformConfig()
    {
        List<Transformer> transformers = new ArrayList<>();
        combinedTransformers.forEach(ct->transformers.add(ct.get()));
        Set<SupportedDefaults> supportedDefaults = defaults.getSupportedDefaults();
        return TransformConfig
                .builder()
                .withTransformers(transformers)
                .withTransformOptions(combinedTransformOptions)
                .withSupportedDefaults(supportedDefaults)
                .build();
    }

    public void registerCombinedTransformers(AbstractTransformRegistry registry)
    {
        combinedTransformers.forEach(ct ->
                registry.register(ct.get(), combinedTransformOptions, ct.getBaseUrl(), ct.getReadFrom()));
    }

    /**
     * Discards transformers that are invalid (e.g. transformers that have both pipeline and failover sections). Calls
     * {@link #removeInvalidTransformer(int, List, AbstractTransformRegistry, Origin, Transformer, String, String,
     * boolean, boolean)} for each transform, so that individual invalid transforms or overridden
     * transforms may be discarded.
     *
     * @param registry that will hold the transforms.
     */
    private void removeInvalidTransformers(AbstractTransformRegistry registry)
    {
        for (int i=0; i<combinedTransformers.size(); i++)
        {
            try
            {
                Origin<Transformer> transformAndItsOrigin = combinedTransformers.get(i);
                Transformer transformer = transformAndItsOrigin.get();
                String readFrom = transformAndItsOrigin.getReadFrom();
                String name = transformer.getTransformerName();
                List<TransformStep> pipeline = transformer.getTransformerPipeline();
                List<String> failover = transformer.getTransformerFailover();
                boolean isPipeline = pipeline != null && !pipeline.isEmpty();
                boolean isFailover = failover != null && !failover.isEmpty();

                if (isPipeline && isFailover)
                {
                    throw new IllegalArgumentException("Transformer " + transformerName(name) +
                            " cannot have pipeline and failover sections. Read from " + readFrom);
                }

                // Remove transforms as they may override each other or be invalid
                int indexToRemove = removeInvalidTransformer(i, combinedTransformers, registry, transformAndItsOrigin,
                        transformer, name, readFrom, isPipeline, isFailover);

                // Remove an overridden transform
                if (indexToRemove >= 0)
                {
                    combinedTransformers.remove(indexToRemove);
                    // The current index i should be decremented so we don't skip one.
                    // Condition not really needed as IllegalArgumentException is thrown if the latest entry is removed.
                    if (i >= indexToRemove)
                    {
                        i--;
                    }
                }
            }
            catch (IllegalStateException e)
            {
                String msg = e.getMessage();
                registry.logWarn(msg);
                combinedTransformers.remove(i--);
            }
            catch (IllegalArgumentException e)
            {
                String msg = e.getMessage();
                registry.logError(msg);
                combinedTransformers.remove(i--);
            }
        }
    }

    /**
     * Discards a transformer that is
     * 1) invalid:
     *    a) has no name
     *    b) the pass through transformer name is specified in a T-Engine
     *    c) specifies transform options that don't exist,
     *    d) has the same name as another T-Engine transform (i.e. there should be no duplicate names from t-engines),
     *    e) the pass through transformer name is specified in a pipeline file
     *    f) a single step transform defined outside a t-engine without it being an override,
     *    g) a pipeline or failover transform is being overridden by a single step transform. Invalid because we
     *       don't know if a t-engine will be able to do it.
     * 2) an earlier transform with the same name (it is being overridden). If the overridden transform is from a
     *    T-Engine and the overriding transform is not a pipeline or a failover, we also copy the {@code baseUrl}
     *    from the overridden transform so that the original T-Engine will still be called.
     *
     * @param i the current transform's index into combinedTransformers.
     * @param combinedTransformers the full list of transformers in the order they were read.
     * @param registry that wil hold the transforms.
     * @param transformAndItsOrigin the current combinedTransformers element.
     * @param transformer the current transformer.
     * @param name the current transformer's name.
     * @param readFrom where the current transformer was read from.
     * @param isPipeline if the current transform is a pipeline.
     * @param isFailover if the current transform is a failover.
     *
     * @return the index of a transform to be removed. {@code -1} is returned if there should not be a remove.
     * @throws IllegalArgumentException if the current transform has a problem and should be removed.
     * @throws IllegalStateException if the current transform is dependent on config from another transform which
     *         is currently unavailable.
     */
    private int removeInvalidTransformer(int i, List<Origin<Transformer>> combinedTransformers,
                                         AbstractTransformRegistry registry,
                                         Origin<Transformer> transformAndItsOrigin, Transformer transformer,
                                         String name, String readFrom, boolean isPipeline, boolean isFailover)
    {
        int indexToRemove = -1;

        if (name == null || "".equals(name.trim()))
        {
            throw new IllegalArgumentException("Transformer names may not be null. Read from " + readFrom);
        }

        // Get the baseUrl - test code might change it
        String baseUrl = transformAndItsOrigin.getBaseUrl();
        String testBaseUrl = registry.getBaseUrlIfTesting(name, baseUrl);
        if (!nullSafeEquals(baseUrl, testBaseUrl))
        {
            baseUrl = testBaseUrl;
            transformAndItsOrigin = new Origin<>(transformer, baseUrl, readFrom);
            combinedTransformers.set(i, transformAndItsOrigin);
        }
        boolean isTEngineTransform = baseUrl != null;

        boolean isPassThroughTransform = isPassThroughTransformName(name);
        if (isPassThroughTransform && isTEngineTransform)
        {
            throw new IllegalArgumentException("T-Engines should not use " + transformerName(name) +
                    " as a transform name. Read from " + readFrom);
        }

        for (String transformOptionsLabel : transformer.getTransformOptions())
        {
            if (!combinedTransformOptions.containsKey(transformOptionsLabel))
            {
                throw new IllegalStateException("Transformer " + transformerName(name) +
                        " references \"" + transformOptionsLabel +
                        "\" which do not exist. Read from " + readFrom);
            }
        }

        boolean isOneStepTransform = !isPipeline && !isFailover && !isPassThroughTransform;

        // Check to see if the name has been used before.
        int j = lastIndexOf(name, combinedTransformers, i);
        if (j >= 0)
        {
            if (isTEngineTransform)
            {
                // We currently don't allow different t-engines to override each others single step transforms,
                // but we could if the order is defined in which they are read. Would need to check the baseUrl
                // is different.
                throw new IllegalArgumentException("Transformer " + transformerName(name) +
                        " must be a unique name. Read from " + readFrom);
            }

            if (isPassThroughTransform)
            {
                throw new IllegalArgumentException("Pipeline files should not use " + transformerName(name) +
                        " as a transform name. Read from " + readFrom);
            }

            if (isOneStepTransform)
            {
                Origin<Transformer> overriddenTransformAndItsOrigin = combinedTransformers.get(j);
                Transformer overriddenTransformer = overriddenTransformAndItsOrigin.get();
                List<TransformStep> overriddenPipeline = overriddenTransformer.getTransformerPipeline();
                List<String> overriddenFailover = overriddenTransformer.getTransformerFailover();
                boolean isOverriddenPipeline = overriddenPipeline != null && !overriddenPipeline.isEmpty();
                boolean isOverriddenFailover = overriddenFailover != null && !overriddenFailover.isEmpty();
                if (isOverriddenPipeline || isOverriddenFailover)
                {
                    throw new IllegalArgumentException("Single step transformers (such as " + transformerName(name) +
                            ") may not override a pipeline or failover transform as there is no T-Engine to perform" +
                            " work. Read from " + readFrom);
                }

                // We need to set the baseUrl of the original transform in the one overriding,
                // so we can talk to its T-Engine
                String overriddenBaseUrl = overriddenTransformAndItsOrigin.getBaseUrl();
                Transformer overriddenTransformTransform = transformAndItsOrigin.get();
                Origin<Transformer> overridingTransform =
                        new Origin<>(overriddenTransformTransform, overriddenBaseUrl, readFrom);
                combinedTransformers.set(i, overridingTransform);
            }
            indexToRemove = j;
        }
        else if (isOneStepTransform && baseUrl == null)
        {
            throw new IllegalArgumentException("Single step transformers (such as " + transformerName(name) +
                    ") must be defined in a T-Engine rather than in a pipeline file, unless they are overriding " +
                    "an existing single step definition. Read from " + readFrom);
        }

        return indexToRemove;
    }

    protected boolean isPassThroughTransformName(String name)
    {
        return false; // There is no pass through transformer in ATS but there is in the Repo.
    }

    private static int lastIndexOf(String name, List<Origin<Transformer>> combinedTransformers, int toIndex)
    {
        // Lists are short (< 100) entries and this is not a frequent or time critical step, so walking the list
        // should be okay.
        for (int j = toIndex-1; j >=0; j--)
        {
            Origin<Transformer> transformAndItsOrigin = combinedTransformers.get(j);
            Transformer transformer = transformAndItsOrigin.get();
            String transformerName = transformer.getTransformerName();
            if (name.equals(transformerName))
            {
                return j;
            }
        }
        return -1;
    }

    protected static String transformerName(String name)
    {
        return name == null ? " without a name" : "\"" + name + "\"";
    }

    // Copied from EqualsHelper
    private static boolean nullSafeEquals(Object left, Object right)
    {
        return (left == right) || (left != null && left.equals(right));
    }

    /**
     * Sort transformers so there are no forward references, if that is possible.
     * Logs warning message for those that have missing step transformers and removes them.
     * @param registry used to log messages
      */
    private void sortTransformers(AbstractTransformRegistry registry)
    {
        List<Origin<Transformer>> transformers = new ArrayList<>(combinedTransformers.size());
        List<Origin<Transformer>> todo = new ArrayList<>(combinedTransformers.size());
        Set<String> transformerNames = new HashSet<>();
        boolean added;
        do
        {
            added = false;
            for (Origin<Transformer> transformAndItsOrigin : combinedTransformers)
            {
                Transformer transformer = transformAndItsOrigin.get();
                String name = transformer.getTransformerName();
                Set<String> referencedTransformerNames = getReferencedTransformerNames(transformer);

                boolean addEntry = true;
                for (String referencedTransformerName : referencedTransformerNames)
                {
                    if (!transformerNames.contains(referencedTransformerName))
                    {
                        todo.add(transformAndItsOrigin);
                        addEntry = false;
                        break;
                    }
                }

                if (addEntry)
                {
                    transformers.add(transformAndItsOrigin);
                    added = true;
                    if (name != null)
                    {
                        transformerNames.add(name);
                    }
                }
            }
            combinedTransformers.clear();
            combinedTransformers.addAll(todo);
            todo.clear();
        }
        while (added && !combinedTransformers.isEmpty());

        transformers.addAll(todo);

        for (Origin<Transformer> transformAndItsOrigin : combinedTransformers)
        {
            Transformer transformer = transformAndItsOrigin.get();
            String name = transformer.getTransformerName();
            registry.logWarn("Transformer " + transformerName(name) +
                    " ignored as step transforms " + getUnknownReferencedTransformerNames(transformer, transformerNames) +
                    " do not exist. Read from " + transformAndItsOrigin.getReadFrom());
        }

        this.combinedTransformers = transformers;
    }

    private Set<String> getReferencedTransformerNames(Transformer transformer)
    {
        Set<String> referencedTransformerNames = new HashSet<>();
        List<TransformStep> pipeline = transformer.getTransformerPipeline();
        if (pipeline != null)
        {
            for (TransformStep step : pipeline)
            {
                String stepName = step.getTransformerName();
                referencedTransformerNames.add(stepName);
            }
        }
        List<String> failover = transformer.getTransformerFailover();
        if (failover != null)
        {
            referencedTransformerNames.addAll(failover);
        }
        return referencedTransformerNames;
    }

    private String getUnknownReferencedTransformerNames(Transformer transformer, Set<String> transformerNames)
    {
        StringJoiner sj = new StringJoiner(", ", "(", ")");
        Set<String> referencedTransformerNames = getReferencedTransformerNames(transformer);
        for (String referencedTransformerName : referencedTransformerNames)
        {
            if (!transformerNames.contains(referencedTransformerName))
            {
                sj.add(transformerName(referencedTransformerName));
            }
        }
        return sj.toString();
    }

    /**
     * Applies priority and size defaults. Must be called before {@link #addWildcardSupportedSourceAndTarget(AbstractTransformRegistry)}
     * as it uses the priority value.
     */
    private void applyDefaults()
    {
        combinedTransformers.stream()
                .map(Origin::get)
                .forEach(transformer ->
                {
                    transformer.setSupportedSourceAndTargetList(
                            transformer.getSupportedSourceAndTargetList().stream().map(supportedSourceAndTarget ->
                            {
                                Integer priority = supportedSourceAndTarget.getPriority();
                                Long maxSourceSizeBytes = supportedSourceAndTarget.getMaxSourceSizeBytes();
                                if (defaults.valuesUnset(priority, maxSourceSizeBytes))
                                {
                                    String transformerName = transformer.getTransformerName();
                                    String sourceMediaType = supportedSourceAndTarget.getSourceMediaType();
                                    supportedSourceAndTarget.setPriority(defaults.getPriority(transformerName, sourceMediaType, priority));
                                    supportedSourceAndTarget.setMaxSourceSizeBytes(defaults.getMaxSourceSizeBytes(transformerName, sourceMediaType, maxSourceSizeBytes));
                                }
                                return supportedSourceAndTarget;
                            }).collect(toSet()));
                });

        defaults.clear();
    }

    /**
     * When no supported source and target mimetypes have been defined in a failover or pipeline transformer
     * this method adds all possible values that make sense.
     * <lu>
     *     <li>Failover - all the supported values from the step transformers</li>
     *     <li>Pipeline - builds up supported source and target values. The list of source types and max sizes will come
     *                    from the initial step transformer that have a target mimetype that matches the first
     *                    intermediate mimetype. We then step through all intermediate transformers checking the next
     *                    intermediate type is supported. When we get to the last step transformer, it provides all the
     *                    target mimetypes based on the previous intermediate mimetype. Any combinations supported by
     *                    the first transformer are excluded.</li>
     * </lu>
     * @param registry used to log messages
     */
    private void addWildcardSupportedSourceAndTarget(AbstractTransformRegistry registry)
    {
        Map<String, Transformer> transformers = new HashMap<>();
        combinedTransformers.forEach(ct -> transformers.put(ct.get().getTransformerName(), ct.get()));

        combinedTransformers.forEach(transformAndItsOrigin ->
        {
            Transformer transformer = transformAndItsOrigin.get();

            // If there are no SupportedSourceAndTarget, then work out all the wildcard combinations.
            if (transformer.getSupportedSourceAndTargetList().isEmpty())
            {
                List<TransformStep> pipeline = transformer.getTransformerPipeline();
                List<String> failover = transformer.getTransformerFailover();
                boolean isPipeline = pipeline != null && !pipeline.isEmpty();
                boolean isFailover = failover != null && !failover.isEmpty();
                String errorReason = null;
                if (isFailover)
                {
                    Set<SupportedSourceAndTarget> supportedSourceAndTargets = failover.stream().flatMap(
                            name -> transformers.get(name).getSupportedSourceAndTargetList().stream()).
                            collect(toSet());

                    // The failover transform might not be picked if the priority is the same as the step transforms
                    // so reduce it here by 1. In future we might want to specify the priority in the json, but for now
                    // it should be okay.
                    supportedSourceAndTargets.forEach(s->s.setPriority(s.getPriority()-1));
                    transformer.setSupportedSourceAndTargetList(supportedSourceAndTargets);
                    errorReason = "the step transforms don't support any"; // only used if there are none
                }
                else if (isPipeline)
                {
                    String sourceMediaType = null;
                    Set<SupportedSourceAndTarget> sourceMediaTypesAndMaxSizes = null;
                    Set<String> firstTransformOptions = null;
                    String firstTransformStepName = null;
                    int numberOfSteps = pipeline.size();
                    for (int stepIndex=0; stepIndex<numberOfSteps; stepIndex++)
                    {
                        TransformStep step = pipeline.get(stepIndex);
                        String name = step.getTransformerName();
                        Transformer stepTransformer = transformers.get(name);
                        if (stepTransformer == null) // should not happen as previous checks avoid this
                        {
                            errorReason = "one of the step transformers is missing";
                            break;
                        }

                        String stepTrg = step.getTargetMediaType();
                        if (stepIndex == 0)
                        {
                            sourceMediaTypesAndMaxSizes = stepTransformer.getSupportedSourceAndTargetList().stream().
                                    filter(s -> stepTrg.equals(s.getTargetMediaType())).
                                    collect(toSet());
                            sourceMediaType = stepTrg;
                            firstTransformOptions = stepTransformer.getTransformOptions();
                            firstTransformStepName = name;
                            if (sourceMediaTypesAndMaxSizes.isEmpty())
                            {
                                errorReason = "the first step transformer " + transformerName(name) +
                                        " does not support to \"" + stepTrg + "\"";
                                break;
                            }
                        }
                        else
                        {
                            final String src = sourceMediaType;
                            if (stepIndex+1 == numberOfSteps) // if final step
                            {
                                if (stepTrg != null)
                                {
                                    errorReason = THE_FINAL_STEP_SHOULD_NOT_HAVE_A_TARGET_MIMETYPE;
                                    break;
                                }

                                // Create a cartesian product of sourceMediaType,MaxSourceSize and TargetMediaType where
                                // the source matches the last intermediate.
                                Set<SupportedSourceAndTarget>  supportedSourceAndTargets = sourceMediaTypesAndMaxSizes.stream().
                                        flatMap(s -> stepTransformer.getSupportedSourceAndTargetList().stream().
                                                filter(st ->
                                                {
                                                    String targetMimetype = st.getTargetMediaType();
                                                    return st.getSourceMediaType().equals(src) &&
                                                            !(MIMETYPE_METADATA_EXTRACT.equals(targetMimetype) ||
                                                              MIMETYPE_METADATA_EMBED.equals(targetMimetype));
                                                }).
                                                map(Types::getTargetMediaType).
                                                map(trg -> SupportedSourceAndTarget.builder().
                                                        withSourceMediaType(s.getSourceMediaType()).
                                                        withMaxSourceSizeBytes(s.getMaxSourceSizeBytes()).
                                                        withPriority(s.getPriority()).
                                                        withTargetMediaType(trg).build())).
                                        collect(toSet());

                                if (supportedSourceAndTargets.isEmpty())
                                {
                                    errorReason = "the final step transformer " + transformerName(name) +
                                            " does not support from \"" + src + "\"";
                                }
                                else
                                {
                                    // Exclude duplicates with the first transformer, if it has the same options.
                                    // There is no point doing more work.
                                    Set<String> transformOptions = transformer.getTransformOptions();
                                    if (sameOptions(transformOptions, firstTransformOptions))
                                    {
                                        supportedSourceAndTargets.removeAll(sourceMediaTypesAndMaxSizes);
                                    }
                                    if (supportedSourceAndTargets.isEmpty())
                                    {
                                        errorReason = "the first transformer " + transformerName(firstTransformStepName) +
                                                " in the pipeline already supported all source and target mimetypes" +
                                                " that would have been added as wildcards";
                                    }
                                }

                                transformer.setSupportedSourceAndTargetList(supportedSourceAndTargets);
                            }
                            else // if intermediate step
                            {
                                if (stepTrg == null)
                                {
                                    errorReason = INTERMEDIATE_STEPS_SHOULD_HAVE_A_TARGET_MIMETYPE;
                                    break;
                                }

                                // Check source to target is supported (it normally is)
                                if (stepTransformer.getSupportedSourceAndTargetList().stream().
                                        noneMatch(st -> st.getSourceMediaType().equals(src) &&
                                                        st.getTargetMediaType().equals(stepTrg)))
                                {
                                    errorReason = "the step transformer " +
                                            transformerName(stepTransformer.getTransformerName()) + " does not support \"" +
                                            src + "\" to \"" + stepTrg + "\"";
                                    break;
                                }

                                sourceMediaType = stepTrg;
                            }
                        }
                    }
                }
                if (transformer.getSupportedSourceAndTargetList().isEmpty() && (isFailover || isPipeline))
                {
                    registry.logError("No supported source and target mimetypes could be added to the" +
                            " transformer " + transformerName(transformer.getTransformerName()) + " as " + errorReason +
                            ". Read from " + transformAndItsOrigin.getReadFrom());
                }
            }
        });
    }

    private boolean sameOptions(Set<String> transformOptionNames1, Set<String> transformOptionNames2)
    {
        // They have the same names
        if (transformOptionNames1.equals(transformOptionNames2))
        {
            return true;
        }

        // Check the actual options.
        Set<TransformOption> transformOptions1 = getTransformOptions(transformOptionNames1);
        Set<TransformOption> transformOptions2 = getTransformOptions(transformOptionNames2);
        return transformOptions1.equals(transformOptions2);
    }

    private Set<TransformOption> getTransformOptions(Set<String> transformOptionNames)
    {
        Set<TransformOption> transformOptions = new HashSet<>();
        transformOptionNames.forEach(name->transformOptions.addAll(combinedTransformOptions.get(name)));
        return transformOptions;
    }

    private void setCoreVersionOnCombinedMultiStepTransformers()
    {
        setCoreVersionOnMultiStepTransformers(combinedTransformOptions, combinedTransformers.stream()
                .map(Origin::get)
                .collect(Collectors.toList()));
    }

    protected int transformerCount()
    {
        return combinedTransformers.size();
    }
}
