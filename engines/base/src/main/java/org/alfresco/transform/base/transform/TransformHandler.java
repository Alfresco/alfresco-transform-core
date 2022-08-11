/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.clients.AlfrescoSharedFileStoreClient;
import org.alfresco.transform.base.messaging.TransformReplySender;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.common.ExtensionService;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.common.TransformerDebug;
import org.alfresco.transform.messages.TransformRequestValidator;
import org.alfresco.transform.messages.TransformStack;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.jms.Destination;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.joining;
import static org.alfresco.transform.base.fs.FileManager.createAttachment;
import static org.alfresco.transform.base.fs.FileManager.createTargetFile;
import static org.alfresco.transform.base.fs.FileManager.getDirectAccessUrlInputStream;
import static org.alfresco.transform.base.fs.FileManager.getMultipartFileInputStream;
import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Handles the transform requests from either http or a message.
 */
@Component
public class TransformHandler
{
    private static final Logger logger = LoggerFactory.getLogger(TransformHandler.class);

    @Autowired(required = false)
    private List<TransformEngine> transformEngines;
    @Autowired(required = false)
    private List<CustomTransformer> customTransformers;
    @Autowired
    private AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient;
    @Autowired
    private TransformRequestValidator transformRequestValidator;
    @Autowired
    private TransformServiceRegistry transformRegistry;
    @Autowired
    private TransformReplySender transformReplySender;
    @Autowired
    private TransformerDebug transformerDebug;

    private final AtomicInteger httpRequestCount = new AtomicInteger(1);
    private final Map<String, CustomTransformer> customTransformersByName = new HashMap<>();

    @PostConstruct
    private void initCustomTransformersByName()
    {
        if (customTransformers != null)
        {
            customTransformers.forEach(customTransformer ->
                    customTransformersByName.put(customTransformer.getTransformerName(), customTransformer));

            logger.info("Transformers:");
            customTransformers.stream()
                              .sorted(Comparator.comparing(CustomTransformer::getTransformerName))
                              .map(customTransformer -> "  "+customTransformer.getTransformerName()).forEach(logger::info);
        }
    }

    public ResponseEntity<Resource> handleHttpRequest(HttpServletRequest request,
            MultipartFile sourceMultipartFile, String sourceMimetype, String targetMimetype,
            Map<String, String> requestParameters, ProbeTransform probeTransform)
    {
        AtomicReference<ResponseEntity<Resource>> responseEntity = new AtomicReference<>();

        new ProcessHandler(sourceMimetype, targetMimetype, requestParameters,
            "e" + httpRequestCount.getAndIncrement(), transformRegistry,
            transformerDebug, probeTransform, customTransformersByName)
        {
            @Override
            protected void init() throws IOException
            {
                transformManager.setRequest(request);
                transformManager.setTargetFile(createTargetFile(request, sourceMimetype, targetMimetype));
                transformManager.keepTargetFile(); // Will be deleted in TransformInterceptor.afterCompletion()
                super.init();
            }

            @Override
            protected InputStream getInputStream()
            {
                return getInputStreamForHandleHttpRequest(requestParameters, sourceMultipartFile);
            }

            @Override
            protected OutputStream getOutputStream() throws IOException
            {
                return getOutputStreamFromFile(transformManager.getTargetFile());
            }

            @Override
            protected long getSourceSize()
            {
                return -1L; // Ignore for http requests as the Alfresco repo will have checked.
            }

            protected void sendTransformResponse(TransformManagerImpl transformManager)
            {
                String extension = ExtensionService.getExtensionForTargetMimetype(targetMimetype, sourceMimetype);
                responseEntity.set(createAttachment("transform."+extension, transformManager.getTargetFile()));
            }
        }.handleTransformRequest();

        return responseEntity.get();
    }

