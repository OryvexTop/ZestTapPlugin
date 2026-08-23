package com.brugnevom.zesttapplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class RunTick implements Listener {
  public static double cpslimit = 16.0D;
  
  private final Map<UUID, List<Long>> playerClicks = new HashMap<>();
  
  public static boolean customhit = true;
  
  public static boolean consistantkb;
  
  public static int intmaxdmtick;
  
  public static double damage;
  
  public static double groundy;
  
  public static int hitcount;
  
  public static Player victim;
  
  public static Player damager;
  
  public ZestTapPlugin m;
  
  public RunTick(ZestTapPlugin m) {
    this.m = m;
  }
  
  private void recordClick(UUID uuid) {
    this.playerClicks.putIfAbsent(uuid, new ArrayList<>());
    this.playerClicks.get(uuid).add(System.currentTimeMillis());
  }
  
  private int getCPS(UUID uuid) {
    if (!this.playerClicks.containsKey(uuid))
      return 0; 
    long now = System.currentTimeMillis();
    List<Long> clicks = this.playerClicks.get(uuid);
    clicks.removeIf(timestamp -> (now - timestamp.longValue() > 1000L));
    return clicks.size();
  }
  
  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.playerClicks.remove(event.getPlayer().getUniqueId());
  }
  
  @EventHandler
  public void interact(PlayerAnimationEvent e) {
    if (e.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
      UUID uuid = e.getPlayer().getUniqueId();
      recordClick(uuid);
    } 
  }
  
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onHit(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
      victim = (Player)event.getEntity();
      damager = (Player)event.getDamager();
      UUID damagerUUID = damager.getUniqueId();
      int currentCPS = getCPS(damagerUUID);
      if (currentCPS > cpslimit) {
        event.setCancelled(true);
        this.playerClicks.remove(damagerUUID);
        return;
      } 
      if (customhit) {
        if (victim.isOnGround()) {
          hitcount = 0;
        } else {
          hitcount++;
        } 
        if (hitcount >= 4)
          hitcount = 0; 
        event.setDamage(event.getDamage() * damage);
        victim.setMaximumNoDamageTicks(intmaxdmtick);
        
        if (consistantkb && hitcount >= 1 && !victim.isOnGround() && damager.getLocation().distance(victim.getLocation()) > 2.5D) {
          try {
            Method getHandle = victim.getClass().getMethod("getHandle");
            Object entityPlayer = getHandle.invoke(victim);
            Field hurtTicksField = entityPlayer.getClass().getField("hurtTicks");
            int hurtTicks = hurtTicksField.getInt(entityPlayer);
            
            if (hurtTicks > 0) {
              Bukkit.getScheduler().runTaskLater(m, () -> {
                Vector kb = new Vector(0, 0, 0);
                if (hitcount == 1) {
                  kb.setY(-0.3D); 
                } else if (hitcount == 2) {
                  kb = victim.getLocation().toVector().subtract(damager.getLocation().toVector());
                  kb.setY(0);
                  kb.normalize();
                  kb.multiply(0.35D);
                  kb.setY(-0.35D);
                }
                victim.setVelocity(kb);
              }, 0L);
            }
          } catch (Exception ignored) {}
        } 
      } else {
        victim.setMaximumNoDamageTicks(20);
        event.setDamage(event.getDamage());
      } 
    } 
  }
}