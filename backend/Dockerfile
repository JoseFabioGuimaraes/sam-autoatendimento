# Dockerfile — SAM Backend (multi-stage)

# ===== Stage 1: Build =====
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copia tudo e builda em um único passo (mais confiável com cache de deps)
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -B

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S samgroup && adduser -S samuser -G samgroup
USER samuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
