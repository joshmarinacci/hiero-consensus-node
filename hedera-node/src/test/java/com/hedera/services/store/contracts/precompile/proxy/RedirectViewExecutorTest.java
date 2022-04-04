package com.hedera.services.store.contracts.precompile.proxy;

import com.hedera.services.state.enums.TokenType;
import com.hedera.services.store.contracts.HederaStackedWorldStateUpdater;
import com.hedera.services.store.contracts.WorldLedgers;
import com.hedera.services.store.contracts.precompile.BalanceOfWrapper;
import com.hedera.services.store.contracts.precompile.DecodingFacade;
import com.hedera.services.store.contracts.precompile.EncodingFacade;
import com.hedera.services.store.contracts.precompile.OwnerOfAndTokenURIWrapper;
import com.hedera.services.store.models.Id;
import com.hedera.services.store.models.NftId;
import com.hedera.test.utils.IdUtils;
import com.hedera.test.utils.TxnUtils;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hederahashgraph.api.proto.java.Timestamp;
import com.hederahashgraph.api.proto.java.TokenID;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.Gas;
import org.hyperledger.besu.evm.frame.BlockValues;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.hedera.services.store.contracts.precompile.HTSPrecompiledContract.ABI_ID_BALANCE_OF_TOKEN;
import static com.hedera.services.store.contracts.precompile.HTSPrecompiledContract.ABI_ID_DECIMALS;
import static com.hedera.services.store.contracts.precompile.HTSPrecompiledContract.ABI_ID_NAME;
import static com.hedera.services.store.contracts.precompile.HTSPrecompiledContract.ABI_ID_OWNER_OF_NFT;
import static com.hedera.services.store.contracts.precompile.HTSPrecompiledContract.ABI_ID_SYMBOL;
import static com.hedera.services.store.contracts.precompile.HTSPrecompiledContract.ABI_ID_TOKEN_URI_NFT;
import static com.hedera.services.store.contracts.precompile.HTSPrecompiledContract.ABI_ID_TOTAL_SUPPLY_TOKEN;
import static com.hedera.services.store.contracts.precompile.proxy.RedirectViewExecutor.MINIMUM_TINYBARS_COST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RedirectViewExecutorTest {
	@Mock
	private Bytes input;
	@Mock
	private MessageFrame frame;
	@Mock
	private EncodingFacade encodingFacade;
	@Mock
	private DecodingFacade decodingFacade;
	@Mock
	private RedirectGasCalculator redirectGasCalculator;
	@Mock
	private HederaStackedWorldStateUpdater stackedWorldStateUpdater;
	@Mock
	private WorldLedgers worldLedgers;
	@Mock
	private Bytes nestedInput;
	@Mock
	private Address tokenAddress;
	@Mock
	private BlockValues blockValues;
	@Mock
	private BalanceOfWrapper balanceOfWrapper;
	@Mock
	private OwnerOfAndTokenURIWrapper ownerOfAndTokenURIWrapper;

	public static final AccountID account = IdUtils.asAccount("0.0.777");
	public static final TokenID fungible = IdUtils.asToken("0.0.888");
	public static final TokenID nonfungibletoken = IdUtils.asToken("0.0.999");
	public static final NftId nonfungible = new NftId(0, 0, 999, 1);
	public static final Id fungibleId = Id.fromGrpcToken(fungible);
	public static final Id nonfungibleId = Id.fromGrpcToken(nonfungibletoken);
	public static final Address fungibleTokenAddress = fungibleId.asEvmAddress();
	public static final Address nonfungibleTokenAddress = nonfungibleId.asEvmAddress();

	private static final long timestamp = 10l;
	private static final Timestamp resultingTimestamp = Timestamp.newBuilder().setSeconds(timestamp).build();
	private static final Gas gas = Gas.of(100L);
	private static final Bytes answer = Bytes.of(1);

	RedirectViewExecutor subject;

	@BeforeEach
	void setup() {
		given(frame.getWorldUpdater()).willReturn(stackedWorldStateUpdater);
		given(stackedWorldStateUpdater.trackingLedgers()).willReturn(worldLedgers);
		this.subject = new RedirectViewExecutor(input, frame, encodingFacade, decodingFacade, redirectGasCalculator);
	}

	@Test
	void computeCostedNAME() {
		prerequisites(ABI_ID_NAME);

		final var result = "name";

		given(worldLedgers.nameOf(fungible)).willReturn(result);
		given(encodingFacade.encodeName(result)).willReturn(answer);

		assertEquals(Pair.of(gas, answer), subject.computeCosted());
	}

	@Test
	void computeCostedSYMBOL() {
		prerequisites(ABI_ID_SYMBOL);

		final var result = "symbol";

		given(worldLedgers.symbolOf(fungible)).willReturn(result);
		given(encodingFacade.encodeSymbol(result)).willReturn(answer);

		assertEquals(Pair.of(gas, answer), subject.computeCosted());
	}

	@Test
	void computeCostedDECIMALS() {
		prerequisites(ABI_ID_DECIMALS);

		final var result = 1;

		given(worldLedgers.typeOf(fungible)).willReturn(TokenType.FUNGIBLE_COMMON);
		given(worldLedgers.decimalsOf(fungible)).willReturn(result);
		given(encodingFacade.encodeDecimals(result)).willReturn(answer);

		assertEquals(Pair.of(gas, answer), subject.computeCosted());
	}

	@Test
	void computeCostedTOTAL_SUPPY_TOKEN() {
		prerequisites(ABI_ID_TOTAL_SUPPLY_TOKEN);

		final var result = 1l;

		given(worldLedgers.totalSupplyOf(fungible)).willReturn(result);
		given(encodingFacade.encodeTotalSupply(result)).willReturn(answer);

		assertEquals(Pair.of(gas, answer), subject.computeCosted());
	}

	@Test
	void computeCostedBALANCE_OF_TOKEN() {
		prerequisites(ABI_ID_BALANCE_OF_TOKEN);

		final var result = 1l;

		given(decodingFacade.decodeBalanceOf(eq(nestedInput), any())).willReturn(balanceOfWrapper);
		given(balanceOfWrapper.accountId()).willReturn(account);
		given(worldLedgers.balanceOf(account, fungible)).willReturn(result);
		given(encodingFacade.encodeBalance(result)).willReturn(answer);

		assertEquals(Pair.of(gas, answer), subject.computeCosted());
	}

	@Test
	void computeCostedOWNER_OF_NFT() {
		prerequisites(ABI_ID_OWNER_OF_NFT);
		given(tokenAddress.toArrayUnsafe()).willReturn(nonfungibleTokenAddress.toArray());

		final var result = Address.fromHexString("0x000000000000013");
		final var serialNum = 1l;

		given(decodingFacade.decodeOwnerOf(nestedInput)).willReturn(ownerOfAndTokenURIWrapper);
		given(ownerOfAndTokenURIWrapper.serialNo()).willReturn(serialNum);
		given(worldLedgers.ownerOf(nonfungible)).willReturn(result);
		given(worldLedgers.canonicalAddress(result)).willReturn(result);
		given(encodingFacade.encodeOwner(result)).willReturn(answer);

		assertEquals(Pair.of(gas, answer), subject.computeCosted());
	}

	@Test
	void computeCostedTOKEN_URI_NFT() {
		prerequisites(ABI_ID_TOKEN_URI_NFT);
		given(tokenAddress.toArrayUnsafe()).willReturn(nonfungibleTokenAddress.toArray());

		final var result = "some metadata";
		final var serialNum = 1l;

		given(decodingFacade.decodeTokenUriNFT(nestedInput)).willReturn(ownerOfAndTokenURIWrapper);
		given(ownerOfAndTokenURIWrapper.serialNo()).willReturn(serialNum);
		given(worldLedgers.metadataOf(nonfungible)).willReturn(result);
		given(encodingFacade.encodeTokenUri(result)).willReturn(answer);

		assertEquals(Pair.of(gas, answer), subject.computeCosted());
	}

	@Test
	void computeCostedNOT_SUPPORTED() {
		prerequisites(0);
		TxnUtils.assertFailsWith(() -> subject.computeCosted(), ResponseCodeEnum.NOT_SUPPORTED);
	}

	void prerequisites(int descriptor) {
		given(input.slice(4, 20)).willReturn(tokenAddress);
		given(tokenAddress.toArrayUnsafe()).willReturn(fungibleTokenAddress.toArray());
		given(input.slice(24)).willReturn(nestedInput);
		given(nestedInput.getInt(0)).willReturn(descriptor);
		given(frame.getBlockValues()).willReturn(blockValues);
		given(blockValues.getTimestamp()).willReturn(timestamp);
		given(redirectGasCalculator.compute(resultingTimestamp, MINIMUM_TINYBARS_COST)).willReturn(gas);
	}
}
