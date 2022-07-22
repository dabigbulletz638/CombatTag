package cc.crystalpvp.combattag;

import cc.crystalpvp.combattag.packetwrapper.impl.WrapperPlayServerChat;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CombatTag extends JavaPlugin implements Listener {
    private static final String PREFIX = "\247f[\247dCombat\247bTag\247f]\2477 ";
    private static final long COMBAT_TAG_TIME = 30000;

    private final Object2LongOpenHashMap<UUID> tagged = new Object2LongOpenHashMap<>();
    private final List<UUID> killedPlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getScheduler().runTaskTimer(this, () -> {
            final ObjectIterator<Object2LongMap.Entry<UUID>> iterator = this.tagged.object2LongEntrySet().fastIterator();
            final long now = System.currentTimeMillis();
            while (iterator.hasNext()) {
                final Object2LongMap.Entry<UUID> entry = iterator.next();
                final Player player = this.getServer().getPlayer(entry.getKey());
                if (player == null) {
                    return;
                }
                final long time = entry.getLongValue();
                if (now - time >= COMBAT_TAG_TIME) {
                    this.sendActionBar(player, "You are no longer in combat.");
                    iterator.remove();
                } else {
                    this.sendActionBar(player, "In combat for \247c" + (COMBAT_TAG_TIME / 1000 - (now - time) / 1000) + " \2477seconds.");
                }
            }
        }, 0, 20);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final Entity damager = event.getDamager();
        final Entity victim = event.getEntity();
        final long now = System.currentTimeMillis();
        if (victim.isOp() || !(victim instanceof Player)) {
            return;
        }
        if (damager instanceof Player) {
            this.tagged.put(damager.getUniqueId(), now);
            this.tagged.put(victim.getUniqueId(), now);
        } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            if (damager instanceof Arrow || damager instanceof ThrownPotion) {
                this.tagged.put(((Player) ((Projectile) damager).getShooter()).getUniqueId(), now);
                this.tagged.put(victim.getUniqueId(), now);
            }
        } else if (damager instanceof EnderCrystal) {
            final EnderCrystal crystal = (EnderCrystal) damager;
            if (crystal.hasMetadata("dmp.enderCrystalPlacer")) {
                final List<MetadataValue> metadataValues = crystal.getMetadata("dmp.enderCrystalPlacer");
                final Player attacker = Bukkit.getPlayer(UUID.fromString(metadataValues.get(0).asString()));
                if (attacker != null) {
                    this.tagged.put(attacker.getUniqueId(), now);
                }
                this.tagged.put(victim.getUniqueId(), now);
            }
        }
    }

//    @EventHandler
//    public void onEntityExplode(final EntityExplodeEvent event) {
//        System.out.println(event.getEntity().getLastDamageCause());
//    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player victim = event.getEntity();
        if (this.tagged.containsKey(victim.getUniqueId())) {
            this.tagged.remove(victim.getUniqueId());
            victim.sendMessage(PREFIX + "You are no longer in combat.");
        }
        Entity damager = null;
        if (victim.hasMetadata("dmp.lastDamageEnt")) {
            damager = (Entity) victim.getMetadata("dmp.lastDamageEnt").get(0).value();
        }
        if (damager == null || victim.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.SUICIDE) {
            return;
        }
        if (damager instanceof EnderCrystal) {
            final EnderCrystal crystal = (EnderCrystal) damager;
            if (crystal.hasMetadata("dmp.enderCrystalPlacer")) {
                final List<MetadataValue> metadataValues = crystal.getMetadata("dmp.enderCrystalPlacer");
                if (metadataValues.size() > 0) {
                    final Player killer = Bukkit.getPlayer(
                            UUID.fromString(metadataValues.get(0).asString())
                    );
                    if (killer == null || victim == killer) {
                        return;
                    }
                    if (this.tagged.containsKey(killer.getUniqueId())) {
                        this.tagged.remove(killer.getUniqueId());
                        this.sendActionBar(killer, "You are no longer in combat.");
                    }
                }
            }
        } else if (victim != damager && damager instanceof Player) {
            final Player player = (Player) damager;
            if (this.tagged.containsKey(player.getUniqueId())) {
                this.tagged.remove(player.getUniqueId());
                this.sendActionBar(player, "You are no longer in combat.");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        if (this.killedPlayers.contains(uuid)) {
            player.sendMessage(PREFIX + "\247cYou were killed for combat logging!");
            this.killedPlayers.remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.onDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event) {
        this.onDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandProcess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        if (event.getMessage().startsWith("/kill")) {
            if (this.tagged.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(PREFIX + "\247cYou cannot /kill whilst in combat!");
            }
        }
    }

    private void onDisconnect(final Player player) {
        final UUID uuid = player.getUniqueId();
        if (this.tagged.containsKey(uuid)) {
            this.getServer().broadcastMessage(PREFIX + "\247c" + player.getName() + " \2477logged out in combat!");
            player.setHealth(0.0D);
            this.tagged.remove(uuid);
            if (!this.killedPlayers.contains(uuid)) {
                this.killedPlayers.add(uuid);
            }
        }
    }

    private void sendActionBar(final Player player, final String message) {
        final WrapperPlayServerChat chat = new WrapperPlayServerChat();
        chat.setMessage(WrappedChatComponent.fromText("\2477" + message));
        chat.setChatType(EnumWrappers.ChatType.GAME_INFO);
        chat.sendPacket(player);
    }
}
