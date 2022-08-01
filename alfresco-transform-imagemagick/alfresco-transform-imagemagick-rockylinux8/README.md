# How to create ImageMagick RPM packages for Rocky Linux 8
1. `cd` to this directory
2. Use Rocky Linux base image and execute the [build-rpms.sh](scripts/build-rpms.sh) script

```bash
docker run -it --rm -v `pwd`:/opt/alfresco/imagemagick alfresco/alfresco-base-java:jre11-rockylinux8-202207110835@sha256:01810e3e77d188f48ad6549b63cf1d34ce7d61ba9ca3fb03f0c24cc8f5c73429 /bin/bash /opt/alfresco/imagemagick/scripts/build-rpms.sh
```

> **Note:** If you want to know more details or need to upgrade the ImageMagick version please check the comments in the [build-rpms.sh](scripts/build-rpms.sh) file

3. RPMs will be created in the `rpms` directory

```bash
bash-3.2$ ls rpms
ImageMagick-7.0.10-59.x86_64.rockylinux8.rpm	ImageMagick-libs-7.0.10-59.x86_64.rockylinux8.rpm
```