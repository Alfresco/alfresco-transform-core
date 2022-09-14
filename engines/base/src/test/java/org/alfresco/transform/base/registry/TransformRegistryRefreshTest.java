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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
        waitForRegistryReady();
        assertEquals(4, transformRegistry.getTransformConfig().getTransformers().size());
        verify(transformRegistry, atLeast(1)).retrieveConfig();

        // As we can't change the content of a classpath resource, lets change what is read.
        ReflectionTestUtils.setField(transformConfigFiles, "files", ImmutableMap.of(
            "a",   "config/addA2B.json",
            "foo", "config/addB2C.json"));
        transformConfigFromFiles.initFileConfig();

        Awaitility.await().pollDelay(3, TimeUnit.SECONDS).until( () -> { // i.e. Thread.sleep(3_000) - but keeps sona happy
            verify(transformRegistry, atLeast(1+2)).retrieveConfig();
            assertEquals(6, transformRegistry.getTransformConfig().getTransformers().size());
            return true;
        });
    }

    private void waitForRegistryReady() throws InterruptedException
    {
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                  .pollInterval(100, TimeUnit.MILLISECONDS)
                  .pollDelay(Duration.ZERO)
                  .until(() -> transformRegistry.isReadyForTransformRequests());
    }
}
