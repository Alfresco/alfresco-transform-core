package org.alfresco.transformer.executors;

import java.io.File;

import org.alfresco.transform.exceptions.TransformException;

/**
 * Basic interface for executing transformations inside Java/JVM
 * 
 * @author Cezar Leahu
 */
public interface JavaExecutor
{
    void call(File sourceFile, File targetFile, String... args) throws TransformException;
}
