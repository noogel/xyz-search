FROM openjdk:17-alpine
MAINTAINER noogel@163.com
RUN apk add --no-cache ffmpeg
RUN #apk update && apk add --no-cache ffmpeg gcompat opencv opencv-dev
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone
RUN mkdir -p /data/share /data/transfer /usr/share/xyz-search/data
RUN mkdir -p /data/collect1 /data/collect2 /data/collect3
COPY xyz-search-*.war /usr/share/xyz-search/xyz-search.war
WORKDIR /usr/share/xyz-search
EXPOSE 8081
ENV DEPLOY_ENV=docker
ENV DEPLOY_VER 1
VOLUME ["/usr/share/xyz-search/data"]
ENTRYPOINT ["java", "-jar", "xyz-search.war", "-Xms256m", "-Xmx512m"]
