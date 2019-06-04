package org.alfresco.transformer.transformers;

import java.io.File;
import java.util.Map;

public interface JavaTransformer
{
    public void transform(File sourceFile, File targetFile, Map<String, String> parameters) throws Exception;
}
