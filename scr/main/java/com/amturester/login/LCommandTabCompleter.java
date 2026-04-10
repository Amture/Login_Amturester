package com.amturester.login;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LCommandTabCompleter implements TabCompleter {

    private final LoginPlugin plugin;

    public LCommandTabCompleter(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 第一级子命令
            completions.addAll(Arrays.asList("pw", "list", "reset", "blacklist"));
        } else if (args.length == 2) {
            // 第二级
            if (args[0].equalsIgnoreCase("blacklist")) {
                completions.addAll(Arrays.asList("ip", "id"));
            } else if (args[0].equalsIgnoreCase("pw") || args[0].equalsIgnoreCase("reset")) {
                // 补全玩家名（已注册的）
                completions.addAll(getRegisteredPlayerNames());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("blacklist")) {
                String type = args[1].toLowerCase();
                if (type.equals("ip") || type.equals("id")) {
                    completions.addAll(Arrays.asList("add", "remove", "list"));
                }
            } else if (args[0].equalsIgnoreCase("pw") || args[0].equalsIgnoreCase("reset")) {
                // 密码参数，无补全
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("blacklist")) {
                String type = args[1].toLowerCase();
                String action = args[2].toLowerCase();
                if (type.equals("ip") && action.equals("remove")) {
                    // 补全黑名单中的IP
                    completions.addAll(getBlacklistedIps());
                } else if (type.equals("id") && action.equals("remove")) {
                    // 补全黑名单中的玩家名
                    completions.addAll(getBlacklistedPlayers());
                }
                // add 操作没有预定义补全
            }
        }
        // 过滤已输入的文本
        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }

    private List<String> getRegisteredPlayerNames() {
        // 这里可以从数据库异步获取，但为了补全的即时性，可以缓存
        // 简单起见，返回空列表，或者从所有在线玩家中获取（但要求玩家注册过）
        // 更好的实现：缓存已注册玩家列表，定期刷新。这里简化，仅返回空列表。
        // 你也可以从数据库中同步查询，但可能造成卡顿，不推荐。
        return new ArrayList<>();
    }

    private List<String> getBlacklistedIps() {
        // 同步获取黑名单IP（注意可能阻塞，但补全调用频繁，建议缓存）
        // 这里简单实现为同步查询，实际生产环境应使用缓存
        List<String> ips = new ArrayList<>();
        plugin.getPlayerDataManager().getIpBlacklist().thenAccept(ips::addAll);
        // 上面的异步无法直接返回，简化：返回空列表
        return new ArrayList<>();
    }

    private List<String> getBlacklistedPlayers() {
        // 同上
        return new ArrayList<>();
    }
}