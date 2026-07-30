# ============================================================================
# TICKET-ADV146 — Spring Boot Dockerfile (multi-stage Maven -> JRE 25 slim)
#
# Stage 1: build the JAR with Temurin JDK 25 + Maven.
# Stage 2: copy ONLY the JAR onto a slim JRE image.
# ============================================================================

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY backend backend
COPY recon-audit-starter recon-audit-starter
RUN ./backend/mvnw -q -B -f /workspace/pom.xml -pl recon-audit-starter,backend -am dependency:go-offline || true
RUN ./backend/mvnw -q -B -f /workspace/pom.xml -pl recon-audit-starter,backend -am -DskipTests package && \
    mv /workspace/backend/target/reconx-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S reconx && adduser -S reconx -G reconx
USER reconx
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
