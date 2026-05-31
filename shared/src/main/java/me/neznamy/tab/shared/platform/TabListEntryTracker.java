package me.neznamy.tab.shared.platform;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * Injector for tablist entry tracking.
 */
public interface TabListEntryTracker {

    /**
     * Injects this tracker into previously given channel.
     */
    void inject();

    /**
     * Checks whether the given player is currently in the tablist.
     *
     * @param   uuid
     *          UUID of player to check
     * @return  {@code true} if player is in tablist, {@code false} if not
     */
    boolean containsEntry(@NotNull UUID uuid);

    /**
     * Returns collection of all tablist entries.
     *
     * @return  Collection of all tablist entries
     */
    @NotNull
    Collection<UUID> getEntries();
}