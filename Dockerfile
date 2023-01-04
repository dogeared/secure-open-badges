#
# Build stage
#
FROM maven:3.8.7-eclipse-temurin-19 AS build
COPY src /home/app/src
COPY pom.xml /home/app
WORKDIR /home/app
RUN mvn clean install

#
# Package stage
#
FROM openjdk:19
COPY --from=build /home/app/target ./target
EXPOSE 8080
ENTRYPOINT ["java","-jar","./target/secure-open-badges-0.0.1-SNAPSHOT.jar"]