/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
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
package org.alfresco.transform.registry;

import org.alfresco.transform.config.OverrideSupported;

/**
 * Holds override information for deferred processing after wildcard generation.
 */
public class DeferredOverride
{
    private final OverrideSupported overrideSupported;
    private final String readFrom;

    public DeferredOverride(OverrideSupported overrideSupported, String readFrom)
    {
        this.overrideSupported = overrideSupported;
        this.readFrom = readFrom;
    }

    public OverrideSupported getOverrideSupported()
    {
        return overrideSupported;
    }

    public String getReadFrom()
    {
        return readFrom;
    }
}
