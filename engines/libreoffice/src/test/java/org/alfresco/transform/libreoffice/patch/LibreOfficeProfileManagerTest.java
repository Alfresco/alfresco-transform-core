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

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test cases for LibreOfficeProfileManager
 * 
 * @author Sayan Bhattacharya
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
    void testGetEffectiveTemplateProfileDir_WithDefaultMarkerCreatesTempDir()
    {
        LibreOfficeProfileManager profileManager = new LibreOfficeProfileManager("alfresco_default");
        String result = profileManager.getEffectiveTemplateProfileDir();
        assertNotNull(result);
        assertFalse(result.isBlank());
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
}
