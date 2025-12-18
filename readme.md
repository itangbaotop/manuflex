# Manuflex - 企业级元数据驱动无代码平台

Manuflex 是一个基于 **Spring Cloud Alibaba** 构建的高性能、可配置化无代码平台底层引擎。其核心目标是抽象通用的业务逻辑，通过元数据配置（No-Code）而非硬编码，实现业务系统的快速构建与交付。

前端地址： https://github.com/itangbaotop/manuflex-react.git

## 🚀 核心架构理念

* **元数据驱动（Metadata-Driven）**: 所有的业务对象（Schema）和字段（Field）均通过元数据定义，系统动态生成数据库表结构及基础 CRUD 能力。
* **AI 智能代理（Agentic AI）**: 集成 MCP (Model Context Protocol)，通过 RAG（检索增强生成）技术，让 AI 能够理解平台 Schema 并辅助用户完成数据分析、流程发起及知识检索。
* **流程编排（Workflow Central）**: 深度集成流程引擎，支持 BPMN/DMN，将动态业务数据与自动化工作流无缝绑定。
* **极简定制化**: 针对特定行业（如 LIMS 实验室信息管理系统），只需配置相应的元数据与流程规则，即可完成从通用平台到垂直领域的切换。

## 🛠 技术栈

* **后端核心**: Java 17+, Spring Boot 3.x, Spring Cloud Alibaba
* **权限安全**: Spring Security, JWT, 分布式权限校验
* **持久层**: MyBatis Plus, MySQL 8.0
* **中间件**: Redis (缓存), RabbitMQ (消息驱动), MinIO (对象存储)
* **分布式治理**: Seata (分布式事务), Nacos (配置与发现), Sentinel (限流)
* **AI/向量化**: Qdrant (向量数据库), MCP 协议实现, LangChain4j 思路
* **监控**: Prometheus, Grafana, Loki (日志链路)

## 📦 核心模块说明

| 模块名称 | 职责描述 |
| :--- | :--- |
| `platform-api-gateway` | 统一流量入口，负责 JWT 鉴权、路由转发与动态限流。 |
| `platform-iam-service` | 身份识别与访问管理。支持多租户、RBAC 权限模型及组织架构管理。 |
| `platform-metadata-service` | 元数据引擎中心。定义业务模型（Schema）、字段属性、校验规则。 |
| `platform-data-service` | 动态数据路由。根据元数据定义，执行动态 SQL 操作，支持复杂的过滤与分页。 |
| `platform-workflow-service` | 工作流中心。管理 BPMN 部署、任务领取、表单绑定及 DMN 决策流。 |
| `platform-agent-service` | AI 助手服务。实现 MCP 协议，提供 Data/Workflow/Knowledge Assistant。 |
| `platform-file-service` | 文件与知识库管理。支持 MinIO 存储及 Qdrant 向量化索引。 |
| `platform-lims-service` | 行业适配示例。展示如何利用平台能力构建实验室采样与检测流程。 |


PS:
引入大数据可参考： https://github.com/itangbaotop/tdm.git
自定义业务可参考： https://github.com/itangbaotop/qms.git

## 🏗 快速开始

### 1. 环境准备
确保已安装 Docker 与 Docker-Compose。

### 2. 启动基础环境
```bash
docker-compose up -d
````

此脚本将启动 MySQL, Redis, MinIO, Nacos, Seata, Qdrant 以及监控组件。

### 3. 初始化数据库

执行 `docker/mysql/init.sql` 及各模块下 `db/migration` 脚本。

### 4. 编译并运行

```Bash
mvn clean install
```

# 依次启动各个模块的 Application 类
```

## 🧩 生产级代码规范

- **统一响应**: 所有接口通过 `platform-common` 中的 `Result<T>` 包装。
    
- **异常处理**: 全局 `GlobalExceptionHandler` 捕获业务异常，并转换为标准错误码。
    
- **日志链路**: 接入 OpenTelemetry 标准，通过 `LogAspect` 记录审计日志。

```
