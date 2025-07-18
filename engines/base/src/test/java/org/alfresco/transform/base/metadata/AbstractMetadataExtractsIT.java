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
package org.alfresco.transform.base.metadata;

import static java.text.MessageFormat.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpStatus.OK;

import static org.alfresco.transform.base.clients.HttpClient.sendTRequest;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_METADATA_EXTRACT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import org.alfresco.transform.base.clients.FileInfo;

/**
 * Super class of metadata integration tests. Sub classes should provide the following:
 * <p>
 * <ul>
 * <li>A method providing a Stream of test files: {@code public static Stream<FileInfo> engineTransformations()};</li>
 * <li>Provide expected json files (&lt;sourceFilename>"_metadata.json") as resources on the classpath.</li>
 * <li>Override the method {@code testTransformation(FileInfo testFileInfo)} such that it calls the super method as a {@code @ParameterizedTest} for example:</li>
 * </ul>
 * 
 * <pre>
 * &#64;ParameterizedTest
 * 
 * &#64;MethodSource("engineTransformations")
 * 
 * &#64;Override

 * public void testTransformation(FileInfo testFileInfo)
 * 
 * { 
 *      super.testTransformation(FileInfo testFileInfo)
 * }
 * </pre>
 *
 * @author adavis
 * @author dedwards
 */
public abstract class AbstractMetadataExtractsIT
{
    private static final String ENGINE_URL = "http://localhost:8090";
    // These are normally variable, hence the lowercase.
    private static final String targetMimetype = MIMETYPE_METADATA_EXTRACT;
    private static final String targetExtension = "json";

    private final ObjectMapper jsonObjectMapper = new ObjectMapper();

    public void testTransformation(FileInfo fileInfo)
    {
        final String sourceMimetype = fileInfo.getMimeType();
        final String sourceFile = fileInfo.getPath();

        final String descriptor = format("Transform ({0}, {1} -> {2}, {3})",
                sourceFile, sourceMimetype, targetMimetype, targetExtension);

        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile,
                    sourceMimetype, targetMimetype, targetExtension);
            assertEquals(OK, response.getStatusCode(), descriptor);

            String metadataFilename = sourceFile + "_metadata.json";
            Map<String, Serializable> actualMetadata = readMetadata(response.getBody().getInputStream());
            File actualMetadataFile = new File(metadataFilename);
            jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValue(actualMetadataFile, actualMetadata);

            Map<String, Serializable> expectedMetadata = readExpectedMetadata(metadataFilename, actualMetadataFile);
            assertEquals(expectedMetadata, actualMetadata,
                    sourceFile + ": The metadata did not match the expected value. It has been saved in " + actualMetadataFile.getAbsolutePath());
            actualMetadataFile.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(descriptor + " exception: " + e.getMessage());
        }
    }

    private Map<String, Serializable> readExpectedMetadata(String filename, File actualMetadataFile) throws IOException
    {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filename))
        {
            if (inputStream == null)
            {
                fail("The expected metadata file " + filename + " did not exist.\n" +
                        "The actual metadata has been saved in " + actualMetadataFile.getAbsoluteFile());
            }
            return readMetadata(inputStream);
        }
    }

    private Map<String, Serializable> readMetadata(InputStream inputStream) throws IOException
    {
        TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {};
        return jsonObjectMapper.readValue(inputStream, typeRef);
    }
}
