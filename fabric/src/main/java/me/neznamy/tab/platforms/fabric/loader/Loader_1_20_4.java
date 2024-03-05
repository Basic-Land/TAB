package me.neznamy.tab.platforms.fabric.loader;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import lombok.SneakyThrows;
import me.neznamy.tab.platforms.fabric.FabricMultiVersion;
import me.neznamy.tab.platforms.fabric.FabricTabPlayer;
import me.neznamy.tab.shared.ProtocolVersion;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.backend.EntityData;
import me.neznamy.tab.shared.chat.TabComponent;
import me.neznamy.tab.shared.platform.TabList;
import me.neznamy.tab.shared.util.ReflectionUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;

/**
 * Method loader compiled using Minecraft 1.20.4.
 */
@SuppressWarnings({
        "unchecked", // Java generic types
        "DataFlowIssue", // Profile is not null on add action
        "unused" // Actually used, just via reflection
})
public class Loader_1_20_4 {

    private static final Scoreboard dummyScoreboard = new Scoreboard();

    /**
     * Constructs new instance and overrides all methods to their current format based on server version.
     *
     * @param   serverVersion
     *          Exact server version
     */
    public Loader_1_20_4(@NotNull ProtocolVersion serverVersion) {
        if (serverVersion.getMinorVersion() >= 15) {
            FabricMultiVersion.isSneaking = Entity::isCrouching;
        }
        if (serverVersion.getMinorVersion() >= 16) {
            FabricMultiVersion.getLevelName = level -> {
                String path = level.dimension().location().getPath();
                return ((ServerLevelData)level.getLevelData()).getLevelName() + switch (path) {
                    case "overworld" -> ""; // No suffix for overworld
                    case "the_nether" -> "_nether";
                    default -> "_" + path; // End + default behavior for other dimensions created by mods
                };
            };
            // sendMessage on 1.16 - 1.18.2 using UUID sender
            FabricMultiVersion.sendMessage = (player, message) ->
                    player.getClass().getMethod("method_9203", Component.class, UUID.class).invoke(player, message, new UUID(0, 0));
        }
        if (serverVersion.getMinorVersion() >= 17) {
            FabricMultiVersion.registerTeam = team -> ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
            FabricMultiVersion.unregisterTeam = ClientboundSetPlayerTeamPacket::createRemovePacket;
            FabricMultiVersion.updateTeam = team -> ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, false);
            FabricMultiVersion.newHeaderFooter = ClientboundTabListPacket::new;
            FabricMultiVersion.isTeamPacket = packet -> packet instanceof ClientboundSetPlayerTeamPacket; // Fabric-mapped name changed
            if (serverVersion.getNetworkId() >= ProtocolVersion.V1_17_1.getNetworkId()) {
                FabricMultiVersion.getDestroyedEntities = packet -> ((ClientboundRemoveEntitiesPacket) packet).getEntityIds().toIntArray();
            } else {
                FabricMultiVersion.getDestroyedEntities = packet -> new int[]{ReflectionUtils.getOnlyField(packet.getClass()).getInt(packet)};
            }
        }
        if (serverVersion.getMinorVersion() >= 19) {
            FabricMultiVersion.sendMessage = ServerPlayer::sendSystemMessage;
            FabricMultiVersion.sendMessage2 = CommandSourceStack::sendSystemMessage;
            FabricMultiVersion.spawnEntity = (level, entityId, id, entityType, location) ->
                    new ClientboundAddEntityPacket(entityId, id, location.getX(), location.getY(), location.getZ(),
                            0, 0, (EntityType<?>) entityType, 0, Vec3.ZERO, 0);
        }
        if (serverVersion.getNetworkId() >= ProtocolVersion.V1_19_3.getNetworkId()) {
            FabricMultiVersion.newEntityMetadata = (entityId, data) ->  new ClientboundSetEntityDataPacket(entityId, (List<SynchedEntityData.DataValue<?>>) data.build());
            FabricMultiVersion.createDataWatcher = (viewer, flags, displayName, nameVisible) -> {
                Optional<Component> name = Optional.of(((FabricTabPlayer)viewer).getPlatform().toComponent(TabComponent.optimized(displayName), viewer.getVersion()));
                return () -> Arrays.asList(
                        new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, flags),
                        new SynchedEntityData.DataValue<>(2, EntityDataSerializers.OPTIONAL_COMPONENT, name),
                        new SynchedEntityData.DataValue<>(3, EntityDataSerializers.BOOLEAN, nameVisible),
                        new SynchedEntityData.DataValue<>(EntityData.getArmorStandFlagsPosition(serverVersion.getMinorVersion()), EntityDataSerializers.BYTE, EntityData.MARKER_FLAG)
                );
            };
            Map<TabList.Action, EnumSet<ClientboundPlayerInfoUpdatePacket.Action>> actionMap = Register1_19_3.createActionMap();
            FabricMultiVersion.buildTabListPacket = (action, entry) -> {
                if (action == TabList.Action.REMOVE_PLAYER) {
                    return new ClientboundPlayerInfoRemovePacket(Collections.singletonList(entry.getId()));
                }
                ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(actionMap.get(action), Collections.emptyList());
                ReflectionUtils.getFields(ClientboundPlayerInfoUpdatePacket.class, List.class).get(0).set(packet,
                        Collections.singletonList(new ClientboundPlayerInfoUpdatePacket.Entry(
                                entry.getId(),
                                entry.createProfile(),
                                true,
                                entry.getLatency(),
                                GameType.byId(entry.getGameMode()),
                                entry.getDisplayName(),
                                null
                        )));
                return packet;
            };
            FabricMultiVersion.onPlayerInfo = (receiver, packet0) -> {
                ClientboundPlayerInfoUpdatePacket packet = (ClientboundPlayerInfoUpdatePacket) packet0;
                EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = packet.actions();
                List<ClientboundPlayerInfoUpdatePacket.Entry> updatedList = new ArrayList<>();
                for (ClientboundPlayerInfoUpdatePacket.Entry nmsData : packet.entries()) {
                    GameProfile profile = nmsData.profile();
                    Component displayName = nmsData.displayName();
                    int latency = nmsData.latency();
                    if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)) {
                        Component expectedDisplayName = ((FabricTabPlayer)receiver).getTabList().getExpectedDisplayName(nmsData.profileId());
                        if (expectedDisplayName != null) displayName = expectedDisplayName;
                    }
                    if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY)) {
                        latency = TAB.getInstance().getFeatureManager().onLatencyChange(receiver, nmsData.profileId(), latency);
                    }
                    if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                        TAB.getInstance().getFeatureManager().onEntryAdd(receiver, nmsData.profileId(), profile.getName());
                    }
                    updatedList.add(new ClientboundPlayerInfoUpdatePacket.Entry(nmsData.profileId(), profile, nmsData.listed(), latency, nmsData.gameMode(), displayName, nmsData.chatSession()));
                }
                ReflectionUtils.getFields(ClientboundPlayerInfoUpdatePacket.class, List.class).get(0).set(packet, updatedList);
            };
            FabricMultiVersion.isPlayerInfo = packet -> packet instanceof ClientboundPlayerInfoUpdatePacket;
        }
        if (serverVersion.getNetworkId() >= ProtocolVersion.V1_19_4.getNetworkId()) {
            FabricMultiVersion.isBundlePacket = packet -> packet instanceof ClientboundBundlePacket;
            FabricMultiVersion.getBundledPackets = packet -> (Iterable<Object>) (Object) ((ClientboundBundlePacket)packet).subPackets();
            FabricMultiVersion.sendPackets = (player, packets) ->
                    // Reflection because
                    // 1.20.4- uses Iterable<Packet<ClientGamePacketListener>
                    // 1.20.5+ uses Iterable<Packet<? super ClientGamePacketListener>
                    // this is the only way to merge them
                    player.connection.send(ClientboundBundlePacket.class.getConstructor(Iterable.class).newInstance(packets));
        }
        if (serverVersion.getMinorVersion() >= 20) {
            FabricMultiVersion.getLevel = Entity::level;
        }
        if (serverVersion.getNetworkId() >= ProtocolVersion.V1_20_2.getNetworkId()) {
            FabricMultiVersion.propertyToSkin = property -> new TabList.Skin(property.value(), property.signature());
            FabricMultiVersion.isSpawnPlayerPacket = packet -> packet instanceof ClientboundAddEntityPacket;
            FabricMultiVersion.getPing = player -> player.connection.latency();
            FabricMultiVersion.getDisplaySlot = packet -> packet.getSlot().ordinal();
            FabricMultiVersion.setDisplaySlot = (slot, objective) -> new ClientboundSetDisplayObjectivePacket(DisplaySlot.values()[slot], objective);
            FabricMultiVersion.getChannel = player -> {
                Connection c = (Connection) ReflectionUtils.getFields(ServerCommonPacketListenerImpl.class, Connection.class).get(0).get(player.connection);
                return (Channel) ReflectionUtils.getFields(Connection.class, Channel.class).get(0).get(c);
            };
        }
        if (serverVersion.getNetworkId() >= ProtocolVersion.V1_20_3.getNetworkId()) {
            FabricMultiVersion.getMSPT = server -> (float) server.getAverageTickTimeNanos() / 1000000;
            FabricMultiVersion.removeScore = (objective, holder) -> new ClientboundResetScorePacket(holder, objective);
            Register1_20_3.register();
        }
        if (serverVersion.getNetworkId() >= 766) { // TODO 1.20.5 constant
            FabricMultiVersion.deserialize = component -> Component.Serializer.fromJson(component, HolderLookup.Provider.create(Stream.empty()));
        }
    }

    /**
     * Why is this needed? Because otherwise it throws error about a class
     * not existing despite the code never running.
     * Why? Nobody knows.
     */
    private static class Register1_20_3 {

        @SneakyThrows
        public static void register() {
            FabricMultiVersion.newObjective = (name, displayName, renderType, numberFormat) ->
                    new Objective(dummyScoreboard, name, ObjectiveCriteria.DUMMY, displayName, renderType, false,
                            numberFormat == null ? null : new FixedFormat(numberFormat));
            try {
                // 1.20.5+
                Constructor<ClientboundSetScorePacket> constructor = ClientboundSetScorePacket.class.getConstructor(
                        String.class, String.class, int.class, Optional.class, Optional.class
                );
                FabricMultiVersion.setScore = (objective, scoreHolder, score, displayName, numberFormat) ->
                        constructor.newInstance(scoreHolder, objective, score, Optional.ofNullable(displayName),
                                Optional.ofNullable(numberFormat == null ? null : new FixedFormat(numberFormat)));
            } catch (NoSuchMethodException e) {
                // 1.20.3 / 1.20.4
                Constructor<ClientboundSetScorePacket> constructor = ClientboundSetScorePacket.class.getConstructor(
                        String.class, String.class, int.class, Component.class, NumberFormat.class
                );
                FabricMultiVersion.setScore = (objective, scoreHolder, score, displayName, numberFormat) ->
                        constructor.newInstance(scoreHolder, objective, score, displayName,
                                numberFormat == null ? null : new FixedFormat(numberFormat));
            }
        }
    }

    private static class Register1_19_3 {

        public static EnumSet<ClientboundPlayerInfoUpdatePacket.Action> convertAction(TabList.Action action) {
            return EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.valueOf(action.name()));
        }

        public static Map<TabList.Action, EnumSet<ClientboundPlayerInfoUpdatePacket.Action>> createActionMap() {
            Map<TabList.Action, EnumSet<ClientboundPlayerInfoUpdatePacket.Action>> actions = new EnumMap<>(TabList.Action.class);
            actions.put(TabList.Action.ADD_PLAYER, EnumSet.allOf(ClientboundPlayerInfoUpdatePacket.Action.class));
            actions.put(TabList.Action.UPDATE_GAME_MODE, EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE));
            actions.put(TabList.Action.UPDATE_DISPLAY_NAME, EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME));
            actions.put(TabList.Action.UPDATE_LATENCY, EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY));
            return actions;
        }
    }
}
