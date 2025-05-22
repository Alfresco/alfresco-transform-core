## Alfresco Transform Core
[![Build Status](https://github.com/Alfresco/alfresco-transform-core/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Alfresco/alfresco-transform-core/actions/workflows/ci.yml)

Contains the common transformer (T-Engine) code, plus a few implementations

> When upgrading to 3.0.0, you will find that a number of classes in the alfresco-transform-model
have moved. See the [alfresco-transform-model README](https://github.com/Alfresco/alfresco-transform-core/blob/master/model/README.md)

### Sub-projects

* `model` - library packaged as a jar file which contains the data model of json
 configuration files and messages sent between clients, T-Engines and T-Router. Also contains code to
 to combine and then work out which transform to use for a combination of source and target mimetypes
 and transform options.
* `engines/base` - contains code common to t-engines, packaged as a jar.
  [README](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/base/README.md)
* `engines/<name>` - multiple T-Engines, which extend the `engines/base`; each one builds a SpringBoot jar
  and a [Docker image](https://github.com/Alfresco/alfresco-transform-core#docker)
* `deprecated/alfresco-base-t-engine` - The original t-engine base, which may still be used, 
  but has been replaced by the simpler `engines/base`.

### Documentation

* `docs` - provides additional documentation.
* [ACS Packaging docs](https://github.com/Alfresco/acs-packaging/tree/master/docs) folder
* If you're interested in the Alfresco Transform Service (ATS) see https://docs.alfresco.com/transform/concepts/transformservice-overview.html

### Building and testing

The project can be built by running the Maven command:
```bash
mvn clean install -Plocal,docker-it-setup
```
> The `local` Maven profile builds local Docker images for each T-Engine.

## Run in Docker

Execute the following commands to run a t-engine in detached mode on port 8090 and to show the logs:

```bash
docker run -d -p 8090:8090 --name <t-engine-project-name> <t-engine-project-name>:latest
docker logs -f <t-engine-project-name>
```

## Run the Spring Boot Application

Since a T-Engine is a Spring Boot application, it might be helpful to run it as such during development by executing
one of the following:
* `mvn spring-boot:run`
* `java -jar target/helloworld-t-engine-{version}.jar` in the project directory.
* Run or debug the application `org.alfresco.transform.base.Application` from within an IDE.


## Test page

The application will be accessible on port 8090 and the test page is: `http://localhost:8090/`.
The config is available on `http://localhost:8090/transform/config`.

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
    <artifactId>alfresco-base-t-engine</artifactId>
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

Or as a set of individual T-Engines:
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
