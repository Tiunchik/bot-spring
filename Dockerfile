FROM alpine:latest

# Установка зависимостей
RUN apk add --no-cache \
    python3 \
    py3-pip \
    ffmpeg \
    curl \
    ca-certificates

# Установка yt-dlp через pip
RUN apk add --no-cache yt-dlp

# Проверка установки
RUN yt-dlp --version
RUN ffmpeg -version

RUN apk add --no-cache openjdk25-jdk

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
ENV PATH="$JAVA_HOME/bin:${PATH}"

ENV SERVICE_NAME=bot-spring
ARG JAR_FILE=build/libs/$SERVICE_NAME*.jar

COPY ${JAR_FILE} ${SERVICE_NAME}.jar

ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /$SERVICE_NAME.jar"]
