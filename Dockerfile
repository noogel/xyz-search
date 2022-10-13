FROM openjdk:17-alpine
RUN mkdir -p /data/log /data/config /data/share /usr/share/search
COPY target/xyz-search-*.war /usr/share/search/xyz-search.war
WORKDIR /usr/share/search
EXPOSE 8081
ENV DEPLOY_ENV=docker
VOLUME ["/data/log", "/data/config"]
ENTRYPOINT ["java", "-jar", "xyz-search.war", "-Xms256m", "-Xmx512m"]
