package com.projecki.trialeconomy;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * <b>TrialEconomy</b> is the API accessor for the player-economy plugin.
 *
 * @author Jab
 */
public final class TrialEconomy extends JavaPlugin implements Listener {

  public static TrialEconomy INSTANCE;
  public static Logger logger;

  private final Map<UUID, EconomyProfile> profiles = new HashMap<>();

  @Getter(AccessLevel.PACKAGE)
  private Database database;

  @Getter private Dialog dialog;

  @Override
  public void onEnable() {

    INSTANCE = this;
    logger = getLogger();

    if (!loadDialog()) return;

    MySQLCredentials credentials = getCredentials();
    if (credentials == null) return;

    database = new Database(credentials);

    try {
      database.connect();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    BalanceCommand balanceCommand = new BalanceCommand();
    PluginCommand commandBalance = Objects.requireNonNull(getCommand("balance"));
    commandBalance.setExecutor(balanceCommand);
    commandBalance.setTabCompleter(balanceCommand);

    Bukkit.getPluginManager().registerEvents(this, this);

    // Load any online players if the plugin is reloaded.
    for (Player player : Bukkit.getOnlinePlayers()) {
      load(player);
    }
  }

  @Override
  public void onDisable() {

    if (!profiles.isEmpty()) {

      // If any profiles are loaded, save them.
      for (UUID playerId : profiles.keySet()) {

        EconomyProfile profile = profiles.get(playerId);

        try {
          database.save(profile);
        } catch (SQLException e) {
          logger.warning("Failed to save EconomyProfile for player: " + profile.getPlayerName());
          e.printStackTrace(System.err);
        }
      }

      profiles.clear();
    }

    if (database != null) {
      try {
        if (database.isConnected()) database.disconnect();
      } catch (SQLException e) {
        logger.warning("Failed to shut down database.");
        e.printStackTrace(System.err);
      }
      database = null;
    }
  }

  @EventHandler
  void on(PlayerJoinEvent event) {
    load(event.getPlayer());
  }

  @EventHandler
  void on(PlayerQuitEvent event) {
    save(event.getPlayer().getUniqueId());
  }

  @Nullable
  private MySQLCredentials getCredentials() {

    File fileCredentials = new File(getDataFolder(), "credentials.yml");

    // Generate credentials.yml if not present and let the console know it needs to be filled out.
    if (!fileCredentials.exists()) {
      saveResource("credentials.yml", false);
      logger.warning("Created credentials.yml. Configure this file before running TrialEconomy.");
      getPluginLoader().disablePlugin(this);
      return null;
    }

    ConfigurationSection cfgCredentials = YamlConfiguration.loadConfiguration(fileCredentials);
    if (!cfgCredentials.contains("mysql")) {
      throw new YAMLException("The section 'mysql' in credentials.yml doesn't exist.");
    } else if (!cfgCredentials.isConfigurationSection("mysql")) {
      throw new YAMLException("The field 'mysql' in credentials.yml isn't a section.");
    }

    ConfigurationSection cfgMySQLCredentials =
        Objects.requireNonNull(cfgCredentials.getConfigurationSection("mysql"));
    return new MySQLCredentials(cfgMySQLCredentials);
  }

  private boolean loadDialog() {

    File folderDialog = new File(getDataFolder(), "dialog");
    if (!folderDialog.exists() && !folderDialog.mkdirs()) {
      disable("Failed to create directory: " + folderDialog.getPath());
      return false;
    }

    File fileDialogTrialEconomyEn = new File(folderDialog, "trialeconomy_en.yml");
    if (!fileDialogTrialEconomyEn.exists()) {
      saveResource("dialog/trialeconomy_en.yml", false);
    }

    ConfigurationSection cfgDialogEn =
        YamlConfiguration.loadConfiguration(fileDialogTrialEconomyEn);

    dialog = new Dialog(cfgDialogEn);
    return true;
  }

  private void load(@NotNull Player player) {

    EconomyProfile profile = null;

    try {
      profile = database.getOrCreateProfile(player);
    } catch (SQLException e) {
      e.printStackTrace(System.err);
      disable("A MySQL error occurred.");
    }

    profiles.put(player.getUniqueId(), profile);
  }

  private void save(@NotNull UUID playerId) {

    if (!profiles.containsKey(playerId)) return;

    EconomyProfile profile = profiles.remove(playerId);

    try {
      database.save(profile);
    } catch (SQLException e) {
      e.printStackTrace(System.err);
      disable("A MySQL error occurred.");
    }
  }

  public void save(@NotNull EconomyProfile profile) {
    try {
      database.save(profile);
    } catch (SQLException e) {
      INSTANCE.disable("A MySQL error occurred.");
      e.printStackTrace();
    }
  }

  private void disable(@Nullable String message) {
    if (message != null) logger.warning(message);
    getPluginLoader().disablePlugin(this);
  }

  @NotNull
  public static EconomyProfile getProfile(@NotNull OfflinePlayer offlinePlayer) {

    try {

      EconomyProfile profile = INSTANCE.database.getProfile(offlinePlayer);

      if (profile == null) {
        throw new NullPointerException(
            "No EconomyProfile exists for the player: " + offlinePlayer.getName());
      }

      return profile;

    } catch (SQLException e) {
      INSTANCE.disable("A MySQL error occurred.");
      e.printStackTrace();
    }

    //noinspection ConstantConditions
    return null;
  }

  public static boolean hasProfile(@NotNull UUID playerId) {

    try {
      return INSTANCE.database.hasProfile(playerId);
    } catch (SQLException e) {
      INSTANCE.disable("A MySQL error occurred.");
      e.printStackTrace();
    }

    return false;
  }
}
