package com.brugnevom.zesttapplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class ExecuteHit implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        if (arg0 instanceof Player) {
            Player player = (Player) arg0;
            if (arg1.getName().equalsIgnoreCase("reloadhit")) {
                ZestTapPlugin.read();
                player.sendMessage(ChatColor.GREEN + "Reloaded hit!");
                return true;
            }
            if (arg1.getName().equalsIgnoreCase("zesttapplugin") && arg3.length > 0 && arg3[0].equalsIgnoreCase("creator")) {
                player.sendMessage(ChatColor.GOLD + "Created by Muvixo!");
                return true;
            }
        }
        return false;
    }
}