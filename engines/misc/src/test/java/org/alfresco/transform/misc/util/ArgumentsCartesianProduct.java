/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.misc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Creates a cartesian product of arguments provided either in streams or as an objects. Result is a {@link Stream} of JUnit's {@link Arguments}.
 */
public class ArgumentsCartesianProduct
{
    /**
     * Creates cartesian product of fixed argument and a stream of arguments.
     * Example: a ✕ {x,y,z} = {a,x}, {a,y}, {a,z}
     */
    public static Stream<Arguments> of(final Object fixedFirstArgument, final Stream<?> secondArguments)
    {
        return secondArguments.map(secondArgument -> Arguments.of(fixedFirstArgument, secondArgument));
    }

    /**
     * Creates cartesian product of a stream of arguments and fixed arguments.
     * Example: {a,b,c} ✕ y ✕ z = {a,y,z}, {b,y,z}, {c,y,z}
     */
    public static Stream<Arguments> of(final Stream<?> firstArguments, final Object... otherFixedArguments)
    {
        return firstArguments.map(firstArgument -> Arguments.of(firstArgument, otherFixedArguments));
    }

    /**
     * Creates cartesian product of two streams of arguments.
     * Example: {a,b} ✕ {y,z} = {a,y}, {a,z}, {b,y}, {b,z}
     */
    public static Stream<Arguments> of(final Stream<?> firstArguments, final Stream<?> secondArguments)
    {
        return cartesianProductOf(firstArguments, secondArguments).map(arguments -> Arguments.of(arguments.toArray()));
    }

    /**
     * Creates cartesian product of multiple streams of arguments.
     * Example: {a,b} ✕ {k,l,m} ✕ ... ✕ {y,z} = {a,k,...,y}, {a,k,...,z}, {a,l,...,y}, ..., {b,m,...,z}
     */
    public static Stream<Arguments> of(final Stream<?>... argumentsStreams)
    {
        return cartesianProductOf(argumentsStreams).map(arguments -> Arguments.of(arguments.toArray()));
    }

    private static Stream<Stream<?>> cartesianProductOf(final Stream<?>... streams)
    {
        if (streams == null)
        {
            return Stream.empty();
        }

        return Stream.of(streams)
            .filter(Objects::nonNull)
            .map(stream -> stream.map(Collections::<Object>singletonList))
            .reduce((result, nextElements) -> {
                final List<List<Object>> nextElementsCopy = nextElements.collect(Collectors.toList());
                return result.flatMap(resultPortion -> nextElementsCopy.stream().map(nextElementsPortion -> {
                    final List<Object> extendedResultPortion = new ArrayList<>();
                    extendedResultPortion.addAll(resultPortion);
                    extendedResultPortion.addAll(nextElementsPortion);
                    return extendedResultPortion;
                }));
            }).orElse(Stream.empty())
            .map(Collection::stream);
    }
}
