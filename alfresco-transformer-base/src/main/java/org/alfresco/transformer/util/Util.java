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
