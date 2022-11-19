FROM openjdk:17-alpine
MAINTAINER noogel@163.com
RUN mkdir -p /data/share /data/ebook /data/transfer /usr/share/xyz-search/data
COPY xyz-search-*.war /usr/share/xyz-search/xyz-search.war
WORKDIR /usr/share/xyz-search
EXPOSE 8081
ENV DEPLOY_ENV=docker
ENV DEPLOY_VER 1
LABEL version="1.0.2"
LABEL description="Noogel's mirror image using fulltext search."
VOLUME ["/usr/share/xyz-search/data"]
ENTRYPOINT ["java", "-jar", "xyz-search.war", "-Xms256m", "-Xmx512m"]
