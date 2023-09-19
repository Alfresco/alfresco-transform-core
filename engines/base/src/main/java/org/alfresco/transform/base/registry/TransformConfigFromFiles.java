/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2023 Alfresco Software Limited
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
package org.alfresco.transform.base.registry;

import org.alfresco.transform.config.reader.TransformConfigResourceReader;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Makes {@link TransformConfig} from files on the classpath or externally available to the {@link TransformRegistry}.
 */
@Component
public class TransformConfigFromFiles
{
    @Autowired
    private List<TransformConfigSource> transformConfigSources;
    @Autowired
    private TransformConfigFiles transformConfigFiles;
    @Autowired
    private TransformConfigFilesHistoric transformConfigFilesHistoric;
    @Autowired
    private TransformConfigResourceReader transformConfigResourceReader;
    @Value("${container.isTRouter}")
    private boolean isTRouter;

    @PostConstruct
    public void initFileConfig()
    {
        final List<Resource> resources = new ArrayList<>();
        resources.addAll(transformConfigFiles.retrieveResources());
        resources.addAll(transformConfigFilesHistoric.retrieveResources());
        resources.forEach(resource ->
        {
            String filename = resource.getFilename();
            transformConfigSources.add(
                new AbstractTransformConfigSource(filename, filename, isTRouter ? null : "---")
                {
                    @Override public TransformConfig getTransformConfig()
                    {
                        return transformConfigResourceReader.read(resource);
                    }
                });
        });
    }

    public static List<Resource> retrieveResources(Map<String, String> additional)
    {
        return additional
                   .values()
                   .stream()
                   .filter(Objects::nonNull)
                   .map(String::trim)
                   .filter(s -> !s.isBlank())
                   .map(TransformConfigFromFiles::retrieveResource)
                   .collect(toList());
    }

    public static Resource retrieveResource(final String filename)
    {
        final Resource resource = new FileSystemResource(filename);
        if (resource.exists())
        {
            return resource;
        }
        return new ClassPathResource(filename);
    }
}
