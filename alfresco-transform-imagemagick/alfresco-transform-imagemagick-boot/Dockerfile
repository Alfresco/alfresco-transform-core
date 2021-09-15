# Image provides a container in which to run ImageMagick transformations for Alfresco Content Services.

# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.

FROM alfresco/alfresco-base-java:11.0.12-centos-7@sha256:6abdbfd14492fb78ae5d865eb25627bb241b11da406ef8df91a77716a3538f36

ARG IMAGEMAGICK_VERSION=7.0.10-59

ENV IMAGEMAGICK_SRC_URL=https://download.imagemagick.org/ImageMagick/download/releases/ImageMagick-${IMAGEMAGICK_VERSION}.tar.xz
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG IMAGEUSERNAME=imagemagick
ARG USERID=33002

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    yum clean all && \
    yum -y group install "Development Tools" && \
    curl -s -S $IMAGEMAGICK_SRC_URL -o ImageMagick-7.0.10-59.tar.xz && \
    tar -xf ImageMagick-7.0.10-59.tar.xz && \
    cd ImageMagick-7.0.10-59 && \
    ./configure && \
    make && \
    make install && \
    ldconfig /usr/local/lib && \
    yum clean all && \
    cd /

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY src/main/resources/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${IMAGEUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${IMAGEUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
