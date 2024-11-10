# Content
* [Requirements](#requirements)
* [Supported server software and versions](#supported-server-software-and-versions)
* [Supported features per platform](#supported-features-per-platform)
* [Plugin hooks](#plugin-hooks)
  * [All platforms](#all-platforms)
  * [Bukkit](#bukkit)
  * [BungeeCord](#bungeecord)
  * [Fabric](#fabric)
  * [Sponge](#sponge)
  * [Velocity](#velocity)
* [Compatibility issues](#compatibility-issues)

# Requirements
TAB does not depend on any other plugins.  
It is compiled with Java 8 and therefore supports any version of 8 and above.

# Supported server software and versions
<table>
    <thead>
        <tr>
            <th>Software type</th>
            <th>Software name</th>
            <th>Supported versions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td rowspan=2>Vanilla</td>
            <td rowspan=1><a href="https://getbukkit.org/">Bukkit</a> (+forge hybrids)</td>
            <td>✔ (1.5 - 1.21.3)</td>
        </tr>
        <tr>
            <td rowspan=1><a href="https://www.spongepowered.org/">Sponge</a></td>
            <td>✔ (1.9<sup>1</sup> - 1.21.3)</td>
        </tr>
    </tbody>
    <tbody>
        <tr>
            <td rowspan=2>Modded</td>
            <td rowspan=1><a href="https://fabricmc.net">Fabric</a></td>
            <td>✔ (1.14 - 1.21.3)</td>
        </tr>
        <tr>
            <td rowspan=1><a href="https://minecraftforge.net">Forge</a></td>
            <td>❌</td>
        </tr>
    </tbody>
    <tbody>
        <tr>
            <td rowspan=2>Proxies</td>
            <td rowspan=1><a href="https://ci.md-5.net/job/BungeeCord/">BungeeCord</a></td>
            <td>✔ (latest only<sup>2</sup>)</td>
        </tr>
        <tr>
            <td rowspan=1><a href="https://www.velocitypowered.com/">Velocity</a></td>
            <td>✔ (latest only<sup>2</sup>)</td>
        </tr>
    </tbody>
</table>
<sup>1</sup> Due to a bug in Sponge 7, it cannot coexist in the same jar with classes compiled with Java higher than 8, which is required by some other platforms. To get the plugin to run on Sponge 7, you will need to do one of the following:  

* Compile the plugin yourself and use the jar from `sponge7/build/libs`, which only contains sponge support instead of the universal jar from `jar/build/libs`.
* Unzip the plugin jar, go to `me/neznamy/tab/platforms/` and delete everything except `sponge7`.

<sup>2</sup> Latest only doesn't mean only the latest build will work, it means the plugin was made to be compatible with latest version/build. Since breaking changes don't happen too often, it means a wide range of versions is usually supported. When a breaking change occurs, plugin is updated to support new version, automatically making old versions incompatible. Since proxies support all client versions, there is never a reason to stay outdated, so you can always safely update to new version version/build of your proxy software if plugin requires it.

# Supported features per platform
| Feature \ Platform | Bukkit / Hybrid | Sponge | Fabric | BungeeCord | Velocity |
| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |
| [Belowname](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Belowname) | ✔ | ✔ | ✔ | ✔ | ✔ (via [VSAPI](https://github.com/NEZNAMY/VelocityScoreboardAPI/releases)) |
| [BossBar](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Bossbar) | ✔ | ✔ | ✔ | ✔ (1.9+) | ✔ (1.9+) |
| [Global Playerlist](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Global-playerlist) | ❌ | ❌ | ❌ | ✔ | ✔ |
| [Header/Footer](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Header-&-Footer) | ✔ | ✔ | ✔ | ✔ | ✔ |
| [Layout](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Layout) | ✔ | ✔ | ✔ | ✔ | ✔ |
| [Nametags](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Nametags) & [Sorting](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Sorting-players-in-tablist) | ✔ |  ❗ | ✔ | ✔ | ✔ (via [VSAPI](https://github.com/NEZNAMY/VelocityScoreboardAPI/releases)) |
| [Per world playerlist](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Per-world-playerlist) | ✔ | ❌ | ❌ | ❌ | ❌ |
| [Playerlist objective](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Playerlist-Objective) | ✔ | ✔ | ✔ | ✔ | ✔ (via [VSAPI](https://github.com/NEZNAMY/VelocityScoreboardAPI/releases)) |
| [Scoreboard](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Scoreboard) | ✔ | ❗ | ✔ | ✔ | ✔ (via [VSAPI](https://github.com/NEZNAMY/VelocityScoreboardAPI/releases)) |
| [Spectator fix](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Spectator-fix) | ✔ | ✔ | ✔ | ✔ | ✔ |
| [Tablist names](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Tablist-name-formatting) | ✔ | ✔ | ✔ | ✔ | ✔ |

✔ = Fully functional  
 ❗ = NameTags: Anti-override missing, Scoreboard: [compatibility with other plugins](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Scoreboard#compatibility-with-other-plugins) missing.  
❌ = Completely missing

# Plugin hooks
In order to enhance user experience, TAB hooks into other plugins for better experience. This set of plugins is different for each platform based on their availability. Some are available on all platforms, some only in a few.
## All platforms
[**Floodgate**](https://github.com/GeyserMC/Floodgate) - For properly detecting bedrock players to adapt features for best possible user experience.  
[**LuckPerms**](https://github.com/LuckPerms/LuckPerms) - Detecting permission groups of players for per-group settings.  
[**ViaVersion**](https://github.com/ViaVersion/ViaVersion) - For properly detecting player version to adapt features for best possible user experience.

## Bukkit
[**LibsDisguises**](https://github.com/libraryaddict/LibsDisguises) - Detecting disguised players to disable collision to avoid endless push by colliding with own copy created by LibsDisguises.  
[**PlaceholderAPI**](https://github.com/PlaceholderAPI/PlaceholderAPI) - Allows users to use its placeholders inside TAB.  
[**PremiumVanish**](https://www.spigotmc.org/resources/14404) - Supporting PV's vanish levels instead of using a basic vanish compatibility system.  
[**Vault**](https://github.com/milkbowl/Vault) - Detecting permission groups of players for per-group settings.

## BungeeCord
[**PremiumVanish**](https://www.spigotmc.org/resources/14404) - Supporting PV's vanish levels instead of using a basic vanish compatibility system.  
[**RedisBungee**](https://github.com/ProxioDev/RedisBungee) - Communicating with other proxies to properly display visuals on players on another proxy.

## Fabric
[**fabric-permissions-api**](https://github.com/lucko/fabric-permissions-api) - Supporting permission nodes instead of OP levels.

## Sponge
*None*

## Velocity
[**PremiumVanish**](https://www.spigotmc.org/resources/14404) - Supporting PV's vanish levels instead of using a basic vanish compatibility system.  
[**RedisBungee**](https://github.com/ProxioDev/RedisBungee) - Communicating with other proxies to properly display visuals on players on another proxy.  
[**VelocityScoreboardAPI**](https://github.com/NEZNAMY/VelocityScoreboardAPI) - Sending scoreboard packets (scoreboard-teams, belowname-objective, playerlist-objective, scoreboard)

# Compatibility issues
* **Glow plugins** will fail to apply glow color correctly. Check [How to make the plugin compatible with glow plugins](https://github.com/NEZNAMY/TAB/wiki/How-to-make-TAB-compatible-with-glow-plugins) for more information.
* **[Tablisknu](https://forums.skunity.com/resources/tablisknu.727/)** (skript addon) prevents TAB from assigning teams (sorting & nametags).
* **SkBee** (skript addon) sends empty scoreboard, causing TAB's to not show sometimes.
* **Waterfall**'s `disable_tab_list_rewrite: true` **may** cause tablist to use offline UUIDs while TAB expects online uuids, causing various problems (most notably tablist formatting not working). Checking for this option is not an option either, because tablist rewrite might still be enabled despite being disabled (don't ask how, I have no idea). Set the option to `false` if you are experiencing issues.
* **ViaVersion on BungeeCord and TAB on backend** acts like a client-sided protocol hack, making it impossible for TAB to know player's real version and causing issues related to it, see [Per-version experience](https://github.com/NEZNAMY/TAB/wiki/Additional-information#per-version-experience) for more info. Just avoid this combination - either install ViaVersion on all backend servers instead or install TAB on BungeeCord instead.
* **Custom clients / resource packs** - Unofficially modified minecraft clients often tend to break things. Just Lunar client has 3 bugs that can be reproduced with TAB. Resource packs may also contain modifications you are not aware of, making things not look the way you want them to. If you are experiencing any visual issue and are using a custom client or resource pack, try it with clean vanilla client. If it works there, it's an issue with the client / resource pack and TAB cannot do anything about it. Here are a few bugs in LunarClient / FeatherClient that you may run into when using TAB:
  * They add their icon to players in tablist, but don't widen the entries. This results in player names overlapping with latency bar. You can avoid this by configuring some spaces in tabsuffix.
  * They don't support color field in chat components, which means they don't support RGB codes and will display bossbar without colors as well.
  * Bossbar is not visible on 1.8.
  * Rendering belowname 3 times.
  * They don't respect nametag visibility rule, showing own nametag using F5 even if set to invisible by the plugin.
  * When scoreboard is set to use all 0s, lines are rendered in opposite order on 1.20.3+.
  * They don't display 1.20.3+ NumberFormat feature in scoreboard objectives.
* **Random Spigot/BungeeCord forks** - All safe patches for improving security & performance are present in well-known public opensource projects, such as Paper, Purpur or Waterfall. Using a random overpriced closed-source (and probably obfuscated) fork from BuiltByBit may not be safe, since they likely include unsafe patches that may break compatibility with plugins in an attempt to fix things that are not broken. Before spending your entire budget on such fork, reconsider it first. Paper (and its forks) is a performance-oriented fork used by 2/3 of all MC servers worldwide, while the rest is still stuck on Spigot. It is highly unlikely your needs are so specific you need every single "improvement" anyone can come up with. If you need a feature, Purpur is a feature-oriented fork. Together with plugins you should achieve what you are looking for.