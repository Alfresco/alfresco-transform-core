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
