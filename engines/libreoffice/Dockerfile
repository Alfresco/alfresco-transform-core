# Image provides a container in which to run LibreOffice transformations for Alfresco Content Services.

# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
FROM alfresco/alfresco-base-java:jre17-rockylinux9@sha256:905004d3b37334182a836146c4566151e003a0a6f99976571f8ceae256d78687

ARG LIBREOFFICE_VERSION=7.2.5

ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/org/libreoffice/libreoffice-dist/${LIBREOFFICE_VERSION}/libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz
ENV LIBREOFFICE_ARM64_RPM_URL=https://dl.rockylinux.org/pub/rocky/9/devel/aarch64/os/Packages/l/
ENV LIBREOFFICE_ARM64_RPM_VERSION=libreoffice-7.1.8.1
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG LIBREUSERNAME=libreoffice
ARG USERID=33003

COPY target/${env.project_artifactId}-${env.project_version}.jar libreoffice-dist-*-linux.gz /

RUN ln /${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    yum install -y cairo cups-libs libSM libGLU && \
    if [ "$(uname -m)" = "x86_64" ]; then \
        test -f libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz && \
        ln -s libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz libreoffice-dist-linux.gz || \
        curl -s -S $LIBREOFFICE_RPM_URL      -o libreoffice-dist-linux.gz && \
        tar xzf libreoffice-dist-linux.gz && \
        yum localinstall -y LibreOffice*/RPMS/*.rpm && \
        rm -rf libreoffice-dist-*linux.gz LibreOffice_*_Linux_x86-64_rpm; \
    else \
      LIBREOFFICE_ARM64_RPM_VERSION=$(curl -s $LIBREOFFICE_ARM64_RPM_URL | grep -o "${LIBREOFFICE_ARM64_RPM_VERSION}.*" | cut -d '"' -f 1 | sort | tail -n1) && \
      dnf --enablerepo=devel install -y ${LIBREOFFICE_ARM64_RPM_URL}${LIBREOFFICE_ARM64_RPM_VERSION} && \
      update-alternatives --set java java-17-openjdk.aarch64; \
    fi && \
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
