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
package org.alfresco.transform.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.messages.TransformStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_WORD;
import static org.alfresco.transform.common.RepositoryClientData.CLIENT_DATA_SEPARATOR;
import static org.alfresco.transform.common.RepositoryClientData.DEBUG;
import static org.alfresco.transform.common.RepositoryClientData.DEBUG_SEPARATOR;
import static org.alfresco.transform.common.RepositoryClientData.REPO_ID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests TransformerDebug. AbstractRouterTest in the t-router contains more complete end to end tests. The tests in this
 * class are a smoke test at the moment.
 */
class TransformerDebugTest
{
    private static String MIMETYPE_PDF = "application/pdf";

    TransformerDebug transformerDebug = new TransformerDebug();

    private StringJoiner transformerDebugOutput = new StringJoiner("\n");

    public String getTransformerDebugOutput()
    {
        return transformerDebugOutput.toString()
                .replaceAll(" [\\d,]+ ms", " -- ms");
    }

    private void twoStepTransform(boolean isTEngine, boolean fail, Level logLevel, String renditionName,
        long sourceSize)
    {
        transformerDebug.setIsTEngine(isTEngine);
        monitorLogs(logLevel);

        TransformRequest request = TransformRequest.builder()
                .withSourceSize(sourceSize)
                .withInternalContext(InternalContext.initialise(null))
                .withClientData(clientDataWithDebugRequest(renditionName))
                .build();
        TransformStack.setInitialSourceReference(request.getInternalContext(), "fileRef");

        TransformReply reply = TransformReply.builder()
                .withInternalContext(request.getInternalContext())
                .build();

        TransformStack.addTransformLevel(request.getInternalContext(),
                TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                        .withStep("wrapper", MIMETYPE_TEXT_PLAIN, MIMETYPE_PDF));
        transformerDebug.pushTransform(request);

        TransformStack.addTransformLevel(request.getInternalContext(),
                TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                        .withStep("transformer1", MIMETYPE_TEXT_PLAIN, MIMETYPE_WORD)
                        .withStep("transformer2", MIMETYPE_WORD, MIMETYPE_PDF));
        transformerDebug.pushTransform(request);
        transformerDebug.logOptions(request);
        TransformStack.removeSuccessfulStep(reply, transformerDebug);

        request.setTransformRequestOptions(ImmutableMap.of("k1", "v1", "k2","v2"));
        transformerDebug.pushTransform(request);
        transformerDebug.logOptions(request);
        if (fail)
        {
            reply.setErrorDetails("Dummy error");
            transformerDebug.logFailure(reply);
        }
        else
        {
            TransformStack.removeSuccessfulStep(reply, transformerDebug);
            TransformStack.removeTransformLevel(reply.getInternalContext());
        }
    }

    private String clientDataWithDebugRequest(String renditionName)
    {
        return new StringJoiner(CLIENT_DATA_SEPARATOR)
            .add(REPO_ID + "ACS1234")
            .add("1")
            .add(renditionName)
            .add("3")
            .add("4")
            .add("5")
            .add("54321")
            .add("7")
            .add("8")
            .add(DEBUG)
            .toString();
    }

    private void monitorLogs(Level logLevel)
    {
        Logger logger = (Logger)LoggerFactory.getLogger(TransformerDebug.class);
        AppenderBase<ILoggingEvent> logAppender = new AppenderBase<>()
        {
            @Override
            protected void append(ILoggingEvent iLoggingEvent)
            {
                transformerDebugOutput.add(iLoggingEvent.getMessage());
            }
        };
        logAppender.setContext((LoggerContext)LoggerFactory.getILoggerFactory());
        logger.setLevel(logLevel);
        logger.addAppender(logAppender);
        logAppender.start();
    }

    @Test
    void testRouterTwoStepTransform()
    {
        twoStepTransform(false, false, Level.DEBUG, "", 1234L);

        Assertions.assertEquals("" +
                        "1                 txt  pdf   1.2 KB wrapper\n" +
                        "1.1               txt  doc   transformer1\n" +
                        "1.2               doc  pdf   transformer2\n" +
                        "1.2                 k1=\"v1\"\n" +
                        "1.2                 k2=\"v2\"\n" +
                        "1                 Finished in -- ms",
                getTransformerDebugOutput());
    }

    @Test
    void testRouterTwoStepTransformWithTrace()
    {
        twoStepTransform(false, false, Level.TRACE, "", 1234L);

        // With trace there are "Finished" lines for nested transforms, like a T-Engine's debug but still without
        // the size and rendition name
        Assertions.assertEquals("" +
                        "1                 txt  pdf   1.2 KB wrapper\n" +
                        "1.1               txt  doc   transformer1\n" +
                        "1.1               Finished in -- ms\n" +
                        "1.2               doc  pdf   transformer2\n" +
                        "1.2                 k1=\"v1\"\n" +
                        "1.2                 k2=\"v2\"\n" +
                        "1.2               Finished in -- ms\n" +
                        "1                 Finished in -- ms",
                getTransformerDebugOutput());
    }