    public void handleProbRequest(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
        File sourceFile, File targetFile, ProbeTransform probeTransform)
    {
        new ProcessHandler(sourceMimetype, targetMimetype, transformOptions,
            "p" + httpRequestCount.getAndIncrement(), transformRegistry,
            transformerDebug, probeTransform, customTransformersByName)
        {
            @Override
            protected void init() throws IOException
            {
                transformManager.setSourceFile(sourceFile);
                transformManager.setTargetFile(targetFile);
                transformManager.keepTargetFile();
                super.init();
            }

            @Override
            protected InputStream getInputStream()
            {
                return getInputStreamForHandleProbRequest(sourceFile);
            }

            @Override
            protected long getSourceSize()
            {
                return sourceFile.length();
            }

            @Override
            protected OutputStream getOutputStream() throws IOException
            {
                return getOutputStreamFromFile(targetFile);
            }
        }.handleTransformRequest();
    }

    public TransformReply handleMessageRequest(TransformRequest request, Long timeout, Destination replyToQueue,
        ProbeTransform probeTransform)
    {
        TransformReply reply = createBasicTransformReply(request);
        new ProcessHandler(request.getSourceMediaType(), request.getTargetMediaType(),
            request.getTransformRequestOptions(),"unset", transformRegistry,
            transformerDebug, probeTransform, customTransformersByName)
        {
            @Override
            protected void init() throws IOException
            {
                checkTransformRequestValid(request, reply);
                reference = TransformStack.getReference(reply.getInternalContext());
                initTarget();
                super.init();
            }

            @Override
            protected void initTarget()
            {
                transformManager.setTargetFile(createTargetFile(null, sourceMimetype, targetMimetype));
            }

            @Override
            protected InputStream getInputStream()
            {
                return getInputStreamForHandleMessageRequest(request);
            }

            @Override
            protected long getSourceSize()
            {
                return request.getSourceSize();
            }

            @Override
            protected OutputStream getOutputStream() throws IOException
            {
                return getOutputStreamFromFile(transformManager.getTargetFile());
            }

            protected void sendTransformResponse(TransformManagerImpl transformManager)
            {
                reply.getInternalContext().setCurrentSourceSize(transformManager.getOutputLength());
                saveTargetFileInSharedFileStore(transformManager.getTargetFile(), reply);
                sendSuccessfulResponse(timeout, reply, replyToQueue);
            }

            @Override
            protected void handleTransformException(TransformException e, HttpStatus status)
            {
                sendFailedResponse(reply, e, status, replyToQueue);
            }

            @Override
            protected void handleException(Exception e)
            {
                sendFailedResponse(reply, e, INTERNAL_SERVER_ERROR, replyToQueue);
            }
        }.handleTransformRequest();
        return reply;
    }

    private void sendSuccessfulResponse(Long timeout, TransformReply reply, Destination replyToQueue)
    {
        logger.trace("Sending successful {}, timeout {} ms", reply, timeout);
        transformReplySender.send(replyToQueue, reply);
    }

    private void sendFailedResponse(TransformReply reply, Exception e, HttpStatus status, Destination replyToQueue)
    {
        reply.setStatus(status.value());
        reply.setErrorDetails(messageWithCause("Transform failed", e));

        transformerDebug.logFailure(reply);
        logger.trace("Transform failed. Sending " + reply, e);
        transformReplySender.send(replyToQueue, reply);
    }

    private void checkTransformRequestValid(TransformRequest request, TransformReply reply)
    {
        final Errors errors = validateTransformRequest(request);
        validateInternalContext(request, errors);
        reply.setInternalContext(request.getInternalContext());

        if (!errors.getAllErrors().isEmpty())
        {
            String errorDetails = errors.getAllErrors().stream().map(Object::toString).collect(joining(", "));
            throw new TransformException(BAD_REQUEST, errorDetails);
        }
    }

    private TransformReply createBasicTransformReply(TransformRequest request)
    {
        TransformReply reply = new TransformReply();
        reply.setRequestId(request.getRequestId());
        reply.setSourceReference(request.getSourceReference());
        reply.setSchema(request.getSchema());
        reply.setClientData(request.getClientData());
        reply.setInternalContext(request.getInternalContext());
        return reply;
    }

