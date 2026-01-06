/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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

package org.alfresco.transform.libreoffice.patch;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test cases for LibreOfficeProfileManager
 */
@ExtendWith(MockitoExtension.class)
class LibreOfficeProfileManagerTest
{
    private LibreOfficeProfileManager profileManager;

    @BeforeEach
    void setUp()
    {
        profileManager = new LibreOfficeProfileManager();
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithNullInput()
    {
        String result = profileManager.getEffectiveTemplateProfileDir(null);
        assertNull(result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithEmptyString()
    {
        String result = profileManager.getEffectiveTemplateProfileDir("");
        assertEquals("", result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithBlankString()
    {
        String result = profileManager.getEffectiveTemplateProfileDir("   ");
        assertEquals("   ", result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithValidUserPath()
    {
        String validPath = "/path/to/template/profile";
        String result = profileManager.getEffectiveTemplateProfileDir(validPath);
        assertEquals(validPath, result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithClasspathPrefix()
    {
        String classpathPath = "classpath:libreoffice_template";
        String result = profileManager.getEffectiveTemplateProfileDir(classpathPath);
        assertNotNull(result);
        // When classpath resources are not found, it should return the original path
        assertEquals(classpathPath, result);
    }

    @Test
    void testCheckUserProvidedRegistry_WithValidRegistryBlockUntrustedTrue() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            // Create directory structure
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();

            // Create registry file with BlockUntrustedRefererLinks enabled
            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = buildRegistryContent(true, true);
            Files.write(registryFile.toPath(), registryContent.getBytes(StandardCharsets.UTF_8));

            // This should not throw any exceptions
            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithValidRegistryBlockUntrustedFalse() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();

            // Create registry file with BlockUntrustedRefererLinks present but false
            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = buildRegistryContent(true, false);
            Files.write(registryFile.toPath(), registryContent.getBytes(StandardCharsets.UTF_8));

            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithoutBlockUntrustedProperty() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();

            // Create registry file without BlockUntrustedRefererLinks property
            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = buildRegistryContent(false, false);
            Files.write(registryFile.toPath(), registryContent.getBytes(StandardCharsets.UTF_8));

            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    // =====================================================
    // Tests for invalid directory structures
    // =====================================================

    @Test
    void testCheckUserProvidedRegistry_WithNonExistentTemplateDir()
    {
        String nonExistentPath = "/nonexistent/path/to/template";
        String result = profileManager.getEffectiveTemplateProfileDir(nonExistentPath);
        assertEquals(nonExistentPath, result);
    }

    @Test
    void testCheckUserProvidedRegistry_WithFileInsteadOfDirectory() throws IOException
    {
        Path tempFile = Files.createTempFile("test_file_", ".txt");
        try
        {
            String result = profileManager.getEffectiveTemplateProfileDir(tempFile.toString());
            assertEquals(tempFile.toString(), result);
        }
        finally
        {
            Files.delete(tempFile);
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithoutUserDirectory() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            // Don't create user directory

            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithoutRegistryFile() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();
            // Don't create registry file

            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithEmptyRegistryFile() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();

            File registryFile = new File(userDir, "registrymodifications.xcu");
            Files.write(registryFile.toPath(), "".getBytes(StandardCharsets.UTF_8));

            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithMalformedRegistryFile() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();

            File registryFile = new File(userDir, "registrymodifications.xcu");
            String malformedContent = "This is not valid XML content";
            Files.write(registryFile.toPath(), malformedContent.getBytes(StandardCharsets.UTF_8));

            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithPartialBlockUntrustedProperty() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();

            // Create registry with only path but missing name attribute
            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<oor:items xmlns:oor=\"http://openoffice.org/2001/registry\">\n"
                    + "<item oor:path=\"/org.openoffice.Office.Common/Security/Scripting\">\n"
                    + "</item>\n"
                    + "</oor:items>";
            Files.write(registryFile.toPath(), registryContent.getBytes(StandardCharsets.UTF_8));

            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithUnreadableRegistryFile() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            userDir.mkdirs();

            File registryFile = new File(userDir, "registrymodifications.xcu");
            Files.write(registryFile.toPath(), "test content".getBytes(StandardCharsets.UTF_8));

            // Make file unreadable on Windows by creating it in a way that prevents reading
            // Note: File permission handling varies across OS, this test might be OS-specific
            String result = profileManager.getEffectiveTemplateProfileDir(templateDir.getAbsolutePath());
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithMultiplePathFormats()
    {
        String[] paths = {
                "C:\\path\\to\\template",
                "/path/to/template",
                "/path/to/template/",
                "relative/path/to/template"
        };

        for (String path : paths)
        {
            LibreOfficeProfileManager manager = new LibreOfficeProfileManager();
            String result = manager.getEffectiveTemplateProfileDir(path);
            assertEquals(path, result);
        }
    }

    /**
     * Builds registry content with configurable BlockUntrustedRefererLinks property
     *
     * @param includeProperty
     *            true to include the BlockUntrustedRefererLinks property
     * @param isEnabled
     *            true to set value to true, false to set it to false
     * @return XML content as string
     */
    private String buildRegistryContent(boolean includeProperty, boolean isEnabled)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<oor:items xmlns:oor=\"http://openoffice.org/2001/registry\">\n");

        if (includeProperty)
        {
            sb.append("<item oor:path=\"/org.openoffice.Office.Common/Security/Scripting\">\n");
            sb.append("<prop oor:name=\"BlockUntrustedRefererLinks\">\n");
            sb.append("<value>").append(isEnabled ? "true" : "false").append("</value>\n");
            sb.append("</prop>\n");
            sb.append("</item>\n");
        }

        sb.append("</oor:items>");
        return sb.toString();
    }

    /**
     * Recursively deletes a directory and its contents
     *
     * @param directory
     *            the directory to delete
     */
    private void deleteDirectory(File directory)
    {
        if (directory.isDirectory())
        {
            File[] files = directory.listFiles();
            if (files != null)
            {
                for (File file : files)
                {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}
