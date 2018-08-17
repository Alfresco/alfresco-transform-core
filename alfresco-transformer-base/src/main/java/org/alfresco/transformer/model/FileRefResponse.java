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
