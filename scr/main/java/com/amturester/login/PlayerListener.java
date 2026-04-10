package com.amturester.login;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final LoginPlugin plugin;
    private final Set<UUID> loggedInPlayers = new HashSet<>();
    private final ConcurrentHashMap<UUID, Long> lastMoveWarning = new ConcurrentHashMap<>();
    private static final long MOVE_WARNING_COOLDOWN = 3000;

    public PlayerListener(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isLoggedIn(Player player) {
        return loggedInPlayers.contains(player.getUniqueId());
    }

    public void setLoggedIn(Player player, boolean loggedIn) {
        if (loggedIn) {
            loggedInPlayers.add(player.getUniqueId());
        } else {
            loggedInPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";
        String playerName = player.getName();

        // 先检查黑名单
        plugin.getPlayerDataManager().isIpBlacklisted(ip).thenAcceptAsync(ipBlacklisted -> {
            if (ipBlacklisted) {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.blacklist-kick", "&c你的IP已被列入黑名单。")));
                return;
            }
            plugin.getPlayerDataManager().isPlayerBlacklisted(playerName).thenAcceptAsync(nameBlacklisted -> {
                if (nameBlacklisted) {
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.blacklist-kick", "&c你的账号已被列入黑名单。")));
                    return;
                }
                // 黑名单通过，继续自动登录逻辑
                plugin.getPlayerDataManager().shouldAutoLogin(player).thenAccept(auto -> {
                    if (auto) {
                        setLoggedIn(player, true);
                        player.sendMessage("§a自动登录成功！欢迎回来。");
                        plugin.getPlayerDataManager().updateLoginInfo(player);
                    } else {
                        plugin.getPlayerDataManager().isRegistered(player).thenAccept(registered -> {
                            String messageKey = registered ? "welcome-registered" : "welcome-unregistered";
                            String rawMessage = plugin.getConfig().getString("messages." + messageKey,
                                    registered ? "§a请使用 /login <密码> 登录" : "§e欢迎！请使用 /register 注册");
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawMessage));
                        });
                    }
                });
            });
        });
    }

    // 以下原有事件方法保持不变（省略，避免重复）
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            Long last = lastMoveWarning.get(event.getPlayer().getUniqueId());
            if (last == null || now - last > MOVE_WARNING_COOLDOWN) {
                lastMoveWarning.put(event.getPlayer().getUniqueId(), now);
                sendNotLoggedMessage(event.getPlayer(), "not-logged-move");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
            sendNotLoggedMessage(event.getPlayer(), "not-logged-move");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
            sendNotLoggedMessage(event.getPlayer(), "not-logged-move");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
            sendNotLoggedMessage(event.getPlayer(), "not-logged-move");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
            sendNotLoggedMessage(event.getPlayer(), "not-logged-chat");
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        if (!isLoggedIn(event.getPlayer()) &&
                !(msg.startsWith("/login") || msg.startsWith("/l") || msg.startsWith("/register") || msg.startsWith("/reg"))) {
            event.setCancelled(true);
            sendNotLoggedMessage(event.getPlayer(), "not-logged-command");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (!isLoggedIn((Player) event.getEntity())) {
                event.setCancelled(true);
                sendNotLoggedMessage((Player) event.getEntity(), "not-logged-move");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        loggedInPlayers.remove(event.getPlayer().getUniqueId());
        lastMoveWarning.remove(event.getPlayer().getUniqueId());
    }

    private void sendNotLoggedMessage(Player player, String configKey) {
        String raw = plugin.getConfig().getString("messages." + configKey,
                "&c你尚未登录，请先登录！");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', raw));
    }
}