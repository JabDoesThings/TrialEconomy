package com.projecki.trialeconomy;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Dialog</b> is a simple solution for grabbing text, injecting values and coloring for dialog
 * use in TrialEconomy.
 *
 * @author Jab
 */
class Dialog {

  private final Map<String, String> dialog = new HashMap<>();

  /**
   * @param cfg The cfg section storing dialog text fields.
   * @throws NullPointerException Thrown if the cfg is null.
   */
  Dialog(@NotNull ConfigurationSection cfg) {
    for (String key : cfg.getKeys(false)) {
      if (cfg.isString(key)) {
        dialog.put(key, color(cfg.getString(key)));
      }
    }
  }

  /**
   * Colors a string using the alternative color code '&'.
   *
   * @param string The string to color.
   * @return The colored string. If the string is null, null is returned. If the string is empty, an
   *     empty string is returned.
   */
  @Nullable
  private static String color(@Nullable String string) {
    if (string == null || string.isEmpty()) return string;
    return ChatColor.translateAlternateColorCodes('&', string);
  }

  /**
   * @param id The id of the dialog text field.
   * @param args (Optional) arguments to inject in the text.
   * @return The dialog text.
   * @throws IllegalArgumentException Thrown if the id is empty.
   * @throws NullPointerException Thrown if the id is null.
   */
  @NotNull
  String get(@NotNull String id, Arg... args) {

    if (id.isEmpty()) throw new IllegalArgumentException("The ID is empty.");

    if (!dialog.containsKey(id)) {
      throw new NullPointerException("The dialog for the id '" + id + "' doesn't exist.");
    }

    String dialog = this.dialog.get(id);
    if (args.length != 0) {
      for (Arg arg : args) {
        dialog = dialog.replaceAll("%" + arg.getId() + "%", arg.getValue());
      }
    }

    return dialog;
  }

  /**
   * <b>DialogArg</b> is a simple argument struct for providing values to inject into dialog text
   * for the Dialog utility.
   *
   * @author Jab
   */
  static class Arg {

    /** The ID of the value. Identified in dialog text as '%id%'. */
    @Getter private final String id;

    /** The value to inject. */
    private final Object value;

    /**
     * @param id The ID of the value. Identified in dialog text as '%id%'.
     * @param value The value to inject.
     * @throws NullPointerException Thrown if the id is null.
     */
    Arg(@NotNull String id, @Nullable Object value) {
      this.id = id;
      this.value = value;
    }

    /** @return The string interpretation of the value. If the value is null, the id is returned. */
    @NotNull
    String getValue() {
      if (value == null) return id;
      return value.toString();
    }
  }
}
