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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages LibreOffice template user profile and work directory, including apply patches to disable external link updates.
 * 
 * @author Sayan Bhattacharya
 */

public class LibreOfficeProfileManager
{

    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeProfileManager.class);
    private static final String PROBE_RESOURCE = "/probe.doc";
    private static final String PATCH_RESOURCE = "/disable-external-link-patch.txt";
    private static final String REGISTRY_FILE = "registrymodifications.xcu";
    private static final String USER_PROFILE_DIR = "user";

    private final File workDir;
    private final File templateProfileDir;
    private final OfficeManager officeManager;
    private final boolean disableExternalLinks;

    public LibreOfficeProfileManager(File workDir, File templateProfileDir,
            OfficeManager officeManager, boolean disableExternalLinks)
    {
        this.workDir = workDir;
        this.templateProfileDir = templateProfileDir;
        this.officeManager = officeManager;
        this.disableExternalLinks = disableExternalLinks;
    }

    public void setupTemplateUserProfile() throws Exception
    {
        OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);
        convertProbeDocument(converter);
        copyUserProfile();

        if (disableExternalLinks)
        {
            patchLibreOfficeRegistry();
        }
    }

    /**
     * Performs a probe document conversion to ensure the user profile is created
     * 
     * @param converter
     * @throws Exception
     */
    private void convertProbeDocument(OfficeDocumentConverter converter) throws Exception
    {
        InputStream probeInput = getClass().getResourceAsStream(PROBE_RESOURCE);
        if (probeInput == null)
        {
            throw new IllegalStateException("probe.docx resource not found!");
        }

        File tempProbeFile = new File(workDir, "probe.doc");
        FileUtils.copyInputStreamToFile(probeInput, tempProbeFile);
        converter.convert(tempProbeFile, new File(workDir, "probeoutput.pdf"));
    }

    private void copyUserProfile() throws Exception
    {
        File officeUserProfile = findLibreOfficeUserProfile(workDir);
        if (officeUserProfile != null)
        {
            File destination = new File(templateProfileDir, officeUserProfile.getName());
            FileUtils.copyDirectory(officeUserProfile, destination);
        }
    }

    /**
     * Patches the LibreOffice user profile to disable external link updates.
     * 
     * @throws Exception
     */
    private void patchLibreOfficeRegistry() throws Exception
    {
        File userProfileDir = findLibreOfficeUserProfile(templateProfileDir);
        if (userProfileDir == null)
        {
            throw new IllegalStateException("Cannot find LO user profile to patch");
        }

        File registry = new File(userProfileDir, REGISTRY_FILE);
        if (!registry.exists())
        {
            throw new IllegalStateException(REGISTRY_FILE + " not found!");
        }

        String registryContent = FileUtils.readFileToString(registry, StandardCharsets.UTF_8);
        List<PatchItem> patchItems = readPatchItemsFromJson();

        // Remove existing matching items
        for (PatchItem item : patchItems)
        {
            String pattern = String.format(
                    "<item oor:path=\"%s\"><prop oor:name=\"%s\"[^>]*>.*?</prop></item>",
                    Pattern.quote(item.path),
                    Pattern.quote(item.propName));
            registryContent = registryContent.replaceAll(pattern, "");
        }

        // Insert the new patch before closing tag
        String patch = generatePatchXml(patchItems);
        registryContent = registryContent.replace("</oor:items>", "  " + patch + "\n</oor:items>");

        FileUtils.writeStringToFile(registry, registryContent, StandardCharsets.UTF_8);
    }

    // private String readPatchFromResource() throws Exception
    // {
    // InputStream inputStream = getClass().getResourceAsStream(PATCH_RESOURCE);
    // if (inputStream == null)
    // {
    // throw new IllegalStateException("disable-external-link-patch.txt resource not found!");
    // }
    // return IOUtils.toString(inputStream, StandardCharsets.UTF_8).trim();
    // }

    private File findLibreOfficeUserProfile(File dir)
    {
        File userDir = findDirectChild(dir, USER_PROFILE_DIR);
        if (userDir != null)
        {
            return userDir;
        }
        return findUserInJodConverterDirs(dir);
    }

    private File findDirectChild(File dir, String name)
    {
        File[] children = dir.listFiles(File::isDirectory);
        if (children != null)
        {
            for (File child : children)
            {
                if (name.equals(child.getName()))
                {
                    return child;
                }
            }
        }
        return null;
    }

    private File findUserInJodConverterDirs(File dir)
    {
        File[] jodDirs = dir.listFiles(f -> f.isDirectory() && f.getName().startsWith(".jodconverter_"));
        if (jodDirs == null)
        {
            return null;
        }

        for (File d : jodDirs)
        {
            File user = new File(d, "user");
            if (user.exists())
            {
                return user;
            }
        }
        return null;
    }

    private List<PatchItem> readPatchItemsFromJson() throws Exception
    {
        InputStream inputStream = getClass().getResourceAsStream("/libreoffice_registry_patch.json");
        if (inputStream == null)
        {
            throw new IllegalStateException("libreoffice_registry_patch.json not found!");
        }

        String jsonContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonContent);

        JsonNode items = root.get("items");
        if (items == null || !items.isArray() || items.size() == 0)
        {
            throw new IllegalStateException("JSON 'items' array is missing or empty!");
        }

        List<PatchItem> patchItems = new ArrayList<>();
        for (JsonNode item : items)
        {
            String path = item.get("oor:path").asText();
            JsonNode prop = item.get("prop");
            String name = prop.get("oor:name").asText();
            String op = prop.get("oor:op").asText();
            boolean value = item.get("value").asBoolean();

            patchItems.add(new PatchItem(path, name, op, value));
        }

        return patchItems;
    }

    private String generatePatchXml(List<PatchItem> items)
    {
        StringBuilder xml = new StringBuilder();
        for (PatchItem item : items)
        {
            xml.append("<item oor:path=\"").append(item.path).append("\">");
            xml.append("<prop oor:name=\"").append(item.propName).append("\" oor:op=\"").append(item.op).append("\">");
            xml.append("<value>").append(item.value).append("</value>");
            xml.append("</prop></item>");
        }
        return xml.toString();
    }

    private class PatchItem
    {
        String path;
        String propName;
        String op;
        boolean value;

        PatchItem(String path, String propName, String op, boolean value)
        {
            this.path = path;
            this.propName = propName;
            this.op = op;
            this.value = value;
        }
    }
}
