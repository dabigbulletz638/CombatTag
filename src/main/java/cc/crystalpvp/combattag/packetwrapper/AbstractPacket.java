package cc.crystalpvp.combattag.packetwrapper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Objects;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractPacket {
    protected final PacketContainer handle;

    protected AbstractPacket(final PacketContainer handle, final PacketType type) {
        if (handle == null) {
            throw new IllegalArgumentException("Packet handle cannot be NULL.");
        } else if (!Objects.equal(handle.getType(), type)) {
            throw new IllegalArgumentException(handle.getHandle() + " is not a packet of type " + type);
        }
        this.handle = handle;
    }

    public PacketContainer getHandle() {
        return this.handle;
    }

    public void sendPacket(final Player receiver) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, this.getHandle());
        } catch (final InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet.", e);
        }
    }

    public void broadcastPacket() {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(this.getHandle());
    }

    public void receivePacket(final Player sender) {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender, this.getHandle());
        } catch (final Exception e) {
            throw new RuntimeException("Cannot receive packet.", e);
        }
    }
}
