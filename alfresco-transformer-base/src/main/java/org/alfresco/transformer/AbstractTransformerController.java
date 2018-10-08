/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.client.model.TransformRequestValidator;
import org.alfresco.transformer.model.FileRefResponse;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.logging.Log;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
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
import org.springframework.web.util.UriUtils;

/**
 * <p>Abstract Controller, provides structure and helper methods to sub-class transformer controllers.</p>
 *
 * <p>Status Codes:</p>
 * <ul>
 *   <li>200 Success</li>
 *   <li>400 Bad Request: Request parameter <name> is missing (missing mandatory parameter)</li>
 *   <li>400 Bad Request: Request parameter <name> is of the wrong type</li>
 *   <li>400 Bad Request: Transformer exit code was not 0 (possible problem with the source file)</li>
 *   <li>400 Bad Request: The source filename was not supplied</li>
 *   <li>500 Internal Server Error: (no message with low level IO problems)</li>
 *   <li>500 Internal Server Error: The target filename was not supplied (should not happen as targetExtension is checked)</li>
 *   <li>500 Internal Server Error: Transformer version check exit code was not 0</li>
 *   <li>500 Internal Server Error: Transformer version check failed to create any output</li>
 *   <li>500 Internal Server Error: Could not read the target file</li>
 *   <li>500 Internal Server Error: The target filename was malformed (should not happen because of other checks)</li>
 *   <li>500 Internal Server Error: Transformer failed to create an output file (the exit code was 0, so there should be some content)</li>
 *   <li>500 Internal Server Error: Filename encoding error</li>
 *   <li>507 Insufficient Storage: Failed to store the source file</li>
 *
 *   <li>408 Request Timeout         -- TODO implement general timeout mechanism rather than depend on transformer timeout
 *                                  (might be possible for external processes)</li>
 *   <li>415 Unsupported Media Type  -- TODO possibly implement a check on supported source and target mimetypes (probably not)</li>
 *   <li>429 Too Many Requests: Returned by liveness probe</li>
 * </ul>
 * <p>Provides methods to help super classes perform /transform requests. Also responses to /version, /ready and /live
 * requests.</p>
 */
public abstract class AbstractTransformerController
{
    public static final String SOURCE_FILE = "sourceFile";
    public static final String TARGET_FILE = "targetFile";
    public static final String FILENAME = "filename=";

    @Autowired
    private AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient;

    @Autowired
    private TransformRequestValidator transformRequestValidator;

    protected static Log logger;

    protected RuntimeExec transformCommand;
    private RuntimeExec checkCommand;

    private ProbeTestTransform probeTestTransform = null;

    public void setTransformCommand(RuntimeExec runtimeExec)
    {
        transformCommand = runtimeExec;
    }

    public void setCheckCommand(RuntimeExec runtimeExec)
    {
        checkCommand = runtimeExec;
    }

    protected void logEnterpriseLicenseMessage()
    {
        logger.info("This image is only intended to be used with the Alfresco Enterprise Content Repository which is covered by ");
        logger.info("https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use");
        logger.info("");
        logger.info("License rights for this program may be obtained from Alfresco Software, Ltd. pursuant to a written agreement");
        logger.info("and any use of this program without such an agreement is prohibited.");
        logger.info("");
    }

    protected abstract String getTransformerName();

