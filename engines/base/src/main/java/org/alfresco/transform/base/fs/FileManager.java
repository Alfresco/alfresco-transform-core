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
import java.net.URI;
import java.net.URISyntaxException;
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

@SuppressWarnings("PMD.GodClass")
public class FileManager
{
    public static final String SOURCE_FILE = "sourceFile";
    public static final String TARGET_FILE = "targetFile";

    private FileManager()
    {}

    static File assertContained(File candidate, File parent)
    {
        try
        {
            String candidateCanonical = candidate.getCanonicalPath();
            String parentCanonical = parent.getCanonicalPath();
            if (!candidateCanonical.equals(parentCanonical)
                    && !candidateCanonical.startsWith(parentCanonical + File.separator))
            {
                throw new TransformException(BAD_REQUEST, "The resolved path escapes the temp directory");
            }
            return new File(candidateCanonical);
        }
        catch (IOException e)
        {
            throw new TransformException(BAD_REQUEST, "Unable to resolve canonical path", e);
        }
    }

    public static File assertWithinTempDir(File file)
    {
        if (file == null)
        {
            return null;
        }
        return assertContained(file, new File(System.getProperty("java.io.tmpdir")));
    }

    public static File createSourceFile(HttpServletRequest request, InputStream inputStream, String sourceMimetype, String sourceFileName)
    {
        try
        {
            String extension = "." + getExtensionForMimetype(sourceMimetype);
            File file = StringUtils.isEmpty(sourceFileName)
                    ? TempFileProvider.createTempFile("source_", extension)
                    : TempFileProvider.createFileWithinUUIDTempDir(sourceFileName);

            File safeFile = assertContained(file, file.getParentFile());
            Files.copy(inputStream, safeFile.toPath(), REPLACE_EXISTING);

            if (request != null)
            {
                request.setAttribute(SOURCE_FILE, safeFile);
            }
            LogEntry.setSource(safeFile.getName(), safeFile.length());
            return safeFile;
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
            File raw = TempFileProvider.createTempFile("target_", extension);
            File file = assertContained(raw, raw.getParentFile());
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
            java.net.URL url = new java.net.URL(directUrl);
            String protocol = url.getProtocol();
            if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
            {
                String host = url.getHost();
                if (host == null || !host.matches("[A-Za-z0-9._\\-]+"))
                {
                    throw new TransformException(BAD_REQUEST, "Direct Access Url host is not allowed.");
                }
                return new URI(protocol, null, host, url.getPort(),
                        url.getPath(), url.getQuery(), null).toURL().openStream();
            }
            if ("file".equalsIgnoreCase(protocol))
            {
                File f = assertWithinTempDir(new File(url.toURI()));
                return Files.newInputStream(f.toPath());
            }
            throw new TransformException(BAD_REQUEST, "Direct Access Url protocol is not allowed.");
        }
        catch (URISyntaxException | IllegalArgumentException | MalformedURLException e)
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
            String baseName = new File(sourceFileName == null ? "" : sourceFileName).getName();
            if (baseName.isEmpty())
            {
                throw new TransformException(BAD_REQUEST, "The source filename was not supplied");
            }
            return assertContained(new File(tempDir, baseName), tempDir);
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
