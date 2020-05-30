/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005-2020 Alfresco Software Limited
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.exceptions.TransformException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Helper methods for MetadataExtractors.
 *
 * @author Jesper Steen Møller
 * @author Derek Hulley
 * @author adavis
 */
public class AbstractMetadataExtractor
{
    private final ObjectMapper jsonObjectMapper = new ObjectMapper();

    /**
     * Adds a value to the map, conserving null values.  Values are converted to null if:
     * <ul>
     *   <li>it is an empty string value after trimming</li>
     *   <li>it is an empty collection</li>
     *   <li>it is an empty array</li>
     * </ul>
     * String values are trimmed before being put into the map.
     * Otherwise, it is up to the extracter to ensure that the value is a <tt>Serializable</tt>.
     * It is not appropriate to implicitly convert values in order to make them <tt>Serializable</tt>
     * - the best conversion method will depend on the value's specific meaning.
     *
     * @param key           the destination key
     * @param value         the serializable value
     * @param destination   the map to put values into
     * @return              Returns <tt>true</tt> if set, otherwise <tt>false</tt>
     */
    // Copied from the content repository's AbstractMappingMetadataExtractor.
    protected boolean putRawValue(String key, Serializable value, Map<String, Serializable> destination)
    {
        if (value == null)
        {
            // Just keep this
        }
        else if (value instanceof String)
        {
            String valueStr = ((String) value).trim();
            if (valueStr.length() == 0)
            {
                value = null;
            }
            else
            {
                if(valueStr.indexOf("\u0000") != -1)
                {
                    valueStr = valueStr.replaceAll("\u0000", "");
                }
                // Keep the trimmed value
                value = valueStr;
            }
        }
        else if (value instanceof Collection)
        {
            Collection<?> valueCollection = (Collection<?>) value;
            if (valueCollection.isEmpty())
            {
                value = null;
            }
        }
        else if (value.getClass().isArray())
        {
            if (Array.getLength(value) == 0)
            {
                value = null;
            }
        }
        // It passed all the tests
        destination.put(key, value);
        return true;
    }

    protected void writeMetadataIntoTargetFile(File targetFile, Map<String, Serializable> results, Logger logger)
            throws IOException
    {
        jsonObjectMapper.writeValue(targetFile, results);
    }
}
