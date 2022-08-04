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
package org.alfresco.transform.base.transform;

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Separation of InputStream, OutputStream, sourceFile and targetFile from the {@link ProcessHandler} logic. Allows
 * {@link CustomTransformer} implementations to call {@link TransformManager#createSourceFile()} and
 * {@link TransformManager#createTargetFile()} so that extra Files are not created if there was one already in
 * existence.
 *
 * Subclasses MUST call transformManager.setSourceFile(File) and transformManager.setSourceFile(File) if they start
 * with files rather than streams, before calling the {@link #init()} method which calls
 * transformManager.setOutputStream(InputStream) and transformManager.setOutputStream(OutputStream).
 */
public abstract class StreamHandler
{
    protected TransformManagerImpl transformManager = new TransformManagerImpl();
    protected InputStream inputStream;
    protected OutputStream outputStream;

    public abstract void handleTransformRequest() throws Exception;

    protected void init() throws IOException
    {
        setInputStream();
        setOutputStream();
    }

    private void setInputStream() throws IOException
    {
        inputStream = transformManager.setInputStream(getInputStream());
    }

    protected void setOutputStream() throws IOException
    {
        outputStream = transformManager.setOutputStream(getOutputStream());
    }

    protected abstract InputStream getInputStream() throws IOException;

    protected abstract OutputStream getOutputStream() throws IOException;

    protected void handleTransform(CustomTransformer customTransformer) throws Exception
    {
        try
        {
            transform(customTransformer);
            handleSuccessfulTransform();
        }
        finally
        {
            closeOutputStream();
            closeInputStreamWithoutException();
            deleteTmpFiles();
        }
    }

    protected abstract void transform(CustomTransformer customTransformer) throws Exception;

    protected void handleSuccessfulTransform() throws IOException
    {
        transformManager.copyTargetFileToOutputStream();
        onSuccessfulTransform();
    }

    protected void onSuccessfulTransform()
    {
    }

    protected void closeOutputStream() throws IOException
    {
        transformManager.getOutputStream().close();
    }

    private void closeInputStreamWithoutException()
    {
        if (inputStream != null)
        {
            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void deleteTmpFiles()
    {
        transformManager.deleteSourceFile();
        transformManager.deleteTargetFile();
    }
}
