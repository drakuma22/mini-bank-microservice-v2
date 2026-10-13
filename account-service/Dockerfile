# ============================================
# STAGE 1 — BUILD
# CONCETTO: questo stage esiste solo per compilare il progetto.
# Alla fine, tutto quello che contiene (JDK completo, cache Maven,
# codice sorgente) verrà BUTTATO VIA. Non finisce nell'immagine finale.
# Lo chiamiamo "build" con AS per poterlo richiamare dopo.
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=builder /build/target/account-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]