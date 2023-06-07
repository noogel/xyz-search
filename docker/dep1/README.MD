# xyz-search:1 dockerfile

构建命令：

docker build -t xyz-search:1.0.3 .
docker tag xyz-search:1.0.3 noogel/xyz-search:1.0.3
docker tag xyz-search:1.0.3 10.168.1.102:8111/noogel/xyz-search:1.0.3

一键构建：
mvn clean package && cd docker/dep1 && docker build -t xyz-search:1.0.3 . && docker tag xyz-search:1.0.3 10.168.1.102:8111/noogel/xyz-search:1.0.3 && cd ../../
mvn clean package && cd docker/dep1 && docker build -t xyz-search:1.0.3 . && docker tag xyz-search:1.0.3 noogel/xyz-search:1.0.3 && cd ../../

发布命令：

docker push 10.168.1.102:8111/noogel/xyz-search:1.0.3
docker push noogel/xyz-search:1.0.3


拉取命令：

docker pull 10.168.1.102:8111/noogel/xyz-search:1.0.3
docker pull noogel/xyz-search:1.0.3

测试启动：

docker run -d --restart=always --name xyzSearch --network xyz-bridge-net -p 8081:8081 \
-v /home/xyz/DockerSharingData/xyzSearch/searchData:/usr/share/xyz-search/data \
-v /home/xyz/DockerSharingData/TestSearch:/data/share \
xyz-search:1.0.3
