package com.amturester.login;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {

    private final LoginPlugin plugin;

    public LoginCommand(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令。");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("用法: /login <密码>");
            return false;
        }

        String password = args[0];

        // 先异步检查是否已注册
        plugin.getPlayerDataManager().isRegistered(player).thenAccept(registered -> {
            if (!registered) {
                player.sendMessage("你还没有注册，请使用 /register <密码> <确认密码> 注册。");
                return;
            }

            // 异步检查密码是否正确
            plugin.getPlayerDataManager().checkLogin(player, password).thenAccept(success -> {
                if (success) {
                    // 登录成功，标记为已登录
                    plugin.getPlayerListener().setLoggedIn(player, true);
                    player.sendMessage("登录成功！欢迎回来。");
                } else {
                    player.sendMessage("密码错误，请重试。");
                }
            });
        });

        return true;
    }
}