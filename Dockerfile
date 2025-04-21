FROM bellsoft/liberica-openjdk-alpine:24-cds

ENV SERVICE_NAME=bot-spring
ENV LOG_PATH=./docker/logs

RUN mkdir -p ${LOG_PATH}/${SERVICE_NAME}/ && ulimit -s 65536
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring ${LOG_PATH}/${SERVICE_NAME}/
USER spring:spring
ARG JAR_FILE=build/libs/${SERVICE_NAME}*.jar
COPY ${JAR_FILE} ${SERVICE_NAME}.jar

ENV JAVA_OPTS="-Xss1m -Xms128m -Xmx256m -Duser.timezone=UTC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /$SERVICE_NAME.jar"]
