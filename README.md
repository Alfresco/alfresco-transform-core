## Alfresco Transform Core
[![Build Status](https://travis-ci.com/Alfresco/alfresco-transform-core.svg?branch=master)](https://travis-ci.com/Alfresco/alfresco-transform-core)

Contains the common transformer (T-Engine) code, plus a few actual implementations.

### Sub-projects

* `alfresco-transformer-base` - library packaged as a jar file which contains code that is common
 to all the transformers; see the sub-project's
  [README](https://github.com/Alfresco/alfresco-transform-core/tree/master/alfresco-transformer-base)
* `alfresco-docker-<name>` - multiple T-Engines; each one of them builds both a SpringBoot fat jar
 and a Docker image


### Building and testing

The project can be built by running the Maven command:
```bash
mvn clean install -Plocal,docker-it-setup
```
> The `local` Maven profile builds local Docker images for each T-Engine.

### Artifacts

#### Maven
The artifacts can be obtained by:
* downloading from [Alfresco repository](https://artifacts.alfresco.com/nexus/content/groups/public)
* getting as Maven dependency by adding the dependency to your pom file:
```xml
<dependency>
    <groupId>org.alfresco</groupId>
    <artifactId>alfresco-transformer-base</artifactId>
    <version>version</version>
</dependency>
```
and Alfresco Maven repository:
```xml
<repository>
  <id>alfresco-maven-repo</id>
  <url>https://artifacts.alfresco.com/nexus/content/groups/public</url>
</repository>
```

#### Docker
The core T-Engine images are available on Docker Hub:
* [alfresco/alfresco-imagemagick](https://hub.docker.com/r/alfresco/alfresco-imagemagick)
* [alfresco/alfresco-pdf-renderer](https://hub.docker.com/r/alfresco/alfresco-pdf-renderer)
* [alfresco/alfresco-libreoffice](https://hub.docker.com/r/alfresco/alfresco-libreoffice)
* [alfresco/alfresco-tika](https://hub.docker.com/r/alfresco/alfresco-tika)
* [alfresco/alfresco-transform-misc](https://hub.docker.com/r/alfresco/alfresco-transform-misc)

### Release Process

For a complete walk-through check out the
[build-and-release.MD](https://github.com/Alfresco/alfresco-transform-core/tree/master/docs/build-and-release.md)
under the `docs` folder.


### Contributing guide

Please use [this guide](https://github.com/Alfresco/alfresco-repository/blob/master/CONTRIBUTING.md)
to make a contribution to the project.

