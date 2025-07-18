/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.base.registry;

public abstract class AbstractTransformConfigSource implements TransformConfigSource
{
    private final String sortOnName;
    private final String readFrom;
    private final String baseUrl;

    protected AbstractTransformConfigSource(String sortOnName, String readFrom, String baseUrl)
    {
        this.sortOnName = sortOnName;
        this.readFrom = readFrom;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getSortOnName()
    {
        return sortOnName;
    }

    @Override
    public String getReadFrom()
    {
        return readFrom;
    }

    @Override
    public String getBaseUrl()
    {
        return baseUrl;
    }
}
