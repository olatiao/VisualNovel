# Minecraft 视觉小说引擎 (Visual Novel Engine)

一个 Minecraft 的视觉小说引擎模组，允许玩家与实体互动并进行分支对话。

## 功能特点

1. **JSON 配置对话**：所有对话通过 JSON 文件配置，存放在`.minecraft/config/visualnovel/`目录下
2. **右键交互**：玩家可以通过右键实体进行对话，且支持连续对话
3. **音频播放**：可以在对话的同时播放指定路径的音频文件
4. **变量支持**：可以在对话文本中插入变量，如玩家名称、手持物品等
5. **分支对话**：支持基于条件的分支对话，可以根据玩家之前的选择或者物品等条件改变对话流程

## 如何使用

### 安装

1. 安装 Fabric 加载器
2. 下载此模组和 Fabric API，放入 mods 文件夹
3. 启动游戏，模组会自动创建示例配置文件

### 配置对话

对话配置文件是 JSON 格式，保存在`.minecraft/config/visualnovel/`目录下。每个 JSON 文件代表一个对话脚本。

示例对话配置：

```json
{
  "id": "example",
  "name": "示例对话",
  "nodes": {
    "start": {
      "id": "start",
      "text": "你好，{player_name}！欢迎使用视觉小说引擎。\n你手中拿着的是 {held_item}。",
      "sound": "visualnovel:dialogue/greeting",
      "nextNodeId": "choice"
    },
    "choice": {
      "id": "choice",
      "text": "你想了解什么？",
      "sound": "visualnovel:dialogue/question",
      "choices": [
        {
          "text": "如何配置对话",
          "nextNodeId": "config_info"
        },
        {
          "text": "结束对话",
          "nextNodeId": "end"
        }
      ]
    },
    "config_info": {
      "id": "config_info",
      "text": "你可以在配置文件夹中创建JSON文件来定义对话。\n每个对话包含多个节点和选择分支。",
      "sound": "visualnovel:dialogue/explanation",
      "nextNodeId": "end"
    },
    "end": {
      "id": "end",
      "text": "感谢使用！再见。",
      "sound": "visualnovel:dialogue/goodbye"
    },
    "conditional": {
      "id": "conditional",
      "text": "这是一个条件分支的示例",
      "condition": "has_item:minecraft:diamond",
      "nextNodeId": "has_diamond",
      "fallbackNodeId": "no_diamond"
    },
    "has_diamond": {
      "id": "has_diamond",
      "text": "你有钻石！真棒！"
    },
    "no_diamond": {
      "id": "no_diamond",
      "text": "你没有钻石。去挖矿吧！"
    }
  }
}
```

### 条件类型

模组支持以下条件类型：

1. `dialogue_completed:对话ID` - 检查玩家是否已完成指定 ID 的对话
2. `has_item:物品ID` - 检查玩家是否拥有特定物品

### 变量

对话文本中支持以下变量：

1. `{player_name}` - 玩家名称
2. `{held_item}` - 玩家手持物品

## 键位绑定

- 空格键：继续对话（可在键位设置中更改）

## 开发者

如果你想为此模组贡献代码或自定义功能，请查看源代码并参考以下目录结构：

- `com.visualnovel.config` - 配置和对话状态管理
- `com.visualnovel.dialogue` - 对话模型类
- `com.visualnovel.event` - 交互事件处理
- `com.visualnovel.network` - 网络处理和通信
- `com.visualnovel.ui` - 客户端用户界面

## 许可证

本模组基于 MIT 许可证开源。
