# Image provides a container in which to run ImageMagick transformations for Alfresco Content Services.

# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.

# ACTUAL IMAGE
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
# jre17-rockylinux9 (29/07/2024)
FROM alfresco/alfresco-base-java@sha256:8dca9445141dd07b9a6b7a9962f0505546b2ed041b4d5cc785cc9c2d39eeda19

# For other ImageMagick versions please look at https://github.com/Alfresco/imagemagick-build tags
ARG IMAGEMAGICK_VERSION=7.1.0-16

ENV IMAGEMAGICK_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/org/imagemagick/imagemagick-distribution/${IMAGEMAGICK_VERSION}-ci-3/imagemagick-distribution-${IMAGEMAGICK_VERSION}-ci-3
ENV IMAGEMAGICK_DEP_RPM_URL=https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG IMAGEUSERNAME=imagemagick
ARG USERID=33002

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    yum install -y $IMAGEMAGICK_DEP_RPM_URL && \
    if [ "$(uname -m)" = "x86_64" ]; then \
            yum install -y ${IMAGEMAGICK_RPM_URL}-el8.rpm ${IMAGEMAGICK_RPM_URL}-el8-libs.rpm; \
        else \
            yum install -y ${IMAGEMAGICK_RPM_URL}-arm64.rpm ${IMAGEMAGICK_RPM_URL}-arm64-libs.rpm; \
        fi && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY target/classes/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${IMAGEUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${IMAGEUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
