package com.hedera.node.app.hapi.fees;

import com.hedera.hapi.node.consensus.ExtraFeeDefinition;
import com.hedera.hapi.node.consensus.Service;
import com.hedera.hapi.node.consensus.ServiceFee;
import com.hedera.hapi.node.consensus.SimpleFeeSchedule;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;


import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class JsonFeesSchedule implements AbstractFeesSchedule {
    public final SimpleFeeSchedule schedule;
    private final HashMap<String, Service> services;
    private final HashMap<String, ServiceFee> serviceMethods;
    public final HashMap<String, ExtraFeeDefinition> extras;

    public static JsonFeesSchedule fromJson() {
        try (final var fin = JsonFeesSchedule.class.getClassLoader().getResourceAsStream("simple-fee-schedule.json")) {
            final var buf = SimpleFeeSchedule.JSON.parse(new ReadableStreamingData(requireNonNull(fin)));
//            System.out.println("parsed simple fees schedule: " + buf);
            return new JsonFeesSchedule(buf);
        } catch (Exception e) {
            System.out.println("exception loading fees schedule " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private JsonFeesSchedule(SimpleFeeSchedule buf) {
        this.schedule = buf;
        this.extras = new HashMap<>();
        for(var extra: buf.definedExtras()) {
            this.extras.put(extra.name(), extra);
        }
        this.services = new HashMap<>();
        this.serviceMethods = new HashMap<>();
        for (var service : buf.serviceFees()) {
            this.services.put(service.name(), service);
            System.out.println("service " + service.name());
            for (var txn : service.transactions()) {
                this.serviceMethods.put(txn.name(),txn);
                System.out.println("transaction " + txn.name());
                System.out.println("txn " + txn);
            }
            for (var txn : service.queries()) {
                this.serviceMethods.put(txn.name(),txn);
                System.out.println("query " + txn.name());
            }
        }
    }


    @Override
    public List<String> getDefinedExtraNames() {
        return this.schedule.definedExtras().stream().map(e -> e.name()).collect(Collectors.toList());
    }

    @Override
    public long getExtrasFee(String name) {
        var res = this.schedule.definedExtras().stream().filter(e -> e.name().equals(name)).findFirst();
        if (res.isPresent()) {
            return res.get().fee();
        } else {
            throw new Error("extra '"+name+"' not found.");
        }
    }

    @Override
    public long getNodeBaseFee() {
        return this.schedule.nodeFee().baseFee();
    }

    @Override
    public List<String> getNodeExtraNames() {
        return this.schedule.nodeFee().extras().stream().map(e -> e.name()).collect(Collectors.toList());
    }

    @Override
    public long getNodeExtraIncludedCount(String name) {
        for(var extra : this.schedule.nodeFee().extras()) {
            if (extra.name().equals(name)) {
                return extra.includedCount();
            }
        }
        throw new Error("node extra '"+name+"' not found.");
    }

    @Override
    public long getNetworkMultiplier() {
        return this.schedule.networkFeeRatio().multiplier();
    }

    @Override
    public long getServiceBaseFee(String method) {
        if(!this.serviceMethods.containsKey(method)) throw new NoSuchElementException("service method '"+method+"' not found.");
        return this.serviceMethods.get(method).baseFee();
    }

    @Override
    public List<String> getServiceExtras(String method) {
        if(!this.serviceMethods.containsKey(method)) throw new NoSuchElementException("service method '"+method+"' not found.");
        return this.serviceMethods.get(method).extras().stream().map(e -> e.name()).collect(Collectors.toList());
    }


    @Override
    public long getServiceExtraIncludedCount(String method, String name) {
        if(!this.serviceMethods.containsKey(method)) throw new NoSuchElementException("service method '"+method+"' not found.");
        var m = this.serviceMethods.get(method);
        for(var extra : m.extras()) {
            if (extra.name().equals(name)) {
                return extra.includedCount();
            }
        }
        throw new Error("service extra included count'"+method+"' : " + name + " not found.");
    }
}
