#!/usr/bin/zsh
git add .
git commit -am "auto update"
git push
mvn clean package
cd docker/dep1
docker build -t xyz-search:1.0.2 .
docker tag xyz-search:1.0.2 10.168.1.102:8111/noogel/xyz-search:1.0.2
cd ../../
docker push 10.168.1.102:8111/noogel/xyz-search:1.0.2
echo 'ok!'