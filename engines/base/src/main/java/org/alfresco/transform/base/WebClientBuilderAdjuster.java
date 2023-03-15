/*
 * Copyright 2015-2023 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.transform.base;

import org.springframework.web.reactive.function.client.WebClient;

@FunctionalInterface
public interface WebClientBuilderAdjuster
{
    void adjust(WebClient.Builder builder);
}
