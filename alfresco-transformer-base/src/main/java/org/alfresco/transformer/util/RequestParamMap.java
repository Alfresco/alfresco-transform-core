/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.transformer.util;

public interface RequestParamMap
{
    // This property can be sent by acs repository's legacy transformers to force a transform,
    // instead of letting this T-Engine determine it based on the request parameters.
    // This allows clients to specify transform names as they appear in the engine config files, for example:
    // imagemagick, libreoffice, PdfBox, TikaAuto, ....
    // See ATS-731.
    @Deprecated
    String TRANSFORM_NAME_PROPERTY = "transformName";

    String TRANSFORM_NAME_PARAMETER = "alfresco.transform-name-parameter";
    String FILE = "file";

    String SOURCE_ENCODING          = "sourceEncoding";
    String SOURCE_EXTENSION         = "sourceExtension";
    String SOURCE_MIMETYPE          = "sourceMimetype";
    String TARGET_EXTENSION         = "targetExtension";
    String TARGET_MIMETYPE          = "targetMimetype";
    String TARGET_ENCODING          = "targetEncoding";
    String TEST_DELAY               = "testDelay";
    String PAGE_REQUEST_PARAM       = "page";    
    String WIDTH_REQUEST_PARAM      = "width";
    String HEIGHT_REQUEST_PARAM     = "height";
    String ALLOW_PDF_ENLARGEMENT    = "allowPdfEnlargement";
    String MAINTAIN_PDF_ASPECT_RATIO = "maintainPdfAspectRatio";
    String START_PAGE              = "startPage";
    String END_PAGE                = "endPage";
    String ALPHA_REMOVE            = "alphaRemove";
    String AUTO_ORIENT             = "autoOrient";
    String CROP_GRAVITY            = "cropGravity";
    String CROP_WIDTH              = "cropWidth";
    String CROP_HEIGHT             = "cropHeight";
    String CROP_PERCENTAGE         = "cropPercentage";
    String CROP_X_OFFSET           = "cropXOffset";
    String CROP_Y_OFFSET           = "cropYOffset";
    String THUMBNAIL               = "thumbnail";
    String RESIZE_WIDTH            = "resizeWidth";
    String RESIZE_HEIGHT           = "resizeHeight";
    String RESIZE_PERCENTAGE       = "resizePercentage";
    String ALLOW_ENLARGEMENT       = "allowEnlargement";
    String MAINTAIN_ASPECT_RATIO   = "maintainAspectRatio";
    String COMMAND_OPTIONS         = "commandOptions";
    String TIMEOUT                 = "timeout";
    String INCLUDE_CONTENTS        = "includeContents";
    String NOT_EXTRACT_BOOKMARKS_TEXT = "notExtractBookmarksText";
    String PAGE_LIMIT              = "pageLimit";

    // TODO PoC for FFmpeg - effectively target options (note: if we need specific source options, may need extra set)
    String TIME_OFFSET             = "timeOffset";
    String DURATION                = "duration";
    String FRAMES_NUM              = "framesNum";
    String FRAME_WIDTH             = "frameWidth";
    String FRAME_HEIGHT            = "frameHeight";
}
