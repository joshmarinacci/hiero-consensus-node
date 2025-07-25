package com.hedera.node.app.hapi.fees;

public interface AbstractSimpleFeesSchedule {
    public static String SignatureVerifications = "SignatureVerifications";

    public double getExtrasFee(String name);

    public double getBaseFee(String api);

    int getBaseExtrasIncluded(String api, String name);
}
