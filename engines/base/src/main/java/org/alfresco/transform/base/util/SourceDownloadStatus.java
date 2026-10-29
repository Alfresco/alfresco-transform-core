/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2026 Alfresco Software Limited
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
package org.alfresco.transform.base.util;

/**
 * A source that knows whether it arrived in full.
 * <p/>
 * Kept separate from the stream that implements it so that a caller can ask the question without holding a reference to something closeable. The source is owned and closed by whoever opened it; code that only wants to know how a transform failed has no business being able to close it.
 *
 * @see RemoteSourceStream
 */
public interface SourceDownloadStatus
{
    /**
     * @return {@code true} if the download failed, or ended before all of the content had arrived. A transform that failed on such a source must be reported as a server error so that it is retried, whatever the parser called the failure.
     */
    boolean sourceDownloadFailed();

    /**
     * @return a description of why the download is considered incomplete, for logging. Never {@code null} once {@link #sourceDownloadFailed()} is {@code true}.
     */
    String describeFailure();
}
