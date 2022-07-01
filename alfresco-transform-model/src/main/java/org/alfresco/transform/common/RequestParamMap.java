/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transform.common;

import org.alfresco.transform.config.CoreVersionDecorator;

/**
 * Request parameters and transform options used in the core transformers.
 */
public interface RequestParamMap
{
    // html parameter names
    String FILE = "file";
    String SOURCE_EXTENSION         = "sourceExtension";
    String TARGET_EXTENSION         = "targetExtension";
    String SOURCE_MIMETYPE          = "sourceMimetype";
    String TARGET_MIMETYPE          = "targetMimetype";

    // Transform options used in the core transformers.
    String SOURCE_ENCODING          = "sourceEncoding";
    String TARGET_ENCODING          = "targetEncoding";
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

    // Parameters interpreted by the AbstractTransformerController
    String DIRECT_ACCESS_URL       = "directAccessUrl";

    // An optional parameter (defaults to 1) to be included in the request to the t-engine {@code /transform/config}
    // endpoint to specify what version (of the schema) to return. Provides the flexibility to introduce changes
    // without getting deserialization issues when we have components at different versions.
    String CONFIG_VERSION          = "configVersion";
    String CONFIG_VERSION_DEFAULT  = "1";
    int    CONFIG_VERSION_LATEST   = CoreVersionDecorator.CONFIG_VERSION_INCLUDES_CORE_VERSION;

    // Endpoints
    String ENDPOINT_TRANSFORM = "/transform";
    String ENDPOINT_TRANSFORM_CONFIG = "/transform/config";
    String ENDPOINT_TRANSFORM_CONFIG_LATEST = ENDPOINT_TRANSFORM_CONFIG + "?" + CONFIG_VERSION + "=" + CONFIG_VERSION_LATEST;
    String ENDPOINT_TRANSFORM_LOG = "/log";
    String ENDPOINT_TRANSFORM_TEST = "/";
}
