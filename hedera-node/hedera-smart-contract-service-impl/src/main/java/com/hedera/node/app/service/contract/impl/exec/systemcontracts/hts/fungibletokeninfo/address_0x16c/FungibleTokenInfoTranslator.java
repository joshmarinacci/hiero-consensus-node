// SPDX-License-Identifier: Apache-2.0
package com.hedera.node.app.service.contract.impl.exec.systemcontracts.hts.fungibletokeninfo.address_0x16c;

import static com.hedera.node.app.service.contract.impl.exec.systemcontracts.HtsSystemContract.HTS_16C_CONTRACT_ID;
import static com.hedera.node.app.service.contract.impl.utils.ConversionUtils.fromHeadlongAddress;
import static java.util.Objects.requireNonNull;

import com.hedera.node.app.service.contract.impl.exec.metrics.ContractMetrics;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.common.AbstractCallTranslator;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.common.Call;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.hts.HtsCallAttempt;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.hts.ReturnTypes;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.hts.fungibletokeninfo.FungibleTokenInfoCall;
import com.hedera.node.app.service.contract.impl.exec.utils.SystemContractMethod;
import com.hedera.node.app.service.contract.impl.exec.utils.SystemContractMethod.Category;
import com.hedera.node.app.service.contract.impl.exec.utils.SystemContractMethod.Variant;
import com.hedera.node.app.service.contract.impl.exec.utils.SystemContractMethodRegistry;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;
import javax.inject.Inject;

public class FungibleTokenInfoTranslator extends AbstractCallTranslator<HtsCallAttempt> {

    /** Selector for getFungibleTokenInfo(address) method. */
    public static final SystemContractMethod FUNGIBLE_TOKEN_INFO_16C = SystemContractMethod.declare(
                    "getFungibleTokenInfo(address)", ReturnTypes.RESPONSE_CODE_FUNGIBLE_TOKEN_INFO_V2)
            .withVariants(Variant.V2, Variant.FT)
            .withSupportedAddress(HTS_16C_CONTRACT_ID)
            .withCategory(Category.TOKEN_QUERY);

    /**
     * Default constructor for injection.
     */
    @Inject
    public FungibleTokenInfoTranslator(
            @NonNull final SystemContractMethodRegistry systemContractMethodRegistry,
            @NonNull final ContractMetrics contractMetrics) {
        // Dagger2
        super(SystemContractMethod.SystemContract.HTS, systemContractMethodRegistry, contractMetrics);

        registerMethods(FUNGIBLE_TOKEN_INFO_16C);
    }

    @Override
    public @NonNull Optional<SystemContractMethod> identifyMethod(@NonNull final HtsCallAttempt attempt) {
        requireNonNull(attempt);

        return attempt.isMethod(FUNGIBLE_TOKEN_INFO_16C);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Call callFrom(@NonNull final HtsCallAttempt attempt) {
        requireNonNull(attempt);
        final var args = FUNGIBLE_TOKEN_INFO_16C.decodeCall(attempt.input().toArrayUnsafe());
        final var token = attempt.linkedToken(fromHeadlongAddress(args.get(0)));
        return new FungibleTokenInfoCall(
                attempt.systemContractGasCalculator(),
                attempt.enhancement(),
                attempt.isStaticCall(),
                token,
                attempt.configuration(),
                FUNGIBLE_TOKEN_INFO_16C.function());
    }
}
