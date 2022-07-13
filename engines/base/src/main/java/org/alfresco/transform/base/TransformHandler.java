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
import org.alfresco.transform.base.probes.ProbeTestTransform;
import org.alfresco.transform.base.util.OutputStreamLengthRecorder;
import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.common.TransformerDebug;
import org.alfresco.transform.messages.TransformRequestValidator;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.joining;
import static org.alfresco.transform.base.fs.FileManager.TempFileProvider.createTempFile;
import static org.alfresco.transform.base.fs.FileManager.createTargetFile;
import static org.alfresco.transform.base.fs.FileManager.deleteFile;
import static org.alfresco.transform.base.fs.FileManager.getDirectAccessUrlInputStream;
import static org.alfresco.transform.base.fs.FileManager.getFilenameFromContentDisposition;
import static org.alfresco.transform.base.fs.FileManager.save;
import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_EXTENSION;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_ENCODING;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.util.StringUtils.getFilenameExtension;

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
    private ProbeTestTransform probeTestTransform;
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
            probeTestTransform = transformEngine.getLivenessAndReadinessProbeTestTransform();
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

    public ProbeTestTransform getProbeTestTransform()
    {
        return probeTestTransform;
    }

    public StreamingResponseBody handleHttpRequest(HttpServletRequest request, MultipartFile sourceMultipartFile,
            String sourceMimetype, String targetMimetype, Map<String, String> requestParameters)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Processing request via HTTP endpoint. Params: sourceMimetype: '{}', targetMimetype: '{}', "
                    + "requestParameters: {}", sourceMimetype, targetMimetype, requestParameters);
        }
        probeTestTransform.incrementTransformerCount();

        // Obtain the source
        final String directUrl = requestParameters.getOrDefault(DIRECT_ACCESS_URL, "");
        InputStream inputStream = directUrl.isBlank()
                ? FileManager.getMultipartFileInputStream(sourceMultipartFile)
                : getDirectAccessUrlInputStream(directUrl);
        long sourceSizeInBytes = -1L; // TODO pass in t-options or just ignore for http request as the repo will have checked.
        Map<String, String> transformOptions = getTransformOptions(requestParameters);
        String transformName = getTransformerName(sourceSizeInBytes, sourceMimetype, targetMimetype, transformOptions);
        CustomTransformer customTransformer = getCustomTransformer(transformName);
        String sourceEncoding = transformOptions.get(SOURCE_ENCODING);
        String targetEncoding = transformOptions.get(TARGET_ENCODING); // TODO not normally set
        String reference = "e"+httpRequestCount.getAndIncrement();
        transformerDebug.pushTransform(reference, sourceMimetype, targetMimetype, sourceSizeInBytes, transformName);
        transformerDebug.logOptions(reference, requestParameters);

        return os -> {
            OutputStreamLengthRecorder outputStream = new OutputStreamLengthRecorder(os);
            try
            {
                TransformManagerImpl transformManager = TransformManagerImpl.builder()
                                                                            .withRequest(request)
                                                                            .withSourceMimetype(sourceMimetype)
                                                                            .withTargetMimetype(targetMimetype)
                                                                            .withInputStream(inputStream)
                                                                            .withOutputStream(outputStream)
                                                                            .build();

                customTransformer.transform(sourceMimetype, inputStream,
                        targetMimetype, outputStream, transformOptions, transformManager);

                transformManager.ifUsedCopyTargetFileToOutputStream();

                LogEntry.setTargetSize(outputStream.getLength());
                long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");

                transformManager.deleteSourceFileIfExists();
                transformManager.deleteTargetFileIfExists();

                probeTestTransform.recordTransformTime(time);
                transformerDebug.popTransform(reference, time);
            }
            catch (Exception e)
            {
                transformerDebug.logFailure(reference, e.getMessage());
                throw new RuntimeException(e);
            }
        };
    }

    public ResponseEntity<TransformReply> handleMessageRequest(TransformRequest request, Long timeout)
    {
        long start = System.currentTimeMillis();
        logger.trace("Received {}, timeout {} ms", request, timeout);
        probeTestTransform.incrementTransformerCount();
        TransformReply reply = createBasicTransformReply(request);

        if (isTransformRequestValid(request, reply) == false)
        {
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }

        InputStream inputStream = getInputStream(request, reply);
        if (inputStream == null)
        {
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }

        String targetMimetype = request.getTargetMediaType();
        String sourceMimetype = request.getSourceMediaType();
        File targetFile = createTargetFile(null, sourceMimetype, targetMimetype);
        transformerDebug.pushTransform(request);

        try
        {
            OutputStreamLengthRecorder outputStream =
                    new OutputStreamLengthRecorder(new BufferedOutputStream(new FileOutputStream(targetFile)));

            long sourceSizeInBytes = request.getSourceSize();
            Map<String, String> transformOptions = getTransformOptions(request.getTransformRequestOptions());
            String sourceEncoding = transformOptions.get(SOURCE_ENCODING);
            String targetEncoding = transformOptions.get(TARGET_ENCODING); // TODO not normally set
            transformerDebug.logOptions(request);
            String transformName = getTransformerName(sourceSizeInBytes, sourceMimetype, targetMimetype, transformOptions);
            CustomTransformer customTransformer = getCustomTransformer(transformName);

            TransformManagerImpl transformManager = TransformManagerImpl.builder()
                                                                        .withSourceMimetype(sourceMimetype)
                                                                        .withTargetMimetype(targetMimetype)
                                                                        .withInputStream(inputStream)
                                                                        .withOutputStream(outputStream)
                                                                        .withTargetFile(targetFile)
                                                                        .build();

            customTransformer.transform(sourceMimetype, inputStream,
                    targetMimetype, outputStream, transformOptions, transformManager);

            transformManager.ifUsedCopyTargetFileToOutputStream();

            reply.getInternalContext().setCurrentSourceSize(outputStream.getLength());

            if (saveTargetFileInSharedFileStore(targetFile, reply) == false)
            {
                return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
            }

            transformManager.deleteSourceFileIfExists();
            transformManager.deleteTargetFileIfExists();

            probeTestTransform.recordTransformTime(System.currentTimeMillis()-start);
            transformerDebug.popTransform(reply);

            logger.trace("Sending successful {}, timeout {} ms", reply, timeout);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
        catch (TransformException e)
        {
            reply.setStatus(e.getStatusCode());
            reply.setErrorDetails(messageWithCause("Failed at processing transformation", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to perform transform (TransformException), sending " + reply, e);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
        catch (Exception e)
        {
            reply.setStatus(INTERNAL_SERVER_ERROR.value());
            reply.setErrorDetails(messageWithCause("Failed at processing transformation", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to perform transform (Exception), sending " + reply, e);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
    }
    private boolean isTransformRequestValid(TransformRequest request, TransformReply reply)
    {
        final Errors errors = validateTransformRequest(request);
        validateInternalContext(request, errors);
        reply.setInternalContext(request.getInternalContext());
        if (!errors.getAllErrors().isEmpty())
        {
            reply.setStatus(BAD_REQUEST.value());
            reply.setErrorDetails(errors
                    .getAllErrors()
                    .stream()
                    .map(Object::toString)
                    .collect(joining(", ")));

            transformerDebug.logFailure(reply);
            logger.trace("Invalid request, sending {}", reply);
            return false;
        }
        return true;
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
            throw new TransformException(BAD_REQUEST.value(), message);
        }

        try
        {
            return body.getInputStream();
        }
        catch (IOException e)
        {
            String message = "Shared File Store reference is invalid.";
            logger.warn(message);
            throw new TransformException(BAD_REQUEST.value(), message, e);
        }
    }

    private InputStream getInputStream(TransformRequest request, TransformReply reply)
    {
        final String directUrl = request.getTransformRequestOptions().getOrDefault(DIRECT_ACCESS_URL, "");
        InputStream inputStream = null;
        try
        {
            inputStream = directUrl.isBlank()
                    ? getSharedFileStoreInputStream(request.getSourceReference())
                    : getDirectAccessUrlInputStream(directUrl);
        }
        catch (TransformException e)
        {
            reply.setStatus(e.getStatusCode());
            reply.setErrorDetails(messageWithCause("Failed at reading the source file", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to load source file (TransformException), sending " + reply);
        }
        catch (HttpClientErrorException e)
        {
            reply.setStatus(e.getStatusCode().value());
            reply.setErrorDetails(messageWithCause("Failed at reading the source file", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to load source file (HttpClientErrorException), sending " + reply, e);
        }
        return inputStream;
    }

    private boolean saveTargetFileInSharedFileStore(File targetFile, TransformReply reply)
    {
        FileRefResponse targetRef;
        try
        {
            targetRef = alfrescoSharedFileStoreClient.saveFile(targetFile);
        }
        catch (TransformException e)
        {
            reply.setStatus(e.getStatusCode());
            reply.setErrorDetails(messageWithCause("Failed at writing the transformed file", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to save target file (TransformException), sending " + reply, e);
            return false;
        }
        catch (HttpClientErrorException e)
        {
            reply.setStatus(e.getStatusCode().value());
            reply.setErrorDetails(messageWithCause("Failed at writing the transformed file. ", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to save target file (HttpClientErrorException), sending " + reply, e);
            return false;
        }
        catch (Exception e)
        {
            reply.setStatus(INTERNAL_SERVER_ERROR.value());
            reply.setErrorDetails(messageWithCause("Failed at writing the transformed file. ", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to save target file (Exception), sending " + reply, e);
            return false;
        }

        try
        {
            deleteFile(targetFile);
        }
        catch (Exception e)
        {
            logger.error("Failed to delete local temp target file. Error will be ignored ", e);
        }

        reply.setTargetReference(targetRef.getEntry().getFileRef());
        reply.setStatus(CREATED.value());

        return true;
    }

    /**
     * Loads the file with the specified sourceReference from Alfresco Shared File Store
     *
     * @param sourceReference reference to the file in Alfresco Shared File Store
     * @param sourceExtension default extension if the file in Alfresco Shared File Store has none
     * @return the file containing the source content for the transformation
     */
    private File loadSourceFile(final String sourceReference, final String sourceExtension)
    {
        ResponseEntity<Resource> responseEntity = alfrescoSharedFileStoreClient.retrieveFile(sourceReference);

        HttpHeaders headers = responseEntity.getHeaders();
        String filename = getFilenameFromContentDisposition(headers);

        String extension = getFilenameExtension(filename) != null ? getFilenameExtension(filename) : sourceExtension;
        MediaType contentType = headers.getContentType();
        long size = headers.getContentLength();

        final Resource body = responseEntity.getBody();
        if (body == null)
        {
            String message = "Source file with reference: " + sourceReference + " is null or empty. "
                    + "Transformation will fail and stop now as there is no content to be transformed.";
            logger.warn(message);
            throw new TransformException(BAD_REQUEST.value(), message);
        }
        final File file = createTempFile("source_", "." + extension);

        logger.debug("Read source content {} length={} contentType={}",
                sourceReference, size, contentType);

        save(body, file);
        LogEntry.setSource(filename, size);
        return file;
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
                throw new TransformException(BAD_REQUEST.value(), "No transforms were able to handle the request");
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
            throw new TransformException(BAD_REQUEST.value(), "Custom Transformer "+customTransformer+" not found");
        }
        return customTransformer;
    }
}
