package com.example.iplogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class AuthCommand implements CommandExecutor {
    
    private final IPLogin plugin;
    
    public AuthCommand(IPLogin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        if (plugin.isLoggedIn(player.getUniqueId())) {
            player.sendMessage("§aYou are already logged in!");
            return true;
        }
        
        String cmd = label.toLowerCase();
        
        if (cmd.equals("register")) {
            if (plugin.hasPassword(player.getUniqueId())) {
                player.sendMessage("§cAccount already registered! Use /login <password>");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§cUsage: /register <password>");
                return true;
            }
            plugin.setPassword(player.getUniqueId(), args[0]);
            plugin.setPlayerIP(player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
            plugin.setLoggedIn(player.getUniqueId(), true);
            player.setInvulnerable(false);
            player.sendMessage("§aAccount registered and IP locked successfully!");
            return true;
        }
        
        if (cmd.equals("login")) {
            if (!plugin.hasPassword(player.getUniqueId())) {
                player.sendMessage("§cNo account found! Use /register <password>");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§cUsage: /login <password>");
                return true;
            }
            if (plugin.checkPassword(player.getUniqueId(), args[0])) {
                plugin.setLoggedIn(player.getUniqueId(), true);
                player.setInvulnerable(false);
                player.sendMessage("§aSuccessfully logged in!");
            } else {
                player.sendMessage("§cInvalid password!");
            }
            return true;
        }
        
        if (cmd.equals("changepass")) {
            if (args.length != 2) {
                player.sendMessage("§cUsage: /changepass <oldpass> <newpass>");
                return true;
            }
            if (!plugin.checkPassword(player.getUniqueId(), args[0])) {
                player.sendMessage("§cInvalid old password!");
                return true;
            }
            plugin.setPassword(player.getUniqueId(), args[1]);
            player.sendMessage("§aPassword changed successfully!");
            return true;
        }
        
        if (cmd.equals("ipdel")) {
            if (!sender.hasPermission("iplogin.admin")) {
                sender.sendMessage("§cNo permission!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage("§cUsage: /ipdel <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                plugin.removeIP(target.getUniqueId());
                sender.sendMessage("§aIP binding removed for " + target.getName());
                target.sendMessage("§eYour IP binding was removed by an admin.");
            } else {
                sender.sendMessage("§cPlayer not found!");
            }
            return true;
        }
        
        return false;
    }
}
