# Image provides a container in which to run LibreOffice transformations for Alfresco Content Services.

# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
ARG JAVA_BASE_IMAGE=alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c
FROM ${JAVA_BASE_IMAGE}

ARG LIBREOFFICE_VERSION=7.2.5

ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/org/libreoffice/libreoffice-dist/${LIBREOFFICE_VERSION}/libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz
ENV LIBREOFFICE_ARM64_RPM_URL=https://dl.rockylinux.org/pub/rocky/9/devel/aarch64/os/Packages/l/
ENV LIBREOFFICE_ARM64_RPM_VERSION=libreoffice-7.1.8.1
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG LIBREUSERNAME=libreoffice
ARG USERID=33003
ENV JAVA_BASE_IMAGE=${JAVA_BASE_IMAGE}

COPY target/${env.project_artifactId}-${env.project_version}.jar libreoffice-dist-*-linux.gz /

# Link the jar file
RUN ln /${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar

# Install required packages
RUN yum install -y cairo cups-libs libSM libGLU procps

# Install LibreOffice for x86_64
RUN if [ "$(uname -m)" = "x86_64" ]; then \
      test -f libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz && \
      ln -s libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz libreoffice-dist-linux.gz || \
      curl -s -S $LIBREOFFICE_RPM_URL -o libreoffice-dist-linux.gz && \
      tar xzf libreoffice-dist-linux.gz && \
      yum localinstall -y LibreOffice*/RPMS/*.rpm && \
      rm -rf libreoffice-dist-*linux.gz LibreOffice_*_Linux_x86-64_rpm; \
    fi

# Install LibreOffice for ARM64 and set Java alternative
RUN uname -m
RUN if [ "$(uname -m)" != "x86_64" ]; then echo "ARM64 detected"; fi
RUN if [ "$(uname -m)" != "x86_64" ]; then \
  LIBREOFFICE_ARM64_RPM_VERSION=$(curl -s $LIBREOFFICE_ARM64_RPM_URL | grep -o "${LIBREOFFICE_ARM64_RPM_VERSION}.*" | cut -d '"' -f 1 | sort | tail -n1); \
  echo $LIBREOFFICE_ARM64_RPM_VERSION > /tmp/libreoffice_version; \
  cat /tmp/libreoffice_version; \
fi
RUN if [ "$(uname -m)" != "x86_64" ]; then \
  dnf --enablerepo=devel install -y ${LIBREOFFICE_ARM64_RPM_URL}$(cat /tmp/libreoffice_version); \
fi
RUN printenv JAVA_BASE_IMAGE
RUN JAVA_VERSION=$(echo "$JAVA_BASE_IMAGE" | sed -E 's/.*jre([0-9]+).*/\1/'); \
    echo $JAVA_VERSION > /tmp/java_version; \
    cat /tmp/java_version
RUN printenv JAVA_VERSION || cat /tmp/java_version
RUN if [ "$(uname -m)" != "x86_64" ]; then \
  update-alternatives --set java java-$(cat /tmp/java_version)-openjdk.aarch64; \
fi

# Clean up yum cache
RUN yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY target/classes/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${LIBREUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${LIBREUSERNAME}

# Copy the script to set LIBREOFFICE_HOME
COPY docker-entrypoint.sh /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh", "${env.project_artifactId}"]
