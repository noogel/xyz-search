<div align="center">

<h1>xyz-search👋</h1>

[![License](https://img.shields.io/badge/license-GPL%203.0-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Docker Pulls](https://img.shields.io/docker/pulls/noogel/xyz-search.svg?style=flat-square)](https://hub.docker.com/r/noogel/xyz-search)
[![Version](https://img.shields.io/badge/version-1.2.2-blue.svg?style=flat-square)](https://github.com/noogel/xyz-search/releases)

**新一代智能文档检索系统 | 让知识管理更高效、更智能**
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

本系统专为个人知识管理和数字图书馆而设计，可以轻松处理从几千到数十万的文档集合。无论是管理个人电子书库，还是构建企业级文档检索平台，畅文全索都能满足您的需求。

## ✨ 主要特性

### 📄 多格式文档支持

* 全面支持办公文档：PDF、Word、Excel、PowerPoint
* 专业的电子书处理：支持 EPUB 格式的内容解析和索引
* 智能图像识别：基于 PaddleOCR 的图片文字提取
* 多媒体支持：视频元数据索引和预览功能
* 网页和文本：支持 HTML 和纯文本文件的解析

### 🔎 高性能搜索引擎

* 基于 Lucene 的分布式索引架构
* 专业的中文分词算法
* 实时索引更新机制
* 精确的文件属性过滤
* 智能化搜索结果高亮

### 🤖 AI 增强功能

* 集成 Spring AI 框架
* 基于大语言模型的内容理解
* 自然语言对话式检索
* 支持本地部署 Ollama 模型
* 智能文档摘要生成

### 📚 电子书管理

* 完整支持 OPDS 电子书协议
* 在线 EPUB 阅读器
* 全面兼容主流阅读设备
* 智能封面和目录索引
* 个性化阅读体验

### 💻 现代化界面设计

* 简洁优雅的 Web 交互界面
* 便捷的文档上传功能
* 智能文件自动采集
* 响应式移动端适配
* 丰富的文档预览功能

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
# 创建容器网络
docker network create -d bridge xyz-bridge-net

# 拉取官方镜像
docker pull noogel/xyz-search:latest

# 启动服务容器
docker run -d --name xyzSearch --network xyz-bridge-net -p 8081:8081 \
-v /path/to/searchData:/usr/share/xyz-search/data \
-v /path/to/share:/data/share \
noogel/xyz-search:latest
```

> 系统默认配置：
>
> * 管理员账号：xyz
> * 初始密码：search
> * 访问地址：<http://localhost:8081>
> * 注：密码可在系统配置页面修改，账号暂不支持修改。登录安全性有待验证，建议内网使用。

## 🔧 配置说明

系统启动后，可在 Web 界面配置以下参数：

```json5
{
  "indexDirectories": [
    {
      // 文档索引主目录，支持配置多个
      "directory": "/homes/xxx/XyzSearchTestData",
      "excludesDirectories": [
        // 需要排除的子目录，必须位于主目录下
        "/homes/xxx/XyzSearchTestData/exclude"
      ],
      // 排除特定类型文件的处理器
      "excludeFileProcessClass": []
    }
  ],
  // 文件自动采集配置
  "collectDirectories": [
    {
      "fromList": [
        // 监控的源目录，支持多个
        "/Users/xyz/Downloads/collect"
      ],
      // 文件存储目标目录
      "to": "/homes/xxx/XyzSearchTestData/collect",
      // 文件类型过滤正则表达式
      "filterRegex": "\\.(pdf|PDF|epub|EPUB|docx|DOCX)",
      // 采集后是否自动删除源文件
      "autoDelete": true
    }
  ],
  // OPDS 服务根目录，推荐使用 Calibre 库目录
  "opdsDirectory": "/homes/xxx/XyzSearchTestData",
  // 手动上传文件的存储目录
  "uploadFileDirectory": "/homes/xxx/XyzSearchTestData/upload",
  // 标记删除文件的临时存储目录
  "markDeleteDirectory": "/homes/xxx/XyzSearchTestData/exclude/deleted",
  // 系统访问通知邮件配置
  "notifyEmail": {
    "senderEmail": null,
    "emailHost": null,
    "emailPort": null,
    "emailPass": null,
    "receivers": []
  },
  // OCR 服务配置
  "paddleOcr": {
    "url": null,
    "timeout": 10000
  },
  // 文档详情页的外部搜索链接
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
  // AI 对话检索配置
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
    },
    // 可选的全文索引
    "elastic": {
      "enable": true,
      "host": "http://192.168.124.13:9200",
      "username": null,
      "password": null,
      "caPath": null,
      "connectionTimeout": 10000,
      "socketTimeout": 30000,
      "highlightMaxAnalyzedOffset": 10000001
    },
    // 向量数据库，可选，增强检索
    "qdrant": {
      "enable": false,
      "host": "192.168.124.13",
      "port": 6334,
      "apiKey": "xEYepb9JSXjSauW2"
    }
  }
}
```

## 🐳 Docker部署

### 构建本地镜像

> 环境要求：JDK 17 或更高版本

```bash
# 构建镜像
mvn clean package && cd docker && docker build -t xyz-search:latest . && \
docker tag xyz-search:latest 用户名/xyz-search:latest && cd ../../

# 推送到镜像仓库
docker push 用户名/xyz-search:latest

# 创建容器网络
docker network create -d bridge xyz-bridge-net

# 部署服务
docker run -d --restart=always --name xyzSearch --network xyz-bridge-net -p 8081:8081 \
-v /实际路径/searchData:/usr/share/xyz-search/data \
-v /实际路径/share:/data/share \
用户名/xyz-search:latest
```

### 使用 Docker Compose 部署

创建 `docker-compose.yml` 配置文件：

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

执行部署命令：

```bash
docker-compose up -d
```

### 完整配置

```yaml
services:
  search:
    container_name: search
    environment:
      - PUID=3000
      - PGID=3000
      - TZ=Asia/Shanghai
      - INIT_MODE=full
    image: noogel/xyz-search:latest
    depends_on:
      - elastic
      - qdrant
      - paddleocr
    ports:
      - '8081:8081'
    restart: unless-stopped
    volumes:
      - ./docker/search/data:/data/share
      - ./docker/search/config:/usr/share/xyz-search/data
  elastic:
    container_name: elastic
    environment:
      - PUID=3000
      - PGID=3000
      - TZ=Asia/Shanghai
      - xpack.security.enabled=false
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms1g -Xmx2g
    image: noogel/elasticsearch:8.17.4-alpha
    restart: unless-stopped
    volumes:
      - ./docker/search/es:/usr/share/elasticsearch/data
  qdrant:
    container_name: qdrant
    image: qdrant/qdrant:latest
    volumes:
      - ./docker/search/qdrant:/qdrant/storage
    environment:
      QDRANT__SERVICE__API_KEY: "3Yptaw9Z8ELMEsqp"
    restart: unless-stopped
  paddleocr:
    image: 'noogel/paddleocr:cpu-pp-ocrv4-server'
    restart: unless-stopped
    container_name: paddleocr
    deploy:
      resources:
        limits:
          memory: 6G
        reservations:
          memory: 500M
```

## 💬 常见问题

**Q: 如何修改系统默认端口？**
A: 可以通过以下两种方式修改：

1. 在 `application.yml` 中修改 `server.port` 配置项
2. 启动时添加参数：`--server.port=新端口号`

**Q: 支持哪些文档格式？**
A: 系统支持以下格式：

* 办公文档：PDF、Word (docx)、Excel (xlsx)
* 电子书：EPUB、MOBI
* 图片：PNG、JPG、JPEG (支持 OCR)
* 网页：HTML、HTM
* 文本：TXT、MD、JSON 等纯文本格式

**Q: 如何提升搜索性能？**
A: 可以通过以下方式优化：

1. 合理配置索引目录，避免索引无关文件
2. 使用 SSD 存储索引文件
3. 适当调整 JVM 内存参数
4. 启用文件类型过滤

## 📊 开发计划

* [ x ] 优化 RAG 检索增强生成模型
* [ x ] 改进中文分词准确率
* [ ] 全新的响应式搜索界面
* [ ] 实现多用户系统
* [ ] 支持更多文档格式

## 👥 贡献指南

我们欢迎各种形式的贡献，包括但不限于：

* 功能开发与改进
* 文档完善
* Bug 修复
* 测试用例编写
* 性能优化建议

参与贡献步骤：

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交改动：`git commit -m 'Add some AmazingFeature'`
4. 推送分支：`git push origin feature/AmazingFeature`
5. 提交 Pull Request

## 📄 许可证

本项目采用 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html) 许可证
