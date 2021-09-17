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

  @Override
  public @Nullable List<String> onTabComplete(
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

  private void onDepositCommand(@NotNull CommandSender commander, @NotNull String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 3) {
      commander.sendMessage(dialog.get("command_deposit_help"));
      return;
    }

    String argPlayer = args[1];
    String argAmount = args[2];

    DialogArg playerDialogArg = new DialogArg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerDialogArg));
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(argAmount);
    } catch (NumberFormatException e) {
      commander.sendMessage(dialog.get("invalid_amount_given", new DialogArg("amount", argAmount)));
      return;
    }

    DialogArg amountDialogArg = new DialogArg("amount", argAmount);

    if (amount < 0) {
      commander.sendMessage(dialog.get("negative_amount_given", amountDialogArg));
      return;
    }

    if (!TrialEconomy.hasProfile(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_profile", playerDialogArg));
      return;
    }

    EconomyProfile profile = TrialEconomy.getProfile(oPlayer);
    profile.deposit(amount);
    profile.save();

    DialogArg balanceDialogArg = new DialogArg("balance", profile.getBalance());

    commander.sendMessage(
        dialog.get("command_deposit_success", playerDialogArg, amountDialogArg, balanceDialogArg));
  }

  private void onReportCommand(@NotNull CommandSender commander, @NotNull String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 2) {
      commander.sendMessage(dialog.get("command_report_help"));
      return;
    }

    String argPlayer = args[1];

    DialogArg playerDialogArg = new DialogArg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerDialogArg));
      return;
    }

    if (!TrialEconomy.hasProfile(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_profile", playerDialogArg));
      return;
    }

    EconomyProfile profile = TrialEconomy.getProfile(oPlayer);
    DialogArg balanceDialogArg = new DialogArg("balance", profile.getBalance());

    commander.sendMessage(dialog.get("command_report_success", playerDialogArg, balanceDialogArg));
  }

  private void onSetCommand(@NotNull CommandSender commander, @NotNull String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 3) {
      commander.sendMessage(dialog.get("command_set_help"));
      return;
    }

    String argPlayer = args[1];
    String argAmount = args[2];

    DialogArg playerDialogArg = new DialogArg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerDialogArg));
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(argAmount);
    } catch (NumberFormatException e) {
      commander.sendMessage(dialog.get("invalid_amount_given", new DialogArg("amount", argAmount)));
      return;
    }

    if (amount < 0) {
      commander.sendMessage(
          dialog.get("negative_amount_given", new DialogArg("amount", argAmount)));
      return;
    }

    if (!TrialEconomy.hasProfile(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_profile", playerDialogArg));
      return;
    }

    EconomyProfile profile = TrialEconomy.getProfile(oPlayer);
    profile.setBalance(amount);
    profile.save();

    DialogArg balanceDialogArg = new DialogArg("balance", profile.getBalance());

    commander.sendMessage(dialog.get("command_set_success", playerDialogArg, balanceDialogArg));
  }

  private void onWithdrawCommand(@NotNull CommandSender commander, @NotNull String[] args) {

    Dialog dialog = TrialEconomy.INSTANCE.getDialog();

    if (args.length != 3) {
      commander.sendMessage(dialog.get("command_withdraw_help"));
      return;
    }

    String argPlayer = args[1];
    String argAmount = args[2];

    DialogArg playerDialogArg = new DialogArg("player", argPlayer);

    OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(argPlayer);
    if (!oPlayer.hasPlayedBefore()) {
      commander.sendMessage(dialog.get("player_not_found", playerDialogArg));
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(argAmount);
    } catch (NumberFormatException e) {
      commander.sendMessage(dialog.get("invalid_amount_given", new DialogArg("amount", argAmount)));
      return;
    }

    DialogArg amountDialogArg = new DialogArg("amount", argAmount);

    if (amount < 0) {
      commander.sendMessage(dialog.get("negative_amount_given", amountDialogArg));
      return;
    }

    if (!TrialEconomy.hasProfile(oPlayer.getUniqueId())) {
      commander.sendMessage(dialog.get("no_profile", playerDialogArg));
      return;
    }

    EconomyProfile profile = TrialEconomy.getProfile(oPlayer);

    DialogArg balanceDialogArg = new DialogArg("balance", profile.getBalance());

    if (!profile.has(amount)) {
      commander.sendMessage(dialog.get("insufficient_balance", playerDialogArg, balanceDialogArg));
      return;
    }

    profile.withdraw(amount);
    profile.save();

    balanceDialogArg = new DialogArg("balance", profile.getBalance());

    commander.sendMessage(
        dialog.get("command_withdraw_success", playerDialogArg, amountDialogArg, balanceDialogArg));
  }
}
