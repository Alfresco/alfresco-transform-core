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
package org.alfresco.transform.base.transform;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.fs.FileManager;
import org.alfresco.transform.base.util.OutputStreamLengthRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Manages the input and output streams and any temporary files that have been created.
 */
@Component
public class TransformManagerImpl implements TransformManager
{
    private static final Logger logger = LoggerFactory.getLogger(TransformManagerImpl.class);

    private HttpServletRequest request;
    private ProcessHandler processHandler;
    private InputStream inputStream;
    private OutputStreamLengthRecorder outputStreamLengthRecorder;
    private String sourceMimetype;
    private String targetMimetype;
    private File sourceFile;
    private File targetFile;
    private boolean keepTargetFile;
    private boolean createSourceFileCalled;
    private boolean createTargetFileCalled;
    private Boolean startedWithSourceFile;
    private Boolean startedWithTargetFile;

    public void setRequest(HttpServletRequest request)
    {
        this.request = request;
    }

    public void setProcessHandler(ProcessHandler processHandler)
    {
        this.processHandler = processHandler;
    }

    @Override public String getRequestId()
    {
        return processHandler.getReference();
    }

    public InputStream setInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
        if (startedWithSourceFile == null)
        {
            startedWithSourceFile = false;
        }
        return inputStream;
    }

    public OutputStream getOutputStream()
    {
        return outputStreamLengthRecorder;
    }

    public OutputStream setOutputStream(OutputStream outputStream)
    {
        outputStreamLengthRecorder = new OutputStreamLengthRecorder(outputStream);
        if (startedWithTargetFile == null)
        {
            startedWithTargetFile = false;
        }
        return outputStreamLengthRecorder;
    }

    public Long getOutputLength()
    {
        return outputStreamLengthRecorder.getLength();
    }

    public void setSourceMimetype(String sourceMimetype)
    {
        this.sourceMimetype = sourceMimetype;
    }

    public void setTargetMimetype(String targetMimetype)
    {
        this.targetMimetype = targetMimetype;
    }

    public File getSourceFile()
    {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile)
    {
        this.sourceFile = sourceFile;
        if (startedWithSourceFile == null)
        {
            startedWithSourceFile = true;
        }
    }

    public File getTargetFile()
    {
        return targetFile;
    }

    public void setTargetFile(File targetFile)
    {
        this.targetFile = targetFile;
        if (startedWithTargetFile == null)
        {
            startedWithTargetFile = true;
        }
    }

    public void keepTargetFile()
    {
        keepTargetFile = true;
    }

    @Override public File createSourceFile()
    {
        if (createSourceFileCalled)
        {
            throw new IllegalStateException("createSourceFile has already been called");
        }
        createSourceFileCalled = true;

        if (sourceFile == null)
        {
            sourceFile = FileManager.createSourceFile(request, inputStream, sourceMimetype);
        }
        return sourceFile;
    }

    @Override public File createTargetFile()
    {
        if (createTargetFileCalled)
        {
            throw new IllegalStateException("createTargetFile has already been called");
        }
        createTargetFileCalled = true;

        if (targetFile == null)
        {
            targetFile = FileManager.createTargetFile(request, sourceMimetype, targetMimetype);
        }
        return targetFile;
    }

    public void copyTargetFileToOutputStream() throws IOException
    {
        if (targetFile != null)
        {
            if (!startedWithTargetFile)
            {
                FileManager.copyFileToOutputStream(targetFile, outputStreamLengthRecorder);
            }
            else if (createTargetFileCalled)
            {
                outputStreamLengthRecorder.setByteCount(targetFile.length());
            }
            else
            {
                outputStreamLengthRecorder.flush();
            }
        }
    }

    public void deleteSourceFile()
    {
        if (sourceFile != null && !sourceFile.delete())
        {
            logger.error("Failed to delete temporary source file {}", sourceFile.getPath());
        }
        outputStreamLengthRecorder = null;
        sourceFile = null;
        createSourceFileCalled = false;
        startedWithSourceFile = null;
    }

    public void deleteTargetFile()
    {
        if (!keepTargetFile && targetFile != null && !targetFile.delete())
        {
            logger.error("Failed to delete temporary target file {}", targetFile.getPath());
        }
        targetFile = null;
        createTargetFileCalled = false;
        startedWithTargetFile = null;
    }

    @Override
    public OutputStream respondWithFragment(Integer index, boolean finished) throws IOException
    {
        if (request != null)
        {
            throw new IllegalStateException("Fragments may only be sent via message queues. This an http request");
        }

        return processHandler.respondWithFragment(index, finished);
    }
}
