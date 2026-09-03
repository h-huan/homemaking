package com.hm.module.homemaking.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;

/** HM forms submit local ISO dates; existing platform clients may still submit epoch milliseconds. */
public class BusinessTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override public LocalDateTime deserialize(JsonParser parser,DeserializationContext context)throws IOException{
        if(parser.hasToken(JsonToken.VALUE_NUMBER_INT))return LocalDateTime.ofInstant(Instant.ofEpochMilli(parser.getLongValue()),ZoneId.systemDefault());
        if(parser.hasToken(JsonToken.VALUE_STRING)) {
            String value=parser.getText().trim();
            try{return LocalDateTime.parse(value.replace(' ','T'));}
            catch(DateTimeParseException e){throw context.weirdStringException(value,LocalDateTime.class,"时间须为 yyyy-MM-ddTHH:mm:ss 或毫秒时间戳");}
        }
        return (LocalDateTime)context.handleUnexpectedToken(LocalDateTime.class,parser);
    }
}
