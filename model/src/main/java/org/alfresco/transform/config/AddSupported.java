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
package org.alfresco.transform.config;

/**
 * Holds information to add new {@link SupportedSourceAndTarget} objects to an existing {@link Transformer}.
 * <p>
 * <br>
 *
 * <pre>
 *   "addSupported": [
 *     {
 *       "transformerName": "Archive",
 *       "sourceMediaType": "application/zip",
 *       "targetMediaType": "text/xml",
 *       "priority": 60,
 *       "maxSourceSizeBytes": 18874368
 *     }
 *   ]
 * </pre>
 */
public class AddSupported extends TransformerTypesSizeAndPriority
{
    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder extends TransformerTypesSizeAndPriority.Builder<Builder, AddSupported>
    {
        private Builder()
        {
            super(new AddSupported());
        }
    }
}
