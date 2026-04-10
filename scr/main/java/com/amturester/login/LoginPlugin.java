package com.amturester.login;

import org.bukkit.plugin.java.JavaPlugin;

public class LoginPlugin extends JavaPlugin {

    private static LoginPlugin instance;
    private PlayerDataManager playerDataManager;
    private PermissionChecker permissionChecker;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        printAsciiArt();
        getLogger().info("Power by Amturester");

        instance = this;
        saveDefaultConfig();

        playerDataManager = new PlayerDataManager(this);
        playerDataManager.initDatabase();

        permissionChecker = new PermissionChecker(this);

        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);

        // 注册命令
        if (getCommand("register") != null) {
            getCommand("register").setExecutor(new RegisterCommand(this));
        } else {
            getLogger().warning("命令 /register 未在 plugin.yml 中定义");
        }
        if (getCommand("login") != null) {
            getCommand("login").setExecutor(new LoginCommand(this));
        } else {
            getLogger().warning("命令 /login 未在 plugin.yml 中定义");
        }
        if (getCommand("l") != null) {
            getCommand("l").setExecutor(new LCommand(this));
            // 注册 Tab Completer
            getCommand("l").setTabCompleter(new LCommandTabCompleter(this));
        } else {
            getLogger().warning("命令 /l 未在 plugin.yml 中定义");
        }

        getLogger().info("登录插件已启用");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.close();
        }
        getLogger().info("登录插件已禁用");
    }

    public static LoginPlugin getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PermissionChecker getPermissionChecker() {
        return permissionChecker;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    private void printAsciiArt() {
        String[] art = {
                "  █████╗ ███╗   ███╗████████╗██╗   ██╗██████╗ ███████╗",
                " ██╔══██╗████╗ ████║╚══██╔══╝██║   ██║██╔══██╗██╔════╝",
                " ███████║██╔████╔██║   ██║   ██║   ██║██████╔╝█████╗  ",
                " ██╔══██║██║╚██╔╝██║   ██║   ██║   ██║██╔══██╗██╔══╝  ",
                " ██║  ██║██║ ╚═╝ ██║   ██║   ╚██████╔╝██║  ██║███████╗",
                " ╚═╝  ╚═╝╚═╝     ╚═╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚══════╝"
        };
        for (String line : art) {
            getLogger().info(line);
        }
    }
}