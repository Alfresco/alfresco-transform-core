/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.transformer;


import java.io.IOException;

import org.alfresco.transform.client.model.TransformRequest;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@WebMvcTest(AIOController.class)
public class AIOControllerTest //extends AbstractTransformerControllerTest 
{

    //@Override
    protected void mockTransformCommand(String sourceExtension, String targetExtension, String sourceMimetype,
            boolean readTargetFileBytes) throws IOException {
        // TODO Auto-generated method stub

    }

    //@Override
    protected AbstractTransformerController getController() {
        // TODO Auto-generated method stub
        return null;
    }

    //@Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest) {
        // TODO Auto-generated method stub

    }
}