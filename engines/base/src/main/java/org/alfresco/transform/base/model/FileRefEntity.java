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
package org.alfresco.transform.base.model;

import java.util.Objects;

/**
 * TODO: Copied from org.alfresco.store.entity (alfresco-shared-file-store). To be discussed
 *
 * POJO that represents content reference ({@link java.util.UUID})
 */
public class FileRefEntity
{
    private String fileRef;

    public FileRefEntity() {}

    public FileRefEntity(String fileRef)
    {
        this.fileRef = fileRef;
    }

    public void setFileRef(String fileRef)
    {
        this.fileRef = fileRef;
    }

    public String getFileRef()
    {
        return fileRef;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileRefEntity that = (FileRefEntity) o;
        return Objects.equals(fileRef, that.fileRef);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fileRef);
    }

    @Override
    public String toString()
    {
        return fileRef;
    }
}
