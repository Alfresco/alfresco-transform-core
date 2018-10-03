# Image provides a container in which to run Tika transformations for Alfresco Enterprise Content Services.

# The container is only intended to be used with the Alfresco Enterprise editon which is covered by https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use.
# Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0.

# TODO ATS-71 - remove quay.io once released (to dockerhub) (DEPLOY-574)
FROM quay.io/alfresco/alfresco-base-java:11-openjdk-centos-7-release-candidate

ENV APACHE_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/Apache%202.0.txt

COPY target/alfresco-docker-tika-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-tika-${env.project_version}.jar /usr/bin/alfresco-docker-tika.jar && \
    yum install -y wget && \
    wget $APACHE_LICENSE_FILE && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java -jar /usr/bin/alfresco-docker-tika.jar
