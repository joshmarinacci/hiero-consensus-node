package com.hedera.node.app.hapi.fees;


import com.hedera.hapi.node.consensus.FeeComponent;
import com.hedera.hapi.node.consensus.ServiceMethod;

import java.util.HashMap;

public class MockSimpleFeesSchedule implements AbstractSimpleFeesSchedule {
    public final HashMap<String, ServiceMethod> methods;
    public MockSimpleFeesSchedule() {
        methods = new HashMap<>();
    }
    @Override
    public double getExtrasFee(String name) {
        return 0;
    }

    @Override
    public double getNetworkBaseFee(String api) {
        return Double.parseDouble(this.methods.get(api).network().base());
    }

    @Override
    public int getNetworkBaseExtrasIncluded(String api, String name) {
        return 0;
    }

    public void setNetworkBaseFee(String name, double v) {
        if(!this.methods.containsKey(name)) {
            this.methods.put(name, ServiceMethod.DEFAULT);
        }
        var network = this.methods.get(name).networkOrElse(FeeComponent.DEFAULT);
        var new_network = network.copyBuilder().base(""+v).build();
        System.out.println(new_network);
        var new_method = this.methods.get(name).copyBuilder().network(new_network).build();
        System.out.println(new_method);
        this.methods.put(name, new_method);
    }
}