    @Test
    void testEngineTwoStepTransform()
    {
        twoStepTransform(true, false, Level.DEBUG, "", 1234L);

        // Note the first and last lines would only ever be logged on the router, but the expected data includes
        // the extra "Finished" lines, sizes and renditions (if set in client data).
        Assertions.assertEquals("" +
                        "1                 txt  pdf   1.2 KB wrapper\n" +
                        "1.1               txt  doc   1.2 KB transformer1\n" +
                        "1.1               Finished in -- ms\n" +
                        "1.2               doc  pdf   1.2 KB transformer2\n" +
                        "1.2                 k1=\"v1\"\n" +
                        "1.2                 k2=\"v2\"\n" +
                        "1.2               Finished in -- ms\n" +
                        "1                 Finished in -- ms",
                getTransformerDebugOutput());
    }

    @Test
    void testRouterTwoStepTransformWithFailure()
    {
        twoStepTransform(false, true, Level.DEBUG, "", 1234L);

        Assertions.assertEquals("" +
                                    "1                 txt  pdf   1.2 KB wrapper\n" +
                                    "1.1               txt  doc   transformer1\n" +
                                    "1.2               doc  pdf   transformer2\n" +
                                    "1.2                 k1=\"v1\"\n" +
                                    "1.2                 k2=\"v2\"\n" +
                                    "1.2               Dummy error",
            getTransformerDebugOutput());
    }

    @Test
    void testRenditionName()
    {
        twoStepTransform(false, false, Level.DEBUG, "renditionName", 1234L);

        Assertions.assertEquals("" +
                                    "1                 txt  pdf   1.2 KB -- renditionName -- wrapper\n" +
                                    "1.1               txt  doc   transformer1\n" +
                                    "1.2               doc  pdf   transformer2\n" +
                                    "1.2                 k1=\"v1\"\n" +
                                    "1.2                 k2=\"v2\"\n" +
                                    "1                 Finished in -- ms",
            getTransformerDebugOutput());
    }

    @Test
    void testMetadataExtract()
    {
        twoStepTransform(false, false, Level.DEBUG, "transform:alfresco-metadata-extract", 1234L);

        Assertions.assertEquals("" +
                                    "1                 txt  pdf   1.2 KB -- metadataExtract -- wrapper\n" +
                                    "1.1               txt  doc   transformer1\n" +
                                    "1.2               doc  pdf   transformer2\n" +
                                    "1.2                 k1=\"v1\"\n" +
                                    "1.2                 k2=\"v2\"\n" +
                                    "1                 Finished in -- ms",
            getTransformerDebugOutput());
    }

    @Test
    void testMetadataEmbed()
    {
        twoStepTransform(false, false, Level.DEBUG, "transform:alfresco-metadata-embed", 1234L);

        Assertions.assertEquals("" +
                                    "1                 txt  pdf   1.2 KB -- metadataEmbed -- wrapper\n" +
                                    "1.1               txt  doc   transformer1\n" +
                                    "1.2               doc  pdf   transformer2\n" +
                                    "1.2                 k1=\"v1\"\n" +
                                    "1.2                 k2=\"v2\"\n" +
                                    "1                 Finished in -- ms",
            getTransformerDebugOutput());
    }

    @Test
    void testSourceSize1Byte()
    {
        twoStepTransform(false, false, Level.DEBUG, "", 1);

        Assertions.assertEquals("" +
                                    "1                 txt  pdf   1 byte wrapper\n" +
                                    "1.1               txt  doc   transformer1\n" +
                                    "1.2               doc  pdf   transformer2\n" +
                                    "1.2                 k1=\"v1\"\n" +
                                    "1.2                 k2=\"v2\"\n" +
                                    "1                 Finished in -- ms",
            getTransformerDebugOutput());
    }

    @Test
    void testSourceSize23TB()
    {
        twoStepTransform(false, false, Level.DEBUG, "", 23L*1024*1024*1024*1024);

        Assertions.assertEquals("" +
                                    "1                 txt  pdf   23 TB wrapper\n" +
                                    "1.1               txt  doc   transformer1\n" +
                                    "1.2               doc  pdf   transformer2\n" +
                                    "1.2                 k1=\"v1\"\n" +
                                    "1.2                 k2=\"v2\"\n" +
                                    "1                 Finished in -- ms",
            getTransformerDebugOutput());
    }

    @Test
    void testLogFailure()
    {
        monitorLogs(Level.TRACE);

        String origClientData = clientDataWithDebugRequest("");
        TransformReply reply = TransformReply.builder()
                .withInternalContext(InternalContext.initialise(null))
                .withErrorDetails("T-Request was null - a major error")
                .withClientData(origClientData)
                .build();

        transformerDebug.logFailure(reply);

        String expectedDebug = "                  T-Request was null - a major error";
        Assertions.assertEquals(expectedDebug, getTransformerDebugOutput());
        assertEquals(origClientData+DEBUG_SEPARATOR+expectedDebug, reply.getClientData());
    }

    @Test
    void tesGetOptionAndValue()
    {
        String sixtyChars    = "12345678 10 345678 20 345678 30 345678 40 345678 50 abcdefgh";
        String sixtyOneChars = "12345678 10 345678 20 345678 30 345678 40 345678 50 abcd12345";
        String expected      = "12345678 10 345678 20 345678 30 345678 40 345678 50 ...12345";

        assertEquals("ref                 key=\"value\"",
                transformerDebug.getOptionAndValue("ref", "key", "value"));
        assertEquals("ref                 key=\""+sixtyChars+"\"",
                transformerDebug.getOptionAndValue("ref", "key", sixtyChars));
        assertEquals("ref                 key=\""+expected+"\"",
                transformerDebug.getOptionAndValue("ref", "key", sixtyOneChars));
    }
}