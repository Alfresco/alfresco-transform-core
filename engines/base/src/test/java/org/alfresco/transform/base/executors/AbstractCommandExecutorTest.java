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
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.exceptions.TransformException;

class AbstractCommandExecutorTest
{
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
    @CsvSource({
            "1, 'Exit code 1'",
            "137, 'Exit code 137 (killed by signal 9)'",
            "139, 'Exit code 139 (killed by signal 11)'",
            "255, 'Exit code 255'"})
    void testAMissingOutputFileReportsTheExitCode(int exitValue, String expectedDetail)
    {
        when(result.getExitValue()).thenReturn(exitValue);
        when(result.getStdErr()).thenReturn("");
        when(result.getStdOut()).thenReturn("");

        TransformException e = assertThrows(TransformException.class,
                () -> executor.run(Map.of(), new File(tempDir, "target.png"), null));

        assertEquals(INTERNAL_SERVER_ERROR, e.getStatus());
        assertEquals("Transformer failed to create an output file. " + expectedDetail, e.getMessage());
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
