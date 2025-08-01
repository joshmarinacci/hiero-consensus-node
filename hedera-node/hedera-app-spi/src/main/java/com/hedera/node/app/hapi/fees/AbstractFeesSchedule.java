package com.hedera.node.app.hapi.fees;

public interface AbstractFeesSchedule {
    public static String SignatureVerifications = "SignatureVerifications";
    public static String Bytes = "Bytes";

    public double getExtrasFee(String name);

    public double getNetworkBaseFee(String api);

    int getNetworkBaseExtrasIncluded(String api, String name);
}
