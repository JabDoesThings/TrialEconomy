package com.projecki.trialeconomy;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * <b>MySQLCredentials</b> handles building JDBC URLs {@literal &} creating MySQL connections.
 *
 * @author Jab
 */
class MySQLCredentials {

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

  MySQLCredentials(@NotNull ConfigurationSection cfg) {

    this.host = getString(cfg, "host");
    this.port = getUnsignedShort(cfg, "port");
    this.username = getString(cfg, "username");
    this.password = getString(cfg, "password");
    this.database = getString(cfg, "database");

    if (port == 0) throw new YAMLException("The field 'port' is zero.");

    this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
  }

  /**
   * @return A new connection instance for the SQL database.
   * @throws SQLException Thrown if the attempt to establish a connection to the SQL database fails.
   */
  @NotNull
  Connection newConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl, username, password);
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
}
