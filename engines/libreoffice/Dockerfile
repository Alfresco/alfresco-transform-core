# Image provides a container in which to run LibreOffice transformations for Alfresco Content Services.

# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
ARG JAVA_BASE_IMAGE=alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c
FROM ${JAVA_BASE_IMAGE}

ARG LIBREOFFICE_VERSION=26.2.3

ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG LIBREUSERNAME=libreoffice
ARG USERID=33003

COPY target/${env.project_artifactId}-${env.project_version}.jar /

RUN ln /${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    yum install -y cairo cups-libs libSM libGLU procps && \
    ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then LO_ARCH="x86-64"; else LO_ARCH="aarch64"; fi && \
    curl -s -S -L "https://download.documentfoundation.org/libreoffice/stable/${LIBREOFFICE_VERSION}/rpm/${ARCH}/LibreOffice_${LIBREOFFICE_VERSION}_Linux_${LO_ARCH}_rpm.tar.gz" \
      -o libreoffice-dist-linux.tar.gz && \
    tar xzf libreoffice-dist-linux.tar.gz && \
    find LibreOffice*/RPMS -name "*.rpm" ! -name "*kde*" ! -name "*gnome*" \
      | xargs yum localinstall -y && \
    rm -rf libreoffice-dist-linux.tar.gz LibreOffice_*_Linux_* && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY target/classes/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${LIBREUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${LIBREUSERNAME}

# Copy the script to set LIBREOFFICE_HOME
COPY docker-entrypoint.sh /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh", "${env.project_artifactId}"]
