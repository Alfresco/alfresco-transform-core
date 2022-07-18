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
package org.alfresco.transform.base;

import org.alfresco.transform.base.clients.AlfrescoSharedFileStoreClient;
import org.alfresco.transform.base.fs.FileManager;
import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.base.util.OutputStreamLengthRecorder;
import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.common.ExtensionService;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.common.TransformerDebug;
import org.alfresco.transform.messages.TransformRequestValidator;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.joining;
import static org.alfresco.transform.base.fs.FileManager.createTargetFile;
import static org.alfresco.transform.base.fs.FileManager.getDirectAccessUrlInputStream;
import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_EXTENSION;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

/**
 * Handles the transform requests from either http or a message.
 */
@Component
public class TransformHandler
{
    private static final Logger logger = LoggerFactory.getLogger(TransformHandler.class);
    private static final List<String> NON_TRANSFORM_OPTION_REQUEST_PARAMETERS = Arrays.asList(SOURCE_EXTENSION,
            TARGET_MIMETYPE, SOURCE_MIMETYPE, DIRECT_ACCESS_URL);

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
    private TransformerDebug transformerDebug;

    private AtomicInteger httpRequestCount = new AtomicInteger(1);
    private TransformEngine transformEngine;
    private ProbeTransform probeTransform;
    private Map<String, CustomTransformer> customTransformersByName = new HashMap<>();

    @PostConstruct
    public void init()
    {
        initTransformEngine();
        initProbeTestTransform();
        initCustomTransformersByName();
    }

    private void initTransformEngine()
    {
        if (transformEngines != null)
        {
            // Normally there is just one TransformEngine per t-engine, but we also want to be able to amalgamate the
            // CustomTransform code from many t-engines into a single t-engine. In this case, there should be a wrapper
            // TransformEngine (it has no TransformConfig of its own).
            transformEngine = transformEngines.stream()
                                              .filter(transformEngine -> transformEngine.getTransformConfig() == null)
                                              .findFirst()
                                              .orElse(transformEngines.get(0));

            logger.info("TransformEngine: " + transformEngine.getTransformEngineName());
            transformEngines.stream()
                            .filter(te -> te != transformEngine)
                            .sorted(Comparator.comparing(TransformEngine::getTransformEngineName))
                            .map(transformEngine -> "  "+transformEngine.getTransformEngineName()).forEach(logger::info);
        }
    }

    private void initProbeTestTransform()
    {
        if (transformEngine != null)
        {
            probeTransform = transformEngine.getProbeTransform();
        }
    }

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

    public TransformEngine getTransformEngine()
    {
        return transformEngine;
    }

    public ProbeTransform getProbeTestTransform()
    {
        return probeTransform;
    }

