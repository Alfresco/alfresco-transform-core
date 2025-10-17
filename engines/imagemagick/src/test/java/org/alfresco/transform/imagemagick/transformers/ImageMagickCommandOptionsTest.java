package org.alfresco.transform.imagemagick.transformers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {ImageMagickCommandOptions.class})
class ImageMagickCommandOptionsTest
{
    @Value("${transform.core.imagemagick.commandOptions.enabled}")
    boolean commandOptionsEnabled;

    @Test
    void shouldBeDisabledByDefault()
    {
        assertFalse(commandOptionsEnabled);
    }
}
