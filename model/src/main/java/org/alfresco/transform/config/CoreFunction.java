/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2025 Alfresco Software Limited
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
package org.alfresco.transform.config;

import static org.alfresco.transform.config.CoreFunction.Constants.NO_UPPER_VERSION;
import static org.alfresco.transform.config.CoreFunction.Constants.NO_VERSION;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Provides a mapping between a transform {@code coreVersion} and functionality (such as the use of Direct Access URLs) supported in that version of the {@code alfresco-transform-base}, so that clients know if they may use it.
 */
public enum CoreFunction
{
    /** May provide a Direct Access URL rather than upload a file **/
    DIRECT_ACCESS_URL("2.5.7", null),

    /** May request a transform via ActiveMQ **/
    // Original version was HTTP only. However none of these are still operational
    ACTIVE_MQ("1", null),

    /** Original way to talk to a T-Engine **/
    // The toValue really should be null rather than "9999" but gives us an upper test value
    HTTP(null, "99999"),

    /** Additional transform option to preserve original file name **/
    SOURCE_FILENAME("5.1.9", null);

    private final ComparableVersion fromVersion;
    private final ComparableVersion toVersion;

    public boolean isSupported(String version)
    {
        ComparableVersion comparableVersion = newComparableVersion(version, Constants.NO_VERSION);
        return comparableVersion.compareTo(fromVersion) >= 0 && comparableVersion.compareTo(toVersion) <= 0;
    }

    public static String standardizeCoreVersion(String version)
    {
        return newComparableVersion(version, NO_VERSION).toString();
    }

    CoreFunction(String fromVersion, String toVersion)
    {
        this.fromVersion = newComparableVersion(fromVersion, NO_VERSION);
        this.toVersion = newComparableVersion(toVersion, NO_UPPER_VERSION);
    }

    static ComparableVersion newComparableVersion(String version, ComparableVersion defaultValue)
    {
        if (version == null)
        {
            return defaultValue;
        }

        int i = version.indexOf('-');
        version = i > 0
                ? version.substring(0, i)
                : version;

        return new ComparableVersion(version);
    }

    static class Constants
    {
        static final ComparableVersion NO_VERSION = new ComparableVersion("");
        static final ComparableVersion NO_UPPER_VERSION = new ComparableVersion(Integer.toString(Integer.MAX_VALUE));
    }
}
