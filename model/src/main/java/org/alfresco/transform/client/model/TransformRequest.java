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

import org.alfresco.transform.common.ExtensionService;
import org.alfresco.transform.messages.TransformStack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.alfresco.transform.messages.TransformStack.PIPELINE_FLAG;
import static org.alfresco.transform.messages.TransformStack.levelBuilder;
import static org.alfresco.transform.messages.TransformStack.setInitialTransformRequestOptions;

// This class is in the package org.alfresco.transform.messages in HxP because that is more readable, but in
// org.alfresco.transform.client.model in Alfresco for backward compatibility.
public class TransformRequest implements Serializable
{
    private String requestId;
    private String sourceReference;
    private String sourceMediaType;
    private Long sourceSize;
    private String sourceExtension;
    private String targetMediaType;
    private String targetExtension;
    private String clientData;
    private int schema;
    private Map<String, String> transformRequestOptions = new HashMap<>();
    private InternalContext internalContext;
    private String sourceFileName;

    // regions [Accessors]
    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public String getSourceReference()
    {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference)
    {
        this.sourceReference = sourceReference;
    }

    public String getSourceMediaType()
    {
        return sourceMediaType;
    }

    public void setSourceMediaType(String sourceMediaType)
    {
        this.sourceMediaType = sourceMediaType;
    }

    public Long getSourceSize()
    {
        return sourceSize;
    }

    public void setSourceSize(Long sourceSize)
    {
        this.sourceSize = sourceSize;
    }

    public String getSourceExtension()
    {
        return sourceExtension;
    }

    public void setSourceExtension(String sourceExtension)
    {
        this.sourceExtension = sourceExtension;
    }

    public String getTargetMediaType()
    {
        return targetMediaType;
    }

    public void setTargetMediaType(String targetMediaType)
    {
        this.targetMediaType = targetMediaType;
    }

    public String getTargetExtension()
    {
        return targetExtension;
    }

    public void setTargetExtension(String targetExtension)
    {
        this.targetExtension = targetExtension;
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

    public Map<String, String> getTransformRequestOptions()
    {
        return transformRequestOptions;
    }

    public void setTransformRequestOptions(Map<String, String> transformRequestOptions)
    {
        this.transformRequestOptions = transformRequestOptions;
    }

    public InternalContext getInternalContext()
    {
        return internalContext;
    }

    public void setInternalContext(InternalContext internalContext)
    {
        this.internalContext = internalContext;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    //endregion

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TransformRequest that = (TransformRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(requestId);
    }

    @Override public String toString()
    {
        return "TransformRequest{" +
               "requestId='" + requestId + '\'' +
               ", sourceReference='" + sourceReference + '\'' +
               ", sourceMediaType='" + sourceMediaType + '\'' +
               ", sourceSize=" + sourceSize +
               ", sourceExtension='" + sourceExtension + '\'' +
               ", targetMediaType='" + targetMediaType + '\'' +
               ", targetExtension='" + targetExtension + '\'' +
               ", clientData='" + clientData + '\'' +
               ", schema=" + schema +
               ", transformRequestOptions=" + transformRequestOptions +
               ", internalContext=" + internalContext +
               '}';
    }

    /**
     * Sets up the internal context structure when a client request is initially received by the router,
     * so that we don't have to keep checking if bits of it are initialised. Prior to making this call,
     * the id, sourceMimetypes, targetMimetype, transformRequestOptions and sourceReference should have
     * been set, if they are to be set.
     */
    public TransformRequest initialiseContextWhenReceivedByRouter()
    {
        setInternalContext(InternalContext.initialise(getInternalContext()));
        setTargetExtension(ExtensionService.getExtensionForTargetMimetype(getTargetMediaType(),
            getSourceMediaType()));
        getInternalContext().getMultiStep().setInitialRequestId(getRequestId());
        getInternalContext().getMultiStep().setInitialSourceMediaType(getSourceMediaType());
        getInternalContext().setTransformRequestOptions(getTransformRequestOptions());
        setInitialTransformRequestOptions(getInternalContext(), getTransformRequestOptions());
        TransformStack.setInitialSourceReference(getInternalContext(), getSourceReference());
        return this;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final TransformRequest request = new TransformRequest();

        private Builder() {}

        public Builder withRequestId(final String requestId)
        {
            request.requestId = requestId;
            return this;
        }

        public Builder withInternalContext(final InternalContext internalContext)
        {
            request.internalContext = internalContext;
            return this;
        }

        public Builder withSourceReference(final String sourceReference)
        {
            request.sourceReference = sourceReference;
            return this;
        }

        public Builder withSourceMediaType(final String sourceMediaType)
        {
            request.sourceMediaType = sourceMediaType;
            return this;
        }

        public Builder withSourceSize(final Long sourceSize)
        {
            request.sourceSize = sourceSize;
            return this;
        }

        public Builder withSourceExtension(final String sourceExtension)
        {
            request.sourceExtension = sourceExtension;
            return this;
        }

        public Builder withTargetMediaType(final String targetMediaType)
        {
            request.targetMediaType = targetMediaType;
            return this;
        }

        public Builder withTargetExtension(final String targetExtension)
        {
            request.targetExtension = targetExtension;
            return this;
        }

        public Builder withClientData(final String clientData)
        {
            request.clientData = clientData;
            return this;
        }

        public Builder withTransformRequestOptions(
            final Map<String, String> transformRequestOptions)
        {
            request.transformRequestOptions = transformRequestOptions;
            return this;
        }

        public Builder withSchema(final int schema)
        {
            request.schema = schema;
            return this;
        }

        public Builder withInternalContextForTransformEngineTests()
        {
            request.initialiseContextWhenReceivedByRouter();
            TransformStack.addTransformLevel(request.internalContext, levelBuilder(PIPELINE_FLAG)
                .withStep("dummyTransformerName", request.sourceMediaType, request.targetMediaType));
            return this;
        }

        public TransformRequest build()
        {
            return request;
        }
    }
}
