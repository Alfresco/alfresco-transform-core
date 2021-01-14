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
package org.alfresco.transformer;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_RFC822;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_XHTML;
import static org.alfresco.transformer.TestFileInfo.testFile;

import java.util.stream.Stream;

/**
 * Metadata integration tests in the Misc T-Engine.
 *
 * @author adavis
 */
public class MiscMetadataExtractsIT extends AbstractMetadataExtractsIT
{

    @Override
    protected Stream<TestFileInfo> engineTransformations() 
    {
        return Stream.of(
                // HtmlMetadataExtractor
                testFile(MIMETYPE_HTML, "html", "quick.html"), testFile(MIMETYPE_XHTML, "xhtml", "quick.xhtml.alf"), // avoid the license header check on xhtml

                // RFC822MetadataExtractor
                testFile(MIMETYPE_RFC822, "eml", "quick.eml"),

                // Special test cases from the repo tests
                // ======================================
                testFile(MIMETYPE_RFC822, "eml", "quick.spanish.eml"),
                testFile(MIMETYPE_HTML, "html", "quick.japanese.html")

        );
    }
}
