FROM python:3.14-alpine

ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

RUN apk add --update --no-cache --virtual .tmp-build-deps \
    gcc libc-dev linux-headers

RUN pip3 install --upgrade pip && pip3
RUN pip install curl_cffi
RUN pip install yt-dlp
RUN pip install ffmpeg

RUN apk update && apk upgrade
RUN apk add --no-cache openjdk25-jdk
RUN apk add ffmpeg

RUN mkdir -p /var/log/spring-bot

ENV SERVICE_NAME=spring-bot
ARG JAR_FILE=build/libs/$SERVICE_NAME*.jar

COPY ${JAR_FILE} ${SERVICE_NAME}.jar

VOLUME /var/log/spring-bot

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /$SERVICE_NAME.jar"]
