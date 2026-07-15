FROM maven:3.8.6-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/svc-tax apps/svc-tax
COPY apps/monolith-core/pom.xml apps/monolith-core/pom.xml
COPY apps/svc-billing/pom.xml apps/svc-billing/pom.xml
RUN mkdir -p apps/monolith-core/src/main/java apps/svc-billing/src/main/java
RUN mvn -q -pl apps/svc-tax -am package -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /workspace/apps/svc-tax/target/svc-tax-*.jar /app/app.jar
ENV PORT=5002
EXPOSE 5002
CMD ["java", "-jar", "/app/app.jar"]
