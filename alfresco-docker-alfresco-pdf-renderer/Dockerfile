# Image provides a container in which to run alfresco-pdf-renderer transformations for Alfresco Content Services.

FROM quay.io/alfresco/alfresco-base-java:9

ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/content/org/alfresco/alfresco-pdf-renderer/1.1/alfresco-pdf-renderer-1.1-linux.tgz

COPY target/alfresco-docker-alfresco-pdf-renderer-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-alfresco-pdf-renderer-${env.project_version}.jar /usr/bin/alfresco-docker-alfresco-pdf-renderer.jar && \
    yum install -y wget && \
    wget $ALFRESCO_PDF_RENDERER_LIB_RPM_URL && \
    tar xf alfresco-pdf-renderer-*-linux.tgz -C /usr/bin && \
	rm -f alfresco-pdf-renderer-*-linux.tgz && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java -jar /usr/bin/alfresco-docker-alfresco-pdf-renderer.jar

