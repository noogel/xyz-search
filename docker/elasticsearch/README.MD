# elasticsearch:8.17.4 dockerfile

在官方的基础上默认增加了 smartcn\ik 分词插件

构建命令:

docker build --platform=linux/amd64 -t elasticsearch:8.17.4-alpha .
docker tag elasticsearch:8.17.4-alpha 192.168.124.11:8111/noogel/elasticsearch:8.17.4-alpha

发包命令：

docker push 192.168.124.11:8111/noogel/elasticsearch:8.17.4-alpha

拉取命令：

docker pull 192.168.124.11:8111/noogel/elasticsearch:8.17.4-alpha

测试启动：

docker run -d --name xyzEsAlpha --network xyz-bridge-net -p 9300:9300 -p 9200:9200 \
-e "xpack.security.enabled=false" \
-e "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms512m -Xmx1g" \
-v /home/xyz/DockerSharingData/xyzEs2:/usr/share/elasticsearch/data \
192.168.124.11:8111/noogel/elasticsearch:8.17.4-alpha
