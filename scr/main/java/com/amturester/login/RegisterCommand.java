package com.amturester.login;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {

    private final LoginPlugin plugin;

    public RegisterCommand(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令。");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 2) {
            player.sendMessage("用法: /register <密码> <确认密码>");
            return false;
        }

        if (!args[0].equals(args[1])) {
            player.sendMessage("两次输入的密码不一致。");
            return true;
        }

        if (args[0].length() < 4) {
            player.sendMessage("密码长度至少4位。");
            return true;
        }

        // 异步检查是否已注册
        plugin.getPlayerDataManager().isRegistered(player).thenAccept(registered -> {
            if (registered) {
                player.sendMessage("你已经注册过了，请使用 /login 登录。");
            } else {
                // 注意：registerPlayer 需要 Player 对象和密码字符串
                plugin.getPlayerDataManager().registerPlayer(player, args[0]).thenAccept(success -> {
                    if (success) {
                        player.sendMessage("注册成功！请使用 /login <密码> 登录。");
                    } else {
                        player.sendMessage("注册失败，请稍后重试。");
                    }
                });
            }
        });

        return true;
    }
}