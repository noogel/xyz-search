FROM  192.168.124.11:8111/dockerhub/openjdk:17-alpine-20250102
MAINTAINER noogel@163.com
ENV PROJECT_NAME="xyz-search"
ENV DEPLOY_ENV=docker
RUN apk add --no-cache ffmpeg
RUN #apk update && apk add --no-cache ffmpeg gcompat opencv opencv-dev
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone
RUN mkdir -p /data/share /usr/share/$PROJECT_NAME/data
COPY $PROJECT_NAME-*.war /usr/share/$PROJECT_NAME/$PROJECT_NAME.war
WORKDIR /usr/share/$PROJECT_NAME
EXPOSE 8081
VOLUME ["/usr/share/$PROJECT_NAME/data"]
ENTRYPOINT ["sh", "-c", "java -jar $PROJECT_NAME.war -Xms256m -Xmx512m"]
