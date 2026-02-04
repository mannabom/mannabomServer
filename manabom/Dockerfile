FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata

ENTRYPOINT ["java", "-jar","app.jar"]