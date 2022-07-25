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
package org.alfresco.transform.base;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link TransformStreamHandler} and {@link TransformManagerImpl#createSourceFile()} and
 * {@link TransformManagerImpl#createTargetFile()} methods.
 */
public class TransformStreamHandlerTest
{
    public static final String ORIGINAL = "Original";
    public static final String CHANGE = " plus some change";
    public static final String EXPECTED = ORIGINAL+ CHANGE;

    TransformManagerImpl transformManager = new TransformManagerImpl();
    @TempDir
    public File tempDir;

    private InputStream getSourceInputStreamFromBytes()
    {
        return new ByteArrayInputStream(ORIGINAL.getBytes(StandardCharsets.ISO_8859_1));
    }

    private OutputStream getOutputStreamToFile(File sourceFile) throws FileNotFoundException
    {
        return new BufferedOutputStream(new FileOutputStream(sourceFile));
    }

    private InputStream getInputStreamFromFile(File sourceFile) throws FileNotFoundException
    {
        return new BufferedInputStream(new FileInputStream(sourceFile));
    }

    private File tempFile() throws IOException
    {
        return File.createTempFile("temp_", null, tempDir);
    }

    private void write(File file, String text) throws IOException
    {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file)))
        {
            write(outputStream, text);
        }
    }

    private void write(OutputStream outputStream, String text) throws IOException
    {
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
        outputStream.write(bytes, 0, bytes.length);
    }

    private String read(File file) throws IOException
    {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file)))
        {
            return read(inputStream);
        }
    }

    private String read(InputStream inputStream) throws IOException
    {
        return new String(inputStream.readAllBytes(), StandardCharsets.ISO_8859_1);
    }

    private String read(ByteArrayOutputStream outputStream)
    {
        return outputStream.toString(StandardCharsets.ISO_8859_1);
    }

    private void closeInputStreamWithoutException(InputStream inputStream)
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

    @Test
    public void testStartWithInputStream() throws Exception
    {
        try (InputStream inputStream = getSourceInputStreamFromBytes();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            write(outputStreamLengthRecorder, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            transformManager.getOutputStream().close();
            closeInputStreamWithoutException(inputStream);
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, read(outputStream));
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
        }
    }

    @Test
    public void testStartWithInputStreamAndCallCreateSourceFile() throws Exception
    {
        try (InputStream inputStream = getSourceInputStreamFromBytes();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            File sourceFileCreatedByTransform = transformManager.createSourceFile();
            assertTrue(sourceFileCreatedByTransform.exists());
            write(outputStreamLengthRecorder, read(sourceFileCreatedByTransform)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            transformManager.getOutputStream().close();
            closeInputStreamWithoutException(inputStream);
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, read(outputStream));
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(sourceFileCreatedByTransform.exists());
        }
    }

    @Test
    public void testStartWithSourceFile() throws Exception
    {
        File sourceFile = tempFile();
        write(sourceFile, ORIGINAL);
        transformManager.setSourceFile(sourceFile);

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            write(outputStreamLengthRecorder, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            closeInputStreamWithoutException(inputStream);
            transformManager.getOutputStream().close();
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, read(outputStream));
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(sourceFile.exists());
        }
    }

    @Test
    public void testStartWithSourceFileAndCallCreateSourceFile() throws Exception
    {
        File sourceFile = tempFile();
        write(sourceFile, ORIGINAL);
        transformManager.setSourceFile(sourceFile);

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            File sourceFileCreatedByTransform = transformManager.createSourceFile();
            assertEquals(sourceFile, sourceFileCreatedByTransform);
            write(outputStreamLengthRecorder, read(sourceFileCreatedByTransform)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            closeInputStreamWithoutException(inputStream);
            transformManager.getOutputStream().close();
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, read(outputStream));
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(sourceFile.exists());
        }
    }

    @Test
    public void testStartWithOutputStream()
    {
        // Do nothing. Same as testUsingInputStream() which just uses the outputStream
    }

    @Test
    public void testStartWithOutputStreamAndCallCreateTargetFile() throws Exception
    {
        try (InputStream inputStream = getSourceInputStreamFromBytes();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            transformManager.setInputStream(inputStream);
            transformManager.setOutputStream(outputStream);

            File targetFileCreatedByTransform = transformManager.createTargetFile();
            assertTrue(targetFileCreatedByTransform.exists());
            write(targetFileCreatedByTransform, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            transformManager.getOutputStream().close();
            closeInputStreamWithoutException(inputStream);
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, read(outputStream));
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(targetFileCreatedByTransform.exists());
        }
    }

    @Test
    public void testStartWithTargetFile() throws Exception
    {
        File targetFile = tempFile();
        transformManager.setTargetFile(targetFile);

        try (InputStream inputStream = getSourceInputStreamFromBytes();
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            write(outputStreamLengthRecorder, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            transformManager.getOutputStream().close();
            closeInputStreamWithoutException(inputStream);
            String actual = read(targetFile);
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, actual);
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(targetFile.exists());
        }
    }

    @Test
    public void testStartWithTargetFileAndCallCreateTargetFile() throws Exception
    {
        File targetFile = tempFile();
        transformManager.setTargetFile(targetFile);

        try (InputStream inputStream = getSourceInputStreamFromBytes();
             OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            transformManager.setInputStream(inputStream);
            transformManager.setOutputStream(outputStream);

            File targetFileCreatedByTransform = transformManager.createTargetFile();
            assertEquals(targetFile, targetFileCreatedByTransform);
            write(targetFileCreatedByTransform, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            transformManager.getOutputStream().close();
            closeInputStreamWithoutException(inputStream);
            String actual = read(targetFile);
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, actual);
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(targetFile.exists());
        }
    }

    @Test
    public void testHandleHttpRequestApproachUsingSourceAndTargetStreams()
    {
        // Do nothing. Same as testUsingInputStream() which uses inputStream and outputStream
    }

    @Test
    public void testHandleProbeRequestApproachUsingSourceAndTargetFilesButKeepingTheTarget() throws Exception
    {
        File targetFile = tempFile();
        File sourceFile = tempFile();
        write(sourceFile, ORIGINAL);
        transformManager.setSourceFile(sourceFile);
        transformManager.keepTargetFile();

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            write(outputStreamLengthRecorder, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            closeInputStreamWithoutException(inputStream);
            transformManager.getOutputStream().close();
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, read(targetFile));
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(sourceFile.exists());
            assertTrue(targetFile.exists());
        }
    }

    @Test
    public void testHandleMessageRequestApproachUsingSourceAndTargetFiles() throws Exception
    {
        File targetFile = tempFile();
        File sourceFile = tempFile();
        write(sourceFile, ORIGINAL);
        transformManager.setSourceFile(sourceFile);
        transformManager.setTargetFile(targetFile);

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            write(outputStreamLengthRecorder, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            closeInputStreamWithoutException(inputStream);
            transformManager.getOutputStream().close();
            String actual = read(targetFile);
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, actual);
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(sourceFile.exists());
            assertFalse(targetFile.exists());
        }
    }

    @Test
    public void testHandleMessageRequestApproachUsingInputStreamAndTargetFile() throws Exception
    {
        File targetFile = tempFile();
        transformManager.setTargetFile(targetFile);

        try (InputStream inputStream = getSourceInputStreamFromBytes();
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            transformManager.setInputStream(inputStream);
            OutputStream outputStreamLengthRecorder = transformManager.setOutputStream(outputStream);

            write(outputStreamLengthRecorder, read(inputStream)+CHANGE);

            transformManager.copyTargetFileToOutputStream();
            closeInputStreamWithoutException(inputStream);
            transformManager.getOutputStream().close();
            String actual = read(targetFile);
            transformManager.deleteSourceFile();
            transformManager.deleteTargetFile();

            assertEquals(EXPECTED, actual);
            assertEquals(EXPECTED.length(), transformManager.getOutputLength());
            assertFalse(targetFile.exists());
        }
    }

    private abstract class FakeTransformStreamHandler extends TransformStreamHandler
    {
        @Override
        public void handleTransformRequest() throws Exception
        {
            init();
            handleTransform(null);
        }

        @Override
        protected void transform(CustomTransformer customTransformer) throws Exception
        {
            write(outputStream, read(inputStream)+CHANGE);
        }
    }

    @Test
    public void testSimulatedHandleHttpRequest() throws Exception
    {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream())
        {
            new FakeTransformStreamHandler()
            {
                @Override
                protected InputStream getInputStream()
                {
                    return getSourceInputStreamFromBytes();
                }

                @Override
                protected OutputStream getOutputStream()
                {
                    return os;
                }
            }.handleTransformRequest();
        }
    }

    @Test
    public void testSimulatedHandleProbeRequest() throws Exception
    {
        File targetFile = tempFile();
        File sourceFile = tempFile();
        write(sourceFile, ORIGINAL);

        new FakeTransformStreamHandler()
        {
            @Override
            protected void init() throws IOException
            {
                transformManager.setSourceFile(sourceFile);
                transformManager.setTargetFile(targetFile);
                transformManager.keepTargetFile();
                super.init();
            }

            @Override
            protected InputStream getInputStream() throws IOException
            {
                return getInputStreamFromFile(sourceFile);
            }

            @Override
            protected OutputStream getOutputStream() throws IOException
            {
                return getOutputStreamToFile(targetFile);
            }
        }.handleTransformRequest();
    }

    @Test
    public void testSimulatedHandleMessageRequestUsingSharedFileStore() throws Exception
    {
        File targetFile = tempFile();
        File sourceFile = tempFile();
        write(sourceFile, ORIGINAL);

        new FakeTransformStreamHandler()
        {
            @Override
            protected InputStream getInputStream() throws IOException
            {
                return getInputStreamFromFile(sourceFile);
            }

            @Override
            protected OutputStream getOutputStream() throws IOException
            {
                return getOutputStreamToFile(targetFile);
            }
        }.handleTransformRequest();
    }

    @Test
    public void testSimulatedHandleMessageRequestUsingDirectAccessUrl() throws Exception
    {
        File targetFile = tempFile();

        new FakeTransformStreamHandler()
        {
            @Override
            protected InputStream getInputStream()
            {
                return getSourceInputStreamFromBytes();
            }

            @Override
            protected OutputStream getOutputStream()
                throws FileNotFoundException
            {
                return getOutputStreamToFile(targetFile);
            }
        }.handleTransformRequest();
    }
}
