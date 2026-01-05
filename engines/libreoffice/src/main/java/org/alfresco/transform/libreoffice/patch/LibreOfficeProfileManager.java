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

package org.alfresco.transform.libreoffice.patch;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Manages LibreOffice user profile templates for transformations.
 * 
 * @author Sayan Bhattacharya
 */
public class LibreOfficeProfileManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LibreOfficeProfileManager.class);

    private static final String USER_DIR_NAME = "user";
    private static final String REGISTRY_FILE_NAME = "registrymodifications.xcu";
    private static final String DEFAULT_LO_TEMPLATE_PROFILE = "libreoffice_templateProfile";

    private String classPathRegistryFile;

    public String getEffectiveTemplateProfileDir(String templateProfileDir)
    {
        if (StringUtils.startsWith(templateProfileDir, "classpath:"))
        {
            createTemplateProfileDir(templateProfileDir);
        }
        else if (StringUtils.isNotBlank(templateProfileDir))
        {
            checkUserProvidedRegistry(templateProfileDir);
        }
        else
        {
            LOGGER.warn("No template profile directory provided, using default settings.");
        }

        return StringUtils.isBlank(classPathRegistryFile) ? templateProfileDir : classPathRegistryFile;
    }

    private void createTemplateProfileDir(String classpathTemplateDir)
    {
        try
        {

            String baseDir = classpathTemplateDir.replace("classpath:", ""); // root folder on classpath

            Path tempDir = Files.createTempDirectory(DEFAULT_LO_TEMPLATE_PROFILE);
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(classpathTemplateDir + "/**");

            for (Resource resource : resources)
            {

                // skip directories or empty resources
                if (!resource.isReadable() || resource.contentLength() == 0)
                {
                    continue;
                }

                // get the path relative to the base folder
                String url = resource.getURL().toString();
                String relative = url.substring(url.indexOf(baseDir) + baseDir.length() + 1);

                Path target = tempDir.resolve(relative);
                Files.createDirectories(target.getParent());

                try (InputStream in = resource.getInputStream())
                {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            this.classPathRegistryFile = tempDir.toString();

        }
        catch (Exception e)
        {
            LOGGER.error("Error creating temporary directory for LibreOffice profile", e);
        }
    }

    private void checkUserProvidedRegistry(String templateProfileDir)
    {
        File templateDir = new File(templateProfileDir);
        if (!templateDir.exists() || !templateDir.isDirectory())
        {
            LOGGER.warn("The provided template profile directory does not exist or is not a directory: {}", templateProfileDir);
            return;
        }
        File userDir = new File(templateDir, USER_DIR_NAME);
        if (!userDir.exists())
        {
            LOGGER.warn("The user directory does not exist in the provided template profile directory: {}", userDir.getAbsolutePath());
            return;
        }
        File registryFile = new File(userDir, REGISTRY_FILE_NAME);
        if (!registryFile.exists())
        {
            LOGGER.warn("The registrymodifications.xcu file does not exist in the provided template profile directory: {}", registryFile.getAbsolutePath());
        }
        else
        {
            checkBlockUntrustedRefererLinks(registryFile);
        }
    }

    private void checkBlockUntrustedRefererLinks(File registryFile)
    {
        try
        {
            String content = Files.readString(registryFile.toPath(), StandardCharsets.UTF_8);

            boolean hasBlockUntrustedProperty = content.contains("oor:path=\"/org.openoffice.Office.Common/Security/Scripting\"")
                    && content.contains("oor:name=\"BlockUntrustedRefererLinks\"");

            if (hasBlockUntrustedProperty)
            {
                boolean isEnabled = content.contains("<prop oor:name=\"BlockUntrustedRefererLinks\"")
                        && content.contains("<value>true</value>");

                if (!isEnabled)
                {
                    LOGGER.warn("BlockUntrustedRefererLinks is present but not set to 'true' in the registry file: {}", registryFile.getAbsolutePath());
                }
            }
            else
            {
                LOGGER.warn("BlockUntrustedRefererLinks property not found in the registry file: {}", registryFile.getAbsolutePath());
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error reading registry file: {}", registryFile.getAbsolutePath(), e);
        }
    }

}
