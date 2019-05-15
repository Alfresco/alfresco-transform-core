# Project that generates Docker images to perform ACS transformations

## Sub-projects

* `alfresco-transformer-base` - contains code that is common to all the transformers. This includes
  the streaming of content to and from the docker images. See the sub-project's
  [README](https://git.alfresco.com/Repository/alfresco-docker-transformers/tree/master/alfresco-transformer-base).
* Each `alfresco-docker-<name>` - contains two sub-projects. One builds an executable jar to communicate with the
  Alfresco repository and another to create a Docker image that includes the jar and any required
  executables.

## Building and testing

The project can be built by running the Maven command:

~~~
mvn clean install
~~~

The build plan in Bamboo is PLAT-TRANS

## Contributing guide

Please use [this guide](https://github.com/Alfresco/alfresco-jodconverter/blob/master/CONTRIBUTING.md) to make a
contribution to the project.

## Release Process

For a complete walk-through check out the
[build-and-release.MD](https://github.com/Alfresco/alfresco-transform-core/tree/master/docs/build-and-release.md)
under the `docs` folder.