package com.hedera.node.app.hapi.fees;

import com.hedera.hapi.node.consensus.FeeExtra;
import com.hedera.hapi.node.consensus.Service;
import com.hedera.hapi.node.consensus.ServiceMethod;
import com.hedera.hapi.node.consensus.SimpleFeesSchedule;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;


import java.util.HashMap;

import static java.util.Objects.requireNonNull;

public class JsonFeesSchedule implements AbstractFeesSchedule {
    public final SimpleFeesSchedule schedule;
    private final HashMap<String, Service> services;
    private final HashMap<String, ServiceMethod> serviceMethods;
    public final HashMap<String, FeeExtra> extras;

    private JsonFeesSchedule(SimpleFeesSchedule buf) {
        this.schedule = buf;
        this.extras = new HashMap<>();
        for(var extra: buf.extras()) {
            this.extras.put(extra.name(), extra);
        }
        this.services = new HashMap<>();
        this.serviceMethods = new HashMap<>();
        for (var service : buf.services()) {
            this.services.put(service.name(), service);
            System.out.println("service " + service.name());
            for (var txn : service.transactions()) {
                this.serviceMethods.put(txn.name(),txn);
                System.out.println("transaction " + txn.name());
                System.out.println("txn " + txn);
                if(!txn.hasNetwork()) {
                    System.err.println("txn has no network");
                }
                if(!txn.hasNode()) {
                    System.err.println("txn has no network");
                }
            }
            for (var txn : service.queries()) {
                this.serviceMethods.put(txn.name(),txn);
                System.out.println("query " + txn.name());
            }
        }
    }

    public static JsonFeesSchedule fromJson() {
        try (final var fin = BaseFeeRegistry.class.getClassLoader().getResourceAsStream("simple-fees.json")) {
            final var buf = SimpleFeesSchedule.JSON.parse(new ReadableStreamingData(requireNonNull(fin)));
//            System.out.println("parsed simple fees schedule: " + buf);
            return new JsonFeesSchedule(buf);
        } catch (Exception e) {
            System.out.println("exception loading fees schedule " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public double getNodeBaseFee(String name) {
        System.out.println("JSON: getBaseFee " + name);
        return Double.parseDouble(this.serviceMethods.get(name).node().base());
    }
    public double getNetworkBaseFee(String name) {
        System.out.println("JSON: getBaseFee " + name);
        return Double.parseDouble(this.serviceMethods.get(name).network().base());
    }

    @Override
    public int getNetworkBaseExtrasIncluded(String api, String name) {
        for (var extra : this.serviceMethods.get(api).network().extras()) {
            System.out.println("JSON: getNetworkBaseExtrasIncluded  " + extra.name());
            if (extra.name().equals(name)) {
                return extra.includedCount();
            }
        }
        return 0;
    }

    @Override
    public double getExtrasFee(String name) {
        System.out.println("JSON: getExtrasFee " + name);
        return Double.parseDouble(this.extras.get(name).fee());
    }

}
