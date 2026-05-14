FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY common/pom.xml common/pom.xml
COPY core-service/pom.xml core-service/pom.xml
COPY currencies-service/pom.xml currencies-service/pom.xml

COPY common common
COPY core-service core-service
COPY currencies-service currencies-service

ARG SERVICE
RUN chmod +x mvnw
RUN ./mvnw -pl ${SERVICE} -am clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

ARG SERVICE
COPY --from=build /app/${SERVICE}/target/*.jar app.jar

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
