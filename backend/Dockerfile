# ============================================================================
# TICKET-ADV146 — Backend Multi-Stage Docker Build
#
# GOAL
# ----------------------------------------------------------------------------
# Package the Spring Boot backend into a small production-ready Docker image.
#
# Instead of shipping Maven, the JDK, source code, and build artifacts, we
# compile the application in one temporary container and copy only the finished
# executable JAR into a lightweight Java runtime image.
#
#
# WHY MULTI-STAGE?
# ----------------------------------------------------------------------------
# A normal Docker build would contain:
#
#   • Java Compiler (JDK)
#   • Maven
#   • Maven Cache
#   • Source Code
#   • Build Artifacts
#
# resulting in an image often 600MB+.
#
# With a multi-stage build:
#
# Stage 1
# --------
# Build everything using a full Java development environment.
#
# Stage 2
# --------
# Throw away the compiler and build tools.
# Copy ONLY the finished Spring Boot fat JAR.
#
# This usually reduces the image to under ~250MB while improving security.
#
#
# BUILD STAGE
# ----------------------------------------------------------------------------
# Uses Eclipse Temurin Java 25 JDK because compiling requires:
#
#   • javac
#   • Maven Wrapper
#   • annotation processors
#
# This stage is temporary and never ships to production.
#
# WORKSPACE
# ----------------------------------------------------------------------------
# /workspace becomes the root directory inside the container.
#
# Repository layout:
#
# /workspace
# ├── pom.xml                 (parent pom)
# ├── backend/
# └── recon-audit-starter/
#
#
# MAVEN BUILD
# ----------------------------------------------------------------------------
# The project is multi-module.
#
# Parent
#   ├── backend
#   └── recon-audit-starter
#
# Backend depends on recon-audit-starter.
#
# Maven flags:
#
# -f
#     Use the parent pom.xml.
#
# -pl
#     Build ONLY backend + recon-audit-starter.
#
# -am
#     Also build required dependent modules.
#
# dependency:go-offline
#     Download dependencies first so Docker can cache them.
#
# package
#     Produces the executable Spring Boot fat JAR.
#
#
# RUNTIME STAGE
# ----------------------------------------------------------------------------
# Uses only Eclipse Temurin Java Runtime (JRE Alpine).
#
# Since the application is already compiled, the runtime only needs:
#
#     java -jar app.jar
#
# No compiler.
# No Maven.
# No source code.
#
#
# SECURITY
# ----------------------------------------------------------------------------
# Never run containers as root.
#
# Create:
#
#     reconx
#
# and execute Spring Boot as this non-privileged user.
#
#
# FINAL IMAGE
# ----------------------------------------------------------------------------
# Runtime contains only:
#
#     Java Runtime
#     app.jar
#
# ENTRYPOINT launches:
#
#     java $JAVA_OPTS -jar app.jar
#
# JAVA_OPTS allows Docker Compose/Kubernetes to inject JVM options without
# rebuilding the image.
#
#
# OBSERVE
# ----------------------------------------------------------------------------
# docker build backend/
#
# docker images
#     Final image should be under roughly 250MB.
#
# docker run reconx-backend id
#     Shows the application runs as the reconx user (not root).
#
# docker run reconx-backend ls /app
#     Shows only app.jar is present.
#
# ============================================================================

FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY pom.xml pom.xml
COPY backend backend
COPY recon-audit-starter recon-audit-starter

# Download dependencies first (Docker layer caching)
RUN ./backend/mvnw -q -B \
    -f /workspace/pom.xml \
    -pl recon-audit-starter,backend \
    -am \
    dependency:go-offline || true

# Build executable Spring Boot JAR
RUN ./backend/mvnw -q -B \
    -f /workspace/pom.xml \
    -pl recon-audit-starter,backend \
    -am \
    -DskipTests \
    package && \
    mv /workspace/backend/target/reconx-*.jar /workspace/app.jar


FROM eclipse-temurin:25-jre-alpine

# Create non-root application user
RUN addgroup -S reconx && adduser -S reconx -G reconx

USER reconx

WORKDIR /app

# Copy only the finished artifact from the build stage
COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080

# Allows Docker Compose/Kubernetes to inject JVM arguments
ENV JAVA_OPTS=""

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]