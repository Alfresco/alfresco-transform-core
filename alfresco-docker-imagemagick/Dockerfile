# Image provides a container in which to run ImageMagick transformations for Alfresco Enterprise Content Services.

# The container is only intended to be used with the Alfresco Enterprise editon which is covered by https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use.
# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.

FROM alfresco/alfresco-base-java:11.0.1-openjdk-centos-7-1fd3c4475374

ENV IMAGEMAGICK_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/7.0.7-27/imagemagick-distribution-7.0.7-27-linux.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/7.0.7-27/imagemagick-distribution-7.0.7-27-libs-linux.rpm
ENV IMAGEMAGICK_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/ImageMagick-license.txt
ENV JAVA_OPTS="-Xms256M -Xmx2048M"

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=imagemagick
ARG USERID=33002

COPY target/alfresco-docker-imagemagick-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-imagemagick-${env.project_version}.jar /usr/bin/alfresco-docker-imagemagick.jar && \
    yum install -y wget && \
    wget $IMAGEMAGICK_RPM_URL && \
    wget $IMAGEMAGICK_LIB_RPM_URL && \
    wget $IMAGEMAGICK_LICENSE_FILE && \
    yum localinstall -y imagemagick-distribution-*-linux.rpm && \
    rm -f imagemagick-distribution-*-linux.rpm && \
    yum remove -y wget && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${USERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/alfresco-docker-imagemagick.jar

EXPOSE 8090

USER ${USERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-docker-imagemagick.jar
