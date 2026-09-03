package com.hm.module.homemaking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hm.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class BusinessTimeTest {
    private final ObjectMapper mapper=new ObjectMapper().registerModule(new JavaTimeModule().addDeserializer(LocalDateTime.class,TimestampLocalDateTimeDeserializer.INSTANCE));
    @Test void browserReceiptDateOverridesPlatformEpochDeserializer()throws Exception{
        var receipt=mapper.readValue("{\"channel\":\"CASH\",\"amountCents\":10000,\"occurredAt\":\"2026-09-03T17:30:00\",\"note\":\"Checked\",\"requestKey\":\"request-123\"}",PaymentLedgerService.Receipt.class);
        assertEquals(LocalDateTime.of(2026,9,3,17,30),receipt.occurredAt());
    }
    @Test void bookingAndScheduleDatesRemainLocalAndEpochClientsStillWork()throws Exception{
        String start="2026-09-05T09:00:00",end="2026-09-05T10:00:00";
        var interval=mapper.readValue("{\"startsAt\":\""+start+"\",\"endsAt\":\""+end+"\"}",ScheduleService.Interval.class);assertEquals(LocalDateTime.parse(start),interval.startsAt());assertEquals(LocalDateTime.parse(end),interval.endsAt());
        long epoch=LocalDateTime.parse(start).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        var book=mapper.readValue("{\"serviceId\":1,\"startsAt\":"+epoch+",\"addressId\":1,\"requestKey\":\"test\"}",OrderService.Book.class);assertEquals(interval.startsAt(),book.startsAt());
    }
    @Test void invalidDatesAreRejectedInsteadOfBecoming1970(){assertThrows(Exception.class,()->mapper.readValue("{\"receiptId\":1,\"occurredAt\":\"not-a-date\",\"note\":\"Test\",\"requestKey\":\"test-date\"}",PaymentLedgerService.Reversal.class));}
}
