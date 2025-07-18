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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CoreFunctionTest
{
    @Test
    void isSupported()
    {
        assertTrue(CoreFunction.HTTP.isSupported("0.1"));
        assertTrue(CoreFunction.HTTP.isSupported("2.5.6"));
        assertFalse(CoreFunction.HTTP.isSupported("100000"));

        assertFalse(CoreFunction.ACTIVE_MQ.isSupported("0.1"));
        assertTrue(CoreFunction.ACTIVE_MQ.isSupported("2.5"));

        assertFalse(CoreFunction.DIRECT_ACCESS_URL.isSupported(null));
        assertFalse(CoreFunction.DIRECT_ACCESS_URL.isSupported(""));
        assertFalse(CoreFunction.DIRECT_ACCESS_URL.isSupported("2.5"));
        assertFalse(CoreFunction.DIRECT_ACCESS_URL.isSupported("2.5.6"));
        assertTrue(CoreFunction.DIRECT_ACCESS_URL.isSupported("2.5.7-SNAPSHOT"));
        assertTrue(CoreFunction.DIRECT_ACCESS_URL.isSupported("2.5.7-A4-SNAPSHOT"));
        assertTrue(CoreFunction.DIRECT_ACCESS_URL.isSupported("2.5.7"));
        assertTrue(CoreFunction.DIRECT_ACCESS_URL.isSupported("2.6"));
        assertTrue(CoreFunction.DIRECT_ACCESS_URL.isSupported("999999"));
    }
}
