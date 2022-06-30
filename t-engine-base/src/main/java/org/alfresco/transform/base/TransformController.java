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

import org.alfresco.transform.base.probes.ProbeTestTransform;
import org.alfresco.transform.common.TransformerDebug;
import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.messages.TransformRequestValidator;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.base.clients.AlfrescoSharedFileStoreClient;
import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.base.model.FileRefResponse;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.joining;
import static org.alfresco.transform.config.CoreVersionDecorator.setOrClearCoreVersion;
import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION_DEFAULT;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG;
import static org.alfresco.transform.base.fs.FileManager.TempFileProvider.createTempFile;
import static org.alfresco.transform.base.fs.FileManager.buildFile;
import static org.alfresco.transform.base.fs.FileManager.createAttachment;
import static org.alfresco.transform.base.fs.FileManager.createSourceFile;
import static org.alfresco.transform.base.fs.FileManager.createTargetFile;
import static org.alfresco.transform.base.fs.FileManager.createTargetFileName;
import static org.alfresco.transform.base.fs.FileManager.deleteFile;
import static org.alfresco.transform.base.fs.FileManager.getFilenameFromContentDisposition;
import static org.alfresco.transform.base.fs.FileManager.save;
import static org.alfresco.transform.base.util.RequestParamMap.FILE;
import static org.alfresco.transform.base.util.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transform.base.util.RequestParamMap.SOURCE_EXTENSION;
import static org.alfresco.transform.base.util.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.base.util.RequestParamMap.TARGET_MIMETYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.util.StringUtils.getFilenameExtension;

/**
 * Provides the main endpoints into the t-engine.
 */
@Controller
public class TransformController
{
    private static final Logger logger = LoggerFactory.getLogger(TransformController.class);
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
    @Value("${transform.core.version}")
    private String coreVersion;

