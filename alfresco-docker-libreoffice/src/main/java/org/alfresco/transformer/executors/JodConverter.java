/*
 * #%L
 * Alfresco Enterprise Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 * #L%
 */
package org.alfresco.transformer.executors;

import org.artofsolving.jodconverter.office.OfficeManager;

///////// THIS FILE WAS A COPY OF THE CODE IN alfresco-repository /////////////

public interface JodConverter
{
    /**
     * Gets the JodConverter OfficeManager.
     * @return
     */
    OfficeManager getOfficeManager();

    /**
     * This method returns a boolean indicating whether the JodConverter connection to OOo is available.
     * @return <code>true</code> if available, else <code>false</code>
     */
    boolean isAvailable();
}
