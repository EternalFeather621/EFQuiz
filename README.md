# EFQuiz

EFQuiz 是一个理论适用于全版本Minecraft bukkit及其分支的全服答题插件。

玩家可以在聊天框中直接回答系统随机广播的题目，答对后获得奖励，并记录答题次数进入全服排行榜。

## 功能介绍

- 支持自定义答题时间间隔
- 支持自定义题库
- 支持自定义答题奖励
- 支持全服答题排行榜
- 支持排行榜分页查看
- 支持点击聊天框中的 `[上一页]`、`[下一页]` 快速翻页
- 支持 `/efquiz help` 查看插件帮助
- 支持 `/efquiz reload` 重载配置文件

## 指令

| 指令 | 说明 | 权限 |
|---|---|---|
| `/efquiz` | 查看插件帮助 | 无 |
| `/efquiz help` | 查看插件帮助 | 无 |
| `/efquiz top` | 查看排行榜第 1 页 | 无 |
| `/efquiz top <页数>` | 查看指定页排行榜 | 无 |
| `/efquiz reload` | 重载配置文件 | `efquiz.admin` |

## 权限

| 权限 | 说明 | 默认 |
|---|---|---|
| `efquiz.admin` | 允许使用 `/efquiz reload` | OP |

## 配置文件示例

```yml
settings:
  prefix: "&6[EFQuiz]&r "
  interval-seconds: 300
  reward-money: 100
  reward-command: "eco give %player% %money%"
  case-sensitive: false

questions:
  - question: "Minecraft 中钻石矿石通常用什么镐挖掘？"
    answers:
      - "铁镐"
      - "钻石镐"
      - "下界合金镐"

  - question: "末影人最害怕什么？"
    answers:
      - "水"
