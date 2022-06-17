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

import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.MultiStep;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.common.TransformerDebug;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.alfresco.transform.messages.TransformStack.OPTIONS_LEVEL;
import static org.alfresco.transform.messages.TransformStack.SEPARATOR;
import static org.alfresco.transform.messages.TransformStack.TOP_STACK_LEVEL;
import static org.alfresco.transform.messages.TransformStack.getInitialSourceReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

class TransformStackTest
{
    private static long START = System.currentTimeMillis();
    private static String STEP = SEPARATOR + "name" + SEPARATOR + "source" + SEPARATOR + "target";

    @Mock
    private TransformerDebug transformerDebug;

    @Mock
    private TransformReply reply;

    // Note: If you change this also change AbstractRouterTest.testNestedTransform as they match
    public static final ImmutableMap<String, TransformStack.LevelBuilder> TEST_LEVELS = ImmutableMap.of(
            "top", TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                    .withStep("pipeline 1-N", "type1", "typeN"),
            "pipeline 1-N", TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                    .withStep("transform1-2", "type1", "type2")
                    .withStep("pipeline 2-3", "type2", "type3")
                    .withStep("failover 3-N", "type3", "typeN"),
            "pipeline 2-3", TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                    .withStep("transform2-4", "type2", "type4")
                    .withStep("transform4-3", "type4", "type3"),
            "failover 3-N", TransformStack.levelBuilder(TransformStack.FAILOVER_FLAG)
                    .withStep("transform3-Na", "type3", "typeN")
                    .withStep("transform3-Nb", "type3", "typeN")
                    .withStep("pipeline 3-Nc", "type3", "typeN"),
            "pipeline 3-Nc", TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                    .withStep("transform3-5", "type3", "type5")
                    .withStep("pipeline 5-N", "type5", "typeN"),
            "pipeline 5-N", TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                    .withStep("transform5-6", "type5", "type6")
                    .withStep("transform6-N", "type6", "typeN"));
    private final InternalContext internalContext = new InternalContext();
    private final Map<String, String> options = ImmutableMap.of("key1", "value1", "key2", "", "key3", "value3");
    private final String sourceReference = UUID.randomUUID().toString();

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);

        // Repeat what is done by Router.initialiseContext
        internalContext.setMultiStep(new MultiStep());
        internalContext.getMultiStep().setTransformsToBeDone(new ArrayList<>());
        TransformStack.setInitialTransformRequestOptions(internalContext, options);
        TransformStack.setInitialSourceReference(internalContext, sourceReference);
        doReturn(internalContext).when(reply).getInternalContext();
    }

    @Test
    public void testOptions()
    {
        assertEquals(options, TransformStack.getInitialTransformRequestOptions(internalContext));
    }

    @Test
    public void testOptionsEmpty()
    {
        ImmutableMap<String, String> options = ImmutableMap.of();

        TransformStack.setInitialTransformRequestOptions(internalContext, options);
        assertEquals(options, TransformStack.getInitialTransformRequestOptions(internalContext));
    }

    @Test
    public void testOptionsEmptyLastValue()
    {
        ImmutableMap<String, String> options = ImmutableMap.of("key1", "value1", "key2", "");

        TransformStack.setInitialTransformRequestOptions(internalContext, options);
        assertEquals(options, TransformStack.getInitialTransformRequestOptions(internalContext));
    }

    @Test
    public void testSourceReference()
    {
        assertEquals(sourceReference, getInitialSourceReference(internalContext));
    }

    @Test
    public void testSourceReferenceNull()
    {
        TransformStack.setInitialSourceReference(internalContext, null);
        assertEquals(null, getInitialSourceReference(internalContext));
    }

    @Test
    public void testSourceReferenceBlank()
    {
        TransformStack.setInitialSourceReference(internalContext, "");
        assertEquals("", getInitialSourceReference(internalContext));
    }

    @Test
    public void testLevelBuilder()
    {
        assertEquals("P⏐1⏐0⏐0⏐pipeline 1-N⏐type1⏐typeN", TEST_LEVELS.get("top").build());
        assertEquals("P⏐1⏐0⏐0⏐failover 3-N⏐type3⏐typeN⏐pipeline 2-3⏐type2⏐type3⏐transform1-2⏐type1⏐type2", TEST_LEVELS.get("pipeline 1-N").build());
        assertEquals("F⏐1⏐0⏐0⏐pipeline 3-Nc⏐type3⏐typeN⏐transform3-Nb⏐type3⏐typeN⏐transform3-Na⏐type3⏐typeN", TEST_LEVELS.get("failover 3-N").build());
    }

    @Test
    public void testAttemptedRetries()
    {
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));
        assertEquals(0, TransformStack.getAttemptedRetries(internalContext));

        TransformStack.incrementAttemptedRetries(internalContext);
        assertEquals(1, TransformStack.getAttemptedRetries(internalContext));

        TransformStack.incrementAttemptedRetries(internalContext);
        assertEquals(2, TransformStack.getAttemptedRetries(internalContext));

        TransformStack.resetAttemptedRetries(internalContext);
        assertEquals(0, TransformStack.getAttemptedRetries(internalContext));
    }

    @Test
    public void testReference()
    {
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));
        assertEquals("1", TransformStack.getReference(internalContext));

        TransformStack.setReference(internalContext, 123);
        assertEquals("123", TransformStack.getReference(internalContext));

        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("pipeline 1-N"));
        assertEquals("123.1", TransformStack.getReference(internalContext));

        TransformStack.incrementReference(internalContext);
        assertEquals("123.2", TransformStack.getReference(internalContext));

        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("pipeline 2-3"));
        assertEquals("123.2.1", TransformStack.getReference(internalContext));

        TransformStack.removeTransformLevel(internalContext);
        TransformStack.incrementReference(internalContext);
        assertEquals("123.3", TransformStack.getReference(internalContext));

        TransformStack.removeTransformLevel(internalContext);
        assertEquals("123", TransformStack.getReference(internalContext));
    }

    @Test
    public void testSetReferenceInADummyTopLevelIfUnset()
    {
        // Used when creating a TransformRouterException prior to setting the reference

        // Undo setup()
        internalContext.getMultiStep().setTransformsToBeDone(new ArrayList<>());

        TransformStack.setReferenceInADummyTopLevelIfUnset(internalContext, 23);
        assertEquals("23", TransformStack.getReference(internalContext));
    }

    @Test
    public void testReplicateWorkflowWithSuccess()
    {
        replicateWorkflowStepsPriorToFailureOrSuccess();

        // Assume a successful transform, so successful there should be no more steps or levels after this
        TransformStack.removeSuccessfulStep(reply, transformerDebug);
        assertTrue(TransformStack.isFinished(internalContext));
    }

    @Test
    // Tests the failure on a transform indirectly (it is a step in a pipeline) under a failover transformer.
    public void testReplicateWorkflowWithFailure()
    {
        replicateWorkflowStepsPriorToFailureOrSuccess();

        // Assume a transform failure. While loop should remove the 2 indirect pipeline levels
        int removedLevels = 0;
        while (!TransformStack.isParentAFailover(internalContext))
        {
            removedLevels++;
            TransformStack.removeTransformLevel(internalContext);
        }
        Assertions.assertEquals(2, removedLevels);
        assertTrue(TransformStack.isLastStepInTransformLevel(internalContext));
        TransformStack.removeFailedStep(reply, transformerDebug); // Should remove the rest as failure was last step in failover
        assertTrue(TransformStack.isFinished(internalContext));
    }

    // Replicates the sequence of TransformStack method calls for a workflow. Steps through each transform, with
    // some failures in a failover transformer, before returning to allow the calling test method to either fail the
    // next step or not.
    private void replicateWorkflowStepsPriorToFailureOrSuccess()
    {
        // Initial transform request, so add a level
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));
        assertFalse(TransformStack.isFinished(internalContext));
        TransformStack.Step step = TransformStack.currentStep(internalContext);

        assertEquals("pipeline 1-N", step.getTransformerName());
        assertEquals("type1", step.getSourceMediaType());
        assertEquals("typeN", step.getTargetMediaType());
        assertEquals(null, TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Because it is a pipeline, add a level
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("pipeline 1-N"));
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform1-2", step.getTransformerName());
        assertEquals("type1", step.getSourceMediaType());
        assertEquals("type2", step.getTargetMediaType());
        assertEquals("pipeline 1-N", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Assume a successful transform, so move on to next step add a level
        TransformStack.removeSuccessfulStep(reply, transformerDebug);
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("pipeline 2-3", step.getTransformerName());
        assertEquals("type2", step.getSourceMediaType());
        assertEquals("type3", step.getTargetMediaType());
        assertEquals("pipeline 1-N", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Because it is a pipeline, add a level
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("pipeline 2-3"));
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform2-4", step.getTransformerName());
        assertEquals("type2", step.getSourceMediaType());
        assertEquals("type4", step.getTargetMediaType());
        assertEquals("pipeline 2-3", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Assume a successful transform, so move on to next step add a level
        TransformStack.removeSuccessfulStep(reply, transformerDebug);
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform4-3", step.getTransformerName());
        assertEquals("type4", step.getSourceMediaType());
        assertEquals("type3", step.getTargetMediaType());
        assertEquals("pipeline 2-3", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Assume a successful transform, so move on to next step add a level
        TransformStack.removeSuccessfulStep(reply, transformerDebug);
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("failover 3-N", step.getTransformerName());
        assertEquals("type3", step.getSourceMediaType());
        assertEquals("typeN", step.getTargetMediaType());
        assertEquals("pipeline 1-N", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Because it is a failover, add a level
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("failover 3-N"));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform3-Na", step.getTransformerName());
        assertEquals("type3", step.getSourceMediaType());
        assertEquals("typeN", step.getTargetMediaType());
        assertEquals("failover 3-N", TransformStack.getParentName(internalContext));
        assertTrue(TransformStack.isParentAFailover(internalContext));

        // Assume 1st failover step fails
        while (!TransformStack.isParentAFailover(internalContext))
        {
            TransformStack.removeTransformLevel(internalContext);
        }
        assertFalse(TransformStack.isLastStepInTransformLevel(internalContext));
        TransformStack.removeFailedStep(reply, transformerDebug);
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform3-Nb", step.getTransformerName());
        assertEquals("type3", step.getSourceMediaType());
        assertEquals("typeN", step.getTargetMediaType());
        assertEquals("failover 3-N", TransformStack.getParentName(internalContext));
        assertTrue(TransformStack.isParentAFailover(internalContext));

        // Assume 2nd failover step fails
        while (!TransformStack.isParentAFailover(internalContext))
        {
            TransformStack.removeTransformLevel(internalContext);
        }
        assertFalse(TransformStack.isLastStepInTransformLevel(internalContext));
        TransformStack.removeFailedStep(reply, transformerDebug);
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("pipeline 3-Nc", step.getTransformerName());
        assertEquals("type3", step.getSourceMediaType());
        assertEquals("typeN", step.getTargetMediaType());
        assertEquals("failover 3-N", TransformStack.getParentName(internalContext));
        assertTrue(TransformStack.isParentAFailover(internalContext));

        // Because it is a pipeline, add a level
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("pipeline 3-Nc"));
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform3-5", step.getTransformerName());
        assertEquals("type3", step.getSourceMediaType());
        assertEquals("type5", step.getTargetMediaType());
        assertEquals("pipeline 3-Nc", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Assume a successful transform, so move on to next step add a level
        TransformStack.removeSuccessfulStep(reply, transformerDebug);
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("pipeline 5-N", step.getTransformerName());
        assertEquals("type5", step.getSourceMediaType());
        assertEquals("typeN", step.getTargetMediaType());
        assertEquals("pipeline 3-Nc", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Because it is a pipeline, add a level
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("pipeline 5-N"));
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform5-6", step.getTransformerName());
        assertEquals("type5", step.getSourceMediaType());
        assertEquals("type6", step.getTargetMediaType());
        assertEquals("pipeline 5-N", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));

        // Assume a successful transform, so move on to next step add a level
        TransformStack.removeSuccessfulStep(reply, transformerDebug);
        assertFalse(TransformStack.isFinished(internalContext));
        step = TransformStack.currentStep(internalContext);

        assertEquals("transform6-N", step.getTransformerName());
        assertEquals("type6", step.getSourceMediaType());
        assertEquals("typeN", step.getTargetMediaType());
        assertEquals("pipeline 5-N", TransformStack.getParentName(internalContext));
        assertFalse(TransformStack.isParentAFailover(internalContext));
    }

    @Test
    // Tests a workflow where all transforms are successful, using a loop.
    public void testWorkflowWithLoop()
    {
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));
        int transformStepCount = 0;
        do
        {
            transformStepCount++;
            TransformStack.Step step = TransformStack.currentStep(internalContext);
            String transformerName = step.getTransformerName();
            int depth = internalContext.getMultiStep().getTransformsToBeDone().size() - 2;
            System.out.println(transformStepCount + "           ".substring(0, depth*2+1) + transformerName);
            TransformStack.LevelBuilder nextLevel = TEST_LEVELS.get(transformerName);
            if (nextLevel == null)
            {
                TransformStack.removeSuccessfulStep(reply, transformerDebug);
            }
            else
            {
                TransformStack.addTransformLevel(internalContext, nextLevel);
            }
            if (transformStepCount >= 25)
            {
                Assertions.fail("Appear to be in an infinite loop");
            }
        } while (!TransformStack.isFinished(internalContext));
        Assertions.assertEquals(7, transformStepCount);
    }

    @Test
    public void testCheckStructureNoOptions()
    {
        internalContext.getMultiStep().setTransformsToBeDone(new ArrayList<>());

        assertEquals("T-Reply InternalContext did not have the Stack set",
                TransformStack.checkStructure(internalContext, "T-Reply"));
    }

    @Test
    public void testCheckStructureNoSourceRef()
    {
        internalContext.getMultiStep().setTransformsToBeDone(new ArrayList<>());
        TransformStack.setInitialTransformRequestOptions(internalContext, options);

        assertEquals("T-Request InternalContext did not have the Stack set",
                TransformStack.checkStructure(internalContext, "T-Request"));
    }

    @Test
    public void testCheckStructureNoStack()
    {
        internalContext.getMultiStep().setTransformsToBeDone(new ArrayList<>());
        TransformStack.setInitialTransformRequestOptions(internalContext, options);
        TransformStack.setInitialSourceReference(internalContext, sourceReference);

        assertEquals("T-something InternalContext did not have the Stack set",
                TransformStack.checkStructure(internalContext, "T-something"));
    }

    @Test
    public void testCheckStructureOptionsOk()
    {
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));

        for (String value : Arrays.asList(
                "",
                "key1" + SEPARATOR + "value1",
                "key1" + SEPARATOR + "value1" + SEPARATOR + "key2" + SEPARATOR + "value2"))
        {
            System.out.println("TransformOptions   value: " + value);
            internalContext.getMultiStep().getTransformsToBeDone().set(OPTIONS_LEVEL, value);
            Assertions.assertNull(TransformStack.checkStructure(internalContext, "T-Reply"));
            // call the getter just in case we have missed something
            TransformStack.getInitialTransformRequestOptions(internalContext);
        }
    }

    @Test
    public void testCheckStructureOptionsBad()
    {
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));

        for (String value : Arrays.asList(
                null,
                "noValue",
                SEPARATOR + "noKey",
                "key" + SEPARATOR + "value" + SEPARATOR + "noKey"))
        {
            System.out.println("TransformOptions   value: " + value);
            internalContext.getMultiStep().getTransformsToBeDone().set(OPTIONS_LEVEL, value);
            assertEquals("T-Reply InternalContext did not have the TransformOptions set correctly",
                    TransformStack.checkStructure(internalContext, "T-Reply"));
        }
    }

    @Test
    public void testCheckStructureStackLevelsOk()
    {
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));

        for (String value : Arrays.asList(
                "P" + SEPARATOR + "20" + SEPARATOR + START + SEPARATOR +  "1" + STEP,
                "P" + SEPARATOR +  "4" + SEPARATOR + "123" + SEPARATOR + "12" + STEP + STEP))
        {
            System.out.println("TransformLevel   value: " + value);
            internalContext.getMultiStep().getTransformsToBeDone().set(TOP_STACK_LEVEL, value);
            Assertions.assertNull(TransformStack.checkStructure(internalContext, "T-Reply"));
            // call a getter just in case we have missed something
            TransformStack.currentStep(internalContext);
        };
    }

    @Test
    public void testCheckStructureStackLevelsBad()
    {
        TransformStack.addTransformLevel(internalContext, TEST_LEVELS.get("top"));

        String MAX_INT_PLUS_1 = BigInteger.valueOf(Integer.MAX_VALUE + 1).toString();
        String MAX_LONG_PLUS_1 = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString();

        for (String value : Arrays.asList(
                null,
                "",
                "F" + SEPARATOR + "12" + SEPARATOR + START + SEPARATOR +  "2",
                "F" + SEPARATOR + "-1" + SEPARATOR + START + SEPARATOR +  "2" + STEP,
                "F" + SEPARATOR +  "1" + SEPARATOR + "-3"  + SEPARATOR +  "2" + STEP,
                "F" + SEPARATOR +  "1" + SEPARATOR + START + SEPARATOR + "-2" + STEP,
                "F" + SEPARATOR +  "a" + SEPARATOR + START + SEPARATOR + "-2" + STEP,
                "F" + SEPARATOR +  "1" + SEPARATOR + START + SEPARATOR +  "b" + STEP,
                "P" + SEPARATOR +  "0" + SEPARATOR + START + SEPARATOR + "12" + SEPARATOR + "name",
                "P" + SEPARATOR +  "0" + SEPARATOR + START + SEPARATOR + "12" + SEPARATOR + "name" + SEPARATOR + "source",
                "P" + SEPARATOR +  "0" + SEPARATOR + START + SEPARATOR + "12" + SEPARATOR + "name" + SEPARATOR + "source" + SEPARATOR +       "",
                "P" + SEPARATOR +  "0" + SEPARATOR + START + SEPARATOR + "12" + SEPARATOR + "name" + SEPARATOR +       "" + SEPARATOR + "target",
                "F" + SEPARATOR + MAX_INT_PLUS_1 + SEPARATOR +            START + SEPARATOR +            "2" + STEP,
                "F" + SEPARATOR +            "1" + SEPARATOR + MAX_LONG_PLUS_1  + SEPARATOR +            "2" + STEP,
                "F" + SEPARATOR +            "1" + SEPARATOR +            START + SEPARATOR + MAX_INT_PLUS_1 + STEP
                ))
        {
            System.out.println("TransformLevel   value: " + value);
            internalContext.getMultiStep().getTransformsToBeDone().set(TOP_STACK_LEVEL, value);
            assertEquals("T-Reply InternalContext did not have levels set correctly",
                    TransformStack.checkStructure(internalContext, "T-Reply"));
        };
    }
}