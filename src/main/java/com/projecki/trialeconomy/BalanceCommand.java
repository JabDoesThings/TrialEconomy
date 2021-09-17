package com.projecki.trialeconomy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>BalanceCommand</b> handles the 'balance' command and sub-commands for TrialEconomy.
 *
 * @author Jab
 */
public class BalanceCommand implements CommandExecutor, TabCompleter {

  @Override
  public boolean onCommand(
      @NotNull CommandSender commander,
      @NotNull Command c,
      @NotNull String l,
      @NotNull String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length == 0) {
      commander.sendMessage(dialog.get("command_help"));
      return true;
    }

    String firstArg = args[0].toLowerCase();

    switch (firstArg) {
      case "deposit" -> onDepositCommand(commander, args);
      case "set" -> onSetCommand(commander, args);
      case "report" -> onReportCommand(commander, args);
      case "withdraw" -> onWithdrawCommand(commander, args);
      default -> {
        commander.sendMessage(dialog.get("command_help"));
        return true;
      }
    }

    return true;
  }

  @Nullable
  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command c,
      @NotNull String l,
      @NotNull String[] args) {

    List<String> tabs = new ArrayList<>();

    switch (args.length) {
      case 1 -> {
        if ("deposit".contains(args[0])) tabs.add("deposit");
        if ("report".contains(args[0])) tabs.add("report");
        if ("set".contains(args[0])) tabs.add("set");
        if ("withdraw".contains(args[0])) tabs.add("withdraw");
      }
      case 2 -> {
        tabs.add("<player>");
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(HumanEntity::getName));
        for (Player player : Bukkit.getOnlinePlayers()) tabs.add(player.getName());
      }
      case 3 -> {
        if (args[0].equalsIgnoreCase("report")) return tabs;
        tabs.add("<amount>");
      }
    }

    return tabs;
  }

  /**
   * Handles the 'balance deposit' sub-command.
   *
   * @param commander The commander executing the command.
   * @param args The arguments for the command.
   */
  private void onDepositCommand(CommandSender commander, String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 3) {
      commander.sendMessage(dialog.get("command_deposit_help"));
      return;
    }

    String argPlayer = args[1];
    String argAmount = args[2];

    Dialog.Arg playerArg = new Dialog.Arg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerArg));
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(argAmount);
    } catch (NumberFormatException e) {
      commander.sendMessage(
          dialog.get("invalid_amount_given", new Dialog.Arg("amount", argAmount)));
      return;
    }

    Dialog.Arg amountArg = new Dialog.Arg("amount", argAmount);

    if (amount < 0) {
      commander.sendMessage(dialog.get("negative_amount_given", amountArg));
      return;
    }

    if (!TrialEconomy.hasAccount(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_account", playerArg));
      return;
    }

    PlayerAccount account = TrialEconomy.getAccount(oPlayer);
    account.deposit(amount);
    account.save();

    Dialog.Arg balanceArg = new Dialog.Arg("balance", account.getBalance());

    commander.sendMessage(dialog.get("command_deposit_success", playerArg, amountArg, balanceArg));
  }

  /**
   * Handles the 'balance report' sub-command.
   *
   * @param commander The commander executing the command.
   * @param args The arguments for the command.
   */
  private void onReportCommand(CommandSender commander, String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 2) {
      commander.sendMessage(dialog.get("command_report_help"));
      return;
    }

    String argPlayer = args[1];

    Dialog.Arg playerArg = new Dialog.Arg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerArg));
      return;
    }

    if (!TrialEconomy.hasAccount(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_account", playerArg));
      return;
    }

    PlayerAccount account = TrialEconomy.getAccount(oPlayer);
    Dialog.Arg balanceArg = new Dialog.Arg("balance", account.getBalance());

    commander.sendMessage(dialog.get("command_report_success", playerArg, balanceArg));
  }

  /**
   * Handles the 'balance set' sub-command.
   *
   * @param commander The commander executing the command.
   * @param args The arguments for the command.
   */
  private void onSetCommand(CommandSender commander, String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 3) {
      commander.sendMessage(dialog.get("command_set_help"));
      return;
    }

    String argPlayer = args[1];
    String argAmount = args[2];

    Dialog.Arg playerArg = new Dialog.Arg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerArg));
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(argAmount);
    } catch (NumberFormatException e) {
      commander.sendMessage(
          dialog.get("invalid_amount_given", new Dialog.Arg("amount", argAmount)));
      return;
    }

    if (amount < 0) {
      commander.sendMessage(
          dialog.get("negative_amount_given", new Dialog.Arg("amount", argAmount)));
      return;
    }

    if (!TrialEconomy.hasAccount(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_account", playerArg));
      return;
    }

    PlayerAccount account = TrialEconomy.getAccount(oPlayer);
    account.setBalance(amount);
    account.save();

    Dialog.Arg balanceArg = new Dialog.Arg("balance", account.getBalance());

    commander.sendMessage(dialog.get("command_set_success", playerArg, balanceArg));
  }

  /**
   * Handles the 'balance withdraw' sub-command.
   *
   * @param commander The commander executing the command.
   * @param args The arguments for the command.
   */
  private void onWithdrawCommand(CommandSender commander, String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 3) {
      commander.sendMessage(dialog.get("command_withdraw_help"));
      return;
    }

    String argPlayer = args[1];
    String argAmount = args[2];

    Dialog.Arg playerArg = new Dialog.Arg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerArg));
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(argAmount);
    } catch (NumberFormatException e) {
      commander.sendMessage(
          dialog.get("invalid_amount_given", new Dialog.Arg("amount", argAmount)));
      return;
    }

    Dialog.Arg amountArg = new Dialog.Arg("amount", argAmount);

    if (amount < 0) {
      commander.sendMessage(dialog.get("negative_amount_given", amountArg));
      return;
    }

    if (!TrialEconomy.hasAccount(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_account", playerArg));
      return;
    }

    PlayerAccount account = TrialEconomy.getAccount(oPlayer);

    Dialog.Arg balanceArg = new Dialog.Arg("balance", account.getBalance());

    if (!account.has(amount)) {
      commander.sendMessage(dialog.get("insufficient_balance", playerArg, balanceArg));
      return;
    }

    account.withdraw(amount);
    account.save();

    balanceArg = new Dialog.Arg("balance", account.getBalance());

    commander.sendMessage(dialog.get("command_withdraw_success", playerArg, amountArg, balanceArg));
  }
}
