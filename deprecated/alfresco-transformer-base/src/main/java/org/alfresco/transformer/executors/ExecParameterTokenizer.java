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
package org.alfresco.transformer.executors;

import static java.util.Collections.singletonList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 *
 *             DUPLICATED FROM *alfresco-core*.
 *
 *             This class is used to tokenize strings used as parameters for {@link RuntimeExec} objects. Examples of such strings are as follows (ImageMagick-like parameters):
 *             <ul>
 *             <li><tt>-font Helvetica -pointsize 50</tt></li>
 *             <li><tt>-font Helvetica -pointsize 50 -draw "circle 100,100 150,150"</tt></li>
 *             <li><tt>-font Helvetica -pointsize 50 -draw "gravity south fill black text 0,12 'CopyRight'"</tt></li>
 *             </ul>
 *             The first is the simple case which would be parsed into Strings as follows: <tt>"-font", "Helvetica", "-pointsize", "50"</tt>
 *             <p/>
 *             The second is more complex in that it includes a quoted parameter, which would be parsed as a single String: <tt>"-font", "Helvetica", "-pointsize", "50", "circle 100,100 150,150"</tt> Note however that the quotation characters will be stripped from the token.
 *             <p/>
 *             The third shows an example with embedded quotation marks, which would parse to: <tt>"-font", "Helvetica", "-pointsize", "50", "gravity south fill black text 0,12 'CopyRight'"</tt> In this case, the embedded quotation marks (which must be different from those surrounding the parameter) are preserved in the extracted token.
 *             <p/>
 *             The class does not understand escaped quotes such as <tt>p1 p2 "a b c \"hello\" d" p4</tt>
 *
 * @author Neil Mc Erlean
 * @since 3.4.2
 */
@Deprecated
public class ExecParameterTokenizer
{
    /**
     * The string to be tokenized.
     */
    private final String str;

    /**
     * The list of tokens, which will take account of quoted sections.
     */
    private List<String> tokens;

    public ExecParameterTokenizer(String str)
    {
        this.str = str;
    }

    /**
     * This method returns the tokens in a parameter string. Any tokens not contained within single or double quotes will be tokenized in the normal way i.e. by using whitespace separators and the standard StringTokenizer algorithm. Any tokens which are contained within single or double quotes will be returned as single String instances and will have their quote marks removed.
     * <p/>
     * See above for examples.
     *
     * @throws NullPointerException
     *             if the string to be tokenized was null.
     */
    public List<String> getAllTokens()
    {
        if (this.str == null)
        {
            throw new NullPointerException("Illegal null string cannot be tokenized.");
        }

        if (tokens == null)
        {
            tokens = new ArrayList<>();

            // Preserve original behaviour from RuntimeExec.
            if (str.indexOf('\'') == -1 && str.indexOf('"') == -1)
            {
                // Contains no quotes.
                for (StringTokenizer standardTokenizer = new StringTokenizer(
                        str); standardTokenizer.hasMoreTokens();)
                {
                    tokens.add(standardTokenizer.nextToken());
                }
            }
            else
            {
                // There are either single or double quotes or both.
                // So we need to identify the quoted regions within the string.
                List<Pair<Integer, Integer>> quotedRegions = new ArrayList<>();

                for (Pair<Integer, Integer> next = identifyNextQuotedRegion(str, 0); next != null;)
                {
                    quotedRegions.add(next);
                    next = identifyNextQuotedRegion(str, next.getSecond() + 1);
                }

                // Now we've got a List of index pairs identifying the quoted regions.
                // We need to get substrings of quoted and unquoted blocks, whilst maintaining order.
                List<Substring> substrings = getSubstrings(str, quotedRegions);

                for (Substring r : substrings)
                {
                    tokens.addAll(r.getTokens());
                }
            }
        }

        return this.tokens;
    }

    /**
     * The substrings will be a list of quoted and unquoted substrings. The unquoted ones need to be further tokenized in the normal way. The quoted ones must not be tokenized, but need their quotes stripped off.
     */
    private List<Substring> getSubstrings(String str,
            List<Pair<Integer, Integer>> quotedRegionIndices)
    {
        List<Substring> result = new ArrayList<>();

        int cursorPosition = 0;
        for (Pair<Integer, Integer> nextQuotedRegionIndices : quotedRegionIndices)
        {
            if (cursorPosition < nextQuotedRegionIndices.getFirst())
            {
                int startIndexOfNextQuotedRegion = nextQuotedRegionIndices.getFirst() - 1;
                result.add(new UnquotedSubstring(
                        str.substring(cursorPosition, startIndexOfNextQuotedRegion)));
            }
            result.add(new QuotedSubstring(str.substring(nextQuotedRegionIndices.getFirst(),
                    nextQuotedRegionIndices.getSecond())));
            cursorPosition = nextQuotedRegionIndices.getSecond();
        }

        // We've processed all the quoted regions, but there may be a final unquoted region
        if (cursorPosition < str.length() - 1)
        {
            result.add(new UnquotedSubstring(str.substring(cursorPosition, str.length() - 1)));
        }

        return result;
    }

