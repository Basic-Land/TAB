package me.neznamy.tab.shared.platform;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Netty injector for tablist entry tracking.
 */
@RequiredArgsConstructor
public abstract class NettyTabListEntryTracker extends ChannelDuplexHandler implements TabListEntryTracker {

    /** Players in the tablist */
    protected final Set<UUID> tablistEntries = Collections.synchronizedSet(new HashSet<>());

    /** Channel to inject into */
    @NotNull
    private final Channel channel;

    @Override
    public void write(@NotNull ChannelHandlerContext context, @Nullable Object packet, @NotNull ChannelPromise channelPromise) throws Exception{
        if (packet == null) return;
        onPacketSend(packet);
        super.write(context, packet, channelPromise);
    }

    @Override
    public void inject() {
        if (!channel.pipeline().names().contains("packet_handler"))
            return; // Player got disconnected instantly or fake player
        try {
            channel.pipeline().addBefore("packet_handler", "TAB-TablistEntryTracker", this);
        } catch (NoSuchElementException | IllegalArgumentException ignored) {
        }
    }

    @Override
    public boolean containsEntry(@NotNull UUID uuid) {
        return tablistEntries.contains(uuid);
    }

    @Override
    @NotNull
    public Collection<UUID> getEntries() {
        return Collections.unmodifiableSet(tablistEntries);
    }

    /**
     * Processes outgoing packet, updating tablist entries accordingly.
     *
     * @param   packet
     *          Packet to process
     */
    public abstract void onPacketSend(@NotNull Object packet);
}