package com.amturester.login;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PermissionChecker {

    private final LoginPlugin plugin;
    private final List<String> adminGroups;   // 使用 final 也可以

    public PermissionChecker(LoginPlugin plugin) {
        this.plugin = plugin;
        // 从 config 读取管理员组列表
        List<String> groups = plugin.getConfig().getStringList("admin-groups");
        if (groups == null || groups.isEmpty()) {
            groups = List.of("admin", "owner", "administrator");
        }
        this.adminGroups = groups;
        plugin.getLogger().info("权限检查器已初始化，管理员组: " + adminGroups);
    }

    /**
     * 检查 CommandSender 是否有管理员权限（控制台默认为管理员）
     */
    public boolean isAdmin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return isAdmin((Player) sender);
    }

    /**
     * 检查玩家是否有管理员权限
     */
    public boolean isAdmin(Player player) {
        // 1. OP
        if (player.isOp()) return true;
        // 2. 拥有 login.admin 权限
        if (player.hasPermission("login.admin")) return true;
        // 3. 属于配置中的任意组（通过 group.<组名> 权限判断）
        for (String group : adminGroups) {
            if (player.hasPermission("group." + group)) {
                return true;
            }
        }
        return false;
    }
}