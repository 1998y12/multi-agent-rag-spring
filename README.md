
# Multi-Agent RAG Application

基于 Spring AI Alibaba 框架构建的多智能体检索增强生成（RAG）应用，集成了网络搜索和对话记忆功能。

blog：https://blog.csdn.net/qq_41508508/article/details/149117494

## 功能特性

- 🤖 **多智能体架构** - 支持多个 AI 智能体协同工作
- 🔍 **网络搜索工具** - 集成 Tavily API 实现实时网络信息检索
- 💬 **对话记忆** - 维护会话历史，支持上下文连续对话
- 📚 **RAG 检索增强** - 向量存储和文档检索功能
- 🧠 **OpenAI 集成** - 支持 GPT-4o-mini 模型和文本嵌入

## 技术栈

- **框架**: Spring AI Alibaba
- **语言**: Java
- **构建工具**: Maven
- **AI 模型**: OpenAI GPT-4o-mini
- **搜索 API**: Tavily Search API
- **嵌入模型**: text-embedding-3-small

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+

### 配置

1. 克隆项目
```bash
git clone <your-repository-url>
cd <project-directory>
```

2. 配置 API 密钥

编辑 `src/main/resources/application.yaml` 文件，替换以下配置：

```yaml
spring:
  ai:
    openai:
      api-key: your-openai-api-key
      base-url: https://api.openai.com

tavily:
  api-key: your-tavily-api-key
```

### 运行应用

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:6666` 启动。

## API 端点

| 端点            | 方法 | 描述 |
|---------------|------|------|
| `/graph/add`  | GET | 初始化向量存储并加载文档（FAQ 和概述文档）|
| `/graph/chat` | GET | 执行多智能体图扩展查询，支持 RAG 检索 |

### 详细说明

#### `/graph/add`
- **功能**: 将 Markdown 文档加载到向量存储中
- **文档**:
    - `faq.md` - Spring AI Alibaba 常见问题
    - `overview.md` - Spring AI Alibaba 概述
- **持久化**: 自动保存到 `vectorstore.json`

#### `/graph/chat`
- **功能**: 基于多智能体图进行查询和 RAG 检索
- **参数**:
    - `query` (可选) - 查询问题，默认值: "你好，我想知道一些关于大模型的知识"
- **返回**: JSON 格式的查询结果和扩展信息
- **线程 ID**: 固定为 "001"

## 配置说明

### OpenAI 配置
- **模型**: gpt-4o-mini
- **最大令牌**: 4096
- **嵌入模型**: text-embedding-3-small

### Tavily 搜索配置
- **基础 URL**: https://api.tavily.com
- **支持实时网络信息检索**

## 项目结构

```
multi-agent-rag-spring/
├── pom.xml                      # Maven 构建配置
├── READEME.md                   # 项目说明文档
├── HELP.md                      # 帮助文档
├── mvnw & mvnw.cmd             # Maven Wrapper
│
└── src/
├── main/
│   ├── java/com/ai/demo/
│   │   ├── SpringAiAlibabaDemoApplication.java  # 主启动类
│   │   │
│   │   ├── config/          # 配置类
│   │   │   ├── ChatClientConfig.java   # LLM配置
│   │   │   ├── GraphConfig.java    # 图配置
│   │   │   └── RagConfig.java  # RAG 组件配置
│   │   │
│   │   ├── controller/      # REST 控制器
│   │   │   └── GraphController.java
│   │   │
│   │   ├── edge/           # 图的边
│   │   │   ├── GradeGenerationEdge.java
│   │   │   └── RouteQuestionEdge.java
│   │   │
│   │   ├── entity/         # 实体类
│   │   │   ├── GradeScore.java
│   │   │   └── RouteQueryEntity.java
│   │   │
│   │   ├── node/           # 图节点实现
│   │   │   ├── GenerationNode.java
│   │   │   ├── RetrieveNode.java
│   │   │   ├── TransformQueryNode.java
│   │   │   └── WebSearchNode.java
│   │   │
│   │   └── tool/           # AI 工具类
│   │       └── WebSearchTool.java
│   │
│   └── resources/
│       ├── application.yaml           # 环境配置
│       ├── workflow.png              # 工作流程图
│       │
│       ├── documents/                # 知识库文档
│       │   ├── faq.md
│       │   └── overview.md
│       │
│       ├── evaluation/               # 效果截图
│       │   ├── RAG向量库.png
│       │   ├── RAG回答质量自评估.png
│       │   ├── 网络搜索1.png
│       │   ├── 网络搜索2.png
│       │   ├── 网络搜索3.png
│       │   ├── 网络搜索4.png
│       │   └── 记忆.png
│       │
│       ├── vectorstore/              # 向量存储
│       │   └── vectorstore.json
│       │
│       ├── static/                   # 静态资源
│       └── templates/                # 模板文件
│
└── test/
```

### 核心模块说明

- **config/**: Spring AI Alibaba 相关配置
- **controller/**: RESTful API 接口
- **edge/**: 多智能体图的边缘路由逻辑
- **entity/**: 数据实体和评分模型
- **node/**: 多智能体图的节点实现
- **tool/**: AI 工具集成（网络搜索等）
- **documents/**: RAG 知识库文档
- **evaluation/**: 功能演示和评估截图
- **vectorstore/**: 向量数据库持久化文件

