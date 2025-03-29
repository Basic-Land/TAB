package me.neznamy.tab.platforms.bukkit.platform;

import lombok.Getter;
import lombok.SneakyThrows;
import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.chat.component.*;
import me.neznamy.tab.platforms.bukkit.*;
import me.neznamy.tab.platforms.bukkit.bossbar.BukkitBossBar;
import me.neznamy.tab.platforms.bukkit.bossbar.ViaBossBar;
import me.neznamy.tab.platforms.bukkit.features.BukkitTabExpansion;
import me.neznamy.tab.platforms.bukkit.features.PerWorldPlayerList;
import me.neznamy.tab.platforms.bukkit.header.*;
import me.neznamy.tab.platforms.bukkit.hook.BukkitPremiumVanishHook;
import me.neznamy.tab.platforms.bukkit.nms.BukkitReflection;
import me.neznamy.tab.platforms.bukkit.nms.PingRetriever;
import me.neznamy.tab.platforms.bukkit.provider.BukkitImplementationProvider;
import me.neznamy.tab.platforms.bukkit.provider.ImplementationProvider;
import me.neznamy.tab.platforms.bukkit.provider.ViaVersionImplementationProvider;
import me.neznamy.tab.shared.GroupManager;
import me.neznamy.tab.shared.ProtocolVersion;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.TabConstants;
import me.neznamy.tab.shared.backend.BackendPlatform;
import me.neznamy.tab.shared.features.PerWorldPlayerListConfiguration;
import me.neznamy.tab.shared.features.PlaceholderManagerImpl;
import me.neznamy.tab.shared.features.injection.PipelineInjector;
import me.neznamy.tab.shared.features.types.TabFeature;
import me.neznamy.tab.shared.hook.LuckPermsHook;
import me.neznamy.tab.shared.placeholders.expansion.EmptyTabExpansion;
import me.neznamy.tab.shared.placeholders.expansion.TabExpansion;
import me.neznamy.tab.shared.placeholders.types.PlayerPlaceholderImpl;
import me.neznamy.tab.shared.platform.BossBar;
import me.neznamy.tab.shared.platform.Scoreboard;
import me.neznamy.tab.shared.platform.TabList;
import me.neznamy.tab.shared.platform.TabPlayer;
import me.neznamy.tab.shared.platform.impl.AdventureBossBar;
import me.neznamy.tab.shared.platform.impl.DummyBossBar;
import me.neznamy.tab.shared.util.PerformanceUtil;
import me.neznamy.tab.shared.util.ReflectionUtils;
import net.kyori.adventure.audience.Audience;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of Platform interface for Bukkit platform
 */
@Getter
public class BukkitPlatform implements BackendPlatform {

    /** Plugin instance for registering tasks and events */
    @NotNull
    private final JavaPlugin plugin;

    /** Server version */
    @Getter
    private final ProtocolVersion serverVersion = ProtocolVersion.fromFriendlyName(Bukkit.getBukkitVersion().split("-")[0]);

    /** Variables checking presence of other plugins to hook into */
    private final boolean placeholderAPI = ReflectionUtils.classExists("me.clip.placeholderapi.PlaceholderAPI");

    /** Spigot field for tracking TPS, the array is final and only being modified instead of re-instantiated */
    private double[] recentTps;

    /** Detection for presence of Paper's TPS getter */
    private final boolean paperTps = ReflectionUtils.methodExists(Bukkit.class, "getTPS");

    /** Detection for presence of Paper's MSPT getter */
    private final boolean paperMspt = ReflectionUtils.methodExists(Bukkit.class, "getAverageTickTime");

    /** Header/footer implementation */
    @Getter
    @NotNull
    private final HeaderFooter headerFooter;

    /** Flag tracking if direct mojang-mapped code can be used or not */
    private final boolean canUseDirectNMS = BukkitReflection.isMojangMapped() &&
            (serverVersion == ProtocolVersion.V1_21_4 || serverVersion == ProtocolVersion.V1_21_5);

    /** Implementation for creating new instances using content available on the server */
    @NotNull
    private final ImplementationProvider serverImplementationProvider;

    /** Implementation for sending new content to new players on old servers */
    @NotNull
    private final ViaVersionImplementationProvider viaVersionImplementationProvider = new ViaVersionImplementationProvider(serverVersion);

