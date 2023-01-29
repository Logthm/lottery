# lottery

> 便捷的qq群抽奖插件


[![Downloads](https://img.shields.io/github/downloads/Logthm/lottery/total)](https://github.com/Logthm/lottery/releases/latest)
[![Release](https://img.shields.io/github/v/release/Logthm/lottery?display_name=tag)](https://github.com/Logthm/lottery/releases)

## 功能

### 帮助指令

| 指令      | 说明         |
| --------- | ------------ |
| /lot help | 显示帮助信息 |

### 创建、删除抽奖

| 指令                   | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| /lot add               | 创建一个抽奖                                                 |
| /lot remove <抽奖编号> | 删除一个抽奖（需为**抽奖创建者**、**管理员**或 **admin**）   |
| /lot end <抽奖编号>    | 手动结束一个抽奖（需为**抽奖创建者**、**管理员**或 **admin**） |

### 查询抽奖

| 指令                   | 说明                   |
| ---------------------- | ---------------------- |
| /lot list              | 列出所有抽奖           |
| /lot detail <抽奖编号> | 查询指定抽奖的详情     |
| /lot member <抽奖编号> | 查询参与指定抽奖的群员 |

### 启用 / 禁用

| 指令         | 说明                                                |
| ------------ | --------------------------------------------------- |
| /lot enable  | 在群内启用抽奖功能（需为**群管理员**或 **admin** ） |
| /lot disable | 在群内禁用抽奖功能（需为**群管理员**或 **admin** ） |

## 配置

配置目录位于 `./config/com.logs.lottery/config.yml`

| 配置项            | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| adminQQ           | 填写 admin 的 qq 号                                          |
| botQQ             | 填写 bot 的 qq 号                                            |
| whiteGroupList    | 填写启用抽奖功能的群列表（无需主动填写，使用 启用 / 禁用 命令即可） |
| enable_for_others | 非 admin 是否可以创建抽奖                                    |
| remain_hour       | 抽奖结束后，保留的时间（小时）                               |
| remind_hour       | 在抽奖截止前多久进行提醒（小时）                             |

