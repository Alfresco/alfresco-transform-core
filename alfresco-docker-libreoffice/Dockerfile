# Image provides a container in which to run LibreOffice transformations for Alfresco Enterprise Content Services.

# The container is only intended to be used with the Alfresco Enterprise editon which is covered by https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use.
# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.

# TODO ATS-71 - remove quay.io once released (to dockerhub) (DEPLOY-574)
FROM quay.io/alfresco/alfresco-base-java:11-openjdk-centos-7-release-candidate

ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/5.4.6/libreoffice-dist-5.4.6-linux.gz
ENV LIBREOFFICE_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/libreoffice.txt
ENV JAVA_OPTS=""

COPY target/alfresco-docker-libreoffice-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-libreoffice-${env.project_version}.jar /usr/bin/alfresco-docker-libreoffice.jar && \
    yum install -y wget && \
    yum install -y cairo cups-libs libSM && \
    wget $LIBREOFFICE_RPM_URL && \
    wget $LIBREOFFICE_LICENSE_FILE && \
    tar xzf libreoffice-dist-*-linux.gz && \
    yum localinstall -y LibreOffice*/RPMS/*.rpm && \
    rm -rf libreoffice-dist-*-linux.gz LibreOffice_*_Linux_x86-64_rpm && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-docker-libreoffice.jar
