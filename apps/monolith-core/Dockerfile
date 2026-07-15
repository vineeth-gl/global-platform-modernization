FROM maven:3.8.6-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/monolith-core apps/monolith-core
COPY apps/svc-billing/pom.xml apps/svc-billing/pom.xml
COPY apps/svc-tax/pom.xml apps/svc-tax/pom.xml
RUN mkdir -p apps/svc-billing/src/main/java apps/svc-tax/src/main/java
RUN mvn -q -pl apps/monolith-core -am package -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /workspace/apps/monolith-core/target/monolith-core-*.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
