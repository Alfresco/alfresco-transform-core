/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.transformer;

import static java.text.MessageFormat.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.alfresco.transformer.EngineClient.sendTRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ImmutableList;

/**
 * @author Cezar Leahu
 */
@RunWith(Parameterized.class)
public class ImageMagickTransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(ImageMagickTransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";
    private static final List<String> targetMimetypes = ImmutableList.of(
        "3fr", "arw", "bmp", "cgm",
        "cr2", "dng", "eps", "gif", "ief", "jp2", "jpg",
        "k25", "mrw", "nef", "orf", "pbm", "pef", "pgm", "png", "pnm", "ppj", "ppm",
        "psd", "r3d", "raf", "ras", "rw2", "rwl", "tiff", "x3f", "xbm", "xpm", "xwd");

    private final String sourceFile;
    private final String targetExtension;

    public ImageMagickTransformationIT(final Pair<String, String> entry)
    {
        sourceFile = entry.getLeft();
        targetExtension = entry.getLeft();
    }

    @Parameterized.Parameters
    public static Set<Pair<String, String>> engineTransformations()
    {
        return Stream
            .of(
                allTargets("quick.bmp"),
                allTargets("quick.eps"),
                allTargets("quick.gif"),
                allTargets("quick.jpg"),
                allTargets("quick.pbm"),
                allTargets("quick.pgm"),
                allTargets("quick.png"),
                allTargets("quick.pnm"),
                allTargets("quick.ppm"),
                //allTargets("quick.psd"),
                //allTargets("quick.tiff"),
                allTargets("quick.xbm"),
                allTargets("quick.xpm"),
                allTargets("quick.xwd")
            )
            .flatMap(identity())
            .collect(toSet());
    }

    @Test
    public void testTransformation()
    {
        final String descriptor = format("Transform ({0} -> {1})", sourceFile, null,
            targetExtension);
        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, null,
                null, targetExtension);
            assertEquals(descriptor, OK, response.getStatusCode());
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }

    private static Stream<Pair<String, String>> allTargets(final String sourceFile)
    {
        return targetMimetypes
            .stream()
            .map(k -> Pair.of(sourceFile, k));
    }
}
