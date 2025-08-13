package com.hedera.node.app.hapi.simplefees;



import com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class MockServiceMethod {
    long base;
    Map<String, Long> extras;

    MockServiceMethod(long base) {
        this.base = base;
        this.extras = new HashMap<>();
    }

    public void addExtra(String extra, long included) {
        this.extras.put(extra,included);
    }

    public long getBaseFee() {
        return this.base;
    }
}
public class MockFeesSchedule implements AbstractFeesSchedule {
    final HashMap<String, MockServiceMethod> methods;
    final HashMap<String, Long> extras;
    final HashMap<String, Long> nodeExtras;
    private long node_base;
    private long networkMultiplier;

    public MockFeesSchedule() {
        methods = new HashMap<>();
        extras = new HashMap<>();
        nodeExtras = new HashMap<>();
        node_base = 0L;
    }

    @Override
    public List<String> getDefinedExtraNames() {
        return this.extras.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public long getExtrasFee(String name) {
        if(!this.extras.containsKey(name)) {
            throw new Error(" Missing extra parameter '" + name + "'.");
        }
        return this.extras.get(name);
    }
    public void setExtrasFee(String name, long value) {
        this.extras.put(name,value);
    }
    public void setExtrasFee(FeeConstants.Extras name, long value) {
        this.extras.put(name.name(),value);
    }

    @Override
    public long getNodeBaseFee() {
        return this.node_base;
    }
    public void setNodeBaseFee(long value) {
        this.node_base = value;
    }

    @Override
    public List<String> getNodeExtraNames() {
        return nodeExtras.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public long getNodeExtraIncludedCount(String name) {
        return nodeExtras.get(name);
    }
    public void setNodeExtraIncludedCount(String signatures, long value) {
        this.nodeExtras.put(signatures,value);
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
        if(!methods.containsKey(method)) throw new Error("ServiceBaseFee for " + method + " not found");
        return this.methods.get(method).getBaseFee();
    }
    public void setServiceBaseFee(String method, long value) {
        System.out.println("inserting " + method + " " + value);
        if(!methods.containsKey(method)) this.methods.put(method, new MockServiceMethod(0));
        this.methods.get(method).base = value;
    }

    @Override
    public List<String> getServiceExtras(String method) {
        if(!methods.containsKey(method)) throw new Error("ServiceBaseFee for " + method + " not found");
        return this.methods.get(method).extras.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public long getServiceExtraIncludedCount(String method, String name) {
        if(!methods.containsKey(method)) throw new Error("ServiceBaseFee for " + method + " not found");
        if(!methods.get(method).extras.containsKey(name)) throw new Error("ServiceExtraIncludedCount for " + method + " " + name + " not found");
        return this.methods.get(method).extras.get(name);
    }
    public void setServiceExtraIncludedCount(String method, String signatures, long value) {
        if(!methods.containsKey(method)) this.methods.put(method, new MockServiceMethod(0));
        this.methods.get(method).extras.put(signatures,value);
    }
    public void setServiceExtraIncludedCount(String method, FeeConstants.Extras extra, long value) {
        this.setServiceExtraIncludedCount(method, extra.name(), value);
    }

}
