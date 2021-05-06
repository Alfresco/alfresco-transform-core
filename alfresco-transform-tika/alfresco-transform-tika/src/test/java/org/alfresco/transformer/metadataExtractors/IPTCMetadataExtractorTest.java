/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.transformer.metadataExtractors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class IPTCMetadataExtractorTest {

    IPTCMetadataExtractor extractor = new IPTCMetadataExtractor();

    @Test
    public void testIptcToIso8601DateStrings() {
        String[] testStrings = { "1890:01:01", "1901:02:01 00:00:00.000Z", "1901-02-01 00:00:00.000Z",
                "1901-02-01T00:00:00.000Z", "1901:02:01T00:00+00:00", "1901:02:01 00:00+00:00" };
        String[] expected = { "1890-01-01", "1901-02-01T00:00:00.000Z", "1901-02-01T00:00:00.000Z",
                "1901-02-01T00:00:00.000Z", "1901-02-01T00:00+00:00", "1901-02-01T00:00+00:00" };

        assertArrayEquals(expected, extractor.iptcToIso8601DateStrings(testStrings));

    }

}
