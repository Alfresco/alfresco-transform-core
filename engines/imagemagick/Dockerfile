# Image provides a container in which to run GraphicsMagick transformations for Alfresco Content Services.
#
# PRODMAN-916 / ACS-12649 PoC: this engine has been switched from the custom (capability-reduced)
# ImageMagick build to the publicly available GraphicsMagick package from EPEL, installed with its
# full default capability set (delegates are NOT stripped, unlike the custom ImageMagick build).
#
# GraphicsMagick is from the GraphicsMagick Group. See http://www.graphicsmagick.org/ .

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
ARG JAVA_BASE_IMAGE=alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c
FROM ${JAVA_BASE_IMAGE}

ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG IMAGEUSERNAME=imagemagick
ARG USERID=33002

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

# Install GraphicsMagick from EPEL (public package, full capability - no reduced/stripped delegates).
RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    dnf install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm && \
    dnf install -y dnf-plugins-core && \
    (dnf config-manager --set-enabled crb || true) && \
    dnf install -y GraphicsMagick && \
    dnf clean all && rm -rf /var/cache/dnf

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
