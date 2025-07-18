/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.base.transform;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.io.OutputStream;

import org.alfresco.transform.exceptions.TransformException;

/**
 * Separation of transform fragments logic from the {@link ProcessHandler} logic and {@link StreamHandler}.
 */
public abstract class FragmentHandler extends StreamHandler
{
    private boolean methodHasBeenCall;
    private boolean noMoreFragments;

    protected void initTarget()
    {}

    public OutputStream respondWithFragment(Integer index, boolean finished) throws IOException
    {
        try
        {
            if (index == null && !methodHasBeenCall)
            {
                throw new TransformException(INTERNAL_SERVER_ERROR, "No fragments were produced");
            }

            if (index != null && noMoreFragments)
            {
                throw new TransformException(INTERNAL_SERVER_ERROR, "Final fragment already sent");
            }

            if (index != null)
            {
                super.handleSuccessfulTransform();
                logFragment(index, transformManager.getOutputLength());
            }
        }
        finally
        {
            methodHasBeenCall = true;
            noMoreFragments = noMoreFragments || index == null || finished;
        }
        return noMoreFragments ? null : switchToNewOutputStreamForNewFragment();
    }

    protected void logFragment(Integer index, Long outputLength)
    {}

    @Override
    protected void handleSuccessfulTransform() throws IOException
    {
        if (!methodHasBeenCall)
        {
            super.handleSuccessfulTransform();
        }
    }

    private OutputStream switchToNewOutputStreamForNewFragment() throws IOException
    {
        transformManager.getOutputStream().close();
        transformManager.deleteTargetFile();
        initTarget();
        setOutputStream();
        return outputStream;
    }
}
