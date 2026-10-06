FROM gradle:8.14-jdk21-noble AS base
WORKDIR /java/app/base

RUN apt-get update 
RUN apt-get install -y build-essential
RUN apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false
RUN rm -rf /var/lib/apt/lists/*

COPY . .
COPY settings.gradle .
RUN gradle dependencies

FROM gradle:8.14-jdk21-noble AS builder
WORKDIR /java/app/builder

COPY --from=base /java/app/base /java/app/builder
RUN gradle clean build -x test --no-daemon


FROM gcr.io/distroless/java21-debian12 AS runtime
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
ENV JAVA_OPTS=""

USER nobody
WORKDIR /app

COPY --from=builder /java/app/builder/build/libs/azure-app-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar", "${JAVA_OPTS}", "{0}", "{@}" ]
