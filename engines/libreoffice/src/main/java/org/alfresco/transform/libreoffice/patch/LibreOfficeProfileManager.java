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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.lang3.ArrayUtils;
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
    private static final String DEFAULT_LO_TEMPLATE_PROFILE = "libreoffice_templateProfile";
    private static final String DEFAULT_TEMPLATE_MARKER = "alfresco_default";

    private final String templateProfileDir;

    public LibreOfficeProfileManager(String templateProfileDir)
    {
        this.templateProfileDir = templateProfileDir;
    }

    public String getEffectiveTemplateProfileDir()
    {
        if (DEFAULT_TEMPLATE_MARKER.equalsIgnoreCase(templateProfileDir))
        {
            return createDefaultTemplateProfileDirFromResource();
        }
        if (StringUtils.isNotBlank(templateProfileDir))
        {
            LOGGER.warn("Template profile directory found. Make sure that the BlockUntrustedRefererLinks security setting is set to true in the registrymodifications.xcu file of the LibreOffice profile.");
        }
        else
        {
            LOGGER.warn("No template profile directory provided, using default settings. Untrusted referer links are allowed. " +
                    "It is recommended to set the template profile directory to `{}` to prevent Blind SSRF risks.", DEFAULT_TEMPLATE_MARKER);
        }
        return templateProfileDir;
    }

    private String createDefaultTemplateProfileDirFromResource()
    {
        try
        {
            String baseDir = "templateProfileDir";
            Resource[] resources = loadResources("classpath:" + baseDir);
            if (ArrayUtils.isEmpty(resources))
            {
                return null;
            }

            Path tempDir = Files.createTempDirectory(DEFAULT_LO_TEMPLATE_PROFILE);
            for (Resource resource : resources)
            {
                // skip non-readable or empty resources
                if (!resource.isReadable() || resource.contentLength() == 0)
                {
                    continue;
                }

                String relative = resolveRelativePath(resource, baseDir);
                if (StringUtils.isBlank(relative))
                {
                    continue;
                }
                copyResource(resource, tempDir.resolve(relative));
            }
            return tempDir.toString();
        }
        catch (IOException e)
        {
            LOGGER.warn("Error creating temporary directory for LibreOffice profile. {}", e.getMessage());
            return null;
        }
    }

    private Resource[] loadResources(String classpathTemplateDir)
    {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try
        {
            resources = resolver.getResources(classpathTemplateDir + "/**");
        }
        catch (Exception e)
        {
            LOGGER.warn("No resources found for classpath: {}.\n {}", classpathTemplateDir, e.getMessage());
            return new Resource[0];
        }
        if (ArrayUtils.isEmpty(resources))
        {
            LOGGER.warn("No resources found for classpath: {}", classpathTemplateDir);
        }
        return resources;
    }

    private String resolveRelativePath(Resource resource, String baseDir)
    {
        try
        {
            String url = resource.getURL().toString();
            if (!url.contains(baseDir))
            {
                LOGGER.warn("Base directory '{}' not found in resource URL '{}'. Skipping.", baseDir, url);
                return null;
            }
            int baseIndex = url.indexOf(baseDir);
            String relative = url.substring(baseIndex + baseDir.length() + 1);

            if (relative.isEmpty())
            {
                LOGGER.warn("Relative path is empty for resource URL '{}'. Skipping.", url);
                return null;
            }
            return relative;
        }
        catch (Exception e)
        {
            LOGGER.warn("Error resolving URL for resource: {}. {}", resource, e.getMessage());
            return null;
        }
    }

    private void copyResource(Resource resource, Path target)
    {
        try
        {
            Files.createDirectories(target.getParent());
            try (InputStream in = resource.getInputStream())
            {
                LOGGER.info("Creating temporary libreoffice profile file");
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Error copying resource to temporary file: {}", e.getMessage());
        }

    }

}
