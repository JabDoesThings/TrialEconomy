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
 * <b>TrialEconomy</b> is the API accessor and JavaPlugin class for TrialEconomy.
 *
 * @author Jab
 */
public final class TrialEconomy extends JavaPlugin implements Listener {

  public static TrialEconomy INSTANCE;
  static Logger logger;

  /** All online-player accounts are stored here. */
  private final Map<UUID, PlayerAccount> accounts = new HashMap<>();

  /** The internal database management. */
  @Getter(AccessLevel.PACKAGE)
  private Database database;

  /** All dialog for the plugin. */
  @Getter private Dialog dialog;

  /**
   * Gets the account for a player.
   *
   * <p><b>NOTE:</b> Use {@link TrialEconomy#hasAccount(UUID)} to check if the player has an account
   * before using this method.
   *
   * @param offlinePlayer The player associated with the account.
   * @return The account of the player.
   * @throws NullPointerException Thrown if the player doesn't have an account.
   */
  @NotNull
  public static PlayerAccount getAccount(@NotNull OfflinePlayer offlinePlayer) {

    PlayerAccount account = INSTANCE.accounts.get(offlinePlayer.getUniqueId());

    if (account != null) return account;

    try {

      account = INSTANCE.database.getAccount(offlinePlayer);

      if (account == null) {
        throw new NullPointerException(
            "No account exists for the player: " + offlinePlayer.getName());
      }

      return account;

    } catch (SQLException e) {
      INSTANCE.disable("A MySQL error occurred.");
      e.printStackTrace();
    }

    //noinspection ConstantConditions
    return null;
  }

  /**
   * @param playerId The player ID to test.
   * @return Returns true if the player has an account.
   */
  public static boolean hasAccount(@NotNull UUID playerId) {

    try {
      return INSTANCE.accounts.containsKey(playerId) || INSTANCE.database.hasAccount(playerId);
    } catch (SQLException e) {
      INSTANCE.disable("A MySQL error occurred.");
      e.printStackTrace();
    }

    return false;
  }

  @Override
  public void onEnable() {

    INSTANCE = this;
    logger = getLogger();

    if (!loadDialog()) return;

    Database.Credentials credentials = getCredentials();
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

    if (!accounts.isEmpty()) {

      // If any accounts are loaded, save them.
      for (UUID playerId : accounts.keySet()) {

        PlayerAccount account = accounts.get(playerId);

        try {
          database.save(account);
        } catch (SQLException e) {
          logger.warning("Failed to save account for player: " + account.getPlayerName());
          e.printStackTrace(System.err);
        }
      }

      accounts.clear();
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
  private Database.Credentials getCredentials() {

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
    return new Database.Credentials(cfgMySQLCredentials);
  }

  private boolean loadDialog() {

    File folderDialog = new File(getDataFolder(), "dialog");
    if (!folderDialog.exists() && !folderDialog.mkdirs()) {
      disable("Failed to create directory: " + folderDialog.getPath());
      return false;
    }

    File fileDialogTrialEconomyEn = new File(folderDialog, "trial_economy_en.yml");
    if (!fileDialogTrialEconomyEn.exists()) {
      saveResource("dialog/trial_economy_en.yml", false);
    }

    ConfigurationSection cfgDialogEn =
        YamlConfiguration.loadConfiguration(fileDialogTrialEconomyEn);

    dialog = new Dialog(cfgDialogEn);
    return true;
  }

  private void load(@NotNull Player player) {

    PlayerAccount account = null;

    try {
      account = database.getOrCreateAccount(player);
    } catch (SQLException e) {
      e.printStackTrace(System.err);
      disable("A MySQL error occurred.");
    }

    accounts.put(player.getUniqueId(), account);
  }

  private void save(@NotNull UUID playerId) {

    if (!accounts.containsKey(playerId)) return;

    PlayerAccount account = accounts.remove(playerId);

    try {
      database.save(account);
    } catch (SQLException e) {
      e.printStackTrace(System.err);
      disable("A MySQL error occurred.");
    }
  }

  void save(@NotNull PlayerAccount account) {
    try {
      database.save(account);
    } catch (SQLException e) {
      INSTANCE.disable("A MySQL error occurred.");
      e.printStackTrace();
    }
  }

  private void disable(@Nullable String message) {
    if (message != null) logger.warning(message);
    getPluginLoader().disablePlugin(this);
  }
}
