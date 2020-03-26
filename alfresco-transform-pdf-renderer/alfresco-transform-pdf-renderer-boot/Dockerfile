# Image provides a container in which to run alfresco-pdf-renderer transformations for Alfresco Content Services.

# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

FROM alfresco/alfresco-base-java:11.0.1-openjdk-centos-7-72b88c6f1f4c

ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/content/org/alfresco/alfresco-pdf-renderer/1.1/alfresco-pdf-renderer-1.1-linux.tgz
ENV PDFIUM_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/pdfium.txt
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG PDFUSERNAME=pdf
ARG USERID=33001
ARG VERSIONJAR=${env.project_artifactId}-${env.project_version}.jar
ARG SHORTJAR=${env.project_artifactId}.jar

COPY target/${VERSIONJAR} /usr/bin

RUN ln /usr/bin/${VERSIONJAR} /usr/bin/${SHORTJAR} && \
    curl -s -S $ALFRESCO_PDF_RENDERER_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz && \
    curl -s -S $PDFIUM_LICENSE_FILE               -o pdfium.txt && \
    tar xf alfresco-pdf-renderer-linux.tgz -C /usr/bin && \
	rm -f alfresco-pdf-renderer-linux.tgz && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${PDFUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${SHORTJAR}

EXPOSE 8090

USER ${PDFUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${SHORTJAR}

