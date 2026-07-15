FROM maven:3.8.6-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/svc-billing apps/svc-billing
COPY apps/monolith-core/pom.xml apps/monolith-core/pom.xml
COPY apps/svc-tax/pom.xml apps/svc-tax/pom.xml
RUN mkdir -p apps/monolith-core/src/main/java apps/svc-tax/src/main/java
RUN mvn -q -pl apps/svc-billing -am package -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /workspace/apps/svc-billing/target/svc-billing-*.jar /app/app.jar
ENV PORT=5001
EXPOSE 5001
CMD ["java", "-jar", "/app/app.jar"]
