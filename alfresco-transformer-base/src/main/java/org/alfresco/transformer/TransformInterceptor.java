/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.transformer;

import static org.alfresco.transformer.fs.FileManager.SOURCE_FILE;
import static org.alfresco.transformer.fs.FileManager.TARGET_FILE;
import static org.alfresco.transformer.fs.FileManager.deleteFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.transformer.logging.LogEntry;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * TransformInterceptor
 * <br/>
 * Handles ThreadLocal Log entries for each request.
 */
public class TransformInterceptor extends HandlerInterceptorAdapter
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
