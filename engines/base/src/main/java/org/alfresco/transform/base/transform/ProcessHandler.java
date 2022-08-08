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
package org.alfresco.transform.base.transform;

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformController;
import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.common.TransformerDebug;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.jms.Destination;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_EXTENSION;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_EXTENSION;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

/**
 * Provides the transform logic common to http (upload/download), message and probe requests. See
 * {@link TransformHandler#handleHttpRequest(HttpServletRequest, MultipartFile, String, String, Map)},
 * {@link TransformHandler#handleMessageRequest(TransformRequest, Long, Destination)} and
 * {@link TransformHandler#handleProbRequest(String, String, Map, File, File)}. Note the handing of transform requests
 * via a message queue is the same as via the {@link TransformController#transform(TransformRequest, Long)}.
 */
abstract class ProcessHandler extends FragmentHandler
{
    private static final List<String> NON_TRANSFORM_OPTION_REQUEST_PARAMETERS = Arrays.asList(SOURCE_EXTENSION,
        TARGET_EXTENSION, TARGET_MIMETYPE, SOURCE_MIMETYPE, DIRECT_ACCESS_URL);

    protected final String sourceMimetype;
    protected final String targetMimetype;
    private final Map<String, String> transformOptions;
    protected String reference;
    private final TransformServiceRegistry transformRegistry;
    private final TransformerDebug transformerDebug;
    private final ProbeTransform probeTransform;
    private final Map<String, CustomTransformer> customTransformersByName;

    ProcessHandler(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
        String reference, TransformServiceRegistry transformRegistry, TransformerDebug transformerDebug,
        ProbeTransform probeTransform, Map<String, CustomTransformer> customTransformersByName)
    {
        this.sourceMimetype = sourceMimetype;
        this.targetMimetype = targetMimetype;
        this.transformOptions = cleanTransformOptions(transformOptions);
        this.reference = reference;

        this.transformRegistry = transformRegistry;
        this.transformerDebug = transformerDebug;
        this.probeTransform = probeTransform;
        this.customTransformersByName = customTransformersByName;
    }

    private static Map<String, String> cleanTransformOptions(Map<String, String> requestParameters)
    {
        Map<String, String> transformOptions = new HashMap<>(requestParameters);
        NON_TRANSFORM_OPTION_REQUEST_PARAMETERS.forEach(transformOptions.keySet()::remove);
        transformOptions.values().removeIf(String::isEmpty);
        return transformOptions;
    }

    @Override
    protected void init() throws IOException
    {
        transformManager.setProcessHandler(this);
        super.init();
    }

    public void handleTransformRequest()
    {
        LogEntry.start();
        transformManager.setSourceMimetype(sourceMimetype);
        transformManager.setTargetMimetype(targetMimetype);
        probeTransform.incrementTransformerCount();
        try
        {
            init();
            long sourceSizeInBytes = getSourceSize();
            String transformName = getTransformerName(sourceMimetype, sourceSizeInBytes, targetMimetype, transformOptions);
            CustomTransformer customTransformer = getCustomTransformer(transformName);
            transformerDebug.pushTransform(reference, sourceMimetype, targetMimetype, sourceSizeInBytes, transformName);
            transformerDebug.logOptions(reference, transformOptions);
            handleTransform(customTransformer);
        }
        catch (TransformException e)
        {
            transformerDebug.logFailure(reference, "  Error: "+e.getMessage());
            LogEntry.setStatusCodeAndMessage(e.getStatus(), e.getMessage());
            handleTransformException(e, e.getStatus());
        }
        catch (Exception e)
        {
            transformerDebug.logFailure(reference, "  Error: "+e.getMessage());
            LogEntry.setStatusCodeAndMessage(INTERNAL_SERVER_ERROR, e.getMessage());
            handleException(e);
        }
        finally
        {
            long time = LogEntry.getTransformDuration();
            probeTransform.recordTransformTime(time);
            transformerDebug.popTransform(reference, time);
            LogEntry.complete();
        }
    }

    protected void logFragment(Integer index, Long outputLength)
    {
        transformerDebug.logFragment(reference, index, outputLength);
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
    }

    protected void handleTransformException(TransformException e, HttpStatus status)
    {
        throw e;
    }

    protected void handleException(Exception e)
    {
        throw new TransformException(INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }

    private String getTransformerName(final String sourceMimetype, long sourceSizeInBytes, final String targetMimetype,
        final Map<String, String> transformOptions)
    {
        // The transformOptions always contains sourceEncoding when sent to a T-Engine, even though it should not be
        // used to select a transformer. Similar to source and target mimetypes and extensions, but these are not
        // passed in transformOptions.
        String sourceEncoding = transformOptions.remove(SOURCE_ENCODING);
        try
        {
            final String transformerName = transformRegistry.findTransformerName(sourceMimetype,
                sourceSizeInBytes, targetMimetype, transformOptions, null);
            if (transformerName == null)
            {
                throw new TransformException(BAD_REQUEST, "No transforms for: "+
                    sourceMimetype+" -> "+targetMimetype+transformOptions.entrySet().stream()
                    .map(entry -> entry.getKey()+"="+entry.getValue())
                    .collect(Collectors.joining(", ", " ", "")));
            }
            return transformerName;
        }
        finally
        {
            if (sourceEncoding != null)
            {
                transformOptions.put(SOURCE_ENCODING, sourceEncoding);
            }
        }
    }

    private CustomTransformer getCustomTransformer(String transformName)
    {
        CustomTransformer customTransformer = customTransformersByName.get(transformName);
        if (customTransformer == null)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, "Custom Transformer "+transformName+" not found");
        }
        return customTransformer;
    }
}
