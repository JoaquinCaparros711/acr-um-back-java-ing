FROM maven:3.9-eclipse-temurin-21-noble AS base
WORKDIR /java/app/base

RUN apt-get update 
RUN apt-get install -y build-essential
RUN apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false
RUN rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:resolve -B

FROM maven:3.9-eclipse-temurin-21-noble AS builder
WORKDIR /java/app/builder

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && curl -fsSL -o /java/app/builder/applicationinsights-agent.jar https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.1/applicationinsights-agent-3.7.1.jar \
    && chmod 644 /java/app/builder/applicationinsights-agent.jar \
    && rm -rf /var/lib/apt/lists/*

COPY --from=base /root/.m2 /root/.m2
COPY . .
RUN mvn clean package -DskipTests -B

FROM gcr.io/distroless/java21-debian13:nonroot AS runtime
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
ENV JAVA_OPTS=""

USER nobody
WORKDIR /app

COPY --from=builder /java/app/builder/target/app.jar /app/app.jar
COPY --from=builder /java/app/builder/applicationinsights-agent.jar /app/applicationinsights-agent.jar
EXPOSE 8080

ENTRYPOINT [ "java", "-javaagent:/app/applicationinsights-agent.jar", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar" ]

