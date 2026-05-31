package me.neznamy.tab.platforms.fabric;

import io.netty.channel.Channel;
import me.neznamy.tab.shared.platform.NettyTabListEntryTracker;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fabric implementation of TabListEntryTracker.
 */
public class FabricTabListEntryTracker extends NettyTabListEntryTracker {

    public FabricTabListEntryTracker(@NotNull Channel channel) {
        super(channel);
    }

    @Override
    public void onPacketSend(@NotNull Object packet) {
        if (packet instanceof ClientboundPlayerInfoRemovePacket remove) {
            for (UUID id : remove.profileIds()) {
                tablistEntries.remove(id);
            }
        }
        if (packet instanceof ClientboundPlayerInfoUpdatePacket update) {
            if (update.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                for (ClientboundPlayerInfoUpdatePacket.Entry nmsData : update.entries()) {
                    tablistEntries.add(nmsData.profileId());
                }
            }
        }
    }
}
