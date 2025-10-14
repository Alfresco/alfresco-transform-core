/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2025 Alfresco Software Limited
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
package org.alfresco.transform.imagemagick.transformers.page;

import static org.alfresco.transform.base.util.Util.stringToInteger;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_BMP;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JP2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_XWD;
import static org.alfresco.transform.common.RequestParamMap.END_PAGE;
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PageRangeFactory
{
    private final List<String> singlePageFormats = List.of(MIMETYPE_IMAGE_BMP, MIMETYPE_IMAGE_JP2, MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_PNG, MIMETYPE_IMAGE_XWD);

    public String create(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions)
    {
        String startPageString = transformOptions.get(START_PAGE);
        String endPageString = transformOptions.get(END_PAGE);
        if (!singlePageFormats.contains(sourceMimetype) && singlePageFormats.contains(targetMimetype))
        {
            if (StringUtils.isEmpty(startPageString))
            {
                startPageString = "0";
            }
            if (StringUtils.isEmpty(endPageString))
            {
                endPageString = startPageString;
            }
        }
        Integer startPage = stringToInteger(startPageString);
        Integer endPage = stringToInteger(endPageString);
        return calculatePageRange(startPage, endPage);
    }

    private String calculatePageRange(Integer startPage, Integer endPage)
    {
        return startPage == null
                ? endPage == null
                        ? ""
                        : "[" + endPage + ']'
                : endPage == null || startPage.equals(endPage)
                        ? "[" + startPage + ']'
                        : "[" + startPage + '-' + endPage + ']';
    }
}
