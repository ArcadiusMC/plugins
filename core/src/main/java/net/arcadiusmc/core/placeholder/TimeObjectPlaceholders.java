package net.arcadiusmc.core.placeholder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.ObjectPlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;

public interface TimeObjectPlaceholders {

  DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
  DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm z");
  DateTimeFormatter SECOND_PRECISION = DateTimeFormatter.ofPattern("d LLL yyyy HH:mm:ss z");

  ObjectPlaceholder<LocalDate> LOCAL_DATE = (value, fieldName, ctx) -> {
    return switch (fieldName.toLowerCase()) {
      case "day" -> value.getDayOfMonth();
      case "weekday" -> value.getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale(ctx));
      case "weekdayno" -> value.getDayOfWeek().getValue();
      case "month" -> value.getMonth().getDisplayName(TextStyle.FULL, getLocale(ctx));
      case "monthno" -> value.getMonthValue();
      case "yearday" -> value.getDayOfYear();
      case "year" -> value.getYear();

      default -> LOCAL_DATE_FORMAT.format(value);
    };
  };

  ObjectPlaceholder<LocalTime> LOCAL_TIME = (value, fieldName, ctx) -> {
    return switch (fieldName.toLowerCase()) {
      case "hour" -> value.getHour();
      case "minute" -> value.getMinute();
      case "second" -> value.getSecond();

      default -> {
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.now(), value, ZoneId.systemDefault());
        yield LOCAL_TIME_FORMAT.format(zdt);
      }
    };
  };

  ObjectPlaceholder<LocalDateTime> LOCAL_DATE_TIME = (value, fieldName, ctx) -> {
    return switch (fieldName.toLowerCase()) {
      case "until" -> {
        ZonedDateTime target = ZonedDateTime.of(value, ZoneId.systemDefault());
        ZonedDateTime now = ZonedDateTime.now();
        yield Duration.between(now, target);
      }

      case "day" -> value.getDayOfMonth();
      case "weekday" -> value.getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale(ctx));
      case "weekdayno" -> value.getDayOfWeek().getValue();
      case "month" -> value.getMonth().getDisplayName(TextStyle.FULL, getLocale(ctx));
      case "monthno" -> value.getMonthValue();
      case "yearday" -> value.getDayOfYear();
      case "year" -> value.getYear();

      case "hour" -> value.getHour();
      case "minute" -> value.getMinute();
      case "second" -> value.getSecond();

      case "withseconds" -> {
        ZonedDateTime zdt = ZonedDateTime.of(value, ZoneId.systemDefault());
        yield SECOND_PRECISION.format(zdt);
      }

      default -> {
        ZonedDateTime zdt = ZonedDateTime.of(value, ZoneId.systemDefault());
        yield Text.DATE_TIME_FORMATTER.format(zdt);
      }
    };
  };

  ObjectPlaceholder<ZonedDateTime> ZONED_DATE_TIME = (value, fieldName, ctx) -> {
    return switch (fieldName.toLowerCase()) {
      case "until" -> {
        ZonedDateTime now = ZonedDateTime.now();
        yield Duration.between(now, value);
      }

      case "day" -> value.getDayOfMonth();
      case "weekday" -> value.getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale(ctx));
      case "weekdayno" -> value.getDayOfWeek().getValue();
      case "month" -> value.getMonth().getDisplayName(TextStyle.FULL, getLocale(ctx));
      case "monthno" -> value.getMonthValue();
      case "yearday" -> value.getDayOfYear();
      case "year" -> value.getYear();

      case "hour" -> value.getHour();
      case "minute" -> value.getMinute();
      case "second" -> value.getSecond();

      case "withseconds" -> {
        yield SECOND_PRECISION.format(value);
      }

      default -> {
        yield Text.DATE_TIME_FORMATTER.format(value);
      }
    };
  };

  ObjectPlaceholder<Instant> INSTANT = (value, fieldName, ctx) -> {
    ZonedDateTime zdt = ZonedDateTime.ofInstant(value, ZoneId.systemDefault());
    return ZONED_DATE_TIME.lookup(zdt, fieldName, ctx);
  };

  static Locale getLocale(PlaceholderContext context) {
    Object viewer = context.context().get("viewer");
    if (viewer instanceof Audience audience) {
      return audience.get(Identity.LOCALE).orElse(Locale.ENGLISH);
    }

    return Locale.ENGLISH;
  }
}
