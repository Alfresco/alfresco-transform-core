/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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
package org.alfresco.transformer.fs;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.StringUtils.getFilename;
import static org.springframework.util.StringUtils.getFilenameExtension;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 */
@Deprecated
public class FileManager
{
    public static final String SOURCE_FILE = "sourceFile";
    public static final String TARGET_FILE = "targetFile";
    private static final String FILENAME = "filename=";

    /**
     * Returns a File to be used to store the result of a transformation.
     *
     * @param request
     * @param filename The targetFilename supplied in the request. Only the filename if a path is used as part of the
     *                 temporary filename.
     * @return a temporary File.
     * @throws TransformException if there was no target filename.
     */
    public static File createTargetFile(HttpServletRequest request, String filename)
    {
        File file = buildFile(filename);
        request.setAttribute(TARGET_FILE, file);
        return file;
    }

    public static File buildFile(String filename)
    {
        filename = checkFilename(false, filename);
        LogEntry.setTarget(filename);
        return TempFileProvider.createTempFile("target_", "_" + filename);
    }

    public static void deleteFile(final File file) throws Exception
    {
        if (!file.delete())
        {
            throw new Exception("Failed to delete file");
        }
    }

    /**
     * Checks the filename is okay to uses in a temporary file name.
     *
     * @param filename or path to be checked.
     * @return the filename part of the supplied filename if it was a path.
     * @throws TransformException if there was no target filename.
     */
    private static String checkFilename(boolean source, String filename)
    {
        filename = getFilename(filename);
        if (filename == null || filename.isEmpty())
        {
            String sourceOrTarget = source ? "source" : "target";
            HttpStatus statusCode = source ? BAD_REQUEST : INTERNAL_SERVER_ERROR;
            throw new TransformException(statusCode, "The " + sourceOrTarget + " filename was not supplied");
        }
        return filename;
    }

    private static void save(MultipartFile multipartFile, File file)
    {
        try
        {
            Files.copy(multipartFile.getInputStream(), file.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new TransformException(INSUFFICIENT_STORAGE, "Failed to store the source file", e);
        }
    }

    public static void save(Resource body, File file)
    {
        try
        {
            Files.copy(body.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new TransformException(INSUFFICIENT_STORAGE, "Failed to store the source file", e);
        }
    }

    private static Resource load(File file)
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
                throw new TransformException(INTERNAL_SERVER_ERROR,
                    "Could not read the target file: " + file.getPath());
            }
        }
        catch (MalformedURLException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR,
                "The target filename was malformed: " + file.getPath(), e);
        }
    }

    public static String getFilenameFromContentDisposition(HttpHeaders headers)
    {
        String filename = "";
        String contentDisposition = headers.getFirst(CONTENT_DISPOSITION);
        if (contentDisposition != null)
        {
            String[] strings = contentDisposition.split("; *");
            filename = Arrays.stream(strings)
                             .filter(s -> s.startsWith(FILENAME))
                             .findFirst()
                             .map(s -> s.substring(FILENAME.length()))
                             .orElse("");
        }
        return filename;
    }

    /**
     * Returns the file name for the target file
     *
     * @param fileName        Desired file name
     * @param targetExtension File extension
     * @return Target file name
     */
    public static String createTargetFileName(final String fileName, final String targetExtension)
    {
        final String sourceFilename = getFilename(fileName);

        if (sourceFilename == null || sourceFilename.isEmpty())
        {
            return null;
        }

        final String ext = getFilenameExtension(sourceFilename);

        if (ext == null || ext.isEmpty())
        {
            return sourceFilename + '.' + targetExtension;
        }

        return sourceFilename.substring(0, sourceFilename.length() - ext.length() - 1) +
               '.' + targetExtension;
    }

    /**
     * Returns a File that holds the source content for a transformation.
     *
     * @param request
     * @param multipartFile from the request
     * @return a temporary File.
     * @throws TransformException if there was no source filename.
     */
    public static File createSourceFile(HttpServletRequest request, MultipartFile multipartFile)
    {
        String filename = multipartFile.getOriginalFilename();
        long size = multipartFile.getSize();
        filename = checkFilename(true, filename);
        File file = TempFileProvider.createTempFile("source_", "_" + filename);
        request.setAttribute(SOURCE_FILE, file);
        save(multipartFile, file);
        LogEntry.setSource(filename, size);
        return file;
    }

    public static void deleteFile(HttpServletRequest request, String attributeName)
    {
        File file = (File) request.getAttribute(attributeName);
        if (file != null)
        {
            file.delete();
        }
    }

    public static ResponseEntity<Resource> createAttachment(String targetFilename, File
        targetFile)
    {
        Resource targetResource = load(targetFile);
        targetFilename = UriUtils.encodePath(getFilename(targetFilename), "UTF-8");
        return ResponseEntity.ok().header(CONTENT_DISPOSITION,
            "attachment; filename*=UTF-8''" + targetFilename).body(targetResource);
    }

    /**
     * TempFileProvider - Duplicated and adapted from alfresco-core.
     */
    public static class TempFileProvider
    {
        public static File createTempFile(final String prefix, final String suffix)
        {
            final File directory = getTempDir();
            try
            {
                return File.createTempFile(prefix, suffix, directory);
            }
            catch (IOException e)
            {
                throw new RuntimeException(
                    "Failed to created temp file: \n   prefix: " + prefix +
                    "\n   suffix: " + suffix + "\n   directory: " + directory, e);
            }
        }

        private static File getTempDir()
        {
            final String dirName = "Alfresco";
            final String systemTempDirPath = System.getProperty("java.io.tmpdir");
            if (systemTempDirPath == null)
            {
                throw new RuntimeException("System property not available: java.io.tmpdir");
            }

            final File systemTempDir = new File(systemTempDirPath);
            final File tempDir = new File(systemTempDir, dirName);
            if (!tempDir.exists() && !tempDir.mkdirs() && !tempDir.exists())
            {
                throw new RuntimeException("Failed to create temp directory: " + tempDir);
            }

            return tempDir;
        }
    }
}
