# syntax=docker/dockerfile:1
#
# Multi-stage Dockerfile for the portfolio-valuation Spring Boot app.
#
#   Stage 1 ("build")   – compiles and packages the jar with Maven.
#   Stage 2 ("runtime") – a small JRE-only image that just runs the jar.
#
# Why two stages?  The Maven image is ~700 MB and contains the JDK, Maven and
# our whole source tree.  None of that is needed at runtime, so we copy ONLY the
# finished jar into a slim JRE image.  Smaller image = faster pushes/pulls to
# ECR and a smaller attack surface.

# ---------------------------------------------------------------------------
# Stage 1: build the jar
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the pom first so Docker can cache the dependency download layer.
# Source changes then only invalidate the layers below this one.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the source and build.  Tests, Spotless and the JaCoCo coverage gate
# have ALREADY run in the CI pipeline ("mvn -B verify"), so we skip them here
# to keep the image build fast.
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------------------------------------------------------------------------
# Stage 2: runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

# curl is used by the HEALTHCHECK below (the Debian-based JRE image doesn't
# ship with it).  Clean up apt caches in the same layer to keep the image small.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Run as a non-root user.  If the app is ever compromised the attacker does not
# get root inside the container.
RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app

# Copy the fat jar from the build stage and give it a stable name.
COPY --from=build --chown=app:app /workspace/target/portfolio-valuation-*.jar /app/app.jar

USER app

# Spring Boot listens on 8080 by default.
EXPOSE 8080

# Docker-level health check (ECS also has its own, defined in Terraform).
# Spring Boot Actuator returns {"status":"UP"} with HTTP 200 when healthy.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Container-friendly JVM flags:
#   -XX:MaxRAMPercentage=75  – size the heap relative to the container's memory
#                              limit rather than the host's total RAM.
#   -XX:+UseContainerSupport – (default on in JDK 21, shown for clarity).
#   -Djava.security.egd      – faster start-up by using non-blocking randomness.
# JAVA_OPTS lets you append extra flags at runtime without rebuilding the image.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom $JAVA_OPTS -jar /app/app.jar"]
