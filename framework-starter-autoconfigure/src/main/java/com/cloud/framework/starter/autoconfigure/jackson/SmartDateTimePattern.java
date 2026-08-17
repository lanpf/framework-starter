package com.cloud.framework.starter.autoconfigure.jackson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.regex.Pattern;

enum SmartDateTimePattern {
    DATE("", ""),
    DATE_HOUR(" \\d{1,2}", " HH"),
    DATE_HOUR_MINUTE(" \\d{1,2}:\\d{1,2}", " HH:mm"),
    DATE_HOUR_MINUTE_SECOND(" \\d{1,2}:\\d{1,2}:\\d{1,2}", " HH:mm:ss"),
    DATE_HOUR_MINUTE_SECOND_MILLISECOND(" \\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}", " HH:mm:ss.SSS");

    private static final String DATE_REGEX = "\\d{4}([-/])\\d{1,2}\\1\\d{1,2}";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    private final Pattern pattern;
    private final String timeFormatPattern;
    private final DateTimeFormatter formatter;

    SmartDateTimePattern(String timeRegex, String timeFormatPattern) {
        this.pattern = Pattern.compile("^" + DATE_REGEX + timeRegex + "$");
        this.timeFormatPattern = timeFormatPattern;
        this.formatter = createFormatter();
    }

    static String defaultLocalDatePattern() {
        return DATE.getDatePattern();
    }

    static String defaultLocalDateTimePattern() {
        return DATE_HOUR_MINUTE_SECOND_MILLISECOND.getDateTimePattern();
    }

    static String defaultLocalTimePattern() {
        return DATE_HOUR_MINUTE_SECOND_MILLISECOND.getTimePattern();
    }

    static SmartDateTimePattern match(String source) {
        for (SmartDateTimePattern dateTimePattern : values()) {
            if (dateTimePattern.matches(source)) {
                return dateTimePattern;
            }
        }
        return null;
    }

    private boolean matches(String source) {
        return this.pattern.matcher(source).matches();
    }

    DateTimeFormatter getDateTimeFormatter() {
        return this.formatter;
    }

    String getDateTimePattern() {
        return DATE_FORMAT_PATTERN + this.timeFormatPattern;
    }

    String getDatePattern() {
        return DATE_FORMAT_PATTERN;
    }

    String getTimePattern() {
        return this.timeFormatPattern.trim();
    }

    String normalize(String source) {
        return source.replace('/', '-');
    }

    LocalDate parseLocalDate(String source) {
        return parseLocalDateTime(source).toLocalDate();
    }

    LocalDateTime parseLocalDateTime(String source) {
        TemporalAccessor temporalAccessor = this.formatter.parse(normalize(source));
        LocalDate localDate = LocalDate.from(temporalAccessor);
        LocalTime localTime = parseLocalTime(temporalAccessor);
        return LocalDateTime.of(localDate, localTime);
    }

    LocalTime parseLocalTime(String source) {
        return parseLocalDateTime(source).toLocalTime();
    }

    private DateTimeFormatter createFormatter() {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE);

        if (this.timeFormatPattern.contains("H")) {
            builder.appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE);
        }
        if (this.timeFormatPattern.contains("m")) {
            builder.appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE);
        }
        if (this.timeFormatPattern.contains("s")) {
            builder.appendLiteral(':')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE);
        }
        if (this.timeFormatPattern.contains("S")) {
            builder.appendLiteral('.')
                    .appendFraction(ChronoField.NANO_OF_SECOND, 1, 3, false);
        }
        return builder.toFormatter(Locale.ROOT);
    }

    private static LocalTime parseLocalTime(TemporalAccessor temporalAccessor) {
        if (!temporalAccessor.isSupported(ChronoField.HOUR_OF_DAY)) {
            return LocalTime.MIDNIGHT;
        }
        int hour = temporalAccessor.get(ChronoField.HOUR_OF_DAY);
        int minute = temporalAccessor.isSupported(ChronoField.MINUTE_OF_HOUR)
                ? temporalAccessor.get(ChronoField.MINUTE_OF_HOUR) : 0;
        int second = temporalAccessor.isSupported(ChronoField.SECOND_OF_MINUTE)
                ? temporalAccessor.get(ChronoField.SECOND_OF_MINUTE) : 0;
        int nano = temporalAccessor.isSupported(ChronoField.NANO_OF_SECOND)
                ? temporalAccessor.get(ChronoField.NANO_OF_SECOND) : 0;
        return LocalTime.of(hour, minute, second, nano);
    }
}
