# syntax=docker/dockerfile:1

# ---------- Etapa build: JDK 21 + Maven Wrapper del modulo ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 1) Solo wrapper + pom.xml: la descarga de dependencias queda en cache mientras el pom no cambie
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline -DskipTests

# 2) Codigo fuente (sin application-local.properties ni keys/: ver .dockerignore)
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/*.jar app.jar

# ---------- Etapa runtime: solo JRE, usuario no root ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

EXPOSE 8443
ENV JAVA_OPTS="-Xms64m -Xmx384m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
