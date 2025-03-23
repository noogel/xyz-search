<div align="center">

<h1>xyz-search👋</h1>

[![License](https://img.shields.io/badge/license-GPL%203.0-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0.html)
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

畅文全索是一个智能化全文检索系统。基于Spring Boot和Lucene设计，支持多种文件格式的全文内容检索，通过集成Spring AI功能，支持智能搜索和内容分析，为用户提供更精准的搜索体验。

系统专为个人知识管理和数字图书馆而设计，可以轻松处理从几千到数十万的文档集合。无论是管理个人电子书库，还是构建企业级文档检索平台，畅文全索都能满足您的需求。

## ✨ 主要特性

### 📄 多格式文档支持

* 支持PDF、Office文档
* 支持Epub电子书内容识别和索引
* 支持图片内容识别（依赖PaddleOCR）
* 支持网页和纯文本文件
* 支持视频 metadata 索引和预览

### 🔎 高性能搜索引擎

* 基于Lucene的高效索引和检索
* 支持中文分词和智能检索
* 实时索引更新和搜索结果优化
* 支持文件类型和大小过滤
* 支持搜索结果高亮显示

### 🤖 AI增强能力

* 集成Spring AI，支持智能搜索
* 内容理解和语义分析
* 支持对话式搜索体验
* 支持本地Ollama模型
* 文档内容智能总结

### 📚 电子书管理

* 支持OPDS协议，方便电子书管理
* 提供Epub电子书在线阅读
* 兼容主流电子书阅读器
* 支持封面和目录索引

### 💻 易用的界面与API

* 简洁现代的Web界面
* 支持主动添加文档
* 支持特定文件自动搜集
* 移动端自适应设计
* 丰富的文件预览功能

## 🔍 系统架构

```
┌────────────────┐      ┌────────────────┐     ┌────────────────┐
│                │      │                │     │                │
│   Web 界面      │      │  RESTful API   │     │    OPDS协议    │
│                │      │                │     │                │
└───────┬────────┘      └───────┬────────┘     └───────┬────────┘
        │                       │                      │
┌───────┴───────────────────────┴──────────────────────┴─────────┐
│                            Spring Boot                         │
├────────────────┬────────────────┬────────────────┬─────────────┤
│                │                │                │             │
│    搜索服务     │     索引服务     │     AI服务     │   文件服务    │
│                │                │                │             │
└───────┬────────┘──────────┬─────┘───────────┬────┘──────┬──────┘
        │                   │                 │           │
┌───────┴───────┐    ┌──────┴───────┐   ┌─────┴──────┐    │
│               │    │              │   │            │    │
│     Lucene    │    │   SQLite     │   │ Spring AI  │    │
│               │    │              │   │            │    │
└───────────────┘    └──────────────┘   └────────────┘    │
                                                          │
                       ┌────────────────────────────┐     │
                       │                            │     │
                       │         文件存储            ◄─────┘
                       │                            │
                       └────────────────────────────┘
```

## 🚀 快速开始

### 使用预构建镜像

```bash
# 创建网络
docker network create -d bridge xyz-bridge-net

# 拉取镜像
docker pull noogel/xyz-search:latest

# 运行容器
docker run -d --name xyzSearch --network xyz-bridge-net -p 8081:8081 \
-v /path/to/searchData:/usr/share/xyz-search/data \
-v /path/to/share:/data/share \
noogel/xyz-search:latest

```

> 默认账号:xyz
> 默认密码:search
> 访问Web界面:<http://localhost:8081>
> 账号暂不能修改，密码可以在配置页面修改。

## 🔧 配置说明

启动服务后，在页面修改配置：

```json5
{
  "indexDirectories": [
    {
      // 索引主目录，支持多个
      "directory": "/homes/xxx/XyzSearchTestData",
      "excludesDirectories": [
        // 支持排除特定目录，需要在主目录下
        "/homes/xxx/XyzSearchTestData/exclude"
      ],
      // 排除索引的文件类型
      "excludeFileProcessClass": []
    }
  ],
  // 自动收集文件
  "collectDirectories": [
    {
      "fromList": [
        // 源目录，支持多个
        "/Users/xyz/Downloads/collect"
      ],
      // 目标目录
      "to": "/homes/xxx/XyzSearchTestData/collect",
      // 源文件格式筛选正则
      "filterRegex": "\\.(pdf|PDF|epub|EPUB|docx|DOCX)",
      // 收集文件后是否删除源文件
      "autoDelete": true
    }
  ],
  // opds 根目录，建议设置为 calibre 主目录
  "opdsDirectory": "/homes/xxx/XyzSearchTestData",
  // 主动上传文件目录
  "uploadFileDirectory": "/homes/xxx/XyzSearchTestData/upload",
  // 标记删除文件暂存目录
  "markDeleteDirectory": "/homes/xxx/XyzSearchTestData/exclude/deleted",
  // 服务访问邮件通知配置
  "notifyEmail": {
    "senderEmail": null,
    "emailHost": null,
    "emailPort": null,
    "emailPass": null,
    "receivers": []
  },
  // 文件 OCR 服务配置
  "paddleOcr": {
    "url": null,
    "timeout": 10000
  },
  // 文件详情页外部搜索链接配置
  "linkItems": [
    {
      "desc": "豆瓣",
      "searchUrl": "https://m.douban.com/search/?query={query}"
    },
    {
      "desc": "京东",
      "searchUrl": "https://so.m.jd.com/ware/search.action?keyword={query}"
    },
    {
      "desc": "谷歌",
      "searchUrl": "https://www.google.com/search?q={query}"
    }
  ],
  // AI 对话搜索配置，支持对接 Ollama
  "chat": {
    "enable": false,
    "ollama": {
      "baseUrl": "http://192.168.124.101:11434",
      "chatModel": "deepseek-r1:1.5b",
      "chatOptionNumCtx": "4096",
      "chatOptionTemperature": "1.0",
      "chatOptionNumPredict": "10000",
      "embeddingAdditionalModels": [],
      "pullModelStrategy": "when_missing"
    }
  }
}
```

## 🐳 Docker部署

### 自行构建镜像

> 要求最低 JDK 17 环境

```bash
# 一键构建
mvn clean package && cd docker && docker build -t xyz-search:latest . && \
docker tag xyz-search:latest 用户名/xyz-search:latest && cd ../../

# 发布到镜像仓库
docker push 用户名/xyz-search:latest

# 创建网络
docker network create -d bridge xyz-bridge-net

# 一键部署
docker run -d --restart=always --name xyzSearch --network xyz-bridge-net -p 8081:8081 \
-v /实际路径/searchData:/usr/share/xyz-search/data \
-v /实际路径/share:/data/share \
用户名/xyz-search:latest

```

### Docker Compose部署

创建`docker-compose.yml`文件：

```yaml
version: '3'
services:
  xyz-search:
    image: noogel/xyz-search:latest
    container_name: xyzSearch
    restart: always
    ports:
      - "8081:8081"
    volumes:
      - ./searchData:/usr/share/xyz-search/data
      - ./share:/data/share
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

## 💬 常见问题

**Q: 如何修改默认端口?**
A: 在application.yml中修改server.port属性，或在启动命令中添加`--server.port=新端口`参数。

**Q: 支持哪些文件格式?**
A: 支持PDF、Word、Excel、PowerPoint、TXT、HTML、EPUB以及常见图片格式等。

## 📊 开发计划

* [ ] 改进Web界面，提供现代化搜索页面设计
* [ ] 优化RAG检索增强生成
* [ ] 添加多用户认证和权限管理
* [ ] 优化中文分词效果

## 👥 贡献指南

1. Fork 本仓库
2. 新建特性分支
3. 提交代码
4. 新建 Pull Request

我们欢迎各种形式的贡献，包括但不限于：

* 新功能开发
* Bug修复
* 文档改进
* 测试用例编写

## 📄 许可证

本项目采用 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html) 许可证
