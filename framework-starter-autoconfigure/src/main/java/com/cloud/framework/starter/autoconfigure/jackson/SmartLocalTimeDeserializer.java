package com.cloud.framework.starter.autoconfigure.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;

import java.io.IOException;
import java.time.LocalTime;


public final class SmartLocalTimeDeserializer extends LocalTimeDeserializer {

    static final SmartLocalTimeDeserializer INSTANCE = new SmartLocalTimeDeserializer();

    @Override
    public LocalTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            String text = parser.getText().trim();

            SmartDateTimePattern matchedPattern = SmartDateTimePattern.match(text);
            if (matchedPattern != null) {
                return matchedPattern.parseLocalTime(text);
            }
        }

        return super.deserialize(parser, context);
    }
}
