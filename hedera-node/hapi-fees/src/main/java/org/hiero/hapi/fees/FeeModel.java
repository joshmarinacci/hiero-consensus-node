// SPDX-License-Identifier: Apache-2.0
package org.hiero.hapi.fees;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.transaction.ExchangeRate;
import java.util.Map;
import org.hiero.hapi.support.fees.FeeSchedule;

public interface FeeModel {
    HederaFunctionality getApi();

    String getDescription();

    FeeResult computeFee(Map<String, Object> params, ExchangeRate exchangeRate, FeeSchedule feeSchedule);
}
