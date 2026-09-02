package com.hm.module.homemaking.service;

import java.util.Map;
import java.util.Set;
public final class OrderState {
    private OrderState() {}
    private static final Map<String,Set<String>> NEXT=Map.of(
        "UNPAID",Set.of("PAID","CANCELLED"),"PAID",Set.of("ASSIGNED","REFUNDING"),
        "ASSIGNED",Set.of("IN_SERVICE","REFUNDING"),"IN_SERVICE",Set.of("COMPLETED"),
        "COMPLETED",Set.of("REFUNDING"),"REFUNDING",Set.of("REFUNDED"));
    public static boolean allows(String from,String to){return NEXT.getOrDefault(from,Set.of()).contains(to);}
}
