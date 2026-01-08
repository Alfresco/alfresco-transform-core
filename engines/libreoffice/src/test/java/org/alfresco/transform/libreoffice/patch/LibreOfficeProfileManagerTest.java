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

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test cases for LibreOfficeProfileManager
 */
@RunWith(MockitoJUnitRunner.class)
class LibreOfficeProfileManagerTest
{
    @Test
    void testGetEffectiveTemplateProfileDir_WithNullInput()
    {
        LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(null);
        String result = profileManager.getEffectiveTemplateProfileDir();
        assertNull(result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithEmptyString()
    {
        LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager("");
        String result = profileManager.getEffectiveTemplateProfileDir();
        assertEquals("", result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithBlankString()
    {
        LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager("   ");
        String result = profileManager.getEffectiveTemplateProfileDir();
        assertEquals("   ", result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithValidUserPath()
    {
        String validPath = "/path/to/template/profile";
        LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(validPath);
        String result = profileManager.getEffectiveTemplateProfileDir();
        assertEquals(validPath, result);
    }

    @Test
    void testGetEffectiveTemplateProfileDir_WithClasspathPrefix()
    {
        String classpathPath = "classpath:libreoffice_template";
        LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(classpathPath);
        String result = profileManager.getEffectiveTemplateProfileDir();
        assertNotNull(result);
        // When classpath resources are not found, it should return the original path or temp dir
    }

    @Test
    void testCheckUserProvidedRegistry_WithValidRegistryBlockUntrustedTrue() throws IOException
    {
        Path tempDir = Files.createTempDirectory("test_profile_");
        try
        {
            File templateDir = tempDir.toFile();
            File userDir = new File(templateDir, "user");
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = buildRegistryContent(true, true);
            Files.writeString(registryFile.toPath(), registryContent, StandardCharsets.UTF_8);

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = buildRegistryContent(true, false);
            Files.writeString(registryFile.toPath(), registryContent, StandardCharsets.UTF_8);

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = buildRegistryContent(false, false);
            Files.writeString(registryFile.toPath(), registryContent, StandardCharsets.UTF_8);

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
            assertEquals(templateDir.getAbsolutePath(), result);
        }
        finally
        {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    void testCheckUserProvidedRegistry_WithNonExistentTemplateDir()
    {
        String nonExistentPath = "/nonexistent/path/to/template";
        LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(nonExistentPath);
        String result = profileManager.getEffectiveTemplateProfileDir();
        assertEquals(nonExistentPath, result);
    }

    @Test
    void testCheckUserProvidedRegistry_WithFileInsteadOfDirectory() throws IOException
    {
        Path tempFile = Files.createTempFile("test_file_", ".txt");
        try
        {
            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(tempFile.toString());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            File registryFile = new File(userDir, "registrymodifications.xcu");
            Files.writeString(registryFile.toPath(), "", StandardCharsets.UTF_8);

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            File registryFile = new File(userDir, "registrymodifications.xcu");
            String malformedContent = "This is not valid XML content";
            Files.writeString(registryFile.toPath(), malformedContent, StandardCharsets.UTF_8);

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            File registryFile = new File(userDir, "registrymodifications.xcu");
            String registryContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <oor:items xmlns:oor="http://openoffice.org/2001/registry">
                    <item oor:path="/org.openoffice.Office.Common/Security/Scripting">
                    </item>
                    </oor:items>""";
            Files.writeString(registryFile.toPath(), registryContent, StandardCharsets.UTF_8);

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            if (!userDir.mkdirs())
            {
                throw new IOException("Failed to create user directory");
            }

            File registryFile = new File(userDir, "registrymodifications.xcu");
            Files.writeString(registryFile.toPath(), "test content", StandardCharsets.UTF_8);

            LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager(templateDir.getAbsolutePath());
            String result = profileManager.getEffectiveTemplateProfileDir();
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
            LibreOfficeProfileManager manager = new LibreOfficeProfileManager(path);
            String result = manager.getEffectiveTemplateProfileDir();
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
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<oor:items xmlns:oor=\"http://openoffice.org/2001/registry\">\n");

        if (includeProperty)
        {
            sb.append("<item oor:path=\"/org.openoffice.Office.Common/Security/Scripting\">\n")
                    .append("<prop oor:name=\"BlockUntrustedRefererLinks\">\n")
                    .append("<value>").append(isEnabled ? "true" : "false").append("</value>\n")
                    .append("</prop>\n")
                    .append("</item>\n");
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
        // Best-effort cleanup - ignore result
        boolean ignored = directory.delete();
    }
}
