package com.hedera.node.app.hapi.fees;

public interface AbstractSimpleFeesSchedule {

    public double getExtrasFee(String name);

    public double getBaseFee(String api);
}
