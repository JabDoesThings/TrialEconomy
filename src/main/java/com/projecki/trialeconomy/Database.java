package com.projecki.trialeconomy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.UUID;

public class Database {

  private final MySQLCredentials credentials;
  private Connection conn;

  private static final String TABLE_PROFILES = "economy_trial_players";

  Database(@NotNull MySQLCredentials credentials) {
    this.credentials = credentials;
  }

  void connect() throws SQLException {
    this.conn = credentials.newConnection();
    setup();
  }

  void disconnect() throws SQLException {
    this.conn.close();
  }

  private void setup() throws SQLException {

    String sql =
        "CREATE TABLE IF NOT EXISTS `"
            + TABLE_PROFILES
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

  boolean isConnected() throws SQLException {
    return conn != null && !conn.isClosed();
  }

  @NotNull
  public EconomyProfile getOrCreateProfile(@NotNull Player player) throws SQLException {

    String sql = "SELECT * FROM " + TABLE_PROFILES + " WHERE player_id = ?;";

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

    if (isNew) {

      sql = "INSERT INTO " + TABLE_PROFILES + " (player_id, balance) VALUES (?,?);";

      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setBytes(1, playerIdBytes);
        statement.setDouble(2, balance);
        statement.executeUpdate();
      }

      TrialEconomy.logger.info("Created EconomyProfile for player '" + player.getName() + "'.");
    }

    return new EconomyProfile(player, balance);
  }

  void save(@NotNull EconomyProfile profile) throws SQLException {

    if (!profile.isDirty()) return;

    UUID playerId = profile.getPlayerId();
    String playerName = profile.getPlayerName();
    byte[] playerIdBytes = toBytes(playerId);

    String sql = "UPDATE " + TABLE_PROFILES + " SET balance=? WHERE player_id = ?;";

    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setDouble(1, profile.getBalance());
      statement.setBytes(2, playerIdBytes);
      int rowsAffected = statement.executeUpdate();
      if (rowsAffected == 0) {
        throw new SQLException("EconomyProfile does not exist for player: " + playerName);
      }
    }

    profile.setDirty(false);

    TrialEconomy.logger.info("Saved EconomyProfile for player: " + playerName);
  }

  @Nullable
  public EconomyProfile getProfile(@NotNull OfflinePlayer player) throws SQLException {

    EconomyProfile profile = null;

    String sql = "SELECT * FROM " + TABLE_PROFILES + " WHERE player_id = ?;";

    UUID playerId = player.getUniqueId();
    byte[] playerIdBytes = toBytes(playerId);

    try (PreparedStatement statement = conn.prepareStatement(sql)) {

      statement.setBytes(1, playerIdBytes);

      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        profile = new EconomyProfile(player, resultSet.getDouble(2));
      }

      resultSet.close();
    }

    return profile;
  }

  public boolean hasProfile(@NotNull UUID playerId) throws SQLException {

    boolean found;
    byte[] playerIdBytes = toBytes(playerId);

    String sql = "SELECT COUNT(*) from " + TABLE_PROFILES + " WHERE player_id=?;";

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
   * Packages a UUID as a byte array of 16 in length.
   *
   * @param uuid The UUID to convert.
   * @return A byte array of 16 in length.
   * @throws NullPointerException Thrown if the uuid is null.
   */
  public static byte[] toBytes(@NotNull UUID uuid) {

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
  public static byte[] toBytes(long value) {

    byte[] bytes = new byte[8];

    for (int offset = 0; offset < bytes.length; offset++) {
      bytes[offset] = Long.valueOf(value & 0xff).byteValue();
      value = value >> 8;
    }

    return bytes;
  }
}
