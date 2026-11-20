/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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
package org.alfresco.transform.base.executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.exceptions.TransformException;

class AbstractCommandExecutorTest
{
    private static final String CONVERT_DEPRECATED_WARNING = "WARNING: The convert command is deprecated in IMv7, use \"magick\" instead of \"convert\" or \"magick convert\"\n\n";

    private final AbstractCommandExecutor executor = new MockedCommandExecutor();
    private final ExecutionResult result = mock(ExecutionResult.class);

    @TempDir
    public File tempDir;

    @BeforeEach
    void before()
    {
        when(executor.transformCommand.execute(any(), anyLong())).thenReturn(result);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 128, 193, 255})
    void testAMissingOutputFileReportsTheExitCode(int exitValue)
    {
        when(result.getExitValue()).thenReturn(exitValue);
        when(result.getStdErr()).thenReturn("");
        when(result.getStdOut()).thenReturn("");

        TransformException e = assertThrows(TransformException.class,
                () -> executor.run(Map.of(), new File(tempDir, "target.png"), null));

        assertEquals(INTERNAL_SERVER_ERROR, e.getStatus());
        assertEquals("Transformer failed to create an output file. Exit code " + exitValue, e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"129, 1", "137, 9", "192, 64"})
    void testAKilledToolIsARetryableFailureEvenIfItPrintedAWarning(int exitValue, int signal)
    {
        when(result.getExitValue()).thenReturn(exitValue);
        when(result.getStdErr()).thenReturn(CONVERT_DEPRECATED_WARNING);
        when(result.getStdOut()).thenReturn("");

        TransformException e = assertThrows(TransformException.class,
                () -> executor.run(Map.of(), new File(tempDir, "target.png"), null));

        assertEquals(INTERNAL_SERVER_ERROR, e.getStatus());
        assertEquals("Transformer was killed by signal " + signal + ". Exit code " + exitValue + ", stderr: "
                + CONVERT_DEPRECATED_WARNING.strip(), e.getMessage());
    }

    @Test
    void testAKilledToolIsAFailureEvenIfItLeftSomeOutput() throws IOException
    {
        File targetFile = new File(tempDir, "target.png");
        Files.writeString(targetFile.toPath(), "partial");
        when(result.getExitValue()).thenReturn(139);
        when(result.getStdErr()).thenReturn("");
        when(result.getStdOut()).thenReturn("");

        TransformException e = assertThrows(TransformException.class,
                () -> executor.run(Map.of(), targetFile, null));

        assertEquals(INTERNAL_SERVER_ERROR, e.getStatus());
        assertEquals("Transformer was killed by signal 11. Exit code 139", e.getMessage());
    }

    @Test
    void testAFailingToolThatPrintedAnErrorIsStillABadRequest()
    {
        String stderr = CONVERT_DEPRECATED_WARNING + "convert: improper image header `source.png' @ error/png.c/ReadPNGImage/3953.\n";
        when(result.getExitValue()).thenReturn(1);
        when(result.getStdErr()).thenReturn(stderr);

        TransformException e = assertThrows(TransformException.class,
                () -> executor.run(Map.of(), new File(tempDir, "target.png"), null));

        assertEquals(BAD_REQUEST, e.getStatus());
        assertEquals("Transformer exit code was not 0: \n" + stderr, e.getMessage());
    }

    @Test
    void testAnEmptyOutputFileReportsWhatTheToolPrinted() throws IOException
    {
        File targetFile = new File(tempDir, "target.png");
        assertTrue(targetFile.createNewFile());
        when(result.getExitValue()).thenReturn(0);
        when(result.getStdErr()).thenReturn("  warning: no images defined\n");
        when(result.getStdOut()).thenReturn("done\n");

        TransformException e = assertThrows(TransformException.class,
                () -> executor.run(Map.of(), targetFile, null));

        assertEquals(INTERNAL_SERVER_ERROR, e.getStatus());
        assertEquals("Transformer failed to create an output file. Exit code 0, stderr: warning: no images defined, stdout: done",
                e.getMessage());
    }

    private static class MockedCommandExecutor extends AbstractCommandExecutor
    {
        @Override
        protected RuntimeExec createTransformCommand()
        {
            return mock(RuntimeExec.class);
        }

        @Override
        protected RuntimeExec createCheckCommand()
        {
            return mock(RuntimeExec.class);
        }
    }
}
