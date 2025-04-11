package me.neznamy.tab.platforms.bukkit.v1_8_R3;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.neznamy.chat.component.TabComponent;
import me.neznamy.tab.platforms.bukkit.BukkitTabPlayer;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.platform.TabList;
import me.neznamy.tab.shared.platform.decorators.TrackedTabList;
import me.neznamy.tab.shared.util.ReflectionUtils;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo.PlayerInfoData;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * TabList implementation using direct NMS code.
 */
public class NMSPacketTabList extends TrackedTabList<BukkitTabPlayer> {

    private static final Field ACTION = ReflectionUtils.getOnlyField(PacketPlayOutPlayerInfo.class, EnumPlayerInfoAction.class);
    private static final Field PLAYERS = ReflectionUtils.getOnlyField(PacketPlayOutPlayerInfo.class, List.class);

    private static final Field PlayerInfoData_Latency = ReflectionUtils.getFields(PlayerInfoData.class, int.class).get(0);
    private static final Field PlayerInfoData_DisplayName = ReflectionUtils.getOnlyField(PlayerInfoData.class, IChatBaseComponent.class);

    private static final Field FOOTER = ReflectionUtils.getFields(PacketPlayOutPlayerListHeaderFooter.class, IChatBaseComponent.class).get(1);

    /**
     * Constructs new instance.
     *
     * @param   player
     *          Player this tablist will belong to
     */
    public NMSPacketTabList(@NotNull BukkitTabPlayer player) {
        super(player);
    }

    @Override
    @SneakyThrows
    public void removeEntry(@NonNull UUID entry) {
        sendPacket(EnumPlayerInfoAction.REMOVE_PLAYER, entry, "", null, 0, 0, null);
    }

    @Override
    public void updateDisplayName0(@NonNull UUID entry, @Nullable TabComponent displayName) {
        sendPacket(EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, entry, "", null, 0, 0, displayName);
    }

    @Override
    public void updateLatency(@NonNull UUID entry, int latency) {
        sendPacket(EnumPlayerInfoAction.UPDATE_LATENCY, entry, "", null, latency, 0, null);
    }

    @Override
    public void updateGameMode(@NonNull UUID entry, int gameMode) {
        sendPacket(EnumPlayerInfoAction.UPDATE_GAME_MODE, entry, "", null, 0, gameMode, null);
    }

    @Override
    public void updateListed(@NonNull UUID entry, boolean listed) {
        // Added in 1.19.3
    }

    @Override
    public void updateListOrder(@NonNull UUID entry, int listOrder) {
        // Added in 1.21.2
    }

    @Override
    public void updateHat(@NonNull UUID entry, boolean showHat) {
        // Added in 1.21.4
    }

    @Override
    public void addEntry0(@NonNull Entry entry) {
        sendPacket(EnumPlayerInfoAction.ADD_PLAYER, entry.getUniqueId(), entry.getName(), entry.getSkin(), entry.getLatency(),
                entry.getGameMode(), entry.getDisplayName());
    }

    @Override
    @SneakyThrows
    public void setPlayerListHeaderFooter(@NonNull TabComponent header, @NonNull TabComponent footer) {
        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(header.convert());
        FOOTER.set(packet, footer.convert());
        sendPacket(packet);
    }

    @Override
    public boolean containsEntry(@NonNull UUID entry) {
        return true; // TODO?
    }

    @Override
    @Nullable
    public Skin getSkin() {
        Collection<Property> properties = ((CraftPlayer)player.getPlayer()).getProfile().getProperties().get(TEXTURES_PROPERTY);
        if (properties.isEmpty()) return null; // Offline mode
        Property property = properties.iterator().next();
        return new Skin(property.getValue(), property.getSignature());
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void onPacketSend(@NonNull Object packet) {
        if (!(packet instanceof PacketPlayOutPlayerInfo)) return;
        EnumPlayerInfoAction action = (EnumPlayerInfoAction) ACTION.get(packet);
        for (PlayerInfoData nmsData : (List<PlayerInfoData>) PLAYERS.get(packet)) {
            GameProfile profile = nmsData.a();
            UUID id = profile.getId();
            if (action == EnumPlayerInfoAction.UPDATE_DISPLAY_NAME || action == EnumPlayerInfoAction.ADD_PLAYER) {
                TabComponent expectedName = getExpectedDisplayNames().get(id);
                if (expectedName != null) PlayerInfoData_DisplayName.set(nmsData, expectedName.convert());
            }
            if (action == EnumPlayerInfoAction.UPDATE_LATENCY || action == EnumPlayerInfoAction.ADD_PLAYER) {
                int oldLatency = PlayerInfoData_Latency.getInt(nmsData);
                int newLatency = TAB.getInstance().getFeatureManager().onLatencyChange(player, id, oldLatency);
                if (oldLatency != newLatency) {
                    PlayerInfoData_Latency.set(nmsData, newLatency);
                }
            }
            if (action == EnumPlayerInfoAction.ADD_PLAYER) {
                TAB.getInstance().getFeatureManager().onEntryAdd(player, id, profile.getName());
            }
        }
    }

    @SneakyThrows
    private void sendPacket(@NonNull EnumPlayerInfoAction action, @NonNull UUID id, @NonNull String name,
                            @Nullable Skin skin, int latency, int gameMode, @Nullable TabComponent displayName) {
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(action);
        PLAYERS.set(packet, Collections.singletonList(packet.new PlayerInfoData(
                createProfile(id, name, skin),
                latency,
                WorldSettings.EnumGamemode.values()[gameMode],
                displayName == null ? null : displayName.convert())
        ));
        sendPacket(packet);
    }

    /**
     * Creates GameProfile from given parameters.
     *
     * @param   id
     *          Profile ID
     * @param   name
     *          Profile name
     * @param   skin
     *          Player skin
     * @return  GameProfile from given parameters
     */
    @NotNull
    private GameProfile createProfile(@NonNull UUID id, @NonNull String name, @Nullable Skin skin) {
        GameProfile profile = new GameProfile(id, name);
        if (skin != null) {
            profile.getProperties().put(TabList.TEXTURES_PROPERTY,
                    new Property(TabList.TEXTURES_PROPERTY, skin.getValue(), skin.getSignature()));
        }
        return profile;
    }

    /**
     * Sends the packet to the player.
     *
     * @param   packet
     *          Packet to send
     */
    private void sendPacket(@NotNull Packet<?> packet) {
        ((CraftPlayer)player.getPlayer()).getHandle().playerConnection.sendPacket(packet);
    }
}
