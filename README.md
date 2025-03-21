# xyz-search

<div align="center">
<img src="data:image/png;base64,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" alt="xyz-search logo" width="150px">

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Docker Pulls](https://img.shields.io/docker/pulls/noogel/xyz-search.svg?style=flat-square)](https://hub.docker.com/r/noogel/xyz-search)
[![Version](https://img.shields.io/badge/version-1.2.1-blue.svg?style=flat-square)](https://github.com/noogel/xyz-search/releases)

**一个强大的全文搜索与智能检索系统 | 让数据检索更智能、更简单**
</div>

## 📚 目录

* [💡 项目介绍](#-项目介绍)
* [✨ 主要特性](#-主要特性)
* [🔍 系统架构](#-系统架构)
* [🚀 快速开始](#-快速开始)
* [🔧 配置说明](#-配置说明)
* [🐳 Docker部署](#-docker部署)
* [📊 开发计划](#-开发计划)
* [💬 常见问题](#-常见问题)
* [👥 贡献指南](#-贡献指南)
* [📄 许可证](#-许可证)

## 💡 项目介绍

xyz-search是一个基于Spring Boot和Lucene的全文搜索系统，支持多种文件格式的索引和搜索。它提供了简单易用的API和Web界面，可以帮助用户快速构建全文搜索应用。通过集成Spring AI功能，支持智能搜索和内容分析，为用户提供更精准的搜索体验。

系统专为个人知识管理、企业文档搜索和数字图书馆而设计，可以轻松处理从几千到数十万的文档集合。无论是管理个人电子书库，还是构建企业级文档检索平台，xyz-search都能满足您的需求。

### 🎮 在线演示

*即将上线*

## ✨ 主要特性

### 📄 多格式文档支持
* 支持PDF、Office文档等多种格式
* 支持电子书（epub）识别和索引
* 支持图片内容识别
* 支持HTML和纯文本文件
* 支持视频索引（仅文件名，内容暂未支持），和预览
* 支持多级目录结构

### 🔎 高性能搜索引擎
* 基于Lucene的高效索引和检索
* 支持中文分词和智能检索
* 实时索引更新和搜索结果优化
* 支持标签和元数据搜索
* 支持文件类型和大小过滤
* 支持搜索结果高亮显示

### 🤖 AI增强能力
* 集成Spring AI，支持智能搜索
* 内容理解和语义分析
* 支持对话式搜索体验
* 集成OpenAI和本地Ollama模型
* 文档内容智能总结
* 搜索结果智能排序

### 📚 电子书管理
* 支持OPDS协议，方便电子书管理
* 提供格式转换和阅读功能
* 支持电子书元数据提取和管理
* 兼容主流电子书阅读器
* 支持封面和目录索引

### 💻 易用的界面与API
* 简洁现代的Web界面
* 完整的RESTful API
* 支持自定义主题和布局
* 移动端自适应设计
* 丰富的文件预览功能

## 🔍 系统架构

```
┌────────────────┐    ┌────────────────┐    ┌────────────────┐
│                │    │                │    │                │
│   Web界面      │    │  RESTful API   │    │  OPDS协议      │
│                │    │                │    │                │
└───────┬────────┘    └───────┬────────┘    └───────┬────────┘
        │                     │                     │
┌───────┴─────────────────────┴─────────────────────┴────────┐
│                                                            │
│                      Spring Boot                           │
│                                                            │
├────────────────┬────────────────┬────────────────┬─────────┤
│                │                │                │         │
│    搜索服务     │     索引服务     │     AI服务     │  文件服务│
│                │                │                │         │
└───────┬────────┘    └───────┬───┘    └──────┬────┘ └───┬───┘
        │                     │                │          │
┌───────┴─────────┐    ┌──────┴───────┐   ┌───┴──────┐   │
│                 │    │              │   │          │   │
│     Lucene      │    │   SQLite     │   │ Spring AI│   │
│                 │    │              │   │          │   │
└─────────────────┘    └──────────────┘   └──────────┘   │
                                                         │
                       ┌────────────────────────────┐    │
                       │                            │    │
                       │        文件存储           ◄─────┘
                       │                            │
                       └────────────────────────────┘
```

## 🚀 快速开始

### Maven依赖

在您的项目中添加以下依赖：

```xml
<dependency>
    <groupId>noogel.xyz</groupId>
    <artifactId>xyz-search</artifactId>
    <version>1.2.1</version>
</dependency>
```

### 启动服务

```bash
# 使用Maven启动
mvn spring-boot:run

# 或使用JAR包启动
java -jar xyz-search-1.2.1.jar

# 访问Web界面
http://localhost:8081
```

### 使用示例

**1. 索引文件**
```bash
# 重置索引
curl http://localhost:8081/admin/es/index/reset

# 同步数据
curl http://localhost:8081/admin/es/data/sync

# 指定目录索引
curl "http://localhost:8081/admin/es/data/sync?dir=/path/to/documents"
```

**2. 搜索文件**
```bash
# 基本搜索
curl "http://localhost:8081/api/search?q=关键词"

# 按类型搜索
curl "http://localhost:8081/api/search?q=关键词&resType=pdf,doc"

# 限制结果数量
curl "http://localhost:8081/api/search?q=关键词&limit=50"
```

**3. 智能聊天**
```bash
# 基于文档的对话
curl "http://localhost:8081/chat/stream?message=请找出关于spring的文档&resId=123456"

# 基于搜索结果的问答
curl -X POST -H "Content-Type: application/json" \
  -d '{"message":"这个项目的主要功能是什么?", "resId":"all"}' \
  http://localhost:8081/chat/stream
```

## 🔧 配置说明

主要配置项在`application.yml`中:

```yaml
server:
  port: 8081

spring:
  # 数据源配置
  datasource:
    url: jdbc:sqlite:${xyz.search.data-path}/db/xyz-search.db
    driver-class-name: org.sqlite.JDBC
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.community.dialect.SQLiteDialect
  
  # 大模型配置
  ai:
    ollama:
      base-url: http://localhost:11434
      model: llama3
    openai:
      api-key: your-api-key
      model: gpt-4o

xyz:
  search:
    # 索引路径
    index-path: /path/to/index
    # 数据路径
    data-path: /path/to/data
    # 文件路径
    file-path: /path/to/files
    # OPDS配置
    opds-directory: /path/to/opds
    # 索引线程数
    thread-pool-size: 4
    # 定时任务配置
    scheduler:
      enabled: true
      cron: "0 0 2 * * ?"  # 每天凌晨2点
```

### 进阶配置

**自定义分词配置**

```yaml
xyz:
  search:
    analyzer:
      type: smart_cn  # 使用中文分词
      custom-dict: /path/to/dict.txt  # 自定义词典
```

**搜索结果优化**

```yaml
xyz:
  search:
    result:
      highlight: true  # 启用高亮
      snippet-length: 200  # 摘要长度
      max-results: 100  # 最大结果数
```

## 🐳 Docker部署

### 使用预构建镜像

```bash
# 创建网络
docker network create -d bridge xyz-bridge-net

# 拉取镜像
docker pull noogel/xyz-search:1.2.1

# 运行容器
docker run -d --name xyzSearch --network xyz-bridge-net -p 8081:8081 \
-v /path/to/searchData:/usr/share/xyz-search/data \
-v /path/to/share:/data/share \
noogel/xyz-search:1.2.1
```

### 自行构建镜像

```bash
# 基本构建
docker build -t xyz-search:1.2.1 .

# 标记镜像
docker tag xyz-search:1.2.1 用户名/xyz-search:1.2.1
```

### 一键构建与部署

```bash
# 一键构建
mvn clean package && cd docker/dep1 && docker build -t xyz-search:1.2.1 . && \
docker tag xyz-search:1.2.1 用户名/xyz-search:1.2.1 && cd ../../

# 发布到镜像仓库
docker push 用户名/xyz-search:1.2.1

# 一键部署
docker run -d --restart=always --name xyzSearch --network xyz-bridge-net -p 8081:8081 \
-v /实际路径/searchData:/usr/share/xyz-search/data \
-v /实际路径/share:/data/share \
用户名/xyz-search:1.2.1
```

### Docker Compose部署

创建`docker-compose.yml`文件：

```yaml
version: '3'
services:
  xyz-search:
    image: noogel/xyz-search:1.2.1
    container_name: xyzSearch
    restart: always
    ports:
      - "8081:8081"
    volumes:
      - ./searchData:/usr/share/xyz-search/data
      - ./share:/data/share
    environment:
      - DEPLOY_ENV=docker
      - JAVA_OPTS=-Xms256m -Xmx512m
    networks:
      - xyz-net

networks:
  xyz-net:
    driver: bridge
```

启动服务：

```bash
docker-compose up -d
```

### 环境变量

```bash
# 配置文件路径
-Dconfig.path=/path/to/search-config.yml

# 部署环境
DEPLOY_ENV=docker

# JVM参数
JAVA_OPTS=-Xms256m -Xmx512m

# 日志级别
LOG_LEVEL=INFO
```

## 💬 常见问题

**Q: 如何修改默认端口?**
A: 在application.yml中修改server.port属性，或在启动命令中添加`--server.port=新端口`参数。

**Q: 如何更新索引?**
A: 使用`curl http://localhost:8081/admin/es/data/sync`命令，或在Web界面中点击"更新索引"按钮。

**Q: 支持哪些文件格式?**
A: 支持PDF、Word、Excel、PowerPoint、TXT、HTML、EPUB以及常见图片格式等。

**Q: 如何集成到现有系统?**
A: 可以通过RESTful API或将xyz-search作为依赖添加到项目中进行集成。

## 📊 开发计划

- [ ] 支持音频内容识别与索引
- [ ] 优化搜索性能
- [ ] 添加更多AI模型支持
- [ ] 改进Web界面，提供更丰富的主题
- [ ] 添加用户认证和权限管理
- [ ] 开发移动端应用
- [ ] 优化中文分词效果
- [ ] 支持多语言搜索
- [ ] 添加搜索结果聚合功能

## 👥 贡献指南

1. Fork 本仓库
2. 新建特性分支
3. 提交代码
4. 新建 Pull Request

我们欢迎各种形式的贡献，包括但不限于：
- 新功能开发
- Bug修复
- 文档改进
- 测试用例编写

## 📄 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可证