    private Errors validateTransformRequest(final TransformRequest transformRequest)
    {
        DirectFieldBindingResult errors = new DirectFieldBindingResult(transformRequest, "request");
        transformRequestValidator.validate(transformRequest, errors);
        return errors;
    }

    private void validateInternalContext(TransformRequest request, Errors errors)
    {
        String errorMessage = InternalContext.checkForBasicErrors(request.getInternalContext(), "T-Request");
        if (errorMessage != null)
        {
            errors.rejectValue("internalContext", null, errorMessage);
        }
        initialiseContext(request);
    }

    private void initialiseContext(TransformRequest request)
    {
        // If needed, initialise the context enough to allow logging to take place without NPE checks
        request.setInternalContext(InternalContext.initialise(request.getInternalContext()));
    }

    private InputStream getSharedFileStoreInputStream(String sourceReference)
    {
        ResponseEntity<Resource> responseEntity = alfrescoSharedFileStoreClient.retrieveFile(sourceReference);
        final Resource body = responseEntity.getBody();
        if (body == null)
        {
            String message = "Source file with reference: " + sourceReference + " is null or empty.";
            logger.warn(message);
            throw new TransformException(BAD_REQUEST, message);
        }

        try
        {
            return body.getInputStream();
        }
        catch (IOException e)
        {
            String message = "Shared File Store reference is invalid.";
            logger.warn(message);
            throw new TransformException(BAD_REQUEST, message, e);
        }
    }

    private InputStream getInputStreamForHandleHttpRequest(Map<String, String> requestParameters,
        MultipartFile sourceMultipartFile)
    {
        final String directUrl = requestParameters.getOrDefault(DIRECT_ACCESS_URL, "");
        return new BufferedInputStream(directUrl.isBlank()
            ? getMultipartFileInputStream(sourceMultipartFile)
            : getDirectAccessUrlInputStream(directUrl));
    }

    private InputStream getInputStreamForHandleProbRequest(File sourceFile)
    {
        try
        {
            return new BufferedInputStream(new FileInputStream(sourceFile));
        }
        catch (FileNotFoundException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, messageWithCause("Failed to read the probe source", e));
        }
    }

    private InputStream getInputStreamForHandleMessageRequest(TransformRequest request)
    {
        final String directUrl = request.getTransformRequestOptions().getOrDefault(DIRECT_ACCESS_URL, "");
        try
        {
            return new BufferedInputStream(directUrl.isBlank()
                    ? getSharedFileStoreInputStream(request.getSourceReference())
                    : getDirectAccessUrlInputStream(directUrl));
        }
        catch (TransformException e)
        {
            throw new TransformException(e.getStatus(), messageWithCause("Failed to read the source", e));
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(e.getStatusCode(), messageWithCause("Failed to read the source from the SFS", e));
        }
    }

    private OutputStream getOutputStreamFromFile(File targetFile) throws IOException
    {
        return new BufferedOutputStream(new FileOutputStream(targetFile));
    }

    private void saveTargetFileInSharedFileStore(File targetFile, TransformReply reply)
    {
        FileRefResponse targetRef;
        try
        {
            targetRef = alfrescoSharedFileStoreClient.saveFile(targetFile);
        }
        catch (TransformException e)
        {
            throw new TransformException(e.getStatus(), messageWithCause("Failed writing to SFS", e));
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(e.getStatusCode(), messageWithCause("Failed writing to SFS", e));
        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, messageWithCause("Failed writing to SFS", e));
        }

        reply.setTargetReference(targetRef.getEntry().getFileRef());
        reply.setStatus(CREATED.value());
    }

    private static String messageWithCause(final String prefix, Throwable e)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(" - ");
        if (e.getClass() != TransformException.class)
        {
            sb.append(e.getClass().getSimpleName()).append(": ");
        }
        sb.append(e.getMessage());

        while (e.getCause() != null)
        {
            e = e.getCause();
            sb.append(", cause ")
              .append(e.getClass().getSimpleName()).append(": ")
              .append(e.getMessage());
        }

        return sb.toString();
    }
}
