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

import java.util.Objects;

/**
 * TODO: Copied from org.alfresco.store.entity (alfresco-shared-file-store). To be discussed
 *
 * POJO that represents content reference ({@link java.util.UUID})
 */
public class FileRefEntity
{
    private String fileRef;

    public FileRefEntity()
    {
    }

    public FileRefEntity(String fileRef)
    {
        this.fileRef = fileRef;
    }

    public void setFileRef(String fileRef){
        this.fileRef = fileRef;
    }
    public String getFileRef()
    {
        return fileRef;
    }

    @Override
    public String toString()
    {
        return fileRef;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        FileRefEntity fileRef = (FileRefEntity) o;
        return Objects.equals(this.fileRef, fileRef.fileRef);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fileRef);
    }
}
