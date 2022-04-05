/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.transform.client.registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Wraps an object so that we know where it was read from. The equals() and hashcode() are that of the wrapped object
 * so it is still possible do set operations.
  */
public class Origin<T>
{
    final T t;
    final String baseUrl;
    final String readFrom;

    public Origin(T t, String baseUrl, String readFrom)
    {
        this.t = t;
        this.baseUrl = baseUrl;
        this.readFrom = readFrom;
    }

    public T get()
    {
        return t;
    }
    public String getBaseUrl()
    {
        return baseUrl;
    }

    public String getReadFrom()
    {
        return readFrom;
    }

    public static <T> Set<T> setOf(Collection<Origin<T>> originCollection)
    {
        Set<T> tSet = new HashSet<>(originCollection.size());
        originCollection.forEach(element -> tSet.add(element.get()));
        return tSet;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Origin<?> origin = (Origin<?>)o;
        return t.equals(origin.t);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(t);
    }
}

