package com.hedera.node.app.hapi.simplefees;



import com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.hiero.hapi.support.fees.ServiceFee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class MockServiceMethod {
    long base;
    Map<Extra, Long> extras;

    MockServiceMethod(long base) {
        this.base = base;
        this.extras = new HashMap<>();
    }


    public long getBaseFee() {
        return this.base;
    }
}
public class MockFeesSchedule implements AbstractFeesSchedule {
    final HashMap<String, MockServiceMethod> methods;
    final HashMap<Extra, Long> extras;
    final HashMap<Extra, Long> nodeExtras;
    private FeeSchedule schedule;
    private long node_base;
    private long networkMultiplier;

    public MockFeesSchedule() {
        this.schedule = FeeSchedule.DEFAULT;
        methods = new HashMap<>();
        extras = new HashMap<>();
        nodeExtras = new HashMap<>();
        node_base = 0L;
    }

    @Override
    public FeeSchedule getRawSchedule() {
        return this.schedule;
    }

    @Override
    public List<Extra> getDefinedExtraNames() {
        return this.extras.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public long getExtrasFee(Extra name) {
        var opt = this.schedule.definedExtras().stream().filter(ed -> ed.name() == name).findFirst();
        if (opt.isPresent()) {
            return opt.get().fee();
        }
        if(!this.extras.containsKey(name)) {
            throw new Error(" Missing extra parameter '" + name + "'.");
        }
        return this.extras.get(name);
    }
    public void setExtrasFee(Extra name, long value) {
        this.extras.put(name,value);
    }

    @Override
    public long getNodeBaseFee() {
        return this.node_base;
    }
    public void setNodeBaseFee(long value) {
        this.node_base = value;
    }

    @Override
    public List<Extra> getNodeExtraNames() {
        return nodeExtras.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public long getNodeExtraIncludedCount(Extra name) {
        return nodeExtras.get(name);
    }
    public void setNodeExtraIncludedCount(Extra name, long value) {
        this.nodeExtras.put(name,value);
    }

    @Override
    public long getNetworkMultiplier() {
        return this.networkMultiplier;
    }

    @Override
    public List<String> getServiceNames() {
        return List.of();
    }

    public void setNetworkMultiplier(long multiplier) {
        this.networkMultiplier = multiplier;
    }

    @Override
    public long getServiceBaseFee(String method) {
        for(var service : this.schedule.serviceFees()) {
            for (var trans : service.transactions()) {
                if (trans.name().equals(method)) {
                    return trans.baseFee();
                }
            }
        }
        if(!methods.containsKey(method)) throw new Error("ServiceBaseFee for " + method + " not found");
        return this.methods.get(method).getBaseFee();
    }
    public void setServiceBaseFee(String method, long value) {
        System.out.println("inserting " + method + " " + value);
        if(!methods.containsKey(method)) this.methods.put(method, new MockServiceMethod(0));
        this.methods.get(method).base = value;
    }

    @Override
    public List<Extra> getServiceExtras(String method) {
        var meth = this.getServiceMethod(method);
        if (meth != null) {
            return meth.extras().stream().map(e -> e.name()).collect(Collectors.toList());
        }
        if(!methods.containsKey(method)) throw new Error("ServiceBaseFee for " + method + " not found");
        return this.methods.get(method).extras.keySet().stream().collect(Collectors.toList());
    }

    private ServiceFee getServiceMethod(String method) {
        for(var service : this.schedule.serviceFees()) {
            for (var trans : service.transactions()) {
                if (trans.name().equals(method)) {
                    return trans;
                }
            }
        }
        return null;
    }

    @Override
    public long getServiceExtraIncludedCount(String method, Extra name) {
        var meth = this.getServiceMethod(method);
        if (meth != null) {
            for (var extra : meth.extras()) {
                if (extra.name().equals(name)) {
                    return extra.includedCount();
                }
            }
        }
        if(!methods.containsKey(method)) throw new Error("ServiceBaseFee for " + method + " not found");
        if(!methods.get(method).extras.containsKey(name)) throw new Error("ServiceExtraIncludedCount for " + method + " " + name + " not found");
        return this.methods.get(method).extras.get(name);
    }
    public void setServiceExtraIncludedCount(String method, Extra name, long value) {
        if(!methods.containsKey(method)) this.methods.put(method, new MockServiceMethod(0));
        this.methods.get(method).extras.put(name,value);
    }

    public void setRawSchedule(FeeSchedule raw) {
        this.schedule = raw;
    }
}
