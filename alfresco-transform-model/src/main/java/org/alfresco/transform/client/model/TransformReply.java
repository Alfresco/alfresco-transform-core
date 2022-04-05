/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
import java.util.Objects;

public class TransformReply implements Serializable
{
    private String requestId;
    private int status;
    private String errorDetails;
    private String sourceReference;
    private String targetReference;
    private String clientData;
    private int schema;
    private InternalContext internalContext;

    //region [Accessors]
    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getErrorDetails()
    {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails)
    {
        this.errorDetails = errorDetails;
    }

    public String getSourceReference()
    {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference)
    {
        this.sourceReference = sourceReference;
    }

    public String getTargetReference()
    {
        return targetReference;
    }

    public void setTargetReference(String targetReference)
    {
        this.targetReference = targetReference;
    }

    public String getClientData()
    {
        return clientData;
    }

    public void setClientData(String clientData)
    {
        this.clientData = clientData;
    }

    public int getSchema()
    {
        return schema;
    }

    public void setSchema(int schema)
    {
        this.schema = schema;
    }

    public InternalContext getInternalContext()
    {
        return internalContext;
    }

    public void setInternalContext(InternalContext internalContext)
    {
        this.internalContext = internalContext;
    }

    //endregion

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TransformReply that = (TransformReply) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(requestId);
    }

    @Override public String toString()
    {
        return "TransformReply{" +
               "requestId='" + requestId + '\'' +
               ", status=" + status +
               ", errorDetails='" + errorDetails + '\'' +
               ", sourceReference='" + sourceReference + '\'' +
               ", targetReference='" + targetReference + '\'' +
               ", clientData='" + clientData + '\'' +
               ", schema=" + schema +
               ", internalContext=" + internalContext +
               '}';
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final TransformReply reply = new TransformReply();

        private Builder() {}

        public Builder withRequestId(final String requestId)
        {
            reply.requestId = requestId;
            return this;
        }

        public Builder withStatus(final int status)
        {
            reply.status = status;
            return this;
        }

        public Builder withErrorDetails(final String errorDetails)
        {
            reply.errorDetails = errorDetails;
            return this;
        }

        public Builder withSourceReference(final String sourceReference)
        {
            reply.sourceReference = sourceReference;
            return this;
        }

        public Builder withTargetReference(final String targetReference)
        {
            reply.targetReference = targetReference;
            return this;
        }

        public Builder withClientData(final String clientData)
        {
            reply.clientData = clientData;
            return this;
        }

        public Builder withSchema(final int schema)
        {
            reply.schema = schema;
            return this;
        }

        public Builder withInternalContext(final InternalContext internalContext)
        {
            reply.internalContext = internalContext;
            return this;
        }

        public TransformReply build()
        {
            return reply;
        }
    }
}
