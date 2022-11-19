# elasticsearch：8.4.3 dockerfile

在官方的基础上默认增加了 smartcn\ik 分词插件

构建命令:

docker build -t elasticsearch:8.4.3-alpha .
docker tag elasticsearch:8.4.3-alpha noogel/elasticsearch:8.4.3-alpha

发包命令：

docker push noogel/elasticsearch:8.4.3-alpha
