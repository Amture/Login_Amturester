# Login_Amturester 插件介绍

## 📖 概述
Login_Amturester 是一款为 **Paper 1.21.11** 服务器设计的轻量级、功能全面的登录保护插件。它提供了安全的密码加密存储、未登录玩家行为限制、管理员命令组、自动登录、黑名单系统以及 LuckPerms 权限组对接等特性，适合各类服务器使用。

## ✨ 核心功能

### 🔐 注册与登录
- 玩家通过 `/register <密码> <确认密码>` 注册账号。
- 已注册玩家使用 `/login <密码>` 登录。
- 密码采用 **SHA-256 + 随机盐** 加密存储，绝不存储明文。
- 未登录玩家无法移动、破坏方块、聊天、执行大部分命令（除 `/login` 和 `/register` 外）。

### 👑 管理员命令组 `/l`
管理员（OP 或拥有 `login.admin` 权限的用户）可使用以下命令：

- `/l pw <玩家名> <密码>` – 校验指定玩家的密码是否正确（不显示明文）。
- `/l list` – 列出所有已注册玩家的名称及注册时间。
- `/l reset <玩家名> <新密码>` – 强制重置指定玩家的密码。
- `/l blacklist ip add|remove|list <IP>` – 管理 IP 黑名单。
- `/l blacklist id add|remove|list <玩家名>` – 管理玩家黑名单（添加时要求目标玩家已注册）。

### 🔄 自动登录
- 同一 IP 在**可配置时长内（默认 6 小时）** 再次进入服务器，自动登录，无需输入密码。
- 可在 `config.yml` 中开关或调整有效时长。

### 🚫 黑名单系统
- 支持封禁 IP 或玩家 ID。
- 被封禁的玩家无法进入服务器，踢出消息可自定义。
- 玩家黑名单要求目标玩家必须已在服务器注册过，避免无效操作。

### 🎨 高度自定义
- 所有提示消息均可在 `config.yml` 中修改，支持 Minecraft 颜色代码（`&` 符号）。
- 支持自定义管理员权限组名称（如 `admin`、`owner`、`mygroup`），通过 `admin-groups` 配置项指定，与 LuckPerms 权限组无缝对接。
- 支持 SQLite（默认）或 MySQL 数据库。

### 🔧 命令补全（Tab 补全）
- 输入 `/l` 及子命令时，按 `Tab` 键自动提示可用参数，提升管理效率。

## 📦 命令总览

| 命令 | 说明 | 权限要求 |
|------|------|----------|
| `/register <密码> <确认密码>` | 注册账号 | 无（未注册玩家） |
| `/login <密码>` | 登录游戏 | 无（已注册玩家） |
| `/l pw <玩家名> <密码>` | 校验玩家密码 | `login.admin` 或 OP 或配置组 |
| `/l list` | 查看所有已注册玩家 | 同上 |
| `/l reset <玩家名> <新密码>` | 重置玩家密码 | 同上 |
| `/l blacklist ip add <IP>` | 添加 IP 黑名单 | 同上 |
| `/l blacklist ip remove <IP>` | 移除 IP 黑名单 | 同上 |
| `/l blacklist ip list` | 查看 IP 黑名单 | 同上 |
| `/l blacklist id add <玩家名>` | 添加玩家黑名单（需已注册） | 同上 |
| `/l blacklist id remove <玩家名>` | 移除玩家黑名单 | 同上 |
| `/l blacklist id list` | 查看玩家黑名单 | 同上 |

## 🔐 权限节点

| 权限节点 | 说明 | 默认值 |
| --- | --- | --- |
| `login.admin` | 允许使用所有 `/l` 子命令（管理功能） | OP |
| `login.use` | 允许使用 `/l` 命令（但管理子命令仍需 `login.admin`） | true |

## 🛠️ 安装与使用

1. 下载 `Login_Amturester-1.4.jar` 并放入服务器的 `plugins/` 目录。
2. 重启服务器（或使用 plugman 加载）。
3. 编辑 `config.yml` 按需修改配置。
4. 给予管理员玩家 `login.admin` 权限（或将其加入 `admin-groups` 指定的组中）。
5. 玩家即可通过 `/register` 和 `/login` 正常使用。

## 📄 版本历史

- **v1.0**：基础注册/登录、加密存储、未登录限制。
- **v1.1**：ASCII 艺术字、自定义欢迎消息、移动提示。
- **v1.2**：管理员命令组 `/l`、注册时间记录、自定义权限组。
- **v1.3**：自动登录（同 IP 6 小时内免登录）。
- **v1.4**：黑名单系统（IP/ID）、Tab 补全、移除 `/l` 快捷登录。

## 🧾 版权与许可

本插件由 **Amturester** 开发并维护，遵循 **MIT 开源协议**。  
欢迎使用、修改和分发，但请保留原始版权声明，禁止倒卖或宣称原创。

## ⚙️ 配置文件 config.yml 示例

```yaml
# 登录超时（秒）
login-timeout: 30

# 数据库
database:
  type: sqlite
  sqlite-file: plugins/Login_Amturester/playerdata.db
  # mysql:
  #   host: localhost
  #   port: 3306
  #   database: minecraft
  #   username: root
  #   password: ''

# 自定义消息（支持 & 颜色代码）
messages:
  welcome-unregistered: "&e欢迎！&c你还没有注册\n&a请使用 /register <密码> <确认密码> 注册"
  welcome-registered: "&e欢迎回来！&a请使用 /login <密码> 登录"
  not-logged-move: "&c请先登录！使用 /login <密码>"
  not-logged-chat: "&c请先登录后再发言"
  not-logged-command: "&c请先登录后再执行命令"
  blacklist-kick: "&c你的IP或账号已被列入黑名单，请联系管理员"

# 管理员权限组（与 LuckPerms 组名匹配）
admin-groups:
  - admin
  - owner
  - administrator

# 自动登录设置
auto-login:
  enabled: true
  timeout-hours: 6
