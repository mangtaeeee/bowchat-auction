FROM bellsoft/liberica-openjdk-alpine:17

# tzdata 패키지 설치 및 한국 시간대 설정
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apk del tzdata

VOLUME /tmp
COPY build/libs/*.jar app.jar

ENTRYPOINT ["java","-Dspring.profiles.active=prod","-jar","/app.jar"]