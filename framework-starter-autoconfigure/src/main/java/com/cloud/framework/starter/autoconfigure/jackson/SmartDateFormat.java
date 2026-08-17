package com.cloud.framework.starter.autoconfigure.jackson;

import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SmartDateFormat extends SimpleDateFormat {
    @Serial
    private static final long serialVersionUID = 1L;

    private DateFormat dateFormat;

    public SmartDateFormat(DateFormat dateFormat, String pattern) {
        super(pattern);
        this.dateFormat = dateFormat;
    }

    @Override
    public StringBuffer format(@NonNull Date date, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
        if (this.dateFormat != null) {
            return this.dateFormat.format(date, toAppendTo, pos);
        }
        return super.format(date, toAppendTo, pos);
    }

    @Override
    public Date parse(@NonNull String text, @NonNull ParsePosition pos) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        SmartDateTimePattern matchedPattern = SmartDateTimePattern.match(text);
        if (matchedPattern != null) {
            DateFormat matchedDateFormat = new SimpleDateFormat(matchedPattern.getDateTimePattern());
            matchedDateFormat.setTimeZone(getTimeZone());
            matchedDateFormat.setLenient(isLenient());
            return matchedDateFormat.parse(matchedPattern.normalize(text), pos);
        }

        if (this.dateFormat != null) {
            return this.dateFormat.parse(text, pos);
        }

        return super.parse(text, pos);
    }

    @Override
    public void setTimeZone(TimeZone zone) {
        if (this.dateFormat != null) {
            this.dateFormat.setTimeZone(zone);
        }
        super.setTimeZone(zone);
    }

    @Override
    public void setLenient(boolean lenient) {
        if (this.dateFormat != null) {
            this.dateFormat.setLenient(lenient);
        }
        super.setLenient(lenient);
    }

    @Override
    public Object clone() {
        SmartDateFormat smartDateFormat = (SmartDateFormat) super.clone();
        if (this.dateFormat != null) {
            smartDateFormat.dateFormat = (DateFormat) this.dateFormat.clone();
        }
        return smartDateFormat;
    }

}
