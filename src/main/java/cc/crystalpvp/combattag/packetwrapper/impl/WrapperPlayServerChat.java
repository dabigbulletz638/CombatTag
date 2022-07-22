package cc.crystalpvp.combattag.packetwrapper.impl;

import cc.crystalpvp.combattag.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

public class WrapperPlayServerChat extends AbstractPacket {
    public static final PacketType TYPE = PacketType.Play.Server.CHAT;

    public WrapperPlayServerChat() {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrappedChatComponent getMessage() {
        return handle.getChatComponents().read(0);
    }

    public void setMessage(final WrappedChatComponent value) {
        handle.getChatComponents().write(0, value);
    }

    public EnumWrappers.ChatType getChatType() {
        return handle.getChatTypes().read(0);
    }

    public void setChatType(final EnumWrappers.ChatType type) {
        handle.getChatTypes().write(0, type);
    }
}
