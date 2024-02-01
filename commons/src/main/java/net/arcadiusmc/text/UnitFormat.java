package net.arcadiusmc.text;

import java.util.concurrent.TimeUnit;
import net.arcadiusmc.text.loader.MessageRef;
import net.kyori.adventure.text.Component;

/**
 * Utility class for formatting specific units like Rhines, Gems and Votes into messages.
 */
public interface UnitFormat {

  MessageRef FORMAT_REF = Messages.MESSAGE_LIST.reference("units.display");

  /**
   * Formats the given amount into a currency message
   * @param currency Amount of gems
   * @return Formatted message
   */
  static Component currency(Number currency) {
    return unit(currency, "currency");
  }

  /**
   * Formats the given amount into a gem message
   * @param number Amount of gems
   * @return Formatted message
   */
  static Component gems(Number number) {
    return unit(number, "gems");
  }

  /**
   * Formats the given amount into a vote message
   *
   * @param number The amount of votes
   * @return The formatted message
   */
  static Component votes(Number number) {
    return unit(number, "votes");
  }

  /**
   * Formats the given amount of playtime, in seconds, and returns a formatted message with playtime
   * measured in hours.
   *
   * @param seconds The amount of playtime seconds
   * @return The formatted message
   */
  static Component playTime(Number seconds) {
    return unit(TimeUnit.SECONDS.toHours(seconds.longValue()), "playtime");
  }

  private static Component unit(Number number, String key) {
    return unit(number, "units." + key + ".singular", "units." + key + ".plural");
  }

  private static Component unit(Number amount, String singularKey, String pluralKey) {
    long longValue = amount.longValue();

    Component unit = longValue == 1
        ? Messages.MESSAGE_LIST.renderText(singularKey, null)
        : Messages.MESSAGE_LIST.renderText(pluralKey, null);

    return FORMAT_REF.get()
        .addValue("unit", unit)
        .addValue("amount", amount)
        .create(null);
  }

  static String plural(String unit, double dval) {
    if (dval == 1) {
      return unit;
    }

    var lowerCase = unit.toLowerCase();
    if (lowerCase.endsWith("exp")) {
      return unit;
    }

    if (lowerCase.endsWith("s")
        || lowerCase.endsWith("x")
        || lowerCase.endsWith("sh")
        || lowerCase.endsWith("ch")
        || lowerCase.endsWith("y")
    ) {
      return unit + "es";
    }

    return unit + "s";
  }
}