# Image provides a container in which to run alfresco-pdf-renderer transformations for Alfresco Content Services.

# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

FROM alfresco/alfresco-base-java:11.0.10-openjdk-centos-8@sha256:343c8f63cf80c7af51785b93d6972b0c00087a1c0b995098cb8285c4d9db74b5

ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/content/org/alfresco/alfresco-pdf-renderer/1.1/alfresco-pdf-renderer-1.1-linux.tgz
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG PDFUSERNAME=pdf
ARG USERID=33001

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    curl -s -S $ALFRESCO_PDF_RENDERER_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz && \
    tar xf alfresco-pdf-renderer-linux.tgz -C /usr/bin && \
	rm -f alfresco-pdf-renderer-linux.tgz && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY src/main/resources/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${PDFUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${PDFUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
