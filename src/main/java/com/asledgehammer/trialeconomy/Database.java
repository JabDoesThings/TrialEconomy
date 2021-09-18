package com.asledgehammer.trialeconomy;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import java.sql.*;
import java.util.Objects;
import java.util.UUID;

/**
 * <b>Database</b> handles all MySQL transactions & queries for TrialEconomy.
 *
 * @author Jab
 */
class Database {

  private static final String TABLE_ACCOUNTS = "trial_economy_accounts";

  /** The credentials used to connect to the remote MySQL database. */
  private final Credentials credentials;

  /** The MySQL connection instance. */
  private Connection conn;

  /**
   * @param credentials The credentials used to connect to the remote MySQL database.
   * @throws NullPointerException Thrown if the credentials is null.
   */
  Database(@NotNull Credentials credentials) {
    this.credentials = credentials;
  }

  /**
   * Packages a UUID as a byte array of 16 in length.
   *
   * @param uuid The UUID to convert.
   * @return A byte array of 16 in length.
   * @throws NullPointerException Thrown if the uuid is null.
   */
  private static byte[] toBytes(@NotNull UUID uuid) {

    byte[] first = toBytes(uuid.getMostSignificantBits());
    byte[] second = toBytes(uuid.getLeastSignificantBits());

    byte[] bytes = new byte[16];

    System.arraycopy(first, 0, bytes, 0, 8);
    System.arraycopy(second, 0, bytes, 8, 8);

    return bytes;
  }

  /**
   * Unpacks a long to an array of 8 bytes.
   *
   * @param value The long to unpack.
   * @return An array of 8 bytes.
   */
  private static byte[] toBytes(long value) {

    byte[] bytes = new byte[8];

    for (int offset = 0; offset < bytes.length; offset++) {
      bytes[offset] = Long.valueOf(value & 0xff).byteValue();
      value = value >> 8;
    }

    return bytes;
  }

  /**
   * Connects to the MySQL database.
   *
   * @throws SQLException Thrown if an exception occurs while connecting to the remote MySQL
   *     database.
   */
  void connect() throws SQLException {
    this.conn = credentials.newConnection();
    setup();
  }

  /**
   * Disconnects from the MySQL database.
   *
   * @throws SQLException Thrown if an exception occurs while disconnecting from the remote MySQL
   *     database.
   */
  void disconnect() throws SQLException {
    this.conn.close();
  }

  private void setup() throws SQLException {

    String sql =
        "CREATE TABLE IF NOT EXISTS `"
            + TABLE_ACCOUNTS
            + "`("
            + "`player_id` varbinary(16) NOT NULL,"
            + "`balance` double NOT NULL,"
            + "PRIMARY KEY (`player_id`) USING BTREE,"
            + "UNIQUE INDEX `player_id` (`player_id`) USING BTREE"
            + ") ENGINE=InnoDB";

    try (Statement statement = conn.createStatement()) {
      statement.execute(sql);
    }
  }

  /**
   * @return Returns true if connected to the remote MySQL database.
   * @throws SQLException Thrown if an exception occurs while checking the closure stature of the
   *     connection instance.
   */
  boolean isConnected() throws SQLException {
    return conn != null && !conn.isClosed();
  }

  /**
   * Gets an account for a player. If the player doesn't have an account, one is created and saved
   * to the remote MySQL database.
   *
   * @param player The player associated with the account.
   * @return The player's account.
   * @throws NullPointerException Thrown if the player is null.
   * @throws SQLException Thrown if an exception occurs while performing MySQL transactions &
   *     queries.
   */
  @NotNull
  PlayerAccount getOrCreateAccount(@NotNull Player player) throws SQLException {

    String sql = "SELECT * FROM " + TABLE_ACCOUNTS + " WHERE player_id = ?;";

    double balance = 0.0;
    boolean isNew = true;
    UUID playerId = player.getUniqueId();
    byte[] playerIdBytes = toBytes(playerId);

    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setBytes(1, playerIdBytes);

      ResultSet resultSet = statement.executeQuery();
      if (resultSet.next()) {
        isNew = false;
        balance = resultSet.getDouble(2);
      }

      resultSet.close();
    }

    // If the account is new, insert to the database.
    if (isNew) {

      sql = "INSERT INTO " + TABLE_ACCOUNTS + " (player_id, balance) VALUES (?,?);";

      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setBytes(1, playerIdBytes);
        statement.setDouble(2, balance);
        statement.executeUpdate();
      }

