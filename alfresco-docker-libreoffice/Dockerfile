# Image provides a container in which to run LibreOffice transformations for Alfresco Content Services.

FROM quay.io/alfresco/alfresco-base-java:9

ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/5.4.6/libreoffice-dist-5.4.6-linux.gz

COPY target/alfresco-docker-libreoffice-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-libreoffice-${env.project_version}.jar /usr/bin/alfresco-docker-libreoffice.jar && \
    yum install -y wget && \
    yum install -y cairo cups-libs libSM && \
    wget $LIBREOFFICE_RPM_URL && \
    tar xzf LibreOffice_*_Linux_x86-64_rpm.tar.gz && \
    yum localinstall -y LibreOffice*/RPMS/*.rpm && \
    rm -rf LibreOffice_*_Linux_x86-64_rpm.tar.gz LibreOffice_*_Linux_x86-64_rpm && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java -jar /usr/bin/alfresco-docker-libreoffice.jar
