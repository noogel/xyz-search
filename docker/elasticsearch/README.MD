# elasticsearch：8.4.3 dockerfile

在官方的基础上默认增加了 smartcn\ik 分词插件

构建命令:

docker build -t elasticsearch:8.4.3-alpha .
docker tag elasticsearch:8.4.3-alpha 10.168.1.102:8111/noogel/elasticsearch:8.4.3-alpha

发包命令：

docker push 10.168.1.102:8111/noogel/elasticsearch:8.4.3-alpha

拉取命令：

docker pull 10.168.1.102:8111/noogel/elasticsearch:8.4.3-alpha

测试启动：

docker run -d --name xyzEsAlpha --network xyz-bridge-net -p 9300:9300 -p 9200:9200 \
-e "xpack.security.enabled=false" \
-e "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms512m -Xmx1g" \
-v /home/xyz/DockerSharingData/xyzEs2:/usr/share/elasticsearch/data \
elasticsearch:8.4.3-alpha
