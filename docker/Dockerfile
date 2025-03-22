FROM 192.168.124.11:8111/dockerhub/openjdk:17-alpine-20250102
LABEL maintainer="noogel@163.com"
LABEL description="Xyz Search - Full-text search system based on Spring Boot and Lucene"

ENV PROJECT_NAME="xyz-search"
ENV DEPLOY_ENV="docker"
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8"

# gcompat opencv opencv-dev
RUN apk add --no-cache ffmpeg netcat-openbsd && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    mkdir -p /data/share /usr/share/$PROJECT_NAME/data && \
    rm -rf /var/cache/apk/* /tmp/* /var/tmp/*

WORKDIR /usr/share/$PROJECT_NAME

COPY $PROJECT_NAME-*.war ./$PROJECT_NAME.war

EXPOSE 8081

VOLUME ["/usr/share/$PROJECT_NAME/data"]

HEALTHCHECK --interval=30s --timeout=3s \
    CMD nc -z localhost 8081 || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar $PROJECT_NAME.war"]
