# CHANGELOG

## v1.2.3

功能优化：

* 优化 elasticsearch 搜索准确性

## v1.2.2

新增功能：

* 支持 qdrant 向量数据库
* 支持基于本地知识库的增强检索
* 支持 docker-compose 完整版本

功能优化：

* 优化访问邮件通知
* 全文索引支持可选 elasticsearch

问题修复：

* 按修改时间搜索失效问题
* 修复大文本 highlight 处理失败问题
* 搜索页重复请求后端接口问题

## v1.2.1

新增功能：

* 全文搜索从 elasticsearch 切换为本地 lucene
* 新增 AI 对话，支持基于本地数据进行对话，支持 ollama 接口对接
* 增加 spring security 登陆登出页面

问题修复：

* 修复 opds 在 koReader 无法下载文件

依赖变化：

* 升级到 spring boot 3.4.2
* 升级到 twelvemonkeys 3.12.0
* 增加依赖 spring-ai 1.0.0-M6

## v1.1.1

* 增加图片 paddleocr 服务支持
* 支持文件类型排除
* 支持内容片段预览
* 优化前后端交互
* 支持图片预览
* 支持自定义索引

## v1.1.0

新增功能

* 后端组件新增 sqlite 数据库，记录文件信息。
* 支持文件上传能力，并自动加入到数据库记录。
* 支持排除索引目录。
* 新增文件标记清理。

## v1.0.6

实现基于 ES 后端的全文数据检索能力，主要有以下功能。

支持功能

* 数据资料的全文检索能力，支持的类型包括但不限于 PDF、EPUB、TXT、HTML。
* 支持 PDF、EPUB、MP4类型资料的在线预览。
* 支持按照资源目录、文件类型、文件大小、更新时间进行检索。
* 支持指定资源目录的定时自动收集。
* 支持 calibre 电子书库资源索引，并提供 OPDS 接口能力。
* 支持资源的随机获取。
* 支持访问时发送通知邮件。
