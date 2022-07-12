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
package org.alfresco.transform.base;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Allows {@link CustomTransformer} implementations to interact with the base t-engine.
 */
public interface TransformManager
{
    /**
     * Allows a {@link CustomTransformer} to use a local source {@code File} rather than the supplied {@code InputStream}.
     * The file will be deleted once the request is completed. To avoid creating extra files, if a File has already
     * been created by the base t-engine, it is returned.
     * If possible this method should be avoided as it is better not to leave content on disk.
     * @throws IllegalStateException if this method has already been called.
     */
    File createSourceFile();

    /**
     * Allows a {@link CustomTransformer} to use a local target {@code File} rather than the supplied {@code OutputStream}.
     * The file will be deleted once the request is completed. To avoid creating extra files, if a File has already
     * been created by the base t-engine, it is returned.
     * If possible this method should be avoided as it is better not to leave content on disk.
     * @throws IllegalStateException if this method has already been called. A call to {@link #respondWithFragment(Integer)}
     *         allows the method to be called again.
     */
    File createTargetFile();

    /**
     * Allows a single transform request to have multiple transform responses. For example images from a video at
     * different time offsets or different pages of a document. Following a call to this method a transform response is
     * made with output that has already been generated. The {@code CustomTransformer} may then use the returned
     * {@code outputStream} to generate more output for the next transform response.
     * {@code CustomTransformer} to throw an {@code Exception} when it finds it has no more output.
     * @param index returned with the response, so that it may be distinguished from other responses. Renditions
     *        use the index as an offset into elements. A {@code null} value indicates that there is no more output
     *        so there should not be another transform reply once the
     *        {@link CustomTransformer#transform(String, InputStream, String, OutputStream, Map, TransformManager)}
     *        returns.
     * @throws IllegalStateException if a synchronous (http) request has been made as this only works with messages
     *         on queues.
     */
    // This works because all the state is in the TransformResponse and the t-router will just see each response as
    // something to either return to the client or pass to the next stage in a pipeline. We might be able to enhance
    // the logging to include the index. We may also wish to modify the client data or just make the index available
    // in the message.
    OutputStream respondWithFragment(Integer index);
}
