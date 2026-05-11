FROM bellsoft/liberica-openjdk-debian:21

ENV TZ=Asia/Seoul
RUN apt-get update && apt-get install -y tzdata ffmpeg libuuid1 libasound2 && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]