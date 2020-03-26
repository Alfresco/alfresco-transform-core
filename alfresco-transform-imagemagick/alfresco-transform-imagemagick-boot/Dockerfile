# Image provides a container in which to run ImageMagick transformations for Alfresco Content Services.

# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.

FROM alfresco/alfresco-base-java:11.0.1-openjdk-centos-7-72b88c6f1f4c

ARG IMAGEMAGICK_VERSION=7.0.7-27

ENV IMAGEMAGICK_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/${IMAGEMAGICK_VERSION}/imagemagick-distribution-${IMAGEMAGICK_VERSION}-linux.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/${IMAGEMAGICK_VERSION}/imagemagick-distribution-${IMAGEMAGICK_VERSION}-libs-linux.rpm
ENV IMAGEMAGICK_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/ImageMagick-license.txt
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG IMAGEUSERNAME=imagemagick
ARG USERID=33002
ARG VERSIONJAR=${env.project_artifactId}-${env.project_version}.jar
ARG SHORTJAR=${env.project_artifactId}.jar

COPY target/${VERSIONJAR} /usr/bin

RUN ln /usr/bin/${VERSIONJAR} /usr/bin/${SHORTJAR} && \
    curl -s -S $IMAGEMAGICK_RPM_URL      -o imagemagick-distribution-linux.rpm && \
    curl -s -S $IMAGEMAGICK_LIB_RPM_URL  -o imagemagick-distribution-libs-linux.rpm && \
    curl -s -S $IMAGEMAGICK_LICENSE_FILE -o ImageMagick-license.txt && \
    yum localinstall -y imagemagick-distribution-*linux.rpm && \
    rm -f imagemagick-distribution-*.rpm && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${IMAGEUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${SHORTJAR}

EXPOSE 8090

USER ${IMAGEUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${SHORTJAR}
