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
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PSD;
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
        if (selectsWholePsd(sourceMimetype, startPageString, endPageString))
        {
            // ImageMagick stores a merged composite at index 0 of a PSD, so "[0]" is the
            // flattened image. GraphicsMagick has no such frame -- index 0 is the first
            // layer, which for a layered PSD is typically the mask, and the transform
            // silently returns a near-blank image. Emit no range instead and let
            // ImageMagickOptionsBuilder add "-flatten", which composites every layer.
            return "";
        }
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

    /**
     * Whether this request wants the whole PSD rather than one specific layer. That covers both an absent range (the ACS default) and the explicit startPage=0/endPage=0 every image rendition sends. A request for a later layer is left alone: layer indexes are shifted by one between the two engines, and no rendition asks for one.
     */
    public static boolean selectsWholePsd(String sourceMimetype, String startPageString, String endPageString)
    {
        if (!MIMETYPE_IMAGE_PSD.equals(sourceMimetype))
        {
            return false;
        }
        return isAbsentOrFirstPage(startPageString) && isAbsentOrFirstPage(endPageString);
    }

    private static boolean isAbsentOrFirstPage(String pageString)
    {
        return StringUtils.isEmpty(pageString) || Integer.valueOf(0).equals(stringToInteger(pageString));
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
