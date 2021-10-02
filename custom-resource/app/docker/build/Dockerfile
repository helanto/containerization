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

################################################################################
# Assembly the project
################################################################################
ARG JAR_NAME=app.jar
ARG PROJECT_DIR=/app
WORKDIR ${PROJECT_DIR}
COPY . .
RUN sbt \
    clean \
    'set ThisBuild / assembly / assemblyJarName := "'$JAR_NAME'"' \
    assembly

################################################################################
# Run the application
################################################################################
RUN mv $PROJECT_DIR/target/scala-3.0.2/$JAR_NAME .
ENV _JAR_NAME=$JAR_NAME
ENV JVM_OPTS=""
ENTRYPOINT java $JVM_OPTS -jar $_JAR_NAME $0 $@
