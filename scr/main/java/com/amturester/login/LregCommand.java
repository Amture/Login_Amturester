package com.amturester.login;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LregCommand implements CommandExecutor {

    private final LoginPlugin plugin;

    // 构造函数只接受一个参数
    public LregCommand(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 权限检查
        if (sender instanceof Player && !plugin.getPermissionChecker().isAdmin((Player) sender)) {
            sender.sendMessage("你没有权限使用此命令。");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("用法: /lreg <玩家名> <新密码>");
            return false;
        }

        String targetName = args[0];
        String newPassword = args[1];

        if (newPassword.length() < 4) {
            sender.sendMessage("密码长度至少4位。");
            return true;
        }

        plugin.getPlayerDataManager().setPassword(targetName, newPassword).thenAccept(success -> {
            if (success) {
                sender.sendMessage("已为玩家 " + targetName + " 设置新密码。");
            } else {
                sender.sendMessage("未找到玩家 " + targetName + " 或修改失败。");
            }
        });

        return true;
    }
}