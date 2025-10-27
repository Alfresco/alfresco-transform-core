/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.tika.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExifToolParserTest
{

    ExifToolParser exifToolParser = new ExifToolParser();

    @Test
    public void testFindSeparator()
    {

        String testCommand = "env FOO=${OUTPUT} exiftool -args -G1 " + ExifToolParser.SEPARATOR_SETTING
                + " \"|||\" ${INPUT}";
        String expected = "|||";
        String actual = exifToolParser.findSeparator(testCommand);
        assertEquals(expected, actual);

        expected = "TESTWITHOUTQUOTES";
        testCommand = "nothing matters until the " + ExifToolParser.SEPARATOR_SETTING + " " + expected
                + " now all this extra should be ignored";
        actual = exifToolParser.findSeparator(testCommand);
        assertEquals(expected, actual);

        expected = "Test something bonkers 112!£$%^£$^";
        testCommand = ExifToolParser.SEPARATOR_SETTING + " \"" + expected + "\"";
        actual = exifToolParser.findSeparator(testCommand);
        assertEquals(expected, actual);

    }

}