      TrialEconomy.logger.info("Created account for player '" + player.getName() + "'.");
    }

    return new PlayerAccount(player, balance);
  }

  /**
   * Saves an account.
   *
   * @param account The account to save.
   * @throws NullPointerException Thrown if the account is null.
   * @throws SQLException Thrown if an exception occurs while performing MySQL transactions.
   */
  void save(@NotNull PlayerAccount account) throws SQLException {

    if (!account.isDirty()) return;

    UUID playerId = account.getPlayerId();
    String playerName = account.getPlayerName();
    byte[] playerIdBytes = toBytes(playerId);

    String sql = "UPDATE " + TABLE_ACCOUNTS + " SET balance=? WHERE player_id = ?;";

    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setDouble(1, account.getBalance());
      statement.setBytes(2, playerIdBytes);
      int rowsAffected = statement.executeUpdate();
      if (rowsAffected == 0) {
        throw new SQLException("Account does not exist for player: " + playerName);
      }
    }

    account.setDirty(false);

    TrialEconomy.logger.info("Saved account for player: " + playerName);
  }

  /**
   * Gets an account for a player.
   *
   * @param player The player associated with the account.
   * @return The account. If one doesn't exist for the player, null is returned.
   * @throws NullPointerException Thrown if the player is null.
   * @throws SQLException Thrown if an exception occurs while performing MySQL queries.
   */
  @Nullable
  PlayerAccount getAccount(@NotNull OfflinePlayer player) throws SQLException {

    PlayerAccount account = null;

    String sql = "SELECT * FROM " + TABLE_ACCOUNTS + " WHERE player_id = ?;";

    UUID playerId = player.getUniqueId();
    byte[] playerIdBytes = toBytes(playerId);

    try (PreparedStatement statement = conn.prepareStatement(sql)) {

      statement.setBytes(1, playerIdBytes);

      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        account = new PlayerAccount(player, resultSet.getDouble(2));
      }

      resultSet.close();
    }

    return account;
  }

  /**
   * @param playerId The player ID to test.
   * @return Returns true if the account exists on the remote MySQL database.
   * @throws NullPointerException Thrown if the playerId is null.
   * @throws SQLException Thrown if an exception occurs while performing MySQL queries.
   */
  boolean hasAccount(@NotNull UUID playerId) throws SQLException {

    boolean found;
    byte[] playerIdBytes = toBytes(playerId);

    String sql = "SELECT COUNT(*) from " + TABLE_ACCOUNTS + " WHERE player_id=?;";

    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setBytes(1, playerIdBytes);
      ResultSet resultSet = statement.executeQuery();
      resultSet.next();
      found = resultSet.getInt(1) == 1;
      resultSet.close();
    }

    return found;
  }

  /**
   * <b>Credentials</b> handles building JDBC URLs {@literal &} creating MySQL connections.
   *
   * @author Jab
   */
  static class Credentials {

    /** The host URL for the service. (E.G: localhost, IP, domain, etc.) */
    @Getter private final String host;

    /** The port the service is listening on. */
    @Getter private final int port;

    /** The username of the database account. */
    @Getter private final String username;

    /** The password for authenticating with the service. */
    private final String password;

    /** The database to connect to. */
    @Getter private final String database;

    /** The compiled JDBC URL to connect to both connection instances and pooled API. */
    @Getter private final String jdbcUrl;

    Credentials(@NotNull ConfigurationSection cfg) {

      this.host = getString(cfg, "host");
      this.port = getUnsignedShort(cfg, "port");
      this.username = getString(cfg, "username");
      this.password = getString(cfg, "password");
      this.database = getString(cfg, "database");

      if (port == 0) throw new YAMLException("The field 'port' is zero.");

      this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
    }

    /**
     * @param cfg The cfg section storing the field.
     * @param field The field storing the string value.
     * @return The string value.
     * @throws NullPointerException Thrown if the cfg or field are null.
     * @throws YAMLException Thrown if the field does not exist, is not a string, or is an empty
     *     string.
     */
    @NotNull
    private static String getString(@NotNull ConfigurationSection cfg, @NotNull String field) {

      if (field.isEmpty()) throw new IllegalArgumentException("The field is empty.");

      if (!cfg.contains(field)) {
        throw new YAMLException("The field '" + field + "' is not found.");
      } else if (!cfg.isString(field)) {
        throw new YAMLException("The field '" + field + "' is not a string.");
      }

      String value = Objects.requireNonNull(cfg.getString(field));
      if (value.isEmpty()) {
        throw new YAMLException("The field '" + field + "' is empty.");
      }

      return value;
    }

    private static int getUnsignedShort(@NotNull ConfigurationSection cfg, @NotNull String field) {

      if (field.isEmpty()) throw new IllegalArgumentException("The field is empty.");

      if (!cfg.contains(field)) {
        throw new YAMLException("The field '" + field + "' is not found.");
      } else if (!cfg.isInt(field)) {
        throw new YAMLException("The field '" + field + "' is not a valid short.");
      }

      int value = cfg.getInt(field);
      if (value < 0 || value > 65535) {
        throw new YAMLException(
            "The field '"
                + field
                + "' is out of range. Must be between 0 and 65535. (given: "
                + value
                + ")");
      }

      return value;
    }

    /**
     * @return A new connection instance for the SQL database.
     * @throws SQLException Thrown if the attempt to establish a connection to the SQL database
     *     fails.
     */
    @NotNull
    Connection newConnection() throws SQLException {
      return DriverManager.getConnection(jdbcUrl, username, password);
    }
  }
}
