FROM bellsoft/liberica-openjdk-alpine:21

# ffmpeg 패키지 추가
RUN apk add --no-cache tzdata ffmpeg && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]