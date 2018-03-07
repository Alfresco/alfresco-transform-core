# Project that generates Docker images to perform ACS transformations

* alfresco-transformer-base - contains code that is common to all the transformers. This includes
  the streaming of content to and from the docker images.
* alfresco-docker-<name> - contains code to generate an image for a transformer called <name>.

## Building and testing

The project can be built by running the Maven command:

~~~
mvn clean install
~~~

The build plan in Bamboo is PLAT-TRANS

## Contributing guide

Please use [this guide](https://github.com/Alfresco/alfresco-jodconverter/blob/master/CONTRIBUTING.md) to make a
contribution to the project.
