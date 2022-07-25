/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.base;

import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.common.TransformerDebug;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.jms.Destination;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

/**
 * Provides the transform logic common to http upload/download, message and probe requests. See
 * {@link TransformHandler#handleHttpRequest(HttpServletRequest, MultipartFile, String, String, Map)},
 * {@link TransformHandler#handleMessageRequest(TransformRequest, Long, Destination)} and
 * {@link TransformHandler#handleProbRequest(String, String, Map, File, File)}. Note the handing of transform requests
 * via a message queue is the same as via the {@link TransformController#transform(TransformRequest, Long)}.
 */
abstract class TransformProcess extends TransformStreamHandler
{
    private final TransformHandler transformHandler;
    private final TransformerDebug transformerDebug;
    protected final String sourceMimetype;
    protected final String targetMimetype;
    private final Map<String, String> transformOptions;
    protected String reference;

    TransformProcess(TransformHandler transformHandler, String sourceMimetype, String targetMimetype,
        Map<String, String> transformOptions, String reference)
    {
        LogEntry.start();
        this.transformHandler = transformHandler;
        this.sourceMimetype = sourceMimetype;
        this.targetMimetype = targetMimetype;
        this.transformOptions = transformHandler.cleanTransformOptions(transformOptions);
        this.reference = reference;
        this.transformerDebug = transformHandler.getTransformerDebug();
        transformHandler.getProbeTransform().incrementTransformerCount();
    }

    @Override
    public void handleTransformRequest()
    {
        transformManager.setSourceMimetype(sourceMimetype);
        transformManager.setTargetMimetype(targetMimetype);
        try
        {
            init();
            long sourceSizeInBytes = getSourceSize();
            String transformName = transformHandler.getTransformerName(sourceMimetype, sourceSizeInBytes, targetMimetype, transformOptions);
            CustomTransformer customTransformer = transformHandler.getCustomTransformer(transformName);
            transformerDebug.pushTransform(reference, sourceMimetype, targetMimetype, sourceSizeInBytes, transformName);
            transformerDebug.logOptions(reference, transformOptions);
            handleTransform(customTransformer);
        }
        catch (TransformException e)
        {
            transformerDebug.logFailure(reference, e.getMessage());
            LogEntry.setStatusCodeAndMessage(e.getStatus(), e.getMessage());
            handleTransformException(e, e.getStatus());
        }
        catch (Exception e)
        {
            transformerDebug.logFailure(reference, e.getMessage());
            LogEntry.setStatusCodeAndMessage(INTERNAL_SERVER_ERROR, e.getMessage());
            handleException(e);
        }
        finally
        {
            long time = LogEntry.getTransformDuration();
            transformHandler.getProbeTransform().recordTransformTime(time);
            transformerDebug.popTransform(reference, time);
            LogEntry.complete();
        }
    }

    @Override
    public void transform(CustomTransformer customTransformer) throws Exception
    {
        customTransformer.transform(sourceMimetype, inputStream, targetMimetype, outputStream, transformOptions, transformManager);
    }

    protected abstract long getSourceSize();

    @Override
    public void onSuccessfulTransform()
    {
        sendTransformResponse(transformManager);

        LogEntry.setTargetSize(transformManager.getOutputLength());
        LogEntry.setStatusCodeAndMessage(OK, "Success");
    }
    protected void sendTransformResponse(TransformManagerImpl transformManager)
    {
        // Only used in handleMessageRequest(...)
    }

    protected void handleTransformException(TransformException e, HttpStatus status)
    {
        throw e;
    }

    protected void handleException(Exception e)
    {
        throw new RuntimeException(e);
    }
}