    /**
     * Constructs new instance with given plugin.
     *
     * @param   plugin
     *          Plugin
     */
    @SneakyThrows
    public BukkitPlatform(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        long time = System.currentTimeMillis();
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            recentTps = ((double[]) server.getClass().getField("recentTps").get(server));
        } catch (ReflectiveOperationException ignored) {
            //not spigot
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
            new BukkitPremiumVanishHook().register();
        }
        if (canUseDirectNMS) {
            serverImplementationProvider = (ImplementationProvider) Class.forName("me.neznamy.tab.platforms.paper.PaperImplementationProvider").getConstructor().newInstance();
        } else {
            serverImplementationProvider = new BukkitImplementationProvider();
        }
        headerFooter = findHeaderFooter();
        PingRetriever.tryLoad();
        BukkitPipelineInjector.setGetChannel(serverImplementationProvider.getChannelFunction());
        BukkitUtils.sendCompatibilityMessage();
        Bukkit.getConsoleSender().sendMessage("[TAB] §7Loaded NMS hook in " + (System.currentTimeMillis()-time) + "ms");
    }

    @NotNull
    private HeaderFooter findHeaderFooter() {
        if (BukkitReflection.getMinorVersion() >= 8) {
            try {
                Objects.requireNonNull(serverImplementationProvider.getComponentConverter());
                return new PacketHeaderFooter();
            } catch (Exception e) {
                if (PaperHeaderFooter.isAvailable()) return new PaperHeaderFooter();
                if (BukkitHeaderFooter.isAvailable()) {
                    BukkitUtils.compatibilityError(e, "sending Header/Footer", "Bukkit API",
                            "Header/Footer having drastically increased CPU usage",
                            "Header/Footer not supporting fonts (1.16+)");
                    return new BukkitHeaderFooter();
                } else {
                    BukkitUtils.compatibilityError(e, "sending Header/Footer", null,
                            "Header/Footer feature not working");
                }
            }
        }
        return new DummyHeaderFooter();
    }

    @Override
    public void loadPlayers() {
        for (Player p : getOnlinePlayers()) {
            TAB.getInstance().addPlayer(new BukkitTabPlayer(this, p));
        }
    }

    @Override
    public void registerPlaceholders() {
        PlaceholderManagerImpl manager = TAB.getInstance().getPlaceholderManager();
        manager.registerInternalServerPlaceholder("%vault-prefix%", -1, () -> "");
        manager.registerInternalServerPlaceholder("%vault-suffix%", -1, () -> "");
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Chat> rspChat = Bukkit.getServicesManager().getRegistration(Chat.class);
            if (rspChat != null) {
                Chat chat = rspChat.getProvider();
                manager.registerInternalPlayerPlaceholder("%vault-prefix%", 1000, p -> chat.getPlayerPrefix((Player) p.getPlayer()));
                manager.registerInternalPlayerPlaceholder("%vault-suffix%", 1000, p -> chat.getPlayerSuffix((Player) p.getPlayer()));
            }
        }
        // Override for the PAPI placeholder to prevent console errors on unsupported server versions when ping field changes
        manager.registerPlayerPlaceholder("%player_ping%", p -> PerformanceUtil.toString(((TabPlayer) p).getPing()));
        BackendPlatform.super.registerPlaceholders();
    }

    @Override
    @Nullable
    public PipelineInjector createPipelineInjector() {
        return BukkitReflection.getMinorVersion() >= 8 && BukkitPipelineInjector.isAvailable() ? new BukkitPipelineInjector() : null;
    }

    @Override
    @NotNull
    public TabExpansion createTabExpansion() {
        if (placeholderAPI) {
            BukkitTabExpansion expansion = new BukkitTabExpansion();
            expansion.register();
            return expansion;
        }
        return new EmptyTabExpansion();
    }

    @Override
    @Nullable
    public TabFeature getPerWorldPlayerList(@NotNull PerWorldPlayerListConfiguration configuration) {
        return new PerWorldPlayerList(plugin, this, configuration);
    }

    @Override
    public void registerUnknownPlaceholder(@NotNull String identifier) {
        if (!placeholderAPI) {
            registerDummyPlaceholder(identifier);
            return;
        }
        PlaceholderManagerImpl pl = TAB.getInstance().getPlaceholderManager();
        if (identifier.startsWith("%rel_")) {
            // relational placeholder
            pl.registerRelationalPlaceholder(identifier, (viewer, target) ->
                    PlaceholderAPI.setRelationalPlaceholders((Player) viewer.getPlayer(), (Player) target.getPlayer(), identifier));
        } else if (identifier.startsWith("%sync:")) {
            registerSyncPlaceholder(identifier);
        } else if (identifier.contains("{") && identifier.contains("}")) {
            // has nested bracket placeholders
            pl.registerPlayerPlaceholder(identifier, p -> PlaceholderAPI.setPlaceholders((Player) p.getPlayer(), PlaceholderAPI.setBracketPlaceholders((Player) p.getPlayer(), identifier)));
        } else if (identifier.startsWith("%server_")) {
            // placeholder with the same output for all players, register as server for better performance
            pl.registerServerPlaceholder(identifier, () -> PlaceholderAPI.setPlaceholders(null, identifier));
        } else {
            pl.registerPlayerPlaceholder(identifier, p -> PlaceholderAPI.setPlaceholders((Player) p.getPlayer(), identifier));
        }
    }

    /**
     * Registers a sync placeholder with given identifier and automatically decided refresh.
     *
     * @param   identifier
     *          Placeholder identifier
     */
    public void registerSyncPlaceholder(@NotNull String identifier) {
        String syncedPlaceholder = "%" + identifier.substring(6);
        PlayerPlaceholderImpl[] ppl = new PlayerPlaceholderImpl[1];
        ppl[0] = TAB.getInstance().getPlaceholderManager().registerPlayerPlaceholder(identifier, p -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                long time = System.nanoTime();
                ppl[0].updateValue(p, placeholderAPI ? PlaceholderAPI.setPlaceholders((Player) p.getPlayer(), PlaceholderAPI.setBracketPlaceholders((Player) p.getPlayer(), syncedPlaceholder)) : identifier);
                TAB.getInstance().getCPUManager().addPlaceholderTime(identifier, System.nanoTime() - time);
            });
            return null;
        });
    }

    @Override
    public void logInfo(@NotNull TabComponent message) {
        Bukkit.getConsoleSender().sendMessage("[TAB] " + toBukkitFormat(message));
    }

    @Override
    public void logWarn(@NotNull TabComponent message) {
        Bukkit.getConsoleSender().sendMessage("§c[TAB] [WARN] " + toBukkitFormat(message));
    }

    @Override
    @NotNull
    public String getServerVersionInfo() {
        return "[Bukkit] " + Bukkit.getName() + " - " + Bukkit.getBukkitVersion().split("-")[0];
    }

    @Override
    public void registerListener() {
        Bukkit.getPluginManager().registerEvents(new BukkitEventListener(), plugin);
    }

    @Override
    public void registerCommand() {
        PluginCommand command = Bukkit.getPluginCommand(getCommand());
        if (command != null) {
            BukkitTabCommand cmd = new BukkitTabCommand();
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        } else {
            logWarn(SimpleTextComponent.text("Failed to register command, is it defined in plugin.yml?"));
        }
    }

    @Override
    public void startMetrics() {
        Metrics metrics = new Metrics(plugin, TabConstants.BSTATS_PLUGIN_ID_BUKKIT);
        metrics.addCustomChart(new SimplePie(TabConstants.MetricsChart.PERMISSION_SYSTEM,
                () -> TAB.getInstance().getGroupManager().getPermissionPlugin()));
        metrics.addCustomChart(new SimplePie(TabConstants.MetricsChart.SERVER_VERSION,
                () -> "1." + BukkitReflection.getMinorVersion() + ".x"));
    }

    @Override
    @NotNull
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    @NotNull
    public Object convertComponent(@NotNull TabComponent component) {
        if (serverImplementationProvider.getComponentConverter() != null) {
            return serverImplementationProvider.getComponentConverter().convert(component);
        } else {
            return component;
        }
    }

    @Override
    @NotNull
    @SneakyThrows
    public Scoreboard createScoreboard(@NotNull TabPlayer player) {
        Scoreboard scoreboard = viaVersionImplementationProvider.newScoreboard((BukkitTabPlayer) player);
        if (scoreboard != null) return scoreboard;
        return serverImplementationProvider.newScoreboard((BukkitTabPlayer) player);
    }

    @Override
    @NotNull
    public BossBar createBossBar(@NotNull TabPlayer player) {
        //noinspection ConstantValue
        if (AdventureBossBar.isAvailable() && Audience.class.isAssignableFrom(Player.class)) return new AdventureBossBar(player);

        // 1.9+ server, handle using API, potential 1.8 players are handled by ViaVersion
        if (BukkitReflection.getMinorVersion() >= 9) return new BukkitBossBar((BukkitTabPlayer) player);

        // 1.9+ player on 1.8 server, handle using ViaVersion API
        if (player.getVersion().getMinorVersion() >= 9) return new ViaBossBar((BukkitTabPlayer) player);

        // 1.8- server and player, no implementation
        return new DummyBossBar();
    }

    @Override
    @NotNull
    @SneakyThrows
    public TabList createTabList(@NotNull TabPlayer player) {
        TabList tabList = viaVersionImplementationProvider.newTabList((BukkitTabPlayer) player);
        if (tabList != null) return tabList;
        return serverImplementationProvider.newTabList((BukkitTabPlayer) player);
    }

    @Override
    public boolean supportsScoreboards() {
        return true;
    }

    @Override
    public boolean isSafeFromPacketEventsBug() {
        return serverVersion.getMinorVersion() >= 13;
    }

    @Override
    @NotNull
    public GroupManager detectPermissionPlugin() {
        if (LuckPermsHook.getInstance().isInstalled()) {
            return new GroupManager("LuckPerms", LuckPermsHook.getInstance().getGroupFunction());
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Permission> provider = Bukkit.getServicesManager().getRegistration(Permission.class);
            if (provider != null && !provider.getProvider().getName().equals("SuperPerms")) {
                return new GroupManager(provider.getProvider().getName(), p -> provider.getProvider().getPrimaryGroup((Player) p.getPlayer()));
            }
        }
        return new GroupManager("None", p -> TabConstants.NO_GROUP);
    }

    @Override
    public double getTPS() {
        if (recentTps != null) {
            return recentTps[0];
        } else if (paperTps) {
            return Bukkit.getTPS()[0];
        } else {
            return -1;
        }
    }

    @Override
    public double getMSPT() {
        if (paperMspt) return Bukkit.getAverageTickTime();
        return -1;
    }

    /**
     * Runs task in the main thread for given entity.
     *
     * @param   entity
     *          Entity's main thread
     * @param   task
     *          Task to run
     */
    public void runSync(@NotNull Entity entity, @NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Converts component to string using bukkit RGB format if supported by the server.
     * If not, closest legacy color is used instead.
     *
     * @param   component
     *          Component to convert
     * @return  Converted string using bukkit color format
     */
    @NotNull
    public String toBukkitFormat(@NotNull TabComponent component) {
        StringBuilder sb = new StringBuilder();
        if (component.getModifier().getColor() != null) {
            if (serverVersion.supportsRGB()) {
                String hexCode = component.getModifier().getColor().getHexCode();
                sb.append('§').append("x").append('§').append(hexCode.charAt(0)).append('§').append(hexCode.charAt(1))
                        .append('§').append(hexCode.charAt(2)).append('§').append(hexCode.charAt(3))
                        .append('§').append(hexCode.charAt(4)).append('§').append(hexCode.charAt(5));
            } else {
                sb.append('§').append(component.getModifier().getColor().getLegacyColor().getCharacter());
            }
        }
        sb.append(component.getModifier().getMagicCodes());
        if (component instanceof TextComponent) {
            sb.append(((TextComponent) component).getText());
        } else if (component instanceof TranslatableComponent) {
            sb.append(((TranslatableComponent) component).getKey());
        } else if (component instanceof KeybindComponent) {
            sb.append(((KeybindComponent) component).getKeybind());
        } else {
            throw new IllegalStateException("Unexpected component type: " + component.getClass().getName());
        }
        for (TabComponent extra : component.getExtra()) {
            sb.append(toBukkitFormat(extra));
        }
        return sb.toString();
    }

    /**
     * Returns online players from Bukkit API.
     * This method may use reflections, because the return type changed in 1.8,
     * and we want to avoid errors.
     *
     * @return  Online players from Bukkit API.
     */
    @SneakyThrows
    @NotNull
    public Collection<? extends Player> getOnlinePlayers() {
        if (serverVersion.getMinorVersion() >= 8) {
            return Bukkit.getOnlinePlayers();
        }
        return Arrays.asList((Player[]) Bukkit.class.getMethod("getOnlinePlayers").invoke(null));
    }
}