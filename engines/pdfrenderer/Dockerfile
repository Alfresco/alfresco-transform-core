# Image provides a container in which to run alfresco-pdf-renderer transformations for Alfresco Content Services.

# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

# ACTUAL IMAGE
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
# jre17-rockylinux8 (29/07/2024)
FROM alfresco/alfresco-base-java@sha256:18ac53e9a267dc4389eaff80ad1f5b8deaf53da09c234de5fcc6e05ebcd4a77c

ARG PDF_RENDERER_VERSION=1.2
ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/org/alfresco/alfresco-pdf-renderer/${PDF_RENDERER_VERSION}/alfresco-pdf-renderer-${PDF_RENDERER_VERSION}-linux.tgz
ENV ALFRESCO_PDF_RENDERER_ARM64_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/org/alfresco/alfresco-pdf-renderer/${PDF_RENDERER_VERSION}/alfresco-pdf-renderer-${PDF_RENDERER_VERSION}-linux-arm.tgz
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG PDFUSERNAME=pdf
ARG USERID=33001

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    if [ "$(uname -m)" = "x86_64" ]; then \
      curl -s -S $ALFRESCO_PDF_RENDERER_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz; \
    else \
      curl -s -S $ALFRESCO_PDF_RENDERER_ARM64_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz; \
    fi && \
    tar xf alfresco-pdf-renderer-linux.tgz -C /usr/bin && \
	rm -f alfresco-pdf-renderer-linux.tgz && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY target/classes/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${PDFUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${PDFUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
