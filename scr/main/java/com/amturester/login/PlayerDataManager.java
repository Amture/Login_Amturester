package com.amturester.login;

import com.amturester.login.PasswordUtil;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {

    private final LoginPlugin plugin;
    private Connection connection;

    public PlayerDataManager(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbUrl = "jdbc:sqlite:" + plugin.getConfig().getString("database.sqlite-file", "plugins/Login_Amturester/playerdata.db");
            connection = DriverManager.getConnection(dbUrl);
            createTable();
            addRegisterTimeColumnIfNotExists();
            addLastLoginColumnsIfNotExists();
            createBlacklistTable();
        } catch (Exception e) {
            plugin.getLogger().severe("数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "salt TEXT NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void addRegisterTimeColumnIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE players ADD COLUMN register_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            plugin.getLogger().info("已为数据库添加 register_time 列");
        } catch (SQLException ignored) { }
    }

    private void addLastLoginColumnsIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE players ADD COLUMN last_login_time TIMESTAMP");
        } catch (SQLException ignored) { }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE players ADD COLUMN last_login_ip VARCHAR(45)");
        } catch (SQLException ignored) { }
    }

    private void createBlacklistTable() {
        String sql = "CREATE TABLE IF NOT EXISTS blacklist (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type VARCHAR(10) NOT NULL," +   // 'ip' or 'player'
                "value VARCHAR(45) NOT NULL," +
                "UNIQUE(type, value)" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("创建黑名单表失败: " + e.getMessage());
        }
    }

    // ==================== 玩家数据操作 ====================

    public CompletableFuture<Boolean> registerPlayer(Player player, String password) {
        return CompletableFuture.supplyAsync(() -> {
            String uuid = player.getUniqueId().toString();
            String name = player.getName();
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(password, salt);
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";

            String sql = "INSERT INTO players (uuid, name, password_hash, salt, register_time, last_login_time, last_login_ip) " +
                    "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid);
                pstmt.setString(2, name);
                pstmt.setString(3, hash);
                pstmt.setString(4, salt);
                pstmt.setString(5, ip);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("UNIQUE constraint failed")) {
                    return false;
                }
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> isRegistered(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM players WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> isPlayerRegistered(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM players WHERE name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> checkLogin(Player player, String password) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT password_hash, salt FROM players WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    return PasswordUtil.verifyPassword(password, hash, salt);
                }
                return false;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> setPassword(String playerName, String newPassword) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE players SET password_hash = ?, salt = ? WHERE name = ?";
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(newPassword, salt);
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, hash);
                pstmt.setString(2, salt);
                pstmt.setString(3, playerName);
                int affected = pstmt.executeUpdate();
                return affected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<List<Map<String, Object>>> getAllPlayersInfo() {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT name, register_time FROM players ORDER BY register_time DESC";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", rs.getString("name"));
                    map.put("register_time", rs.getTimestamp("register_time"));
                    list.add(map);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("获取玩家列表失败: " + e.getMessage());
            }
            return list;
        });
    }

    public CompletableFuture<Boolean> checkPlayerPassword(String playerName, String password) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT password_hash, salt FROM players WHERE name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    return PasswordUtil.verifyPassword(password, hash, salt);
                }
                return false;
            } catch (SQLException e) {
                plugin.getLogger().severe("校验密码失败: " + e.getMessage());
                return false;
            }
        });
    }

    public void updateLoginInfo(Player player) {
        CompletableFuture.runAsync(() -> {
            String uuid = player.getUniqueId().toString();
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
            String sql = "UPDATE players SET last_login_time = CURRENT_TIMESTAMP, last_login_ip = ? WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ip);
                pstmt.setString(2, uuid);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("更新登录信息失败: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Boolean> shouldAutoLogin(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (!plugin.getConfig().getBoolean("auto-login.enabled", true)) {
                return false;
            }
            int timeoutHours = plugin.getConfig().getInt("auto-login.timeout-hours", 6);
            String uuid = player.getUniqueId().toString();
            String currentIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";
            String sql = "SELECT last_login_time, last_login_ip FROM players WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Timestamp lastTime = rs.getTimestamp("last_login_time");
                    String lastIp = rs.getString("last_login_ip");
                    if (lastTime == null || lastIp == null) return false;
                    if (!lastIp.equals(currentIp)) return false;
                    long diffMillis = System.currentTimeMillis() - lastTime.getTime();
                    long diffHours = diffMillis / (1000 * 60 * 60);
                    return diffHours <= timeoutHours;
                }
                return false;
            } catch (SQLException e) {
                plugin.getLogger().severe("检查自动登录失败: " + e.getMessage());
                return false;
            }
        });
    }

    // ==================== 黑名单操作 ====================

    public CompletableFuture<Boolean> addIpToBlacklist(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT OR IGNORE INTO blacklist (type, value) VALUES ('ip', ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ip);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("添加IP黑名单失败: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> removeIpFromBlacklist(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM blacklist WHERE type = 'ip' AND value = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ip);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("移除IP黑名单失败: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<List<String>> getIpBlacklist() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> ips = new ArrayList<>();
            String sql = "SELECT value FROM blacklist WHERE type = 'ip'";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    ips.add(rs.getString("value"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("获取IP黑名单失败: " + e.getMessage());
            }
            return ips;
        });
    }

    public CompletableFuture<Boolean> addPlayerToBlacklist(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT OR IGNORE INTO blacklist (type, value) VALUES ('player', ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("添加玩家黑名单失败: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> removePlayerFromBlacklist(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM blacklist WHERE type = 'player' AND value = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("移除玩家黑名单失败: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<List<String>> getPlayerBlacklist() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> players = new ArrayList<>();
            String sql = "SELECT value FROM blacklist WHERE type = 'player'";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    players.add(rs.getString("value"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("获取玩家黑名单失败: " + e.getMessage());
            }
            return players;
        });
    }

    public CompletableFuture<Boolean> isIpBlacklisted(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM blacklist WHERE type = 'ip' AND value = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ip);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                plugin.getLogger().severe("检查IP黑名单失败: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> isPlayerBlacklisted(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM blacklist WHERE type = 'player' AND value = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                plugin.getLogger().severe("检查玩家黑名单失败: " + e.getMessage());
                return false;
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}