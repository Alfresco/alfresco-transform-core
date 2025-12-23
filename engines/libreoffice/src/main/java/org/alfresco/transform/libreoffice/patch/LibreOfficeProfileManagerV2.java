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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibreOfficeProfileManagerV2
{
    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeProfileManagerV2.class);

    private final String TEMP_PROFILE_DIR_NAME = "libreoffice-tempProfileDir";
    private final String USER_DIR_NAME = "user";
    private final String REGISTRY_FILE_NAME = "registrymodifications.xcu";
    private final String LOCAL_TEMP_REGISTRY_FILE = "templateRegistrymodifications.xcu";

    private final String userTemplateDir;
    private String systemTempUserDir = "";
    private final boolean blockUntrustedRefererLinks;

    public LibreOfficeProfileManagerV2(String templateProfileDir, boolean blockUntrustedRefererLinks)
    {
        this.userTemplateDir = templateProfileDir;
        this.blockUntrustedRefererLinks = blockUntrustedRefererLinks;
    }

    public String getTemplateProfileDir()
    {
        execute();

        if (StringUtils.isNotBlank(userTemplateDir))
        {
            return userTemplateDir;
        }
        else if (StringUtils.isNotBlank(systemTempUserDir))
        {
            return systemTempUserDir;
        }
        else
        {
            return "";
        }
    }

    private void execute()
    {
        if (StringUtils.isBlank(userTemplateDir))
        {
            validateAndCreateRegistryTemplate();
        }
        else
        {
            checkUserProvidedRegistry();
        }
    }

    private void validateAndCreateRegistryTemplate()
    {
        if (blockUntrustedRefererLinks)
        {
            try (InputStream regStream = getClass().getClassLoader().getResourceAsStream(LOCAL_TEMP_REGISTRY_FILE))
            {
                if (regStream == null)
                {
                    logger.error("Local temporary registry file not found: {}", LOCAL_TEMP_REGISTRY_FILE);
                    return;
                }
                Path tempProfilePath = Files.createTempDirectory(TEMP_PROFILE_DIR_NAME);
                File registryFile = getRegistryFile(tempProfilePath);
                Files.copy(regStream, registryFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                this.systemTempUserDir = tempProfilePath.toString();
            }
            catch (Exception e)
            {
                logger.error("Error creating temporary directory for LibreOffice profile", e);
            }
        }
    }

    private File getRegistryFile(Path tempProfilePath)
    {
        File userDir = new File(tempProfilePath.toFile(), USER_DIR_NAME);
        if (!userDir.exists())
        {
            boolean dirCreated = userDir.mkdirs();
            if (!dirCreated)
            {
                throw new RuntimeException("Failed to create user directory: " + userDir.getAbsolutePath());
            }
        }
        return new File(userDir, REGISTRY_FILE_NAME);
    }

    private void checkUserProvidedRegistry()
    {
        File tempDir = new File(userTemplateDir);
        if (!tempDir.exists() || !tempDir.isDirectory())
        {
            logger.warn("The provided template profile directory does not exist or is not a directory: {}", userTemplateDir);
            return;
        }
        File userDir = new File(tempDir, USER_DIR_NAME);
        if (!userDir.exists())
        {
            logger.warn("The user directory does not exist in the provided template profile directory: {}", userDir.getAbsolutePath());
            return;
        }
        File registryFile = new File(userDir, REGISTRY_FILE_NAME);
        if (!registryFile.exists())
        {
            logger.warn("The registrymodifications.xcu file does not exist in the provided template profile directory: {}", registryFile.getAbsolutePath());
        }
        else
        {
            // TODO: read registryModifications.xcu and check for blocking referer links setting

        }
    }

}
