# Image provides a container in which to run LibreOffice transformations for Alfresco Enterprise Content Services.

# The container is only intended to be used with the Alfresco Enterprise editon which is covered by https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use.
# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.

FROM alfresco/alfresco-base-java:11.0.1-openjdk-centos-7-3e4e9f4e5d6a

ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/5.4.6/libreoffice-dist-5.4.6-linux.gz
ENV LIBREOFFICE_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/libreoffice.txt
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=libreoffice
ARG USERID=33003

COPY target/alfresco-docker-libreoffice-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-libreoffice-${env.project_version}.jar /usr/bin/alfresco-docker-libreoffice.jar && \
    yum install -y cairo cups-libs libSM && \
    ( [ -f libreoffice-dist-5.4.6-linux.gz ] && \
        mv libreoffice-dist-5.4.6-linux.gz libreoffice-dist-linux.gz || \
        curl -s -S $LIBREOFFICE_RPM_URL  -o libreoffice-dist-linux.gz ) && \
    curl -s -S $LIBREOFFICE_LICENSE_FILE -o libreoffice.txt && \
    tar xzf libreoffice-dist-linux.gz && \
    yum localinstall -y LibreOffice*/RPMS/*.rpm && \
    rm -rf libreoffice-dist-linux.gz LibreOffice_*_Linux_x86-64_rpm && \
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
