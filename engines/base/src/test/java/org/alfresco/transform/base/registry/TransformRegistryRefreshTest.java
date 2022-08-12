/*
 * Copyright 2015-2022 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.transform.base.registry;

import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.base.fakes.FakeTransformEngineWithTwoCustomTransformers;
import org.alfresco.transform.base.fakes.FakeTransformerPdf2Png;
import org.alfresco.transform.base.fakes.FakeTransformerTxT2Pdf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@AutoConfigureMockMvc
@SpringBootTest(classes={org.alfresco.transform.base.Application.class}, properties={"transform.engine.config.cron=*/1 * * * * *"})
@ContextConfiguration(classes = {
    FakeTransformEngineWithTwoCustomTransformers.class,
    FakeTransformerTxT2Pdf.class,
    FakeTransformerPdf2Png.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TransformRegistryRefreshTest
{
    @SpyBean
    private TransformRegistry transformRegistry;
    @Autowired
    private TransformConfigFromFiles transformConfigFromFiles;
    @Autowired
    private TransformConfigFiles transformConfigFiles;

    @Test
    public void checkRegistryRefreshes() throws InterruptedException
    {
        assertEquals(4, transformRegistry.getTransformConfig().getTransformers().size());
        verify(transformRegistry, atLeast(1)).retrieveConfig();

        // As we can't change the content of a classpath resource, lets change what is read.
        ReflectionTestUtils.setField(transformConfigFiles, "config", ImmutableMap.of(
            "a",   "config/addA2B.json",
            "foo", "config/addB2C.json"));
        transformConfigFromFiles.initFileConfig();

        Thread.sleep(3000); // to give it a chance to refresh a few (at least 2 more) times
        verify(transformRegistry, atLeast(1+2)).retrieveConfig();
        assertEquals(6, transformRegistry.getTransformConfig().getTransformers().size());
    }
}
