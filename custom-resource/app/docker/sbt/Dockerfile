FROM openjdk:8

RUN apt-get -qq update && apt-get install -qq -y unzip && rm -rf /var/lib/apt/lists/*

################################################################################
# Install sbt
################################################################################
ARG SBT_VERSION=1.5.5
RUN cd /usr/local && \
    curl -sSLO https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.zip && \
    unzip sbt-${SBT_VERSION}.zip && \
    rm sbt-${SBT_VERSION}.zip
ENV PATH="${PATH}:/usr/local/sbt/bin"
RUN sbt -Dsbt.rootdir=true sbtVersion

ENTRYPOINT [ "sbt" ]
CMD [ "sbtVersion" ]
