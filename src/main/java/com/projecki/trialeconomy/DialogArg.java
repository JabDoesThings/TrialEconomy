package com.projecki.trialeconomy;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DialogArg {

  @Getter private final String id;
  private final Object value;

  DialogArg(@NotNull String id, @Nullable Object value) {
    this.id = id;
    this.value = value;
  }

  @NotNull
  String getValue() {
    if (value == null) return id;
    return value.toString();
  }
}
