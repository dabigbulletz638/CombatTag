package cc.crystalpvp.combattag;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class CombatTag extends JavaPlugin implements Listener {
    private static final String PREFIX = "\247f[\247dCombat\247bTag\247f]\2477 ";
    private static final long COMBAT_TAG_TIME = 30000;
    private static CombatTag INSTANCE;

    private final Object2LongOpenHashMap<UUID> tagged = new Object2LongOpenHashMap<>();

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getScheduler().runTaskTimer(this, () -> {
            final ObjectIterator<Object2LongMap.Entry<UUID>> iterator = this.tagged.object2LongEntrySet().fastIterator();
            final long now = System.currentTimeMillis();
            while (iterator.hasNext()) {
                final Object2LongMap.Entry<UUID> entry = iterator.next();
                final long time = entry.getLongValue();
                if (now - time >= COMBAT_TAG_TIME) {
                    // TODO: send thingy here
                    iterator.remove();
                }
            }
        }, 0, 20);
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (this.tagged.containsKey(player.getUniqueId())) {
            this.tagged.remove(player.getUniqueId());
            player.sendMessage(PREFIX + "You are no longer in combat.");
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
        if (this.tagged.containsKey(player.getUniqueId())) {
            player.setHealth(0.0D);
            this.tagged.remove(player.getUniqueId());
        }
    }

    public static CombatTag getInstance() {
        return INSTANCE;
    }

    public Object2LongMap<UUID> getTagged() {
        return this.tagged;
    }
}