    /**
     * '/transform' endpoint which consumes and produces 'application/json'
     *
     * This is the way to tell Spring to redirect the request to this endpoint
     * instead of the older one, which produces 'html'
     *
     * @param transformRequest The transformation request
     * @param timeout Transformation timeout
     * @return A transformation reply
     */
    @PostMapping(value = "/transform", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<TransformReply> transform(@RequestBody TransformRequest transformRequest,
        @RequestParam(value = "timeout", required = false) Long timeout)
    {
        TransformReply transformReply = new TransformReply();
        transformReply.setRequestId(transformRequest.getRequestId());
        transformReply.setSourceReference(transformRequest.getSourceReference());
        transformReply.setSchema(transformRequest.getSchema());
        transformReply.setClientData(transformRequest.getClientData());

        Errors errors = validateTransformRequest(transformRequest);
        if (!errors.getAllErrors().isEmpty())
        {
            transformReply.setStatus(HttpStatus.BAD_REQUEST.value());
            transformReply.setErrorDetails(errors.getAllErrors().stream().map(Object::toString)
                .collect(Collectors.joining(", ")));

            return new ResponseEntity<>(transformReply,
                HttpStatus.valueOf(transformReply.getStatus()));
        }

        // Load the source file
        File sourceFile;
        try
        {
            sourceFile = loadSourceFile(transformRequest.getSourceReference());
        }
        catch (TransformException te)
        {
            transformReply.setStatus(te.getStatusCode());
            transformReply
                .setErrorDetails("Failed at reading the source file. " + te.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }
        catch (HttpClientErrorException hcee)
        {
            transformReply.setStatus(hcee.getStatusCode().value());
            transformReply
                .setErrorDetails("Failed at reading the source file. " + hcee.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }
        catch (Exception e)
        {
            transformReply.setStatus(500);
            transformReply.setErrorDetails("Failed at reading the source file. " + e.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }

        // Create local temp target file in order to run the transformation
        String targetFilename = createTargetFileName(sourceFile.getName(),
            transformRequest.getTargetExtension());
        File targetFile = buildFile(targetFilename);

        // Run the transformation
        try
        {
            processTransform(sourceFile, targetFile,
                transformRequest.getTransformRequestOptions(), timeout);
        }
        catch (TransformException te)
        {
            transformReply.setStatus(te.getStatusCode());
            transformReply
                .setErrorDetails("Failed at processing transformation. " + te.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }
        catch (Exception e)
        {
            transformReply.setStatus(500);
            transformReply
                .setErrorDetails("Failed at processing transformation. " + e.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }

        // Write the target file
        FileRefResponse targetRef;
        try
        {
            targetRef = alfrescoSharedFileStoreClient.saveFile(targetFile);
        }
        catch (TransformException te)
        {
            transformReply.setStatus(te.getStatusCode());
            transformReply
                .setErrorDetails("Failed at writing the transformed file. " + te.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }
        catch (HttpClientErrorException hcee)
        {
            transformReply.setStatus(hcee.getStatusCode().value());
            transformReply
                .setErrorDetails("Failed at writing the transformed file. " + hcee.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }
        catch (Exception e)
        {
            transformReply.setStatus(500);
            transformReply
                .setErrorDetails("Failed at writing the transformed file. " + e.getMessage());

            return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
        }

        transformReply.setTargetReference(targetRef.getEntry().getFileRef());
        transformReply.setStatus(HttpStatus.CREATED.value());

        return new ResponseEntity<>(transformReply, HttpStatus.valueOf(transformReply.getStatus()));
    }

    private Errors validateTransformRequest(TransformRequest transformRequest)
    {
        DirectFieldBindingResult errors = new DirectFieldBindingResult(transformRequest, "request");
        transformRequestValidator.validate(transformRequest, errors);
        return errors;
    }

    protected abstract void processTransform(File sourceFile, File targetFile,
        Map<String, String> transformOptions, Long timeout);

    @RequestMapping("/version")
    @ResponseBody
    protected String version()
    {
        String version = "Version not checked";
        if (checkCommand != null)
        {
            RuntimeExec.ExecutionResult result = checkCommand.execute();
            if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0)
            {
                throw new TransformException(500, "Transformer version check exit code was not 0: \n" + result);
            }

            version = result.getStdOut().trim();
            if (version.isEmpty())
            {
                throw new TransformException(500, "Transformer version check failed to create any output");
            }
        }

        return version;
    }

    @GetMapping("/ready")
    @ResponseBody
    public String ready(HttpServletRequest request)
    {
        return probe(request, false);
    }

    @GetMapping("/live")
    @ResponseBody
    public String live(HttpServletRequest request)
    {
        return probe(request, true);
    }

    private String probe(HttpServletRequest request, boolean isLiveProbe)
    {
        return getProbeTestTransformInternal().doTransformOrNothing(request, isLiveProbe);
    }

    private ProbeTestTransform getProbeTestTransformInternal()
    {
        if (probeTestTransform == null)
        {
            probeTestTransform = getProbeTestTransform();
        }
        return probeTestTransform;
    }

    abstract ProbeTestTransform getProbeTestTransform();

    @GetMapping("/")
    public String transformForm(Model model)
    {
        return "transformForm"; // the name of the template
    }

    @GetMapping("/log")
    public String log(Model model)
    {
        model.addAttribute("title", getTransformerName() + " Log Entries");
        Collection<LogEntry> log = LogEntry.getLog();
        if (!log.isEmpty())
        {
            model.addAttribute("log", log);
        }
        return "log"; // the name of the template
    }

    @GetMapping("/error")
    public String error()
    {
        return "error"; // the name of the template
    }

    @ExceptionHandler(TypeMismatchException.class)
    public void handleParamsTypeMismatch(HttpServletResponse response, MissingServletRequestParameterException e) throws IOException
    {
        String transformerName = getTransformerName();
        String name = e.getParameterName();
        String message = "Request parameter " + name + " is of the wrong type";
        int statusCode = 400;

        if (logger != null && logger.isErrorEnabled())
        {
            logger.error(message);
        }

        LogEntry.setStatusCodeAndMessage(statusCode, message);

        response.sendError(statusCode, transformerName+" - "+message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public void handleMissingParams(HttpServletResponse response, MissingServletRequestParameterException e) throws IOException
    {
        String transformerName = getTransformerName();
        String name = e.getParameterName();
        String message = "Request parameter " + name + " is missing";
        int statusCode = 400;

        if (logger != null && logger.isErrorEnabled())
        {
            logger.error(message);
        }

        LogEntry.setStatusCodeAndMessage(statusCode, message);

        response.sendError(statusCode, transformerName+" - "+message);
    }

    @ExceptionHandler(TransformException.class)
    public void transformExceptionWithMessage(HttpServletResponse response, TransformException e) throws IOException
    {
        String transformerName = getTransformerName();
        String message = e.getMessage();
        int statusCode = e.getStatusCode();

        if (logger != null && logger.isErrorEnabled())
        {
            logger.error(message);
        }

        long time = LogEntry.setStatusCodeAndMessage(statusCode, message);
        getProbeTestTransformInternal().recordTransformTime(time);

        // Forced to include the transformer name in the message (see commented out version of this method)
        response.sendError(statusCode, transformerName+" - "+message);
    }

    // Results in HTML rather than json but there is an error in the log about "template might not exist or might
    // not be accessible by any of the configured Template Resolvers" for the transformer.html (which is correct
    // because that failed). Looks like Spring only supports returning json or XML when returning an Object or even
    // a ResponseEntity without this logged exception, which is a shame as it would have been nicer to have just
    // added the transformerName to the Object.
//    @ExceptionHandler(TransformException.class)
//    public final Map<String, Object> transformExceptionWithMessage(HttpServletResponse response, TransformException e, WebRequest request)
//    {
//        String transformerName = getTransformerName();
//        String message = e.getMessage();
//        int statusCode = e.getStatusCode();
//
//        LogEntry.setStatusCodeAndMessage(statusCode, message);
//
//        Map<String, Object> errorAttributes = new HashMap<>();
//        errorAttributes.put("title", transformerName);
//        errorAttributes.put("message", message);
//        errorAttributes.put("status", Integer.toString(statusCode));
//        errorAttributes.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
//        return errorAttributes;
//    }

    /**
     * Loads the file with the specified sourceReference from Alfresco Shared File Store
     *
     * @param sourceReference reference to the file in Alfresco Shared File Store
     * @return the file containing the source content for the transformation
     */
    protected File loadSourceFile(String sourceReference)
    {

        ResponseEntity<Resource> responseEntity = alfrescoSharedFileStoreClient
            .retrieveFile(sourceReference);
        getProbeTestTransformInternal().incrementTransformerCount();

        HttpHeaders headers = responseEntity.getHeaders();
        String filename = getFilenameFromContentDisposition(headers);

        String extension = StringUtils.getFilenameExtension(filename);
        MediaType contentType = headers.getContentType();
        long size = headers.getContentLength();

        Resource body = responseEntity.getBody();
        File file = TempFileProvider.createTempFile("source_", "." + extension);

        if (logger.isDebugEnabled())
        {
            logger.debug(
                "Read source content " + sourceReference + " length="
                    + size + " contentType=" + contentType);
        }
        save(body, file);
        LogEntry.setSource(filename, size);
        return file;
    }


    private String getFilenameFromContentDisposition(HttpHeaders headers)
    {
        String filename = "";
        String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDisposition != null)
        {
            String[] strings = contentDisposition.split("; *");
            for (String string: strings)
            {
                if (string.startsWith(FILENAME))
                {
                    filename = string.substring(FILENAME.length());
                    break;
                }
            }
        }
        return filename;
    }

    /**
     * Returns the file name for the target file
     *
     * @param fileName Desired file name
     * @param targetExtension File extension
     * @return Target file name
     */
    protected String createTargetFileName(String fileName, String targetExtension)
    {
        String targetFilename = null;
        String sourceFilename = fileName;
        sourceFilename = StringUtils.getFilename(sourceFilename);
        if (sourceFilename != null && !sourceFilename.isEmpty())
        {
            String ext = StringUtils.getFilenameExtension(sourceFilename);
            targetFilename = (ext != null && !ext.isEmpty()
                ? sourceFilename.substring(0, sourceFilename.length()-ext.length()-1)
                : sourceFilename)+
                '.'+targetExtension;
        }
        return targetFilename;
    }

    /**
     * Returns a File that holds the source content for a transformation.
     *
     * @param request
     * @param multipartFile from the request
     * @return a temporary File.
     * @throws TransformException if there was no source filename.
     */
    protected File createSourceFile(HttpServletRequest request, MultipartFile multipartFile)
    {
        getProbeTestTransformInternal().incrementTransformerCount();
        String filename = multipartFile.getOriginalFilename();
        long size = multipartFile.getSize();
        filename = checkFilename(  true, filename);
        File file = TempFileProvider.createTempFile("source_", "_" + filename);
        request.setAttribute(SOURCE_FILE, file);
        save(multipartFile, file);
        LogEntry.setSource(filename, size);
        return file;
    }

    /**
     * Returns a File to be used to store the result of a transformation.
     *
     * @param request
     * @param filename The targetFilename supplied in the request. Only the filename if a path is used as part of the
     *                 temporary filename.
     * @return a temporary File.
     * @throws TransformException if there was no target filename.
     */
    protected File createTargetFile(HttpServletRequest request, String filename)
    {
        File file = buildFile(filename);
        request.setAttribute(TARGET_FILE, file);
        return file;
    }

    private File buildFile(String filename)
    {
        filename = checkFilename( false, filename);
        LogEntry.setTarget(filename);
        return TempFileProvider.createTempFile("target_", "_" + filename);
    }

    /**
     * Checks the filename is okay to uses in a temporary file name.
     *
     * @param filename or path to be checked.
     * @return the filename part of the supplied filename if it was a path.
     * @throws TransformException if there was no target filename.
     */
    private String checkFilename(boolean source, String filename)
    {
        filename = StringUtils.getFilename(filename);
        if (filename == null || filename.isEmpty())
        {
            String sourceOrTarget = source ? "source" : "target";
            int statusCode = source ? 400 : 500;
            throw new TransformException(statusCode, "The " + sourceOrTarget + " filename was not supplied");
        }
        return filename;
    }

    private void save(MultipartFile multipartFile, File file)
    {
        try
        {
            Files.copy(multipartFile.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new TransformException(507, "Failed to store the source file", e);
        }
    }

    private void save(Resource body, File file)
    {
        try
        {
            InputStream inputStream = body == null ? null : body.getInputStream();
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new TransformException(507, "Failed to store the source file", e);
        }
    }


    private Resource load(File file)
    {
        try
        {
            Resource resource = new UrlResource(file.toURI());
            if (resource.exists() || resource.isReadable())
            {
                return resource;
            }
            else
            {
                throw new TransformException(500, "Could not read the target file: " + file.getPath());
            }
        }
        catch (MalformedURLException e)
        {
            throw new TransformException(500, "The target filename was malformed: " + file.getPath(), e);
        }
    }

    public void callTransform(File sourceFile, File targetFile, String... args) throws TransformException
    {
        args = buildArgs(sourceFile, targetFile, args);
        try
        {
            callTransform(args);
        }
        catch (IllegalArgumentException e)
        {
            throw new TransformException(400, getMessage(e));
        }
        catch (Exception e)
        {
            throw new TransformException(500, getMessage(e));
        }
        if (!targetFile.exists() || targetFile.length() == 0)
        {
            throw new TransformException(500, "Transformer failed to create an output file");
        }
    }

    private String getMessage(Exception e)
    {
        return e.getMessage() == null ? e.getClass().getSimpleName(): e.getMessage();
    }

    protected void callTransform(String[] args)
    {
        // Overridden when the transform is done in the JVM rather than in an external command.
    }

    protected String[] buildArgs(File sourceFile, File targetFile, String[] args)
    {
        ArrayList<String> methodArgs = new ArrayList<>(args.length+2);
        StringJoiner sj = new StringJoiner(" ");
        for (String arg: args)
        {
            addArg(methodArgs, sj, arg);
        }

        addFileArg(methodArgs, sj, sourceFile);
        addFileArg(methodArgs, sj, targetFile);

        LogEntry.setOptions(sj.toString());

        return methodArgs.toArray(new String[methodArgs.size()]);
    }

    private void addArg(ArrayList<String> methodArgs, StringJoiner sj, String arg)
    {
        if (arg != null)
        {
            sj.add(arg);
            methodArgs.add(arg);
        }
    }

    private void addFileArg(ArrayList<String> methodArgs, StringJoiner sj, File arg)
    {
        if (arg != null)
        {
            String path = arg.getAbsolutePath();
            int i = path.lastIndexOf('.');
            String ext = i == -1 ? "???" : path.substring(i+1);
            sj.add(ext);
            methodArgs.add(path);
        }
    }

    protected void executeTransformCommand(String options, File sourceFile, File targetFile, Long timeout)
    {
        LogEntry.setOptions(options);

        Map<String, String> properties = new HashMap<String, String>(5);
        properties.put("options", options);
        properties.put("source", sourceFile.getAbsolutePath());
        properties.put("target", targetFile.getAbsolutePath());

        executeTransformCommand(properties, targetFile, timeout);
    }

    public void executeTransformCommand(Map<String, String> properties, File targetFile, Long timeout)
    {
        timeout = timeout != null && timeout > 0 ? timeout : 0;
        RuntimeExec.ExecutionResult result = transformCommand.execute(properties, timeout);

        if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0)
        {
            throw new TransformException(400, "Transformer exit code was not 0: \n" + result.getStdErr());
        }

        if (!targetFile.exists() || targetFile.length() == 0)
        {
            throw new TransformException(500, "Transformer failed to create an output file");
        }
    }

    protected ResponseEntity<Resource> createAttachment(String targetFilename, File targetFile, Long testDelay)
    {
        Resource targetResource = load(targetFile);
        targetFilename = UriUtils.encodePath(StringUtils.getFilename(targetFilename), "UTF-8");
        ResponseEntity<Resource> body = ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*= UTF-8''" + targetFilename).body(targetResource);
        LogEntry.setTargetSize(targetFile.length());
        long time = LogEntry.setStatusCodeAndMessage(200, "Success");
        time += LogEntry.addDelay(testDelay);
        getProbeTestTransformInternal().recordTransformTime(time);
        return body;
    }

    /**
     * Safely converts a {@link String} to an {@link Integer}
     *
     * @param param String to be converted
     * @return Null if param is null or converted value as {@link Integer}
     */
    protected Integer stringToInteger(String param)
    {
        return param == null ? null : Integer.parseInt(param);
    }

    /**
     * Safely converts a {@link String} to an {@link Integer}
     *
     * @param param String to be converted
     * @return Null if param is null or converted value as {@link Boolean}
     */
    protected Boolean stringToBoolean(String param)
    {
        return param == null? null : Boolean.parseBoolean(param);
    }

    public AlfrescoSharedFileStoreClient getAlfrescoSharedFileStoreClient()
    {
        return alfrescoSharedFileStoreClient;
    }

    public void setAlfrescoSharedFileStoreClient(
        AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient)
    {
        this.alfrescoSharedFileStoreClient = alfrescoSharedFileStoreClient;
    }
}
