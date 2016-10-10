package ru.busylee.pushoverlay;

import android.graphics.Color;
import android.support.annotation.ColorInt;

/**
 * Created by busylee on 07.10.16.
 */
public class Pushable {

  @ColorInt
  int color = Color.TRANSPARENT;
  @ColorInt
  int textColor = Color.BLACK;
  final String value;

  public Pushable(String value) {
    this.value = value;
  }

  public Pushable color(int color) {
    this.color = color;
    return this;
  }

  public Pushable textColor(int color) {
    this.textColor = color;
    return this;
  }

}
