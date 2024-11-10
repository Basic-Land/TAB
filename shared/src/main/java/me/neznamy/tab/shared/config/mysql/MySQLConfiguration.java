package me.neznamy.tab.shared.config.mysql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.shared.config.file.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * This class represents MySQL configuration section.
 */
@Getter
@RequiredArgsConstructor
public class MySQLConfiguration {
    
    @NotNull private final String host;
    private final int port;
    @NotNull private final String database;
    @NotNull private final String username;
    @NotNull private final String password;
    private final boolean useSSL;

    /**
     * Returns instance of this class created from given configuration section. If there are
     * issues in the configuration, console warns are printed.
     *
     * @param   section
     *          Configuration section to load from
     * @return  Loaded instance from given configuration section
     */
    @NotNull
    public static MySQLConfiguration fromSection(@NotNull ConfigurationSection section) {
        // Check keys
        section.checkForUnknownKey(Arrays.asList("enabled", "host", "port", "database", "username", "password", "useSSL"));

        return new MySQLConfiguration(
                section.getString("host", "127.0.0.1"),
                section.getInt("port", 3306),
                section.getString("database", "tab"),
                section.getString("username", "user"),
                section.getString("password", "password"),
                section.getBoolean("useSSL", true)
        );
    }
}
