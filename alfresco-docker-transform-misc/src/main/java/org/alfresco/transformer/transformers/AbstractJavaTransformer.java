package org.alfresco.transformer.transformers;

import org.alfresco.transformer.logging.LogEntry;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 *
 * Extended by transformers which wish to be registered with the {@link SelectingTransformer}.
 *
 * @author eknizat
 */
public abstract class AbstractJavaTransformer
{

    public AbstractJavaTransformer(SelectingTransformer miscTransformer)
    {
        miscTransformer.register(this);
    }

    public void transform(File sourceFile, File targetFile, Map<String, String> parameters) throws Exception
    {
        // Check parameters against required options
        // TODO Should we check the actual values? - Maybe check length/type in the implementing class if needed
        Map<String, String> tempParameters = new HashMap<>(parameters);
        Set<String> tempOptionNames = new HashSet<>(getRequiredOptionNames());
        tempParameters.keySet().retainAll(tempOptionNames);
        if (tempParameters.size() != tempOptionNames.size())
        {
            tempOptionNames.removeAll(tempParameters.keySet());
            throw new IllegalArgumentException("The following required parameters are missing: " + tempOptionNames);
        }

        // Set log options
        StringJoiner sj = new StringJoiner(" ");
        parameters.forEach( (k, v) -> sj.add("--" + k + "=" + v)); // keeping the existing style used in other T-Engines
        sj.add(getExtension(sourceFile));
        sj.add(getExtension(targetFile));
        LogEntry.setOptions(sj.toString());

        transformInternal(sourceFile, targetFile, parameters);
    }

    private String getExtension(File file)
    {
        String name = file.getName();
        int i = name.lastIndexOf('.');
        String ext = i == -1 ? "???" : name.substring(i + 1);
        return ext;
    }

    /**
     *
     * @return Set of expected option names required by this transformer.
     */
    public abstract Set<String> getRequiredOptionNames();

    /**
     * Determine whether this transformer is applicable for the given MIME types.
     * @param sourceMimetype
     * @param targetMimetype
     * @param parameters
     * @return
     * @throws Exception
     */
    public abstract boolean isTransformable(String sourceMimetype, String targetMimetype, Map<String, String> parameters)
            throws Exception;

    /**
     * Implementation of the actual transform.
     * @param sourceFile
     * @param targetFile
     * @param parameters
     * @throws Exception
     */
    abstract void transformInternal(File sourceFile, File targetFile, Map<String, String> parameters) throws Exception;
}
