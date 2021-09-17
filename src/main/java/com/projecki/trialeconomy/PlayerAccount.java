package com.projecki.trialeconomy;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * <b>PlayerAccount</b> contains all data for player accounts in TrialEconomy.
 *
 * @author Jab
 */
@Getter
public class PlayerAccount {

  /** The player's unique ID. */
  private final UUID playerId;

  /** The name of the player. */
  private final String playerName;

  /** The balance of the account. */
  private double balance;

  /** If true, the account can save. */
  @Setter(AccessLevel.PACKAGE)
  private boolean dirty = false;

  /**
   * @param player the player associated with the account.
   * @throws NullPointerException Thrown if the player is null.
   */
  PlayerAccount(@NotNull OfflinePlayer player, double balance) {
    this.playerId = player.getUniqueId();
    this.playerName = player.getName();
    this.balance = balance;
  }

  /**
   * Saves the account to the remote MySQL database.
   *
   * <p><b>NOTE:</b> If the account isn't dirty, nothing will occur.
   */
  public void save() {

    if (!dirty) return;

    TrialEconomy.INSTANCE.save(this);
  }

  /**
   * Deposits an amount to the account.
   *
   * @param amount The amount to deposit.
   * @throws IllegalArgumentException Thrown if the amount is negative.
   */
  public void deposit(double amount) {

    if (amount < 0) {
      throw new IllegalArgumentException("The amount cannot be negative. (" + amount + " given)");
    }

    balance += amount;
    dirty = true;
  }

  /**
   * Withdraws an amount to the account.
   *
   * @param amount The amount to withdraw.
   * @throws IllegalArgumentException Thrown if the amount is negative or more than the current
   *     balance.
   */
  public void withdraw(double amount) {

    if (amount < 0) {
      throw new IllegalArgumentException("The amount cannot be negative. (" + amount + " given)");
    } else if (amount > balance) {
      throw new IllegalArgumentException(
          "Cannot withdraw " + amount + " from " + playerName + "'s account. (insufficient funds)");
    }

    balance -= amount;
    dirty = true;
  }

  /**
   * @param amount The balance to set.
   * @throws IllegalArgumentException Thrown if the amount is negative.
   */
  public void setBalance(double amount) {

    if (this.balance == amount) {
      return;
    } else if (amount < 0) {
      throw new IllegalArgumentException("The amount cannot be negative. (" + amount + " given)");
    }

    this.balance = amount;
    this.dirty = true;
  }

  /**
   * @param amount The amount to test.
   * @return Returns true if the amount is <= the balance of the account.
   */
  public boolean has(double amount) {
    return amount <= balance;
  }
}
