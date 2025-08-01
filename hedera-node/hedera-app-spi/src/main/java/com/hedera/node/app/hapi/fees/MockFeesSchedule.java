package com.hedera.node.app.hapi.fees;


import com.hedera.hapi.node.consensus.ServiceMethod;

import java.util.HashMap;
class FeeComponent {
    private double base;
    private HashMap<String,Integer> extras;
    FeeComponent() {
        this.base = 0.0;
        this.extras = new HashMap<String, Integer>();
    }
    public double getBase() {
        return this.base;
    }
    public void setBase(double base) {
        this.base = base;
    }

    public void setExtraIncluded(String name, int count) {
        this.extras.put(name, count);
    }

    public int getExtraIncluded(String name) {
        return this.extras.get(name);
    }
}
class MockServiceMethod {
    private FeeComponent network;
    private FeeComponent node;
    MockServiceMethod() {
        this.network = new FeeComponent();
        this.node = new FeeComponent();
    }

    FeeComponent network() {
        return this.network;
    }
    FeeComponent node() {
        return this.node;
    }
}
public class MockFeesSchedule implements AbstractFeesSchedule {
    final HashMap<String, MockServiceMethod> methods;
    final HashMap<String, Double> extras;
    public MockFeesSchedule() {
        methods = new HashMap<>();
        extras = new HashMap<>();
    }
    @Override
    public double getExtrasFee(String name) {
        return this.extras.get(name);
    }

    @Override
    public double getNetworkBaseFee(String api) {
        return this.methods.get(api).network().getBase();
    }

    @Override
    public double getNodeBaseFee(String api) {
        return this.methods.get(api).node().getBase();
    }

    MockServiceMethod getMethod(String api) {
        if (!this.methods.containsKey(api)) {
            this.methods.put(api, new MockServiceMethod());
        }
        return this.methods.get(api);
    }
    @Override
    public int getNetworkBaseExtrasIncluded(String method, String name) {
        System.out.println("getNetworkBaseExtrasIncluded method " + method + " " + name);
        return this.getMethod(method).network().getExtraIncluded(name);
    }

    @Override
    public int getNodeBaseExtrasIncluded(String method, String extra) {
        return this.getMethod(method).node().getExtraIncluded(extra);
    }

    public void setNetworkBaseFee(String method, double v) {
        this.getMethod(method).network().setBase(v);
    }
    public void setNodeBaseFee(String method, double v) {
        this.getMethod(method).node().setBase(v);
    }

    public void setNetworkExtrasIncluded(String method, String signatureVerifications, int count) {
        if(!this.methods.containsKey(method)) this.methods.put(method, new MockServiceMethod());
        this.methods.get(method).network().setExtraIncluded(signatureVerifications,count);
    }

    public void setNodeExtrasIncluded(String method, String signatureVerifications, int count) {
        if(!this.methods.containsKey(method)) this.methods.put(method, new MockServiceMethod());
        this.methods.get(method).node().setExtraIncluded(signatureVerifications,count);
    }

    public void setExtrasFee(String signatureVerifications, double fee) {
        this.extras.put(signatureVerifications,fee);
    }
}
