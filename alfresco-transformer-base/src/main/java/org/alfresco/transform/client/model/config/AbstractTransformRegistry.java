package org.alfresco.transform.client.model.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used to work out if a transformation is supported. Sub classes should implement {@link #getData()} to return an
 * instance of the {@link Data} class. This allows sub classes to periodically replace the registry's data with newer
 * values. They may also extend the Data class to include extra fields and methods.
 */
public abstract class AbstractTransformRegistry implements TransformServiceRegistry
{
    private static final String TIMEOUT = "timeout";

    public static class Data
    {
        ConcurrentMap<String, ConcurrentMap<String, List<SupportedTransform>>> transformers = new ConcurrentHashMap<>();
        ConcurrentMap<String, ConcurrentMap<String, List<SupportedTransform>>> cachedSupportedTransformList = new ConcurrentHashMap<>();
        protected int transformerCount = 0;
        protected int transformCount = 0;

        @Override
        public String toString()
        {
            return transformerCount == 0 && transformCount == 0
                    ? ""
                    : "(transformers: "+transformerCount+" transforms: "+transformCount+")";
        }
    }

    private static class SupportedTransform
    {
        TransformOptionGroup transformOptions;
        long maxSourceSizeBytes;
        private String name;
        private int priority;

        public SupportedTransform(Data data, String name, Set<TransformOption> transformOptions, long maxSourceSizeBytes, int priority)
        {
            // Logically the top level TransformOptionGroup is required, so that child options are optional or required
            // based on their own setting.
            this.transformOptions = new TransformOptionGroup(true, transformOptions);
            this.maxSourceSizeBytes = maxSourceSizeBytes;
            this.name = name;
            this.priority = priority;
            data.transformCount++;
        }
    }

    /**
     * Logs an error message if there is an error in the configuration supplied to the
     * {@link #register(Transformer, Map, String)}.
     */
    protected abstract void logError(String msg);

    /**
     * Returns the data held by the registry. Sub classes may extend the base Data and replace it at run time.
     */
    protected abstract Data getData();

    public void register(TransformConfig transformConfig, String readFrom)
    {
        Map<String, Set<TransformOption>> transformOptions = transformConfig.getTransformOptions();
        List<Transformer> transformers = transformConfig.getTransformers();
        transformers.forEach(transformer ->register(transformer, transformOptions, readFrom));
    }

    protected void register(Transformer transformer, Map<String, Set<TransformOption>> transformOptions, String readFrom)
    {
        Data data = getData();
        data.transformerCount++;
        transformer.getSupportedSourceAndTargetList().forEach(
                e -> data.transformers.computeIfAbsent(e.getSourceMediaType(),
                        k -> new ConcurrentHashMap<>()).computeIfAbsent(e.getTargetMediaType(),
                        k -> new ArrayList<>()).add(
                        new SupportedTransform(data, transformer.getTransformerName(),
                                lookupTransformOptions(transformer.getTransformOptions(), transformOptions, readFrom),
                                e.getMaxSourceSizeBytes(), e.getPriority())));
    }

    private Set<TransformOption> lookupTransformOptions(Set<String> transformOptionNames,
                                                        Map<String, Set<TransformOption>> transformOptions,
                                                        String readFrom)
    {
        List<TransformOptionGroup> list = new ArrayList<>();

        for (String name : transformOptionNames)
        {
            Set<TransformOption> oneSetOfTransformOptions = transformOptions.get(name);
            if (oneSetOfTransformOptions == null)
            {
                logError("transformOptions in "+readFrom+" with the name "+name+" does not exist. Ignored");
                continue;
            }
            TransformOptionGroup transformOptionGroup = new TransformOptionGroup(true, oneSetOfTransformOptions);
            list.add(transformOptionGroup);
        }

        return list.isEmpty() ? Collections.emptySet()
                : list.size() == 1 ? list.get(0).getTransformOptions()
                : new HashSet<>(list);
    }

    @Override
    public boolean isSupported(String sourceMimetype, long sourceSizeInBytes, String targetMimetype,
                               Map<String, String> actualOptions, String renditionName)
    {
        long maxSize = getMaxSize(sourceMimetype, targetMimetype, actualOptions, renditionName);
        return maxSize != 0 && (maxSize == -1L || maxSize >= sourceSizeInBytes);
    }

    /**
     * Works out the name of the transformer (might not map to an actual transformer) that will be used to transform
     * content of a given source mimetype and size into a target mimetype given a list of actual transform option names
     * and values (Strings) plus the data contained in the Transform objects registered with this class.
     * @param sourceMimetype the mimetype of the source content
     * @param sourceSizeInBytes the size in bytes of the source content. Ignored if negative.
     * @param targetMimetype the mimetype of the target
     * @param actualOptions the actual name value pairs available that could be passed to the Transform Service.
     * @param renditionName (optional) name for the set of options and target mimetype. If supplied is used to cache
     *                      results to avoid having to work out if a given transformation is supported a second time.
     *                      The sourceMimetype and sourceSizeInBytes may still change. In the case of ACS this is the
     *                      rendition name.
     */
    @Override
    public String getTransformerName(String sourceMimetype, long sourceSizeInBytes, String targetMimetype, Map<String, String> actualOptions, String renditionName)
    {
        List<SupportedTransform> supportedTransforms = getTransformListBySize(sourceMimetype, targetMimetype, actualOptions, renditionName);
        for (SupportedTransform supportedTransform : supportedTransforms)
        {
            if (supportedTransform.maxSourceSizeBytes == -1 || supportedTransform.maxSourceSizeBytes >= sourceSizeInBytes)
            {
                return supportedTransform.name;
            }
        }

        return null;
    }

    @Override
    public long getMaxSize(String sourceMimetype, String targetMimetype,
                           Map<String, String> actualOptions, String renditionName)
    {
        List<SupportedTransform> supportedTransforms = getTransformListBySize(sourceMimetype, targetMimetype, actualOptions, renditionName);
        return supportedTransforms.isEmpty() ? 0 : supportedTransforms.get(supportedTransforms.size()-1).maxSourceSizeBytes;
    }

    // Returns transformers in increasing supported size order, where lower priority transformers for the same size have
    // been discarded.
    private List<SupportedTransform> getTransformListBySize(String sourceMimetype, String targetMimetype,
                                                            Map<String, String> actualOptions, String renditionName)
    {
        if (actualOptions == null)
        {
            actualOptions = Collections.emptyMap();
        }
        if (renditionName != null && renditionName.trim().isEmpty())
        {
            renditionName = null;
        }

        Data data = getData();
        List<SupportedTransform> transformListBySize = renditionName == null ? null
                : data.cachedSupportedTransformList.computeIfAbsent(renditionName, k -> new ConcurrentHashMap<>()).get(sourceMimetype);
        if (transformListBySize != null)
        {
            return transformListBySize;
        }

        // Remove the "timeout" property from the actualOptions as it is not used to select a transformer.
        if (actualOptions.containsKey(TIMEOUT))
        {
            actualOptions = new HashMap(actualOptions);
            actualOptions.remove(TIMEOUT);
        }

        transformListBySize = new ArrayList<>();
        ConcurrentMap<String, List<SupportedTransform>> targetMap = data.transformers.get(sourceMimetype);
        if (targetMap !=  null)
        {
            List<SupportedTransform> supportedTransformList = targetMap.get(targetMimetype);
            if (supportedTransformList != null)
            {
                for (SupportedTransform supportedTransform : supportedTransformList)
                {
                    TransformOptionGroup transformOptions = supportedTransform.transformOptions;
                    Map<String, Boolean> possibleTransformOptions = new HashMap<>();
                    addToPossibleTransformOptions(possibleTransformOptions, transformOptions, true, actualOptions);
                    if (isSupported(possibleTransformOptions, actualOptions))
                    {
                        addToSupportedTransformList(transformListBySize, supportedTransform);
                    }
                }
            }
        }

        if (renditionName != null)
        {
            data.cachedSupportedTransformList.get(renditionName).put(sourceMimetype, transformListBySize);
        }

        return transformListBySize;
    }

    // Add newTransform to the transformListBySize in increasing size order and discards lower priority (numerically
    // higher) transforms with a smaller or equal size.
    private void addToSupportedTransformList(List<SupportedTransform> transformListBySize, SupportedTransform newTransform)
    {
        for (int i=0; i < transformListBySize.size(); i++)
        {
            SupportedTransform existingTransform = transformListBySize.get(i);
            int added = -1;
            int compare = compare(newTransform.maxSourceSizeBytes, existingTransform.maxSourceSizeBytes);
            if (compare < 0)
            {
                transformListBySize.add(i, newTransform);
                added = i;
            }
            else if (compare == 0)
            {
                if (newTransform.priority < existingTransform.priority)
                {
                    transformListBySize.set(i, newTransform);
                    added = i;
                }
            }
            if (added == i)
            {
                for (i--; i >= 0; i--)
                {
                    existingTransform = transformListBySize.get(i);
                    if (newTransform.priority <= existingTransform.priority)
                    {
                        transformListBySize.remove(i);
                    }
                }
                return;
            }
        }
        transformListBySize.add(newTransform);
    }

    // compare where -1 is unlimited.
    private int compare(long a, long b)
    {
        return a == -1
                ? b == -1 ? 0 : 1
                : b == -1 ? -1
                : a == b ? 0
                : a > b ? 1 : -1;
    }

    /**
     * Flatten out the transform options by adding them to the supplied possibleTransformOptions.</p>
     *
     * If possible discards options in the supplied transformOptionGroup if the group is optional and the actualOptions
     * don't provide any of the options in the group. Or to put it another way:<p/>
     *
     * It adds individual transform options from the transformOptionGroup to possibleTransformOptions if the group is
     * required or if the actualOptions include individual options from the group. As a result it is possible that none
     * of the group are added if it is optional. It is also possible to add individual transform options that are
     * themselves required but not in the actualOptions. In this the isSupported method will return false.
     * @return true if any options were added. Used by nested call parents to determine if an option was added from a
     * nested sub group.
     */
    boolean addToPossibleTransformOptions(Map<String, Boolean> possibleTransformOptions,
                                          TransformOptionGroup transformOptionGroup,
                                          Boolean parentGroupRequired, Map<String, String> actualOptions)
    {
        boolean added = false;
        boolean required = false;

        Set<TransformOption> optionList = transformOptionGroup.getTransformOptions();
        if (optionList != null && !optionList.isEmpty())
        {
            // We need to avoid adding options from a group that is required but its parents are not.
            boolean transformOptionGroupRequired = transformOptionGroup.isRequired() && parentGroupRequired;

            // Check if the group contains options in actualOptions. This will add any options from sub groups.
            for (TransformOption transformOption : optionList)
            {
                if (transformOption instanceof TransformOptionGroup)
                {
                    added = addToPossibleTransformOptions(possibleTransformOptions, (TransformOptionGroup) transformOption,
                            transformOptionGroupRequired, actualOptions);
                    required |= added;
                }
                else
                {
                    String name = ((TransformOptionValue) transformOption).getName();
                    if (actualOptions.containsKey(name))
                    {
                        required = true;
                    }
                }
            }

            if (required || transformOptionGroupRequired)
            {
                for (TransformOption transformOption : optionList)
                {
                    if (transformOption instanceof TransformOptionValue)
                    {
                        added = true;
                        TransformOptionValue transformOptionValue = (TransformOptionValue) transformOption;
                        String name = transformOptionValue.getName();
                        boolean optionValueRequired = transformOptionValue.isRequired();
                        possibleTransformOptions.put(name, optionValueRequired);
                    }
                }
            }
        }

        return added;
    }

    boolean isSupported(Map<String, Boolean> transformOptions, Map<String, String> actualOptions)
    {
        boolean supported = true;

        // Check all required transformOptions are supplied
        for (Map.Entry<String, Boolean> transformOption : transformOptions.entrySet())
        {
            Boolean required = transformOption.getValue();
            if (required)
            {
                String name = transformOption.getKey();
                if (!actualOptions.containsKey(name))
                {
                    supported = false;
                    break;
                }
            }
        }

        if (supported)
        {
            // Check there are no extra unused actualOptions
            for (String actualOption : actualOptions.keySet())
            {
                if (!transformOptions.containsKey(actualOption))
                {
                    supported = false;
                    break;
                }
            }
        }
        return supported;
    }
}
