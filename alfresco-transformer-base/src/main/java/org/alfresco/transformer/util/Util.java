/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.transformer.util;

/**
 */
public class Util
{
    /**
     * Safely converts a {@link String} to an {@link Integer}
     *
     * @param param String to be converted
     * @return Null if param is null or converted value as {@link Integer}
     */
    public static Integer stringToInteger(String param)
    {
        return param == null ? null : Integer.parseInt(param);
    }

    /**
     * Safely converts a {@link String} to an {@link Integer}
     *
     * @param param String to be converted
     * @return Null if param is null or converted value as {@link Boolean}
     */
    public static Boolean stringToBoolean(String param)
    {
        return param == null? null : Boolean.parseBoolean(param);
    }
}
