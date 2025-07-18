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
package org.alfresco.transform.common;

import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * The client data supplied and echoed back to content repository (the client). May be modified to include TransformerDebug.
 */
public class RepositoryClientData
{
    public static final String CLIENT_DATA_SEPARATOR = "\u23D0";
    public static final String DEBUG_SEPARATOR = "\u23D1";
    public static final String REPO_ID = "Repo";
    public static final String DEBUG = "debug:";
    private static final String NO_DEBUG = "nodebug:";

    private static final int REPO_INDEX = 0;
    private static final int RENDITION_INDEX = 2;
    private static final int REQUEST_ID_INDEX = 6;
    private static final int DEBUG_INDEX = 9;
    private static final int EXPECTED_ELEMENTS = 10;

    private final String origClientData;

    private final String[] split;

    public RepositoryClientData(String clientData)
    {
        origClientData = clientData;
        split = clientData == null ? null : clientData.split(CLIENT_DATA_SEPARATOR);
    }

    private boolean isRepositoryClientData()
    {
        return split != null && split.length == EXPECTED_ELEMENTS && split[REPO_INDEX].startsWith(REPO_ID);
    }

    public String getAcsVersion()
    {
        return isRepositoryClientData() ? split[REPO_INDEX].substring(REPO_ID.length()) : "";
    }

    public String getRequestId()
    {
        return isRepositoryClientData() ? split[REQUEST_ID_INDEX] : "";
    }

    public String getRenditionName()
    {
        return isRepositoryClientData() ? split[RENDITION_INDEX] : "";
    }

    public void appendDebug(String message)
    {
        if (isDebugRequested())
        {
            split[DEBUG_INDEX] += DEBUG_SEPARATOR + message;
        }
    }

    public boolean isDebugRequested()
    {
        return isRepositoryClientData() && split[9].startsWith(DEBUG);
    }

    @Override
    public String toString()
    {
        if (split == null)
        {
            return origClientData;
        }
        StringJoiner sj = new StringJoiner(CLIENT_DATA_SEPARATOR);
        Stream.of(split).forEach(sj::add);
        return sj.toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final RepositoryClientData clientData = new RepositoryClientData(emptyClientData());

        private Builder()
        {}

        private static String emptyClientData()
        {
            StringJoiner sj = new StringJoiner(CLIENT_DATA_SEPARATOR);
            sj.add(REPO_ID + "ACS1234");
            for (int i = 0; i < EXPECTED_ELEMENTS - 2; i++)
            {
                sj.add("");
            }
            sj.add(NO_DEBUG);
            return sj.toString();
        }

        public Builder withRepoId(final String version)
        {
            clientData.split[REPO_INDEX] = REPO_ID + version;
            return this;
        }

        public Builder withRequestId(final String requestId)
        {
            clientData.split[REQUEST_ID_INDEX] = requestId;
            return this;
        }

        public Builder withRenditionName(final String renditionName)
        {
            clientData.split[RENDITION_INDEX] = renditionName;
            return this;
        }

        public Builder withDebug()
        {
            clientData.split[DEBUG_INDEX] = DEBUG;
            return this;
        }

        public Builder withDebugMessage(final String message)
        {
            clientData.split[DEBUG_INDEX] = DEBUG + DEBUG_SEPARATOR + message;
            return this;
        }

        public RepositoryClientData build()
        {
            return clientData;
        }
    }
}
