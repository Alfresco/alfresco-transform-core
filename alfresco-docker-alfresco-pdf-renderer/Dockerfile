# Image provides a container in which to run alfresco-pdf-renderer transformations for Alfresco Enterprise Content Services.

# The container is only intended to be used with the Alfresco Enterprise editon which is covered by https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use.
# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

FROM alfresco/alfresco-base-java:11-openjdk-centos-7-95536c8b599f

ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/content/org/alfresco/alfresco-pdf-renderer/1.1/alfresco-pdf-renderer-1.1-linux.tgz
ENV PDFIUM_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/pdfium.txt
ENV JAVA_OPTS=""

COPY target/alfresco-docker-alfresco-pdf-renderer-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-alfresco-pdf-renderer-${env.project_version}.jar /usr/bin/alfresco-docker-alfresco-pdf-renderer.jar && \
    yum install -y wget && \
    wget $ALFRESCO_PDF_RENDERER_LIB_RPM_URL && \
    wget $PDFIUM_LICENSE_FILE && \
    tar xf alfresco-pdf-renderer-*-linux.tgz -C /usr/bin && \
	rm -f alfresco-pdf-renderer-*-linux.tgz && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-docker-alfresco-pdf-renderer.jar

