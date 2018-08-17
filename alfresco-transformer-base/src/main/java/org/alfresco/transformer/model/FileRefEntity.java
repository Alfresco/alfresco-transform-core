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
