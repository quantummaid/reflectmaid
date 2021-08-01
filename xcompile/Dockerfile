#
# Use this dockerfile to run continuous integration maven goals that require graalvm on your local development machine:
#
# 	$ snap install docker
# 	# in reflectmaid top-level directory
# 	$ docker build -f xcompile/Dockerfile -t reflectmaid-ci .
# 	$ docker run -it --rm --volume=$HOME/.m2:/root/.m2 reflectmaid-ci bash
# 	$ mvn verify -DprepareForMavenCentral (-Ddependency-check.skip)
#
# if you get the following warning, update GRAALVM_VERSION:
# 	...
# 	[WARNING] Major.Minor version mismatch between native-image-maven-plugin (21.1.0) and native-image executable (21.0.0.2)
#
#
FROM openjdk:11-jdk-slim

ARG GRAALVM_VERSION=21.1.0

RUN apt-get update --yes
RUN apt-get upgrade --yes
RUN apt-get autoremove --yes
RUN apt-get install --yes --no-install-recommends unzip curl gnupg python3-pip jq bc git build-essential libz-dev zlib1g-dev
RUN curl -L "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAALVM_VERSION}/graalvm-ce-java11-linux-amd64-${GRAALVM_VERSION}.tar.gz" -o "graalvm.tar.gz" && \
    tar -xzf graalvm.tar.gz
ENV PATH="/graalvm-ce-java11-${GRAALVM_VERSION}/bin:${PATH}"
ENV JAVA_HOME="/graalvm-ce-java11-${GRAALVM_VERSION}/"
RUN gu install native-image
RUN echo "alias python='python3'" >> ~/.bashrc
RUN . ~/.bashrc
RUN ln /usr/bin/python3 /usr/bin/python
RUN pip3 --no-cache-dir install setuptools
RUN pip3 --no-cache-dir install aws-sam-cli
RUN rm -rf /var/lib/apt/lists/*

ARG MAVEN_MAJOR_VERSION=3
ARG MAVEN_VERSION=3.6.3

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm awscliv2.zip && \
    curl https://downloads.apache.org/maven/maven-${MAVEN_MAJOR_VERSION}/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip > apache-maven-${MAVEN_VERSION}-bin.zip && \
    unzip apache-maven-${MAVEN_VERSION}-bin.zip && \
    rm apache-maven-${MAVEN_VERSION}-bin.zip && \
    ln -s /apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

COPY ./ /usr/local/app
WORKDIR /usr/local/app

