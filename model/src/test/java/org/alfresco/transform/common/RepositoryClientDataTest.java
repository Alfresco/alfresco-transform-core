/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
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

import org.junit.jupiter.api.Test;

import java.util.StringJoiner;

import static org.alfresco.transform.common.RepositoryClientData.CLIENT_DATA_SEPARATOR;
import static org.alfresco.transform.common.RepositoryClientData.DEBUG;
import static org.alfresco.transform.common.RepositoryClientData.DEBUG_SEPARATOR;
import static org.alfresco.transform.common.RepositoryClientData.REPO_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositoryClientDataTest
{
    RepositoryClientData repositoryClientData;

    @Test
    void AcsClientDataWithDebugTest()
    {
        repositoryClientData = RepositoryClientData.builder()
                                   .withRepoId("ACS1234")
                                   .withRenditionName("renditionName")
                                   .withRequestId(54321)
                                   .withDebug()
                                   .build();
        String clientData = repositoryClientData.toString();

        assertEquals("ACS1234", repositoryClientData.getAcsVersion());
        assertEquals("renditionName", repositoryClientData.getRenditionName());
        assertEquals(54321, repositoryClientData.getRequestId());
        assertTrue(repositoryClientData.isDebugRequested());
        assertEquals(clientData, repositoryClientData.toString());
    }

    @Test
    void AcsClientDataWithoutDebugTest()
    {
        String clientData = new StringJoiner(CLIENT_DATA_SEPARATOR)
            .add(REPO_ID + "ACS1234")
            .add("1")
            .add("renditionName")
            .add("3")
            .add("4")
            .add("5")
            .add("54321")
            .add("7")
            .add("8")
            .add("9")
            .toString();
        repositoryClientData = new RepositoryClientData(clientData);

        assertEquals("ACS1234", repositoryClientData.getAcsVersion());
        assertEquals("renditionName", repositoryClientData.getRenditionName());
        assertEquals(54321, repositoryClientData.getRequestId());
        assertFalse(repositoryClientData.isDebugRequested());
        assertEquals(clientData, repositoryClientData.toString());
    }

    @Test
    void noLeadingRepoTest()
    {
        String clientData = new StringJoiner(CLIENT_DATA_SEPARATOR)
            .add("ACS1234")
            .add("1")
            .add("renditionName")
            .add("3")
            .add("4")
            .add("5")
            .add("54321")
            .add("7")
            .add("8")
            .add("9")
            .toString();
        repositoryClientData = new RepositoryClientData(clientData);

        assertEquals("", repositoryClientData.getAcsVersion());
        assertEquals("", repositoryClientData.getRenditionName());
        assertEquals(-1, repositoryClientData.getRequestId());
        assertFalse(repositoryClientData.isDebugRequested());
        assertEquals(clientData, repositoryClientData.toString());
    }

    @Test
    void tooFewElementsTest()
    {
        String clientData = new StringJoiner(CLIENT_DATA_SEPARATOR)
            .add(REPO_ID + "ACS1234")
            .add("1")
            .add("renditionName")
            .add("3")
            .add("4")
            .add("5")
            .add("54321")
            .add("7")
            .add("8")
            .toString();
        repositoryClientData = new RepositoryClientData(clientData);

        assertEquals("", repositoryClientData.getAcsVersion());
        assertEquals("", repositoryClientData.getRenditionName());
        assertEquals(-1, repositoryClientData.getRequestId());
        assertFalse(repositoryClientData.isDebugRequested());
        assertEquals(clientData, repositoryClientData.toString());
    }

    @Test
    void tooManyElementsTest()
    {
        String clientData = new StringJoiner(CLIENT_DATA_SEPARATOR)
            .add(REPO_ID + "ACS1234")
            .add("1")
            .add("renditionName")
            .add("3")
            .add("4")
            .add("5")
            .add("54321")
            .add("7")
            .add("8")
            .add(DEBUG)
            .add("10")
            .toString();
        repositoryClientData = new RepositoryClientData(clientData);
        assertEquals("", repositoryClientData.getAcsVersion());
        assertEquals("", repositoryClientData.getRenditionName());
        assertEquals(-1, repositoryClientData.getRequestId());
        assertFalse(repositoryClientData.isDebugRequested());
        assertEquals(clientData, repositoryClientData.toString());
    }

    @Test
    void nullClientDataTest()
    {
        repositoryClientData = new RepositoryClientData(null);
        assertEquals(null, repositoryClientData.toString());
    }

    @Test
    void noElementsClientDataTest()
    {
        String clientData = "There are no CLIENT_DATA_SEPARATOR chars";
        repositoryClientData = new RepositoryClientData(clientData);
        assertEquals(clientData, repositoryClientData.toString());
    }

    @Test
    void debugTest()
    {
        String clientData = new StringJoiner(CLIENT_DATA_SEPARATOR)
            .add(REPO_ID + "ACS1234")
            .add("1")
            .add("2")
            .add("3")
            .add("4")
            .add("5")
            .add("6")
            .add("7")
            .add("8")
            .add(DEBUG)
            .toString();
        repositoryClientData = new RepositoryClientData(clientData);

        assertEquals(clientData, repositoryClientData.toString());

        repositoryClientData.appendDebug("Some debug");
        assertEquals(clientData+DEBUG_SEPARATOR+"Some debug",
            repositoryClientData.toString());

        repositoryClientData.appendDebug("Some other debug");
        assertEquals(clientData+DEBUG_SEPARATOR+"Some debug"+DEBUG_SEPARATOR+"Some other debug",
            repositoryClientData.toString());
    }

    @Test
    void invalidRequestIdTest()
    {
        String clientData = new StringJoiner(CLIENT_DATA_SEPARATOR)
            .add(REPO_ID + "ACS1234")
            .add("1")
            .add("renditionName")
            .add("3")
            .add("4")
            .add("5")
            .add("abc")
            .add("7")
            .add("8")
            .add(DEBUG)
            .toString();
        repositoryClientData = new RepositoryClientData(clientData);

        assertEquals(-1, repositoryClientData.getRequestId());
        assertEquals(clientData, repositoryClientData.toString());
    }
}
