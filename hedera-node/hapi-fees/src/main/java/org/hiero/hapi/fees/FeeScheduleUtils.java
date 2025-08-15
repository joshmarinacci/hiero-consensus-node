package org.hiero.hapi.fees;

import com.hedera.hapi.node.base.HederaFunctionality;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.ExtraFeeDefinition;
import org.hiero.hapi.support.fees.ExtraFeeReference;
import org.hiero.hapi.support.fees.Service;
import org.hiero.hapi.support.fees.ServiceFee;

public class FeeScheduleUtils {
    public static ExtraFeeDefinition makeExtraDef(Extra extra, long fee) {
        return ExtraFeeDefinition.newBuilder().name(extra).fee(fee).build();
    }

    public static ExtraFeeReference makeExtraIncluded(Extra extra, int included) {
        return ExtraFeeReference.DEFAULT.copyBuilder()
                .name(extra).includedCount(included).build();
    }

    public static ServiceFee makeServiceFee(HederaFunctionality name, long baseFee, ExtraFeeReference... reference) {
        return ServiceFee.DEFAULT.copyBuilder()
                .name(name).baseFee(baseFee).extras(reference).build();
    }
    public static Service makeService(String name, ServiceFee... services) {
        return Service.DEFAULT.copyBuilder().name(name).transactions(services).build();
    }

}
