package com.example.iplogin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class IPLogin extends JavaPlugin implements Listener {
    
    private Map<UUID, String> loggedInPlayers = new HashMap<>();
    private Map<UUID, String> playerPasswords = new HashMap<>();
    private Map<UUID, String> playerIPs = new HashMap<>();
    private File dataFolder;
    
    @Override
    public void onEnable() {
        dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        loadData();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("register").setExecutor(new AuthCommand(this));
        getCommand("login").setExecutor(new AuthCommand(this));
        getCommand("changepass").setExecutor(new AuthCommand(this));
        getCommand("ipdel").setExecutor(new AuthCommand(this));
        startLoginTimeout();
    }
    
    @Override
    public void onDisable() {
        saveData();
        loggedInPlayers.clear();
    }
    
    private void loadData() {
        try {
            File passFile = new File(dataFolder, "passwords.yml");
            File ipFile = new File(dataFolder, "ips.yml");
            
            if (passFile.exists()) {
                String content = Files.readString(passFile.toPath());
                for (String line : content.split("\n")) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        playerPasswords.put(UUID.fromString(parts[0]), parts[1]);
                    }
                }
            }
            
            if (ipFile.exists()) {
                String content = Files.readString(ipFile.toPath());
                for (String line : content.split("\n")) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        playerIPs.put(UUID.fromString(parts[0]), parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            getLogger().warning("Failed to load data: " + e.getMessage());
        }
    }
    
    private void saveData() {
        try {
            Files.writeString(Paths.get(dataFolder.toString(), "passwords.yml"), 
                playerPasswords.entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .reduce((a, b) -> a + "\n" + b).orElse(""));
            
            Files.writeString(Paths.get(dataFolder.toString(), "ips.yml"), 
                playerIPs.entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .reduce((a, b) -> a + "\n" + b).orElse(""));
        } catch (IOException e) {
            getLogger().warning("Failed to save data: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String ip = event.getRealAddress().getHostAddress();
        
        if (playerIPs.containsKey(uuid) && !playerIPs.get(uuid).equals(ip)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, 
                "§cYour IP does not match the registered IP for this account!");
            return;
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!playerPasswords.containsKey(uuid)) {
            player.sendMessage("§eWelcome! Use §6/register <password> §eto register.");
            player.setInvulnerable(true);
            return;
        }
        
        if (!loggedInPlayers.containsKey(uuid)) {
            player.sendMessage("§cPlease login with §6/login <password>");
            player.setInvulnerable(true);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!loggedInPlayers.containsKey(uuid)) {
                    player.kickPlayer("§cLogin timeout. Please rejoin and use /login <password>");
                }
            }, 600L);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        loggedInPlayers.remove(uuid);
    }
    
    public boolean isLoggedIn(UUID uuid) {
        return loggedInPlayers.containsKey(uuid);
    }
    
    public void setLoggedIn(UUID uuid, boolean loggedIn) {
        if (loggedIn) {
            loggedInPlayers.put(uuid, "logged");
        } else {
            loggedInPlayers.remove(uuid);
        }
    }
    
    public boolean hasPassword(UUID uuid) {
        return playerPasswords.containsKey(uuid);
    }
    
    public boolean checkPassword(UUID uuid, String password) {
        return playerPasswords.getOrDefault(uuid, "").equals(password);
    }
    
    public void setPassword(UUID uuid, String password) {
        playerPasswords.put(uuid, password);
        saveData();
    }
    
    public String getPlayerIP(UUID uuid) {
        return playerIPs.get(uuid);
    }
    
    public void setPlayerIP(UUID uuid, String ip) {
        playerIPs.put(uuid, ip);
        saveData();
    }
    
    public void removeIP(UUID uuid) {
        playerIPs.remove(uuid);
        saveData();
    }
    
    private void startLoginTimeout() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (!loggedInPlayers.containsKey(uuid) && player.isInvulnerable()) {
                        player.kickPlayer("§cLogin timeout!");
                    }
                }
            }
        }.runTaskTimer(this, 600L, 600L);
    }
}

