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
package org.alfresco.transformer.base;

import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.logging.Log;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;

/**
 * Abstract Controller, provides structure and helper methods to sub-class transformer controllers.
 *
 * Status Codes:
 *
 *   200 Success
 *   400 Bad Request: Request parameter <name> is missing (missing mandatory parameter)
 *   400 Bad Request: Request parameter <name> is of the wrong type
 *   400 Bad Request: Transformer exit code was not 0 (possible problem with the source file)
 *   400 Bad Request: The source filename was not supplied
 *   500 Internal Server Error: (no message with low level IO problems)
 *   500 Internal Server Error: The target filename was not supplied (should not happen as targetExtension is checked)
 *   500 Internal Server Error: Transformer version check exit code was not 0
 *   500 Internal Server Error: Transformer version check failed to create any output
 *   500 Internal Server Error: Could not read the target file
 *   500 Internal Server Error: The target filename was malformed (should not happen because of other checks)
 *   500 Internal Server Error: Transformer failed to create an output file (the exit code was 0, so there should be some content)
 *   500 Internal Server Error: Filename encoding error
 *   507 Insufficient Storage: Failed to store the source file
 *
 *   408 Request Timeout         -- TODO implement general timeout mechanism rather than depend on transformer timeout (might be possible for external processes)
 *   415 Unsupported Media Type  -- TODO possibly implement a check on supported source and target mimetypes (probably not)
 *   429 Too Many Requests       -- TODO implement general throttling mechanism (needs to be done)
 */
public abstract class AbstractTransformerController
{
    public static final String SOURCE_FILE = "sourceFile";
    public static final String TARGET_FILE = "targetFile";

    protected static Log logger;

    protected RuntimeExec transformCommand;
    private RuntimeExec checkCommand;

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

        LogEntry.setStatusCodeAndMessage(statusCode, message);

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

    protected String createTargetFileName(MultipartFile sourceMultipartFile, String targetExtension)
    {
        String targetFilename = null;
        String sourceFilename = sourceMultipartFile.getOriginalFilename();
        sourceFilename = StringUtils.getFilename(sourceFilename);
        if (sourceFilename != null && !sourceFilename.isEmpty())
        {
            String ext = StringUtils.getFilenameExtension(sourceFilename);
            if (ext != null && !ext.isEmpty())
            {
                targetFilename =sourceFilename.substring(0, sourceFilename.length()-ext.length()-1)+'.'+targetExtension;
            }
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
        filename = checkFilename( false, filename);
        LogEntry.setTarget(filename);
        File file = TempFileProvider.createTempFile("target_", "_" + filename);
        request.setAttribute(TARGET_FILE, file);
        return file;
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

    protected void executeTransformCommand(Map<String, String> properties, File targetFile, Long timeout)
    {
        long timeoutMs = timeout != null && timeout > 0 ? timeout : 0;
        RuntimeExec.ExecutionResult result = transformCommand.execute(properties, timeoutMs);

        if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0)
        {
            throw new TransformException(400, "Transformer exit code was not 0: \n" + result);
        }

        if (!targetFile.exists() || targetFile.length() == 0)
        {
            throw new TransformException(500, "Transformer failed to create an output file");
        }
    }

    protected ResponseEntity<Resource> createAttachment(String targetFilename, File targetFile)
    {
        try
        {
            Resource targetResource = load(targetFile);
            targetFilename = UriUtils.encodePath(StringUtils.getFilename(targetFilename), "UTF-8");
            ResponseEntity<Resource> body = ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*= UTF-8''" + targetFilename).body(targetResource);
            LogEntry.setTargetSize(targetFile.length());
            LogEntry.setStatusCodeAndMessage(200, "Success");
            return body;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new TransformException(500, "Filename encoding error", e);
        }
    }
}
