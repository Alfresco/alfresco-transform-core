## Alfresco Transform Core
[![Build Status](https://travis-ci.com/Alfresco/alfresco-transform-core.svg?branch=master)](https://travis-ci.com/Alfresco/alfresco-transform-core)

Contains the common transformer (T-Engine) code, plus a few implementations.

### Sub-projects

* `alfresco-transform-model` - library packaged as a jar file which contains the data model of json
 configuration files and messages sent between clients, T-Engines and T-Router. Also contains code to
 work out which transform should be used for a combination of configuration files; see the sub-project's
  [README](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-transform-model/README.md)
* `alfresco-t-engine-base` - library packaged as a jar file which contains code that is common
  to all the transformers; see the sub-project's
  [README](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-t-engine-base/README.md)
* `alfresco-transform-<name>` - multiple T-Engines; each one of them builds both a SpringBoot fat jar
 and a [Docker image](https://github.com/Alfresco/alfresco-transform-core#docker)
 
### Documentation

In addition to the subprojects (such as `alfresco-t-engine-base` README above) some additional documentation can be found in:

* [this project's docs](docs) folder
* [ACS Packaging docs](https://github.com/Alfresco/acs-packaging/tree/master/docs) folder

Note: if you're interested in the Alfresco Transform Service (ATS) that is part of the enterprise Alfresco Content Services (ACS) please see:

*  https://docs.alfresco.com/transform/concepts/transformservice-overview.html

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
    <artifactId>alfresco-transform-model</artifactId>
    <version>version</version>
</dependency>

<dependency>
    <groupId>org.alfresco</groupId>
    <artifactId>alfresco-t-engine-base</artifactId>
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
The core T-Engine images are available on Docker Hub. 

Either as a single Core AIO (All-In-One) T-Engine:
* [alfresco/alfresco-transform-core-aio](https://hub.docker.com/r/alfresco/alfresco-transform-core-aio)

Or as set of individual T-Engines:
* [alfresco/alfresco-imagemagick](https://hub.docker.com/r/alfresco/alfresco-imagemagick)
* [alfresco/alfresco-pdf-renderer](https://hub.docker.com/r/alfresco/alfresco-pdf-renderer)
* [alfresco/alfresco-libreoffice](https://hub.docker.com/r/alfresco/alfresco-libreoffice)
* [alfresco/alfresco-tika](https://hub.docker.com/r/alfresco/alfresco-tika)
* [alfresco/alfresco-transform-misc](https://hub.docker.com/r/alfresco/alfresco-transform-misc)

You can find examples of using Core AIO in the reference ACS Deployment for Docker Compose:
* [ACS Community](https://github.com/Alfresco/acs-deployment/blob/master/docker-compose/community-docker-compose.yml)
* [ACS Enterprise](https://github.com/Alfresco/acs-deployment/blob/master/docker-compose/docker-compose.yml)

You can find examples of using the individual T-Engines in the reference ACS Deployment for Helm / Kubernetes:
* [ACS Community](https://github.com/Alfresco/acs-deployment/blob/master/helm/alfresco-content-services/community_values.yaml)
* [ACS Enterprise](https://github.com/Alfresco/acs-deployment/blob/master/helm/alfresco-content-services/values.yaml)

### Release Process

For a complete walk-through check out the
[build-and-release.MD](https://github.com/Alfresco/alfresco-transform-core/tree/master/docs/build-and-release.md)
under the `docs` folder.

### Contributing guide

Please use [this guide](https://github.com/Alfresco/alfresco-repository/blob/master/CONTRIBUTING.md)
to make a contribution to the project.
