package me.neznamy.tab.platforms.velocity;

import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.chat.TabComponent;
import me.neznamy.tab.shared.hook.AdventureHook;
import me.neznamy.tab.shared.platform.TabList;
import me.neznamy.tab.shared.platform.TabPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * TabList implementation for Velocity using its API.
 */
@RequiredArgsConstructor
public class VelocityTabList implements TabList {

    /** Player this TabList belongs to */
    @NonNull
    private final VelocityTabPlayer player;

    /** Expected names based on configuration, saving to restore them if another plugin overrides them */
    private final Map<TabPlayer, Component> expectedDisplayNames = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void removeEntry(@NonNull UUID entry) {
        player.getPlayer().getTabList().removeEntry(entry);
    }

    @Override
    public void updateDisplayName(@NonNull UUID entry, @Nullable TabComponent displayName) {
        Component component = displayName == null ? null : AdventureHook.toAdventureComponent(displayName, player.getVersion());
        setExpectedDisplayName(entry, component);
        player.getPlayer().getTabList().getEntry(entry).ifPresent(e -> e.setDisplayName(component));
    }

    @Override
    public void updateLatency(@NonNull UUID entry, int latency) {
        player.getPlayer().getTabList().getEntry(entry).ifPresent(e -> e.setLatency(latency));
    }

    @Override
    public void updateGameMode(@NonNull UUID entry, int gameMode) {
        player.getPlayer().getTabList().getEntry(entry).ifPresent(e -> e.setGameMode(gameMode));
    }

    @Override
    public void addEntry(@NonNull Entry entry) {
        Component displayName = entry.getDisplayName() == null ? null : AdventureHook.toAdventureComponent(entry.getDisplayName(), player.getVersion());
        TabListEntry e = TabListEntry.builder()
                .tabList(player.getPlayer().getTabList())
                .profile(new GameProfile(
                        entry.getUniqueId(),
                        entry.getName(),
                        entry.getSkin() == null ? Collections.emptyList() : Collections.singletonList(
                                new GameProfile.Property(TEXTURES_PROPERTY, entry.getSkin().getValue(), Objects.requireNonNull(entry.getSkin().getSignature())))
                ))
                .latency(entry.getLatency())
                .gameMode(entry.getGameMode())
                .displayName(displayName)
                .build();
        setExpectedDisplayName(entry.getUniqueId(), displayName);

        // Remove entry because:
        // #1 - If player is 1.8 - 1.19.2, KeyedVelocityTabList#addEntry will throw IllegalArgumentException
        //      if the entry is already present (most likely due to an accident trying to add existing player in global playerlist)
        // #2 - If player is 1.20.2+, tablist is cleared by the client itself without requirement to remove
        //      manually by the proxy, however velocity's tablist entry tracker still thinks they are present
        //      and therefore will refuse to add them
        removeEntry(entry.getUniqueId());

        player.getPlayer().getTabList().addEntry(e);

        if (player.getVersion().getMinorVersion() == 8) {
            // Compensation for 1.8.0 client sided bug
            updateDisplayName(entry.getUniqueId(), entry.getDisplayName());
        }
    }

    @Override
    public void setPlayerListHeaderFooter(@NonNull TabComponent header, @NonNull TabComponent footer) {
        player.getPlayer().sendPlayerListHeaderAndFooter(
                AdventureHook.toAdventureComponent(header, player.getVersion()),
                AdventureHook.toAdventureComponent(footer, player.getVersion())
        );
    }

    @Override
    public boolean containsEntry(@NonNull UUID entry) {
        return player.getPlayer().getTabList().containsEntry(entry);
    }

    @Override
    public void checkDisplayNames() {
        for (TabPlayer target : TAB.getInstance().getOnlinePlayers()) {
            player.getPlayer().getTabList().getEntry(target.getUniqueId()).ifPresent(entry -> {
                Component expectedComponent = expectedDisplayNames.get(target);
                if (expectedComponent != null && entry.getDisplayNameComponent().orElse(null) != expectedComponent) {
                    displayNameWrong(entry.getProfile().getName(), player);
                    entry.setDisplayName(expectedComponent);
                }
            });
        }
    }

    private void setExpectedDisplayName(@NonNull UUID entry, @Nullable Component displayName) {
        TabPlayer player = TAB.getInstance().getPlayerByTabListUUID(entry);
        if (player != null) expectedDisplayNames.put(player, displayName);
    }
}
