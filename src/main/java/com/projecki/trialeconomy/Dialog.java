package com.projecki.trialeconomy;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Dialog {

  private final Map<String, String> dialog = new HashMap<>();

  Dialog(@NotNull ConfigurationSection cfg) {
    for (String key : cfg.getKeys(false)) {
      if (cfg.isString(key)) {
        dialog.put(key, color(cfg.getString(key)));
      }
    }
  }

  @NotNull
  String get(@NotNull String id, DialogArg... args) {

    if (id.isEmpty()) throw new IllegalArgumentException("The ID is empty.");

    if (!dialog.containsKey(id)) {
      throw new NullPointerException("The dialog for the id '" + id + "' doesn't exist.");
    }

    String dialog = this.dialog.get(id);
    if (args.length != 0) {
      for (DialogArg arg : args) {
        dialog = dialog.replaceAll("%" + arg.getId() + "%", arg.getValue());
      }
    }

    return dialog;
  }

  @Nullable
  private static String color(@Nullable String string) {
    if (string == null || string.isEmpty()) return string;
    return ChatColor.translateAlternateColorCodes('&', string);
  }
}
