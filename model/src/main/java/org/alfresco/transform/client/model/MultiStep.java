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
package org.alfresco.transform.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds required contextual information for a multi-step transform.
 *
 * @author Lucian Tuca created on 19/12/2018
 */
// This class is in the package org.alfresco.transform.messages in HxP because that is more readable, but in
// org.alfresco.transform.client.model in Alfresco for backward compatibility.
public class MultiStep implements Serializable
{
    private String initialRequestId;
    private String initialSourceMediaType;
    private List<String> transformsToBeDone = new ArrayList<>();

    // regions [Accessors]

    public String getInitialSourceMediaType()
    {
        return initialSourceMediaType;
    }

    public void setInitialSourceMediaType(String initialSourceMediaType)
    {
        this.initialSourceMediaType = initialSourceMediaType;
    }

    public String getInitialRequestId()
    {
        return initialRequestId;
    }

    public void setInitialRequestId(String initialRequestId)
    {
        this.initialRequestId = initialRequestId;
    }

    public List<String> getTransformsToBeDone()
    {
        return transformsToBeDone;
    }

    public void setTransformsToBeDone(List<String> transformsToBeDone)
    {
        this.transformsToBeDone = transformsToBeDone;
    }

    // endregion

    @Override
    public String toString()
    {
        return "MultiStep{" +
                "initialRequestId='" + initialRequestId + '\'' +
                ", initialSourceMediaType='" + initialSourceMediaType + '\'' +
                ", transformsToBeDone=" + transformsToBeDone +
                '}';
    }
}
