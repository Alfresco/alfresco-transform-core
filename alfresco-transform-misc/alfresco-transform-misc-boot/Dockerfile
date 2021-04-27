# Image provides a container in which to run miscellaneous transformations for Alfresco Content Services.

FROM alfresco/alfresco-base-java:11.0.10-openjdk-centos-8@sha256:343c8f63cf80c7af51785b93d6972b0c00087a1c0b995098cb8285c4d9db74b5

ENV APACHE_LICENSE_FILE=https://raw.githubusercontent.com/Alfresco/alfresco-community-repo/master/packaging/distribution/src/main/resources/licenses/3rd-party/Apache%202.0.txt
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG MISCUSERNAME=transform-misc
ARG USERID=33006

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    curl -s -S $APACHE_LICENSE_FILE -o Apache\ 2.0.txt && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${MISCUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${MISCUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
