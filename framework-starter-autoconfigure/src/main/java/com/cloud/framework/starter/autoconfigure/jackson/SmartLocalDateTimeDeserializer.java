package com.cloud.framework.starter.autoconfigure.jackson;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

public final class SmartLocalDateTimeDeserializer extends LocalDateTimeDeserializer {

    static final SmartLocalDateTimeDeserializer INSTANCE = new SmartLocalDateTimeDeserializer();

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasTokenId(JsonTokenId.ID_STRING)) {
            String text = parser.getText().trim();

            SmartDateTimePattern matchedPattern = SmartDateTimePattern.match(text);
            if (matchedPattern != null) {
                return matchedPattern.parseLocalDateTime(text);
            }
        }

        return super.deserialize(parser, context);
    }
}
