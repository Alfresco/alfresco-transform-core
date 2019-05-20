/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
