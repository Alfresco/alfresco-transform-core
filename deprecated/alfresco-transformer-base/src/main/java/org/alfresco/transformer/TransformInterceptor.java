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
package org.alfresco.transformer;

import static org.alfresco.transformer.fs.FileManager.SOURCE_FILE;
import static org.alfresco.transformer.fs.FileManager.TARGET_FILE;
import static org.alfresco.transformer.fs.FileManager.deleteFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.alfresco.transformer.logging.LogEntry;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 *
 * TransformInterceptor
 * <br/>
 * Handles ThreadLocal Log entries for each request.
 */
@Deprecated
public class TransformInterceptor implements AsyncHandlerInterceptor
{
    @Override
    public boolean preHandle(HttpServletRequest request,
        HttpServletResponse response, Object handler)
    {
        LogEntry.start();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
        HttpServletResponse response, Object handler, Exception ex)
    {
        // TargetFile cannot be deleted until completion, otherwise 0 bytes are sent.
        deleteFile(request, SOURCE_FILE);
        deleteFile(request, TARGET_FILE);

        LogEntry.complete();
    }
}
