/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transform.common;

import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.messages.TransformStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class TransformerDebug
{
    public static final Logger logger = LoggerFactory.getLogger(TransformerDebug.class);

    private static final int REFERENCE_SIZE = 15;

    private static final String TRANSFORM_NAMESPACE = "transform:";

    public static final String MIMETYPE_METADATA_EXTRACT = "alfresco-metadata-extract";

    public static final String MIMETYPE_METADATA_EMBED = "alfresco-metadata-embed";

    private static final String TIMEOUT =  "timeout";

    // For truncating long option values
    private static int MAX_OPTION_VALUE = 60;
    private static int MAX_OPTION_END_CHARS = 5;
    private static String MAX_OPTION_DOTS = "...";

    private boolean isTEngine = false;

    public void pushTransform(TransformRequest request)
    {
        RepositoryClientData repositoryClientData = new RepositoryClientData(request.getClientData());
        if (isEnabled(repositoryClientData))
        {
            TransformStack.Step step = TransformStack.currentStep(request.getInternalContext());
            String reference = TransformStack.getReference(request.getInternalContext());
            boolean isTopLevel = isTopLevel(reference);

            String message = getPaddedReference(reference) +
                    getMimetypeExt(step.getSourceMediaType()) +
                    getTargetMimetypeExt(step.getTargetMediaType(), step.getSourceMediaType()) + ' ' +
                    (isTopLevel || isTEngine()
                     ? fileSize(request.getSourceSize()) + ' ' +
                       getRenditionName(new RepositoryClientData(request.getClientData()).getRenditionName())
                     : "") +
                    step.getTransformerName();
            if (isDebugToBeReturned(repositoryClientData))
            {
                repositoryClientData.appendDebug(message);
                request.setClientData(repositoryClientData.toString());
            }
            logger.debug(message);
        }
    }

    /**
     * @deprecated Only called from the deprecated transformer base
     */
    @Deprecated
    public void pushTransform(String reference, String sourceMimetype, String targetMimetype, File sourceFile, String transformerName)
    {
        final long sourceSizeInBytes = sourceFile.length();
        pushTransform(reference, sourceMimetype, targetMimetype, sourceSizeInBytes, transformerName);
    }

    public void pushTransform(String reference, String sourceMimetype, String targetMimetype, long sourceSizeInBytes, String transformerName)
    {
        if (logger.isDebugEnabled())
        {
            String message = getPaddedReference(reference) +
                    getMimetypeExt(sourceMimetype) +
                    getTargetMimetypeExt(targetMimetype, sourceMimetype) + ' ' +
                    fileSize(sourceSizeInBytes) + ' ' +
                    transformerName;
            logger.debug(message);
        }
    }

    public void popTransform(TransformReply reply)
    {
        if (logger.isDebugEnabled())
        {
            InternalContext internalContext = reply.getInternalContext();
            String reference = TransformStack.getReference(internalContext);
            long elapsedTime = TransformStack.getElapsedTime(internalContext);
            popTransform(reference, elapsedTime);
        }
    }

    public void popTransform(String reference, long elapsedTime)
    {
        if (logger.isDebugEnabled())
        {
            String message = getPaddedReference(reference) + "Finished in " + ms(elapsedTime);
            if (isTopLevel(reference) || isTEngine())
            {
                logger.debug(message);
            }
            else
            {
                logger.trace(message);
            }
            // We don't append the Finished message to ClientData as that would be too much
        }
    }

    public void logOptions(TransformRequest request)
    {
        RepositoryClientData repositoryClientData = new RepositoryClientData(request.getClientData());
        if (isEnabled(repositoryClientData))
        {
            Map<String, String> options = request.getTransformRequestOptions();
            if (options != null && !options.isEmpty())
            {
                String reference = TransformStack.getReference(request.getInternalContext());
                for (Map.Entry<String, String> option : options.entrySet())
                {
                    String key = option.getKey();
                    if (!TIMEOUT.equals(key))
                    {
                        String value = option.getValue();
                        String message = getOptionAndValue(reference, key, value);
                        logger.debug(message);
                        if (isDebugToBeReturned(repositoryClientData))
                        {
                            repositoryClientData.appendDebug(message);
                        }
                    }
                }
                request.setClientData(repositoryClientData.toString());
            }
        }
    }


    public void logOptions(String reference, Map<String, String> options)
    {
        if (logger.isDebugEnabled() && options != null && !options.isEmpty())
        {
            for (Map.Entry<String, String> option : options.entrySet())
            {
                String key = option.getKey();
                if (!TIMEOUT.equals(key))
                {
                    String value = option.getValue();
                    String message = getOptionAndValue(reference, key, value);
                    logger.debug(message);
                }
            }
        }
    }

    String getOptionAndValue(String reference, String key, String value)
    {
        // Truncate the value if it is long or needs to be protected, like Direct Access Urls
        int len = value.length();
        if (len > MAX_OPTION_VALUE)
        {
            value = value.substring(0, MAX_OPTION_VALUE-MAX_OPTION_DOTS.length()-MAX_OPTION_END_CHARS) +
                    MAX_OPTION_DOTS +value.substring(len-MAX_OPTION_END_CHARS);
        }
        return getPaddedReference(reference) +
                "  " + key + "=\"" + value.replaceAll("\"", "\\\"") + "\"";
    }

    public void logFragment(String reference, int index, long size)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(getPaddedReference(reference) + "  fragment["+index+"] "+fileSize(size));
        }
    }

    public void logFailure(TransformReply reply)
    {
        RepositoryClientData repositoryClientData = new RepositoryClientData(reply.getClientData());
        if (isEnabled(repositoryClientData))
        {
            String reference = TransformStack.getReference(reply.getInternalContext());
            String message = getPaddedReference(reference) + reply.getErrorDetails();
            logger.debug(message);
            if (isDebugToBeReturned(repositoryClientData))
            {
                repositoryClientData.appendDebug(message);
                reply.setClientData(repositoryClientData.toString());
            }
        }
    }

    public void logFailure(String reference, String message)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(getPaddedReference(reference) + message);
        }
    }

    // T-Engines call this method, as the T-Router will appended the same debug messages
    public TransformerDebug setIsTEngine(boolean isTEngine)
    {
        this.isTEngine = isTEngine;
        return this;
    }

    private boolean isEnabled(RepositoryClientData repositoryClientData)
    {
        return logger.isDebugEnabled() || isDebugToBeReturned(repositoryClientData);
    }

    private boolean isDebugToBeReturned(RepositoryClientData repositoryClientData)
    {
        return !isTEngine() && repositoryClientData.isDebugRequested();
    }

    private boolean isTEngine()
    {
        return isTEngine;
    }

    private boolean isTopLevel(String reference)
    {
        return !reference.contains(".");
    }

    private String getPaddedReference(String reference)
    {
        return reference + spaces(REFERENCE_SIZE + 3 - reference.length()); // 3 for "a) " ordered list
    }

    private String getMimetypeExt(String mimetype)
    {
        return padExt(ExtensionService.getExtensionForMimetype(mimetype), mimetype);
    }

    public String getTargetMimetypeExt(String targetMimetype, String sourceMimetype)
    {
        return padExt(ExtensionService.getExtensionForTargetMimetype(targetMimetype, sourceMimetype), targetMimetype);
    }

    private String padExt(String mimetypeExt, String mimetype)
    {
        StringBuilder sb = new StringBuilder("");
        if (mimetypeExt == null)
        {
            sb.append(mimetype);
        }
        else
        {
            sb.append(mimetypeExt);
            sb.append(spaces(4 - mimetypeExt.length()));   // Pad to normal max ext (4)
        }
        sb.append(' ');
        return sb.toString();
    }

    private String getRenditionName(String renditionName)
    {
        return !renditionName.isEmpty()
                ? "-- "+ replaceWithMetadataRenditionNameIfEmbedOrExtract(renditionName)+" -- "
                : "";
    }

    private static String replaceWithMetadataRenditionNameIfEmbedOrExtract(String renditionName)
    {
        String transformName = getTransformName(renditionName);
        return    transformName.startsWith(MIMETYPE_METADATA_EXTRACT)
                ? "metadataExtract"
                : transformName.startsWith(MIMETYPE_METADATA_EMBED)
                ? "metadataEmbed"
                : renditionName;
    }

    private static String getTransformName(String renditionName)
    {
        return !renditionName.startsWith(TRANSFORM_NAMESPACE)
                ? ""
                : renditionName.substring(TRANSFORM_NAMESPACE.length());
    }

    private String spaces(int i)
    {
        StringBuilder sb = new StringBuilder("");
        while (--i >= 0)
        {
            sb.append(' ');
        }
        return sb.toString();
    }

    private String ms(long time)
    {
        return String.format("%,d ms", time);
    }

    private String fileSize(long size)
    {
        if (size < 0)
        {
            return "unlimited";
        }
        if (size == 1)
        {
            return "1 byte";
        }
        final String[] units = new String[] { "bytes", "KB", "MB", "GB", "TB" };
        long divider = 1;
        for(int i = 0; i < units.length-1; i++)
        {
            long nextDivider = divider * 1024;
            if (size < nextDivider)
            {
                return fileSizeFormat(size, divider, units[i]);
            }
            divider = nextDivider;
        }
        return fileSizeFormat(size, divider, units[units.length-1]);
    }

    private String fileSizeFormat(long size, long divider, String unit)
    {
        size = size * 10 / divider;
        int decimalPoint = (int) size % 10;

        StringBuilder sb = new StringBuilder();
        sb.append(size/10);
        if (decimalPoint != 0)
        {
            sb.append(".");
            sb.append(decimalPoint);
        }
        sb.append(' ');
        sb.append(unit);

        return sb.toString();
    }
}
