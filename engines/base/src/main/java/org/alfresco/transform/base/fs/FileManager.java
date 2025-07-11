/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2025 Alfresco Software Limited
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
package org.alfresco.transform.base.fs;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.StringUtils.getFilename;

import static org.alfresco.transform.common.ExtensionService.getExtensionForMimetype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.common.ExtensionService;
import org.alfresco.transform.exceptions.TransformException;

public class FileManager
{
    public static final String SOURCE_FILE = "sourceFile";
    public static final String TARGET_FILE = "targetFile";

    private FileManager()
    {}

    public static File createSourceFile(HttpServletRequest request, InputStream inputStream, String sourceMimetype, String sourceFileName)
    {
        try
        {
            String extension = "." + getExtensionForMimetype(sourceMimetype);
            File file = StringUtils.isEmpty(sourceFileName)
                    ? TempFileProvider.createTempFile("source_", extension)
                    : TempFileProvider.createFileWithinUUIDTempDir(sourceFileName);

            Files.copy(inputStream, file.toPath(), REPLACE_EXISTING);

            if (request != null)
            {
                request.setAttribute(SOURCE_FILE, file);
            }
            LogEntry.setSource(file.getName(), file.length());
            return file;
        }
        catch (Exception e)
        {
            throw new TransformException(INSUFFICIENT_STORAGE, "Failed to store the source file", e);
        }
    }

    public static File createTargetFile(HttpServletRequest request, String sourceMimetype, String targetMimetype)
    {
        try
        {
            String extension = "." + ExtensionService.getExtensionForTargetMimetype(targetMimetype, sourceMimetype);
            File file = TempFileProvider.createTempFile("target_", extension);
            if (request != null)
            {
                request.setAttribute(TARGET_FILE, file);
            }
            LogEntry.setTarget(file.getName());
            return file;
        }
        catch (Exception e)
        {
            throw new TransformException(INSUFFICIENT_STORAGE, "Failed to create the target file", e);
        }
    }

    public static void deleteFile(final File file) throws Exception
    {
        if (!file.delete())
        {
            throw new Exception("Failed to delete file");
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

    public static InputStream getMultipartFileInputStream(MultipartFile sourceMultipartFile)
    {
        InputStream inputStream;
        if (sourceMultipartFile == null)
        {
            throw new TransformException(BAD_REQUEST, "Required request part 'file' is not present");
        }
        try
        {
            inputStream = sourceMultipartFile.getInputStream();
        }
        catch (IOException e)
        {
            throw new TransformException(BAD_REQUEST, "Unable to read the sourceMultipartFile.", e);
        }
        return inputStream;
    }

    public static InputStream getDirectAccessUrlInputStream(String directUrl)
    {
        try
        {
            return new URL(directUrl).openStream();
        }
        catch (IllegalArgumentException e)
        {
            throw new TransformException(BAD_REQUEST, "Direct Access Url is invalid.", e);
        }
        catch (IOException e)
        {
            throw new TransformException(BAD_REQUEST, "Direct Access Url not found.", e);
        }
    }

    public static void copyFileToOutputStream(File targetFile, OutputStream outputStream)
    {
        try
        {
            Files.copy(targetFile.toPath(), outputStream);
        }
        catch (IOException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, "Failed to copy targetFile to outputStream.", e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteFile(HttpServletRequest request, String attributeName)
    {
        File file = (File) request.getAttribute(attributeName);
        if (file != null)
        {
            file.delete();
        }
    }

    public static ResponseEntity<Resource> createAttachment(String targetFilename, File targetFile)
    {
        Resource targetResource = load(targetFile);
        // targetFilename should never be null (will be "transform."+<something>), so we should not worry about encodePath(null)
        targetFilename = UriUtils.encodePath(getFilename(targetFilename), "UTF-8");
        return ResponseEntity.ok().header(CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + targetFilename).body(targetResource);
    }

    /**
     * TempFileProvider - Duplicated and adapted from alfresco-core.
     */
    public static class TempFileProvider
    {
        private TempFileProvider()
        {}

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
                                "\n   suffix: " + suffix + "\n   directory: " + directory,
                        e);
            }
        }

        public static File createFileWithinUUIDTempDir(String sourceFileName)
        {
            File tempDir = new File(getTempDir(), UUID.randomUUID().toString());
            if (!tempDir.mkdirs() && !tempDir.exists())
            {
                throw new TransformException(INSUFFICIENT_STORAGE, "Failed to create temp directory: " + tempDir);
            }
            return new File(tempDir, sourceFileName);
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
