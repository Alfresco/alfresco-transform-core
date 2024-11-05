# Image provides a container in which to run miscellaneous transformations for Alfresco Content Services.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
FROM alfresco/alfresco-base-java:jre17-rockylinux9@sha256:fe7f8a96dc53835b79b8df9d608766b3c1a64af0b7f4e4c16f00a03352ce28fa

ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG MISCUSERNAME=transform-misc
ARG USERID=33006

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY target/classes/licenses/3rd-party/ /
COPY target/classes/fonts/NotoSans /usr/local/share/fonts/NotoSans

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${MISCUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${MISCUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
