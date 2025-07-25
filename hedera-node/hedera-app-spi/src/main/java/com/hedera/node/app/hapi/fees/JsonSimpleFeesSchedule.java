package com.hedera.node.app.hapi.fees;

import com.hedera.hapi.node.consensus.*;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;


import java.util.HashMap;

import static java.util.Objects.requireNonNull;

public class JsonSimpleFeesSchedule implements AbstractSimpleFeesSchedule {
    private final SimpleFeesSchedule schedule;
    private final HashMap<String, ServiceFee> services;
    private final HashMap<String, TransactionFee> transactions;
    private final HashMap<String, Extra> extras;

    private JsonSimpleFeesSchedule(SimpleFeesSchedule buf) {
        this.schedule = buf;
        this.extras = new HashMap<>();
        for(var extra: buf.extras()) {
            this.extras.put(extra.name(), extra);
        }
        this.services = new HashMap<>();
        this.transactions = new HashMap<>();
        for (var service : buf.services()) {
            this.services.put(service.name(), service);
            for (var txn : service.transactions()) {
                this.transactions.put(txn.name(),txn);
            }
        }
    }

    public static JsonSimpleFeesSchedule fromJson() {
        try (final var fin = BaseFeeRegistry.class.getClassLoader().getResourceAsStream("simple-fees.json")) {
            final var buf = SimpleFeesSchedule.JSON.parse(new ReadableStreamingData(requireNonNull(fin)));
            System.out.println("parsed simple fees schedule: " + buf);
            return new JsonSimpleFeesSchedule(buf);
        } catch (Exception e) {
            System.out.println("exception loading fees schedule " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public double getBaseFee(String name) {
        System.out.println("JSON: get base fee " + name);
        return this.transactions.get(name).baseFee();
    }

    @Override
    public int getBaseExtrasIncluded(String api, String name) {
        for (var extra : this.transactions.get(api).extras()) {
            System.out.println("JSON: extra " + extra.name());
            if (extra.name().equals(name)) {
                return extra.includedCount();
            }
        }
        return 0;
    }

    @Override
    public double getExtrasFee(String name) {
        System.out.println("JSON: getExtrasFee " + name);
        return this.extras.get(name).fee();
    }

}
