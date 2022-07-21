package cc.crystalpvp.combattag;

import org.bukkit.plugin.java.JavaPlugin;

public class CombatTag extends JavaPlugin {
    private static CombatTag INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
    }

    public static CombatTag getInstance() {
        return INSTANCE;
    }
}
