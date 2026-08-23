package com.brugnevom.zesttapplugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ZestTapPlugin extends JavaPlugin {
    public static Plugin thisplugin;
    
    private final Map<UUID, LinkedList<Location>> historyMap = new HashMap<>();
    public static int DELAY;
    
    public static boolean shouldCheckCPS = true, shouldThirdSprintHit;
    
    public static String hitdelaydesc = "hit delay: ";
    public static String damagedesc = "damage multiplier: ";
    public static String cpslimitingdesc = "CPS limiting: ";
    public static String cpslimitdesc = "CPS limit: ";
    public static String thirdsprinthitdesc = "Third Sprint Hit: ";
    public static String delaymovedesc = "Movement Tick Delay: ";
    public static String consistantkbdesc = "Consistant KB: ";
    
    public static String folderPath = Paths.get("").toAbsolutePath().toString() + File.separator + "plugins" + File.separator + "ZestTapPlugin" + File.separator;
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new RunTick(this), this);
        getCommand("reloadhit").setExecutor(new ExecuteHit());
        getCommand("zesttapplugin").setExecutor(new ExecuteHit());
        read();
        thisplugin = this;
        
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (RunTick.damager != null && RunTick.victim != null) {
                if (RunTick.victim.isOnGround()) {
                    RunTick.groundy = RunTick.victim.getLocation().getY();
                    RunTick.hitcount = 0;
                } 
                if (!shouldThirdSprintHit && RunTick.victim != null && RunTick.damager != null) {
                    if (RunTick.victim.getLocation().getY() > RunTick.groundy + 0.4D) {
                        RunTick.damager.setSprinting(false);
                    } else {
                        RunTick.damager.setSprinting(true);
                    }  
                }
            } 
        }, 0L, 0L);
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (DELAY > 0) {
                for (Player subject : Bukkit.getOnlinePlayers()) {
                    UUID uuid = subject.getUniqueId();
                    historyMap.putIfAbsent(uuid, new LinkedList<>());
                    LinkedList<Location> history = historyMap.get(uuid);
                    history.addLast(subject.getLocation().clone());
                    if (!history.isEmpty()) {
                        Location delayedLoc = (history.size() > DELAY) ? history.removeFirst() : history.getFirst();
                        broadcastDelayedPosition(subject, delayedLoc);
                    } 
                }  
            }
        }, 0L, 1L);

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, 
                    ListenerPriority.HIGHEST, 
                    PacketType.Play.Server.ENTITY_TELEPORT, 
                    PacketType.Play.Server.REL_ENTITY_MOVE, 
                    PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, 
                    PacketType.Play.Server.ENTITY_LOOK, 
                    PacketType.Play.Server.ENTITY_HEAD_ROTATION) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    if (DELAY > 0) {
                        PacketContainer packet = event.getPacket();
                        int entityId = packet.getIntegers().read(0);
                        Player subject = null;
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getEntityId() == entityId) {
                                subject = p;
                                break;
                            } 
                        } 
                        if (subject != null) {
                            if (event.getPlayer().getUniqueId().equals(subject.getUniqueId())) return; 
                            event.setCancelled(true);
                        } 
                    } 
                }
            });
        }
    }
    
    private void broadcastDelayedPosition(Player subject, Location loc) {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) return;
        PacketContainer teleport = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        teleport.getIntegers().write(0, subject.getEntityId());
        teleport.getIntegers().write(1, (int) Math.floor(loc.getX() * 32.0D));
        teleport.getIntegers().write(2, (int) Math.floor(loc.getY() * 32.0D));
        teleport.getIntegers().write(3, (int) Math.floor(loc.getZ() * 32.0D));
        teleport.getBytes().write(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
        teleport.getBytes().write(1, (byte) (loc.getPitch() * 256.0F / 360.0F));
        teleport.getBooleans().write(0, true);
        
        PacketContainer headLook = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        headLook.getIntegers().write(0, subject.getEntityId());
        headLook.getBytes().write(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
        
        for (Player observer : Bukkit.getOnlinePlayers()) {
            if (observer.getUniqueId().equals(subject.getUniqueId())) continue; 
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(observer, teleport, false);
                ProtocolLibrary.getProtocolManager().sendServerPacket(observer, headLook, false);
            } catch (Exception ignored) {}
        } 
    }
    
    public static void read() {
        File file = new File(folderPath + "config.txt");
        if (!file.exists()) {
            try {
                Files.createDirectories(Paths.get(folderPath));
                BufferedWriter bf = new BufferedWriter(new FileWriter(file));
                bf.write("enabled: true\n");
                bf.write(hitdelaydesc + "17\n");
                bf.write(damagedesc + "0.7\n");
                bf.write(cpslimitingdesc + "true\n");
                bf.write(cpslimitdesc + "20\n");
                bf.write(thirdsprinthitdesc + "false\n");
                bf.write(delaymovedesc + "2\n");
                bf.write(consistantkbdesc + "true\n");
                bf.close();
            } catch (IOException ignored) {}
        }
        
        try (BufferedReader bfr = new BufferedReader(new FileReader(file))) {
            RunTick.customhit = Boolean.parseBoolean(bfr.readLine().split(": ")[1]);
            RunTick.intmaxdmtick = Integer.parseInt(bfr.readLine().split(": ")[1]);
            RunTick.damage = Double.parseDouble(bfr.readLine().split(": ")[1]);
            shouldCheckCPS = Boolean.parseBoolean(bfr.readLine().split(": ")[1]);
            RunTick.cpslimit = Double.parseDouble(bfr.readLine().split(": ")[1]);
            shouldThirdSprintHit = Boolean.parseBoolean(bfr.readLine().split(": ")[1]);
            DELAY = Integer.parseInt(bfr.readLine().split(": ")[1]);
            RunTick.consistantkb = Boolean.parseBoolean(bfr.readLine().split(": ")[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}