    public ResponseEntity<StreamingResponseBody> handleHttpRequest(HttpServletRequest request,
            MultipartFile sourceMultipartFile, String sourceMimetype, String targetMimetype,
            Map<String, String> requestParameters)
    {
        return createResponseEntity(targetMimetype, os ->
        {
            TransformManagerImpl transformManager = null;
            String reference = "e" + httpRequestCount.getAndIncrement();

            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Processing request via HTTP endpoint. Params: sourceMimetype: '{}', targetMimetype: '{}', "
                            + "requestParameters: {}", sourceMimetype, targetMimetype, requestParameters);
                }
                probeTransform.incrementTransformerCount();

                final String directUrl = requestParameters.getOrDefault(DIRECT_ACCESS_URL, "");
                InputStream inputStream = new BufferedInputStream(directUrl.isBlank() ?
                        FileManager.getMultipartFileInputStream(sourceMultipartFile) :
                        getDirectAccessUrlInputStream(directUrl));
                long sourceSizeInBytes = -1L; // Ignore for http requests as the Alfresco repo will have checked.
                Map<String, String> transformOptions = getTransformOptions(requestParameters);
                String transformName = getTransformerName(sourceSizeInBytes, sourceMimetype, targetMimetype, transformOptions);
                CustomTransformer customTransformer = getCustomTransformer(transformName);
                transformerDebug.pushTransform(reference, sourceMimetype, targetMimetype, sourceSizeInBytes, transformName);
                transformerDebug.logOptions(reference, requestParameters);

                OutputStreamLengthRecorder outputStream = new OutputStreamLengthRecorder(os);
                transformManager = TransformManagerImpl.builder()
                                                       .withRequest(request)
                                                       .withSourceMimetype(sourceMimetype)
                                                       .withTargetMimetype(targetMimetype)
                                                       .withInputStream(inputStream)
                                                       .withOutputStream(outputStream)
                                                       .build();
                transformManager.setOutputStream(outputStream);

                customTransformer.transform(sourceMimetype, inputStream,
                        targetMimetype, outputStream, transformOptions, transformManager);

                transformManager.ifUsedCopyTargetFileToOutputStream();

                LogEntry.setTargetSize(outputStream.getLength());
                long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");

                probeTransform.recordTransformTime(time);
                transformerDebug.popTransform(reference, time);
            }
            catch (TransformException e)
            {
                transformerDebug.logFailure(reference, e.getMessage());
                throw e;
            }
            catch (Exception e)
            {
                transformerDebug.logFailure(reference, e.getMessage());
                throw new RuntimeException(e);
            }
            finally
            {
                deleteTmpFiles(transformManager);
            }
        });
    }

    public ResponseEntity<TransformReply> handleMessageRequest(TransformRequest request, Long timeout)
    {
        long start = System.currentTimeMillis();
        InputStream inputStream = null;
        TransformManagerImpl transformManager = null;
        TransformReply reply = createBasicTransformReply(request);;

        try
        {
            logger.trace("Received {}, timeout {} ms", request, timeout);
            probeTransform.incrementTransformerCount();
            checkTransformRequestValid(request, reply);
            inputStream = getInputStream(request, reply);
            String targetMimetype = request.getTargetMediaType();
            String sourceMimetype = request.getSourceMediaType();
            File targetFile = createTargetFile(null, sourceMimetype, targetMimetype);
            transformerDebug.pushTransform(request);

            long sourceSizeInBytes = request.getSourceSize();
            Map<String, String> transformOptions = getTransformOptions(request.getTransformRequestOptions());
            transformerDebug.logOptions(request);
            String transformName = getTransformerName(sourceSizeInBytes, sourceMimetype, targetMimetype, transformOptions);
            CustomTransformer customTransformer = getCustomTransformer(transformName);

            try (OutputStreamLengthRecorder outputStream = new OutputStreamLengthRecorder(new BufferedOutputStream(
                    new FileOutputStream(targetFile))))
            {
                transformManager = TransformManagerImpl.builder()
                                                       .withSourceMimetype(sourceMimetype)
                                                       .withTargetMimetype(targetMimetype)
                                                       .withInputStream(inputStream)
                                                       .withOutputStream(outputStream)
                                                       .withTargetFile(targetFile)
                                                       .build();

                customTransformer.transform(sourceMimetype, inputStream, targetMimetype, outputStream, transformOptions,
                        transformManager);

                transformManager.ifUsedCopyTargetFileToOutputStream();

                reply.getInternalContext().setCurrentSourceSize(outputStream.getLength());

                saveTargetFileInSharedFileStore(targetFile, reply);
            }
        }
        catch (TransformException e)
        {
            return createFailedResponseEntity(reply, e, e.getStatusCode().value());
        }
        catch (Exception e)
        {
            return createFailedResponseEntity(reply, e, INTERNAL_SERVER_ERROR.value());
        }
        finally
        {
            deleteTmpFiles(transformManager);
            closeInputStreamWithoutException(inputStream);

            probeTransform.recordTransformTime(System.currentTimeMillis()-start);
            transformerDebug.popTransform(reply);

            logger.trace("Sending successful {}, timeout {} ms", reply, timeout);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
    }

    private ResponseEntity<TransformReply> createFailedResponseEntity(TransformReply reply, Exception e,
            int status) {
        reply.setStatus(status);
        reply.setErrorDetails(messageWithCause("Transform failed", e));

        transformerDebug.logFailure(reply);
        logger.trace("Transform failed. Sending " + reply, e);
        return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
    }

    private void deleteTmpFiles(TransformManagerImpl transformManager)
    {
        if (transformManager != null)
        {
            transformManager.deleteSourceFileIfExists();
            transformManager.deleteTargetFileIfExists();
        }
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

    private Map<String, String> getTransformOptions(Map<String, String> requestParameters)
    {
        Map<String, String> transformOptions = new HashMap<>(requestParameters);
        transformOptions.keySet().removeAll(NON_TRANSFORM_OPTION_REQUEST_PARAMETERS);
        transformOptions.values().removeIf(v -> v.isEmpty());
        return transformOptions;
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

    private InputStream getInputStream(TransformRequest request, TransformReply reply)
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
            throw new TransformException(e.getStatusCode(), messageWithCause("Failed to read the source", e));
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(e.getStatusCode(), messageWithCause("Failed to read the source", e));
        }
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
            throw new TransformException(e.getStatusCode(), messageWithCause("Failed writing to SFS", e));
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
        sb.append(prefix).append(" - ")
          .append(e.getClass().getSimpleName()).append(": ")
          .append(e.getMessage());

        while (e.getCause() != null)
        {
            e = e.getCause();
            sb.append(", cause ")
              .append(e.getClass().getSimpleName()).append(": ")
              .append(e.getMessage());
        }

        return sb.toString();
    }

    private String getTransformerName(long sourceSizeInBytes, final String sourceMimetype,
            final String targetMimetype, final Map<String, String> transformOptions)
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
                throw new TransformException(BAD_REQUEST, "No transforms were able to handle the request");
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

    private void closeInputStreamWithoutException(InputStream inputStream)
    {
        if (inputStream != null)
        {
            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private ResponseEntity<StreamingResponseBody> createResponseEntity(String targetMimetype,
            StreamingResponseBody body)
    {
        String extension = ExtensionService.getExtensionForMimetype(targetMimetype);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                                  .filename("transform."+ extension, StandardCharsets.UTF_8)
                                  .build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
