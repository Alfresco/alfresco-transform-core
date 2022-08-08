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
 * The client data supplied and echoed back to content repository (the client). May be modified to include
 * TransformerDebug.
 */
public class RepositoryClientData
{
    public static final String CLIENT_DATA_SEPARATOR = "\u23D0";
    public static final String DEBUG_SEPARATOR = "\u23D1";
    static final String REPO_ID = "Repo";
    public static final String DEBUG = "debug:";

    private final String origClientData;
    private final String[] split;

    public RepositoryClientData(String clientData)
    {
        origClientData = clientData;
        split = clientData == null ? null : clientData.split(CLIENT_DATA_SEPARATOR);
    }

    private boolean isRepositoryClientData()
    {
        return split != null && split.length == 10 && split[0].startsWith(REPO_ID);
    }

    public String getAcsVersion()
    {
        return isRepositoryClientData() ? split[0].substring(REPO_ID.length()) : "";
    }

    public int getRequestId()
    {
        try
        {
            return isRepositoryClientData() ? Integer.parseInt(split[6]) : -1;
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    public String getRenditionName()
    {
        return isRepositoryClientData() ? split[2] : "";
    }

    public void appendDebug(String message)
    {
        if (isDebugRequested())
        {
            split[9] += DEBUG_SEPARATOR+ message;
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
        Stream.of(split).forEach(element -> sj.add(element));
        return sj.toString();
    }
}
