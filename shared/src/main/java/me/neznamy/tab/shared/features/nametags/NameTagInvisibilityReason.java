package me.neznamy.tab.shared.features.nametags;

/**
 * Enum tracking all possible reasons why a player's nametag is globally hidden for everyone.
 */
public enum NameTagInvisibilityReason {

    /** API function for global nametag hiding (#hideNametag) */
    GLOBAL_API_HIDE,

    /** invisible-nametags option returned true for the player */
    MEETING_CONFIGURED_CONDITION
}
