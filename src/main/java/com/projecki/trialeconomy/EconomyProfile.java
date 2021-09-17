package com.projecki.trialeconomy;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class EconomyProfile {

  private final UUID playerId;
  private final String playerName;
  private double balance;

  @Setter(AccessLevel.PACKAGE)
  private boolean dirty = false;

  /**
   * @param player the player associated with the profile.
   * @throws NullPointerException Thrown if the player is null.
   */
  EconomyProfile(@NotNull OfflinePlayer player, double balance) {
    this.playerId = player.getUniqueId();
    this.playerName = player.getName();
    this.balance = balance;
  }

  public void save() {

    if (!dirty) return;

    TrialEconomy.INSTANCE.save(this);
  }

  public void deposit(double amount) {

    if (amount < 0) {
      throw new IllegalArgumentException("The amount cannot be negative. (" + amount + " given)");
    }

    balance += amount;
    dirty = true;
  }

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

  public void setBalance(double balance) {

    if (this.balance == balance) return;

    this.balance = balance;
    this.dirty = true;
  }

  public boolean has(double amount) {
    return amount <= balance;
  }
}
