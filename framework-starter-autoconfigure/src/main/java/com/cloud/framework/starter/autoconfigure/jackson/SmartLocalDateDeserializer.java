package com.cloud.framework.starter.autoconfigure.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import java.io.IOException;
import java.time.LocalDate;

public final class SmartLocalDateDeserializer extends LocalDateDeserializer {

    static final SmartLocalDateDeserializer INSTANCE = new SmartLocalDateDeserializer();

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            String text = parser.getText().trim();

            SmartDateTimePattern matchedPattern = SmartDateTimePattern.match(text);
            if (matchedPattern != null) {
                return matchedPattern.parseLocalDate(text);
            }
        }

        return super.deserialize(parser, context);
    }
}
