# xyz-search

#### 介绍
畅文全索

#### 软件架构
软件架构说明


#### 安装教程

1.  xxxx
2.  xxxx
3.  xxxx

#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)

#### 一些测试命令


http://10.168.1.132:8081/admin/test?resId=4f0175bc87fa8215b99e3eaa177f1fe6


1. 重置索引
   curl http://localhost:8081/admin/es/index/reset
2. 同步索引
   curl http://localhost:8081/admin/es/data/sync
3. 删除文件
4. 查看同目录差异
   http://10.168.1.132:8081/admin/test?resId=4f0175bc87fa8215b99e3eaa177f1fe6
5. 删除索引
   curl http://localhost:8081/admin/es/data/delete\?resId\=a7f14ae1aaf81f02c3d8c37da6545ebe


-Dconfig.path=/home/xyz/TestSearch/search-config.yml
DEPLOY_ENV=docker



docker network create -d bridge xyz-bridge-net

docker build -t xyz-search:v2 .

docker run -d --name xyzSearch --network xyz-bridge-net -p 8082:8081 -v /home/xyz/DockerSharingData/TestSearch:/data/share xyz-search:v2

docker run -d --name xyzEs2 --network xyz-bridge-net -p 9300:9300 -p 9200:9200 -e "xpack.security.enabled=false" -e "discovery.type=single-node" -e ES_JAVA_OPTS="-Xms512m -Xmx1g" -v /home/xyz/DockerSharingData/xyzEs2:/usr/share/elasticsearch/data elasticsearch:8.4.3


重置 es 密码
Auto-configuration will not generate a password for the elastic built-in superuser, as we cannot  determine if there is a terminal attached to the elasticsearch process. You can use the `bin/elasticsearch-reset-password` tool to set the password for the elastic user.


docker op

https://www.runoob.com/docker/docker-dockerfile.html
https://www.runoob.com/docker/docker-run-command.html
https://www.runoob.com/docker/docker-container-connection.html


中文分词器
https://developer.aliyun.com/article/848626

es 安装插件
sudo bin/elasticsearch-plugin install analysis-smartcn
es 移除插件
sudo bin/elasticsearch-plugin remove analysis-smartcn

https://www.elastic.co/guide/en/elasticsearch/plugins/8.3/analysis-smartcn.html

修改 es 配置文件
docker cp elasticsearch.yml xyzEs2:/usr/share/elasticsearch/config/elasticsearch.yml
docker cp xyzEs2:/usr/share/elasticsearch/config/elasticsearch.yml .

获取CA证书
docker cp xyzEs3:/usr/share/elasticsearch/config/certs/http_ca.crt .


How to Run Elasticsearch 8 on Docker for Local Development
https://levelup.gitconnected.com/how-to-run-elasticsearch-8-on-docker-for-local-development-401fd3fff829


docker run -d --name xyzEs3 --network xyz-bridge-net -p 9300:9300 -p 9200:9200 -v /home/xyz/DockerSharingData/xyzEs3:/usr/share/elasticsearch/data elasticsearch:8.4.3