    private Pair<Integer, Integer> identifyNextQuotedRegion(String str, int startingIndex)
    {
        int indexOfNextSingleQuote = str.indexOf('\'', startingIndex);
        int indexOfNextDoubleQuote = str.indexOf('"', startingIndex);

        if (indexOfNextSingleQuote == -1 && indexOfNextDoubleQuote == -1)
        {
            // If there are no more quoted regions
            return null;
        }
        else if (indexOfNextSingleQuote > -1 && indexOfNextDoubleQuote > -1)
        {
            // If there are both single and double quotes in the remainder of the string
            // Then select the closest quote.
            int indexOfNextQuote = Math.min(indexOfNextSingleQuote, indexOfNextDoubleQuote);
            char quoteChar = str.charAt(indexOfNextQuote);

            return findIndexOfClosingQuote(str, indexOfNextQuote, quoteChar);
        }
        else
        {
            // Only one of the quote characters is present.

            int indexOfNextQuote = Math.max(indexOfNextSingleQuote, indexOfNextDoubleQuote);
            char quoteChar = str.charAt(indexOfNextQuote);

            return findIndexOfClosingQuote(str, indexOfNextQuote, quoteChar);
        }
    }

    private Pair<Integer, Integer> findIndexOfClosingQuote(String str, int indexOfStartingQuote,
            char quoteChar)
    {
        // So we know which type of quote char we're dealing with. Either ' or ".
        // Now we need to find the closing quote.
        int indexAfterClosingQuote = str.indexOf(quoteChar,
                indexOfStartingQuote + 1) + 1; // + 1 to search after opening quote. + 1 to give result including closing quote.

        if (indexAfterClosingQuote == 0) // -1 + 1
        {
            // If no closing quote.
            throw new IllegalArgumentException("No closing " + quoteChar + "quote in" + str);
        }

        return new Pair<>(indexOfStartingQuote, indexAfterClosingQuote);
    }

    /**
     * Utility interface for a substring in a parameter string.
     */
    public interface Substring
    {
        /**
         * Gets all the tokens in a parameter string.
         */
        List<String> getTokens();
    }

    /**
     * A substring that is not surrounded by (single or double) quotes.
     */
    public class UnquotedSubstring implements Substring
    {
        private final String regionString;

        public UnquotedSubstring(String str)
        {
            this.regionString = str;
        }

        public List<String> getTokens()
        {
            StringTokenizer t = new StringTokenizer(regionString);
            List<String> result = new ArrayList<>();
            while (t.hasMoreTokens())
            {
                result.add(t.nextToken());
            }
            return result;
        }

        public String toString()
        {
            return UnquotedSubstring.class.getSimpleName() + ": '" + regionString + '\'';
        }
    }

    /**
     * A substring that is surrounded by (single or double) quotes.
     */
    public class QuotedSubstring implements Substring
    {
        private final String regionString;

        public QuotedSubstring(String str)
        {
            this.regionString = str;
        }

        public List<String> getTokens()
        {
            String stringWithoutQuotes = regionString.substring(1, regionString.length() - 1);
            return singletonList(stringWithoutQuotes);
        }

        public String toString()
        {
            return QuotedSubstring.class.getSimpleName() + ": '" + regionString + '\'';
        }
    }

    public static final class Pair<F, S> implements Serializable
    {
        private static final long serialVersionUID = -7406248421185630612L;

        /**
         * The first member of the pair.
         */
        private F first;

        /**
         * The second member of the pair.
         */
        private S second;

        /**
         * Make a new one.
         *
         * @param first
         *            The first member.
         * @param second
         *            The second member.
         */
        public Pair(F first, S second)
        {
            this.first = first;
            this.second = second;
        }

        /**
         * Get the first member of the tuple.
         *
         * @return The first member.
         */
        public final F getFirst()
        {
            return first;
        }

        /**
         * Get the second member of the tuple.
         *
         * @return The second member.
         */
        public final S getSecond()
        {
            return second;
        }

        public final void setFirst(F first)
        {
            this.first = first;
        }

        public final void setSecond(S second)
        {
            this.second = second;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(first, pair.first) &&
                    Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(first, second);
        }

        @Override
        public String toString()
        {
            return "(" + first + ", " + second + ")";
        }
    }
}
