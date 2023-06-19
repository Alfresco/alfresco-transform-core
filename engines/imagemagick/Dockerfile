# Image provides a container in which to run ImageMagick transformations for Alfresco Content Services.

# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.

FROM alfresco/alfresco-base-java:jre17-rockylinux8-202306121108

# For other ImageMagick versions please look at https://github.com/Alfresco/imagemagick-build tags
ARG IMAGEMAGICK_VERSION=7.1.0-16

ENV IMAGEMAGICK_RPM_URL=https://github.com/Alfresco/imagemagick-build/releases/download/v${IMAGEMAGICK_VERSION}/ImageMagick-${IMAGEMAGICK_VERSION}.x86_64.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://github.com/Alfresco/imagemagick-build/releases/download/v${IMAGEMAGICK_VERSION}/ImageMagick-libs-${IMAGEMAGICK_VERSION}.x86_64.rpm
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
    yum install -y $IMAGEMAGICK_LIB_RPM_URL $IMAGEMAGICK_RPM_URL && \
    yum clean all && \
    rpm -e --nodeps libgs

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
