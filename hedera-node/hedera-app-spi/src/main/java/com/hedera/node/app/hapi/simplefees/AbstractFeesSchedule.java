package com.hedera.node.app.hapi.simplefees;

import com.hedera.hapi.node.base.HederaFunctionality;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;

import java.util.List;

public interface AbstractFeesSchedule {
    FeeSchedule getRawSchedule();

    List<Extra> getDefinedExtraNames();
    long getExtrasFee(Extra name);

    long getNodeBaseFee();
    List<Extra> getNodeExtraNames();
    long getNodeExtraIncludedCount(Extra name);

    long getNetworkMultiplier();

    List<HederaFunctionality> getServiceNames();
    long getServiceBaseFee(HederaFunctionality method);
    List<Extra> getServiceExtras(HederaFunctionality method);
    long getServiceExtraIncludedCount(HederaFunctionality method, Extra name);


}
