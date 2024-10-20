FROM openjdk:17-jdk-slim
EXPOSE 8080
WORKDIR /opt/app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]