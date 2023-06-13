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
package org.alfresco.transform.imagemagick;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.alfresco.transform.base.clients.HttpClient.sendTRequest;
import static org.alfresco.transform.base.clients.FileInfo.testFile;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_BMP;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_CGM;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_GIF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_IEF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JP2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PBM;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PGM;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNM;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PPJ;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PPM;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PSD;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_3FR;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_ARW;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_CR2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_DNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_K25;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_MRW;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_NEF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_ORF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_PEF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_R3D;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_RAF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_RW2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_RWL;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_X3F;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_XBM;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_XPM;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_XWD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.alfresco.transform.base.clients.FileInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * @author Cezar Leahu
 */
public class ImageMagickTransformationIT {
    private static final Logger logger = LoggerFactory.getLogger(ImageMagickTransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";
    private static final List<Pair<String, String>> targetExtensions = new ImmutableList.Builder<Pair<String, String>>()
            .add(Pair.of("3fr", MIMETYPE_IMAGE_RAW_3FR))
            .add(Pair.of("arw", MIMETYPE_IMAGE_RAW_ARW))
            .add(Pair.of("bmp", MIMETYPE_IMAGE_BMP))
            .add(Pair.of("cgm", MIMETYPE_IMAGE_CGM))
            .add(Pair.of("cr2", MIMETYPE_IMAGE_RAW_CR2))
            .add(Pair.of("dng", MIMETYPE_IMAGE_RAW_DNG))
            .add(Pair.of("gif", MIMETYPE_IMAGE_GIF))
            .add(Pair.of("ief", MIMETYPE_IMAGE_IEF))
            .add(Pair.of("jp2", MIMETYPE_IMAGE_JP2))
            .add(Pair.of("jpg", MIMETYPE_IMAGE_JPEG))
            .add(Pair.of("k25", MIMETYPE_IMAGE_RAW_K25))
            .add(Pair.of("mrw", MIMETYPE_IMAGE_RAW_MRW))
            .add(Pair.of("nef", MIMETYPE_IMAGE_RAW_NEF))
            .add(Pair.of("orf", MIMETYPE_IMAGE_RAW_ORF))
            .add(Pair.of("pbm", MIMETYPE_IMAGE_PBM))
            .add(Pair.of("pef", MIMETYPE_IMAGE_RAW_PEF))
            .add(Pair.of("pgm", MIMETYPE_IMAGE_PGM))
            .add(Pair.of("png", MIMETYPE_IMAGE_PNG))
            .add(Pair.of("pnm", MIMETYPE_IMAGE_PNM))
            .add(Pair.of("ppj", MIMETYPE_IMAGE_PPJ))
            .add(Pair.of("ppm", MIMETYPE_IMAGE_PPM))
            .add(Pair.of("r3d", MIMETYPE_IMAGE_RAW_R3D))
            .add(Pair.of("raf", MIMETYPE_IMAGE_RAW_RAF))
            .add(Pair.of("ras", MIMETYPE_IMAGE_RAS))
            .add(Pair.of("rw2", MIMETYPE_IMAGE_RAW_RW2))
            .add(Pair.of("rwl", MIMETYPE_IMAGE_RAW_RWL))
            .add(Pair.of("tiff", MIMETYPE_IMAGE_TIFF))
            .add(Pair.of("x3f", MIMETYPE_IMAGE_RAW_X3F))
            .add(Pair.of("xbm", MIMETYPE_IMAGE_XBM))
            .add(Pair.of("xpm", MIMETYPE_IMAGE_XPM))
            .add(Pair.of("xwd", MIMETYPE_IMAGE_XWD))
            .build();

    private static final List<Pair<String, String>> targetExtensionsForPSD = new ImmutableList.Builder<Pair<String, String>>()
            .add(Pair.of("x3f", MIMETYPE_IMAGE_RAW_X3F))
            .add(Pair.of("tiff", MIMETYPE_IMAGE_TIFF))
            .add(Pair.of("rwl", MIMETYPE_IMAGE_RAW_RWL))
            .add(Pair.of("rw2", MIMETYPE_IMAGE_RAW_RW2))
            .add(Pair.of("ras", MIMETYPE_IMAGE_RAS))
            .add(Pair.of("raf", MIMETYPE_IMAGE_RAW_RAF))
            .add(Pair.of("r3d", MIMETYPE_IMAGE_RAW_R3D))
            .add(Pair.of("psd", MIMETYPE_IMAGE_PSD))
            .add(Pair.of("ppm", MIMETYPE_IMAGE_PPM))
            .add(Pair.of("ppj", MIMETYPE_IMAGE_PPJ))
            .add(Pair.of("pnm", MIMETYPE_IMAGE_PNM))
            .add(Pair.of("pgm", MIMETYPE_IMAGE_PGM))
            .add(Pair.of("pef", MIMETYPE_IMAGE_RAW_PEF))
            .add(Pair.of("pbm", MIMETYPE_IMAGE_PBM))
            .add(Pair.of("orf", MIMETYPE_IMAGE_RAW_ORF))
            .add(Pair.of("nef", MIMETYPE_IMAGE_RAW_NEF))
            .add(Pair.of("mrw", MIMETYPE_IMAGE_RAW_MRW))
            .add(Pair.of("k25", MIMETYPE_IMAGE_RAW_K25))
            .add(Pair.of("ief", MIMETYPE_IMAGE_IEF))
            .add(Pair.of("gif", MIMETYPE_IMAGE_GIF))
            .add(Pair.of("dng", MIMETYPE_IMAGE_RAW_DNG))
            .add(Pair.of("cr2", MIMETYPE_IMAGE_RAW_CR2))
            .add(Pair.of("arw", MIMETYPE_IMAGE_RAW_ARW))
            .add(Pair.of("3fr", MIMETYPE_IMAGE_RAW_3FR))
            .build();

    private static final Map<String, FileInfo> TEST_FILES = Stream.of(
            testFile(MIMETYPE_IMAGE_BMP, "bmp", "quick.bmp"), 
            testFile(MIMETYPE_IMAGE_GIF, "gif", "quick.gif"),
            testFile(MIMETYPE_IMAGE_JPEG, "jpg", "quick.jpg"),
            testFile(MIMETYPE_IMAGE_PBM, "pbm", "quick.pbm"), 
            testFile(MIMETYPE_IMAGE_PGM, "pgm", "quick.pgm"),
            testFile(MIMETYPE_IMAGE_PNG, "png", "quick.png"), 
            testFile(MIMETYPE_IMAGE_PNM, "pnm", "quick.pnm"),
            testFile(MIMETYPE_IMAGE_PPM, "ppm", "quick.ppm"), 
            testFile(MIMETYPE_IMAGE_XBM, "xbm", "quick.xbm"),
            testFile(MIMETYPE_IMAGE_XPM, "xpm", "quick.xpm"), 
            testFile(MIMETYPE_IMAGE_PSD, "psd", "quick.psd"),
            testFile(MIMETYPE_IMAGE_TIFF, "tiff", "quick.tiff"), 
            testFile(MIMETYPE_IMAGE_XWD, "xwd", "quick.xwd")
        ).collect(toMap(FileInfo::getPath, identity()));

    public static Stream<Pair<FileInfo, Pair<String,String>>> engineTransformations() {
        return Stream
            .of(
                allTargets("quick.bmp", targetExtensions),
                allTargets("quick.gif", targetExtensions), 
                allTargets("quick.jpg", targetExtensions),
                allTargets("quick.pbm", targetExtensions), 
                allTargets("quick.pgm", targetExtensions),
                allTargets("quick.png", targetExtensions), 
                allTargets("quick.pnm", targetExtensions),
                allTargets("quick.ppm", targetExtensions), 
                allTargets("quick.psd", targetExtensionsForPSD),
                allTargets("quick.tiff", targetExtensions), 
                allTargets("quick.xbm", targetExtensions),
                allTargets("quick.xpm", targetExtensions), 
                allTargets("quick.xwd", targetExtensions)
            ).flatMap(identity());
    }

    @ParameterizedTest
    @MethodSource("engineTransformations")
    public void testTransformation(Pair<FileInfo, Pair<String, String>> entry)
    {
        String sourceFile = entry.getLeft().getPath();
        String targetExtension = entry.getRight().getLeft();
        String sourceMimetype = entry.getLeft().getMimeType();
        String targetMimetype = entry.getRight().getRight();
        
        final String descriptor = format("Transform ({0}, {1} -> {2}, {3})",
            sourceFile, sourceMimetype, targetMimetype, targetExtension);
        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, sourceMimetype,
                targetMimetype, targetExtension, emptyMap());
            assertEquals(OK, response.getStatusCode(), descriptor);
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }

    private static Stream<Pair<FileInfo, Pair<String,String>>> allTargets(final String sourceFile, List<Pair<String,String>> targetExtensionsList)
    {
        return targetExtensionsList
            .stream()
            .map(k -> Pair.of(TEST_FILES.get(sourceFile), k));
    }
}
