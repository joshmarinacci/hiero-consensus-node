package org.hiero.hapi.fees.apis.common;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.transaction.ExchangeRate;
import org.hiero.hapi.fees.FeeModel;
import org.hiero.hapi.fees.FeeResult;
import org.hiero.hapi.support.fees.ExtraFeeDefinition;
import org.hiero.hapi.support.fees.ExtraFeeReference;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.hiero.hapi.support.fees.Service;
import org.hiero.hapi.support.fees.ServiceFee;

import java.util.Map;

import static org.hiero.hapi.fees.FeeScheduleUtils.lookupExtraFee;
import static org.hiero.hapi.fees.FeeScheduleUtils.lookupServiceFee;

public class EntityCreate implements FeeModel {
    private final HederaFunctionality api;
    private final String description;

    public EntityCreate(HederaFunctionality api, String description) {
        this.api = api;
        this.description = description;
    }

    @Override
    public HederaFunctionality getApi() {
        return this.api;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public FeeResult computeFee(Map<String, Object> params, ExchangeRate exchangeRate, FeeSchedule feeSchedule) {
        var result = new FeeResult();
        result.addDetail("Base Fee for "+this.getApi(), 1, 22);

        ServiceFee serviceDef = lookupServiceFee(feeSchedule,this.api);
        for (ExtraFeeReference ref : serviceDef.extras()) {
            if (!params.containsKey(ref.name().name())) {
                throw new Error("input params missing " + ref.name() + " required by method " + this.api);
            }
            int included = ref.includedCount();
            long used = (long) params.get(ref.name().name());
            long extraFee = lookupExtraFee(feeSchedule, ref).fee();
            if (used > included) {
                final long overage = used - included;
                result.addDetail("Overage of "+ref.name().name(), overage, 2 * extraFee);
            }
        }


        System.out.println("final details are "+result);
        return result;
    }

}
