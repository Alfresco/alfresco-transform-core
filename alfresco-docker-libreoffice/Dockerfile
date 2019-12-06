# Image provides a container in which to run LibreOffice transformations for Alfresco Content Services.

# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.

FROM alfresco/alfresco-base-java:11.0.1-openjdk-centos-7-7a6031154417

ARG LIBREOFFICE_VERSION=6.1.6

ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/${LIBREOFFICE_VERSION}/libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz
ENV LIBREOFFICE_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/libreoffice.txt
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=libreoffice
ARG USERID=33003

COPY target/alfresco-docker-libreoffice-${env.project_version}.jar libreoffice-dist-*-linux.gz /

RUN ln /alfresco-docker-libreoffice-${env.project_version}.jar /usr/bin/alfresco-docker-libreoffice.jar && \
    yum install -y cairo cups-libs libSM && \
    test -f libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz && \
    ln -s libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz libreoffice-dist-linux.gz || \
    curl -s -S $LIBREOFFICE_RPM_URL      -o libreoffice-dist-linux.gz && \
    curl -s -S $LIBREOFFICE_LICENSE_FILE -o libreoffice.txt && \
    tar xzf libreoffice-dist-linux.gz && \
    yum localinstall -y LibreOffice*/RPMS/*.rpm && \
    rm -rf libreoffice-dist-*linux.gz LibreOffice_*_Linux_x86-64_rpm && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${USERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/alfresco-docker-libreoffice.jar

EXPOSE 8090

USER ${USERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-docker-libreoffice.jar
