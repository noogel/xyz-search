# xyz-search

<div align="center">
<img src="https://via.placeholder.com/150" alt="xyz-search logo" width="150px">

[![Maven Central](https://img.shields.io/maven-central/v/noogel.xyz/xyz-search.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/noogel.xyz/xyz-search/)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Docker Pulls](https://img.shields.io/docker/pulls/noogel/xyz-search.svg?style=flat-square)](https://hub.docker.com/r/noogel/xyz-search)

**一个强大的全文搜索与智能检索系统**
</div>

## 📚 目录

* [💡 项目介绍](#-项目介绍)
* [✨ 主要特性](#-主要特性)
* [🔍 系统架构](#-系统架构)
* [🚀 快速开始](#-快速开始)
* [🔧 配置说明](#-配置说明)
* [🐳 Docker部署](#-docker部署)
* [📊 开发计划](#-开发计划)
* [👥 贡献指南](#-贡献指南)
* [📄 许可证](#-许可证)

## 💡 项目介绍

xyz-search是一个基于Spring Boot和Lucene的全文搜索系统,支持多种文件格式的索引和搜索。它提供了简单易用的API和Web界面,可以帮助用户快速构建全文搜索应用。通过集成Spring AI功能,支持智能搜索和内容分析,为用户提供更精准的搜索体验。

### 🎮 在线演示

*即将上线*

## ✨ 主要特性

### 📄 多格式文档支持
* 支持PDF、Office文档(Word、Excel、PowerPoint)等多种格式
* 支持图片内容识别和索引
* 支持HTML和纯文本文件

### 🔎 高性能搜索引擎
* 基于Lucene的高效索引和检索
* 支持中文分词和智能检索
* 实时索引更新和搜索结果优化

### 🤖 AI增强能力
* 集成Spring AI,支持智能搜索
* 内容理解和语义分析
* 支持对话式搜索体验

### 📚 电子书管理
* 支持OPDS协议,方便电子书管理
* 提供格式转换和阅读功能
* 支持电子书元数据提取和管理

### 💻 易用的界面与API
* 简洁现代的Web界面
* 完整的RESTful API
* 支持自定义主题和布局

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
│  搜索服务      │  索引服务      │   AI服务       │  文件服务│
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

### 启动服务

```bash
# 使用Maven启动
mvn spring-boot:run

# 访问Web界面
http://localhost:8081
```

### 使用示例

**1. 索引文件**
```bash
curl http://localhost:8081/admin/es/index/reset
curl http://localhost:8081/admin/es/data/sync
```

**2. 搜索文件**
```bash
curl "http://localhost:8081/api/search?q=关键词"
```

**3. 智能聊天**
```bash
curl "http://localhost:8081/chat/stream?message=请找出关于spring的文档&resId=123456"
```

## 🔧 配置说明

主要配置项在`application.yml`中:

```yaml
server:
  port: 8081

spring:
  ai:
    ollama:
      base-url: http://localhost:11434
    openai:
      api-key: your-api-key

xyz:
  search:
    index-path: /path/to/index
    data-path: /path/to/data
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

### 环境变量

```bash
# 配置文件路径
-Dconfig.path=/path/to/search-config.yml

# 部署环境
DEPLOY_ENV=docker
```

## 📊 开发计划

- [ ] 支持更多文件格式
- [ ] 优化搜索性能
- [ ] 添加更多AI功能
- [ ] 改进Web界面
- [ ] 添加用户认证
- [ ] 开发移动端应用
- [ ] 优化中文分词效果

## 👥 贡献指南

1. Fork 本仓库
2. 新建特性分支
3. 提交代码
4. 新建 Pull Request

## 📄 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可证