FROM maven:3.8.7-eclipse-temurin-19 as build
ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD . $HOME
RUN --mount=type=cache,target=/root/.m2 mvn -f $HOME/pom.xml clean package

FROM openjdk:19
RUN groupadd spring && useradd spring -g spring
USER spring:spring
COPY --from=build /usr/app/target/ /app/
ENTRYPOINT java -jar /app/*.jar