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
package org.alfresco.transformer.model;

/**
 * TODO: Copied from org.alfresco.store.entity (alfresco-shared-file-store). To be discussed
 *
 * POJO that describes the ContentRefEntry response, contains {@link FileRefEntity} according to API spec
 */
public class FileRefResponse
{
    private FileRefEntity entry;

    public FileRefResponse()
    {
    }

    public FileRefResponse(FileRefEntity entry)
    {
        this.entry = entry;
    }

    public FileRefEntity getEntry()
    {
        return entry;
    }

    public void setEntry(FileRefEntity entry)
    {
        this.entry = entry;
    }
}