    private TransformEngine transformEngine;
    ProbeTestTransform probeTestTransform;
    private Map<String, CustomTransformer> customTransformersByName = new HashMap<>();
    private AtomicInteger httpRequestCount = new AtomicInteger(1);

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
            customTransformers.forEach(customTransformer -> customTransformersByName.put(customTransformer.getTransformerName(),
                    customTransformer));
        }
    }

    /**
     * @return a string that may be used in client debug.
     */
    @RequestMapping("/version")
    @ResponseBody
    public String version()
    {
        return transformEngine.getTransformEngineName() + ' ' + coreVersion +  " available";
    }

    /**
     * Test UI page to perform a transform.
     */
    @GetMapping("/")
    public String transformForm(Model model)
    {
        return "transformForm";
    }

    /**
     * Test UI error page.
     */
    @GetMapping("/error")
    public String error()
    {
        return "error"; // the name of the template
    }

    /**
     * Test UI log page.
     */
    @GetMapping("/log")
    String log(Model model)
    {
        model.addAttribute("title", transformEngine.getTransformEngineName() + " Log Entries");
        Collection<LogEntry> log = LogEntry.getLog();
        if (!log.isEmpty())
        {
            model.addAttribute("log", log);
        }
        return "log"; // the name of the template
    }

    /**
     * Kubernetes readiness probe.
     */
    @GetMapping("/ready")
    @ResponseBody
    public String ready(HttpServletRequest request)
    {
        return probeTestTransform.doTransformOrNothing(request, false, this);
    }

    /**
     * Kubernetes liveness probe.
     */
    @GetMapping("/live")
    @ResponseBody
    public String live(HttpServletRequest request)
    {
        return probeTestTransform.doTransformOrNothing(request, true, this);
    }

    @GetMapping(value = ENDPOINT_TRANSFORM_CONFIG)
    public ResponseEntity<TransformConfig> transformConfig(
            @RequestParam(value = CONFIG_VERSION, defaultValue = CONFIG_VERSION_DEFAULT) int configVersion)
    {
        logger.info("GET Transform Config version: " + configVersion);
        TransformConfig transformConfig = ((TransformRegistryImpl) transformRegistry).getTransformConfig();
        transformConfig = setOrClearCoreVersion(transformConfig, configVersion);
        return new ResponseEntity<>(transformConfig, OK);
    }

    @PostMapping(value = ENDPOINT_TRANSFORM, consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam(value = FILE, required = false) MultipartFile sourceMultipartFile,
                                              @RequestParam(value = SOURCE_MIMETYPE, required = false) String sourceMimetype,
                                              @RequestParam(value = TARGET_MIMETYPE, required = false) String targetMimetype,
                                              @RequestParam Map<String, String> requestParameters)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Processing request via HTTP endpoint. Params: sourceMimetype: '{}', targetMimetype: '{}', "
                    + "requestParameters: {}", sourceMimetype, targetMimetype, requestParameters);
        }

        final String directUrl = requestParameters.getOrDefault(DIRECT_ACCESS_URL, "");

        File sourceFile;
        String sourceFilename;
        if (directUrl.isBlank())
        {
            if (sourceMultipartFile ==  null)
            {
                throw new TransformException(BAD_REQUEST.value(), "Required request part 'file' is not present");
            }
            sourceFile = createSourceFile(request, sourceMultipartFile);
            sourceFilename = sourceMultipartFile.getOriginalFilename();
        }
        else
        {
            sourceFile = getSourceFileFromDirectUrl(directUrl);
            sourceFilename = sourceFile.getName();
        }

        final String targetFilename = createTargetFileName(sourceFilename, sourceMimetype, targetMimetype);
        probeTestTransform.incrementTransformerCount();
        final File targetFile = createTargetFile(request, targetFilename);

        Map<String, String> transformOptions = getTransformOptions(requestParameters);
        String transformName = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);
        String reference = "e"+httpRequestCount.getAndIncrement();
        transformerDebug.pushTransform(reference, sourceMimetype, targetMimetype, sourceFile, transformName);
        transformerDebug.logOptions(reference, requestParameters);
        try
        {
            transformImpl(transformName, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);

            final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
            LogEntry.setTargetSize(targetFile.length());
            long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");
            probeTestTransform.recordTransformTime(time);
            transformerDebug.popTransform(reference, time);
            return body;
        }
        catch (Throwable t)
        {
            transformerDebug.logFailure(reference, t.getMessage());
            throw t;
        }
    }

    /**
     * '/transform' endpoint which consumes and produces 'application/json'
     *
     * This is the way to tell Spring to redirect the request to this endpoint
     * instead of the one which produces 'html'
     *
     * @param request The transformation request
     * @param timeout Transformation timeout
     * @return A transformation reply
     */
    @PostMapping(value = ENDPOINT_TRANSFORM, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<TransformReply> transform(@RequestBody TransformRequest request,
        @RequestParam(value = "timeout", required = false) Long timeout)
    {
        logger.trace("Received {}, timeout {} ms", request, timeout);

        final TransformReply reply = new TransformReply();
        reply.setRequestId(request.getRequestId());
        reply.setSourceReference(request.getSourceReference());
        reply.setSchema(request.getSchema());
        reply.setClientData(request.getClientData());

        final Errors errors = validateTransformRequest(request);
        validateInternalContext(request, errors);
        initialiseContext(request);
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
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
        transformerDebug.pushTransform(request);

        // Load the source file
        File sourceFile;
        try
        {
            final String directUrl = request.getTransformRequestOptions().getOrDefault(DIRECT_ACCESS_URL, "");
            if (directUrl.isBlank())
            {
                sourceFile = loadSourceFile(request.getSourceReference(), request.getSourceExtension());
            }
            else
            {
                sourceFile = getSourceFileFromDirectUrl(directUrl);
            }
        }
        catch (TransformException e)
        {
            reply.setStatus(e.getStatusCode());
            reply.setErrorDetails(messageWithCause("Failed at reading the source file", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to load source file (TransformException), sending " + reply);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
        catch (HttpClientErrorException e)
        {
            reply.setStatus(e.getStatusCode().value());
            reply.setErrorDetails(messageWithCause("Failed at reading the source file", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to load source file (HttpClientErrorException), sending " + reply, e);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
        catch (Exception e)
        {
            reply.setStatus(INTERNAL_SERVER_ERROR.value());
            reply.setErrorDetails(messageWithCause("Failed at reading the source file", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to load source file (Exception), sending " + reply, e);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }

        // Create local temp target file in order to run the transformation
        final String targetFilename = createTargetFileName(sourceFile.getName(), request.getTargetMediaType(), request.getSourceMediaType());
        final File targetFile = buildFile(targetFilename);

        // Run the transformation
        try
        {
            String targetMimetype = request.getTargetMediaType();
            String sourceMimetype = request.getSourceMediaType();
            Map<String, String> transformOptions = getTransformOptions(request.getTransformRequestOptions());
            transformerDebug.logOptions(request);
            String transformName = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);
            transformImpl(transformName, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
            reply.getInternalContext().setCurrentSourceSize(targetFile.length());
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

        // Write the target file
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
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
        catch (HttpClientErrorException e)
        {
            reply.setStatus(e.getStatusCode().value());
            reply.setErrorDetails(messageWithCause("Failed at writing the transformed file. ", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to save target file (HttpClientErrorException), sending " + reply, e);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }
        catch (Exception e)
        {
            reply.setStatus(INTERNAL_SERVER_ERROR.value());
            reply.setErrorDetails(messageWithCause("Failed at writing the transformed file. ", e));

            transformerDebug.logFailure(reply);
            logger.trace("Failed to save target file (Exception), sending " + reply, e);
            return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
        }

        try
        {
            deleteFile(targetFile);
        }
        catch (Exception e)
        {
            logger.error("Failed to delete local temp target file '{}'. Error will be ignored ",
                targetFile, e);
        }
        try
        {
            deleteFile(sourceFile);
        }
        catch (Exception e)
        {
            logger.error("Failed to delete source local temp file " + sourceFile, e);
        }

        reply.setTargetReference(targetRef.getEntry().getFileRef());
        reply.setStatus(CREATED.value());

        transformerDebug.popTransform(reply);
        logger.trace("Sending successful {}, timeout {} ms", reply, timeout);
        return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
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
    }

    private void initialiseContext(TransformRequest request)
    {
        // If needed, initialise the context enough to allow logging to take place without NPE checks
        request.setInternalContext(InternalContext.initialise(request.getInternalContext()));
    }

    private File getSourceFileFromDirectUrl(String directUrl)
    {
        File sourceFile = createTempFile("tmp", ".tmp");
        try
        {
            FileUtils.copyURLToFile(new URL(directUrl), sourceFile);
        }
        catch (IllegalArgumentException e)
        {
            throw new TransformException(BAD_REQUEST.value(), "Direct Access Url is invalid.", e);
        }
        catch (IOException e)
        {
            throw new TransformException(BAD_REQUEST.value(), "Direct Access Url not found.", e);
        }

        return sourceFile;
    }

    protected Map<String, String> getTransformOptions(Map<String, String> requestParameters)
    {
        Map<String, String> transformOptions = new HashMap<>(requestParameters);
        transformOptions.keySet().removeAll(NON_TRANSFORM_OPTION_REQUEST_PARAMETERS);
        transformOptions.values().removeIf(v -> v.isEmpty());
        return transformOptions;
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
        ResponseEntity<Resource> responseEntity = alfrescoSharedFileStoreClient
            .retrieveFile(sourceReference);
        probeTestTransform.incrementTransformerCount();

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

    private String getTransformerName(final File sourceFile, final String sourceMimetype,
        final String targetMimetype, final Map<String, String> transformOptions)
    {
        // The transformOptions always contains sourceEncoding when sent to a T-Engine, even though it should not be
        // used to select a transformer. Similar to source and target mimetypes and extensions, but these are not
        // passed in transformOptions.
        String sourceEncoding = transformOptions.remove(SOURCE_ENCODING);
        try
        {
            final long sourceSizeInBytes = sourceFile.length();
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

    public void transformImpl(String transformName, String sourceMimetype, String targetMimetype,
            Map<String, String> transformOptions, File sourceFile, File targetFile)
    {
        //javaExecutor.transformExtractOrEmbed(transformName, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public void handleParamsTypeMismatch(HttpServletResponse response, MissingServletRequestParameterException e) throws IOException
    {
        final String message = format("Request parameter ''{0}'' is of the wrong type", e.getParameterName());
        final int statusCode = BAD_REQUEST.value();

        logger.error(message, e);
        LogEntry.setStatusCodeAndMessage(statusCode, message);
        response.sendError(statusCode, transformEngine.getTransformEngineName() + " - " + message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public void handleMissingParams(HttpServletResponse response, MissingServletRequestParameterException e) throws IOException
    {
        final String message = format("Request parameter ''{0}'' is missing", e.getParameterName());
        final int statusCode = BAD_REQUEST.value();

        logger.error(message, e);
        LogEntry.setStatusCodeAndMessage(statusCode, message);
        response.sendError(statusCode, transformEngine.getTransformEngineName() + " - " + message);
    }

    @ExceptionHandler(TransformException.class)
    public void transformExceptionWithMessage(HttpServletResponse response, TransformException e) throws IOException
    {
        final String message = e.getMessage();
        final int statusCode = e.getStatusCode();

        logger.error(message, e);
        long time = LogEntry.setStatusCodeAndMessage(statusCode, message);
        probeTestTransform.recordTransformTime(time);
        response.sendError(statusCode, transformEngine.getTransformEngineName() + " - " + message);
    }
}
