package com.amturester.login;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class LCommand implements CommandExecutor {

    private final LoginPlugin plugin;

    public LCommand(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // 不再接受单参数作为登录密码

        // 检查管理员权限
        if (!plugin.getPermissionChecker().isAdmin(sender)) {
            sender.sendMessage("§c你没有权限使用此命令。");
            return true;
        }

        String subCmd = args[0].toLowerCase();
        switch (subCmd) {
            case "pw":
                if (args.length != 3) {
                    sender.sendMessage("§c用法: /l pw <玩家名> <密码>");
                    return false;
                }
                handlePw(sender, args[1], args[2]);
                break;
            case "list":
                handleList(sender);
                break;
            case "reset":
                if (args.length != 3) {
                    sender.sendMessage("§c用法: /l reset <玩家名> <新密码>");
                    return false;
                }
                handleReset(sender, args[1], args[2]);
                break;
            case "blacklist":
                handleBlacklist(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== 登录插件帮助 ===");
        sender.sendMessage("§a/l pw <玩家名> <密码> §7- 校验玩家密码（管理员）");
        sender.sendMessage("§a/l list §7- 查看所有已注册玩家（管理员）");
        sender.sendMessage("§a/l reset <玩家名> <新密码> §7- 重置玩家密码（管理员）");
        sender.sendMessage("§a/l blacklist ip add <IP> §7- 添加IP黑名单");
        sender.sendMessage("§a/l blacklist ip remove <IP> §7- 移除IP黑名单");
        sender.sendMessage("§a/l blacklist ip list §7- 查看IP黑名单");
        sender.sendMessage("§a/l blacklist id add <玩家名> §7- 添加玩家黑名单（需已注册）");
        sender.sendMessage("§a/l blacklist id remove <玩家名> §7- 移除玩家黑名单");
        sender.sendMessage("§a/l blacklist id list §7- 查看玩家黑名单");
        sender.sendMessage("§a/register <密码> <确认密码> §7- 注册账号");
        sender.sendMessage("§a/login <密码> §7- 登录账号");
    }

    private void handlePw(CommandSender sender, String playerName, String password) {
        plugin.getPlayerDataManager().checkPlayerPassword(playerName, password).thenAccept(matches -> {
            if (matches) {
                sender.sendMessage("§a✓ 玩家 " + playerName + " 的密码与输入匹配。");
            } else {
                sender.sendMessage("§c✗ 玩家 " + playerName + " 的密码不匹配，或者玩家不存在。");
            }
        });
    }

    private void handleList(CommandSender sender) {
        plugin.getPlayerDataManager().getAllPlayersInfo().thenAccept(list -> {
            if (list.isEmpty()) {
                sender.sendMessage("§e暂无任何玩家注册。");
                return;
            }
            sender.sendMessage("§6=== 已注册玩家列表 ===");
            for (Map<String, Object> info : list) {
                String name = (String) info.get("name");
                Timestamp time = (Timestamp) info.get("register_time");
                sender.sendMessage("§a" + name + " §7- 注册于 " + time);
            }
        });
    }

    private void handleReset(CommandSender sender, String playerName, String newPassword) {
        if (newPassword.length() < 4) {
            sender.sendMessage("§c密码长度至少4位。");
            return;
        }
        plugin.getPlayerDataManager().setPassword(playerName, newPassword).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a已重置玩家 " + playerName + " 的密码。");
            } else {
                sender.sendMessage("§c未找到玩家 " + playerName + "，重置失败。");
            }
        });
    }

    private void handleBlacklist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /l blacklist <ip|id> <add|remove|list> [目标]");
            return;
        }
        String type = args[1].toLowerCase(); // ip or id
        if (args.length < 3) {
            sender.sendMessage("§c用法: /l blacklist " + type + " <add|remove|list> [目标]");
            return;
        }
        String action = args[2].toLowerCase(); // add, remove, list

        if (action.equals("list")) {
            if (type.equals("ip")) {
                plugin.getPlayerDataManager().getIpBlacklist().thenAccept(list -> {
                    if (list.isEmpty()) {
                        sender.sendMessage("§eIP黑名单为空。");
                    } else {
                        sender.sendMessage("§6=== IP黑名单 ===");
                        for (String ip : list) {
                            sender.sendMessage("§c" + ip);
                        }
                    }
                });
            } else if (type.equals("id")) {
                plugin.getPlayerDataManager().getPlayerBlacklist().thenAccept(list -> {
                    if (list.isEmpty()) {
                        sender.sendMessage("§e玩家黑名单为空。");
                    } else {
                        sender.sendMessage("§6=== 玩家黑名单 ===");
                        for (String name : list) {
                            sender.sendMessage("§c" + name);
                        }
                    }
                });
            } else {
                sender.sendMessage("§c无效的类型，请使用 ip 或 id。");
            }
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§c请提供目标值。");
            return;
        }
        String target = args[3];

        if (type.equals("ip")) {
            if (action.equals("add")) {
                plugin.getPlayerDataManager().addIpToBlacklist(target).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage("§a已添加IP " + target + " 到黑名单。");
                    } else {
                        sender.sendMessage("§cIP " + target + " 已在黑名单中或添加失败。");
                    }
                });
            } else if (action.equals("remove")) {
                plugin.getPlayerDataManager().removeIpFromBlacklist(target).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage("§a已从黑名单移除IP " + target);
                    } else {
                        sender.sendMessage("§c未找到IP " + target + " 在黑名单中。");
                    }
                });
            } else {
                sender.sendMessage("§c无效的操作，请使用 add/remove/list。");
            }
        } else if (type.equals("id")) {
            // 对于添加操作，需要检查玩家是否已注册
            if (action.equals("add")) {
                plugin.getPlayerDataManager().isPlayerRegistered(target).thenAccept(registered -> {
                    if (!registered) {
                        sender.sendMessage("§c玩家 " + target + " 尚未注册，无法加入黑名单。");
                        return;
                    }
                    plugin.getPlayerDataManager().addPlayerToBlacklist(target).thenAccept(success -> {
                        if (success) {
                            sender.sendMessage("§a已添加玩家 " + target + " 到黑名单。");
                        } else {
                            sender.sendMessage("§c玩家 " + target + " 已在黑名单中或添加失败。");
                        }
                    });
                });
            } else if (action.equals("remove")) {
                plugin.getPlayerDataManager().removePlayerFromBlacklist(target).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage("§a已从黑名单移除玩家 " + target);
                    } else {
                        sender.sendMessage("§c未找到玩家 " + target + " 在黑名单中。");
                    }
                });
            } else {
                sender.sendMessage("§c无效的操作，请使用 add/remove/list。");
            }
        } else {
            sender.sendMessage("§c无效的类型，请使用 ip 或 id。");
        }
    }
}
