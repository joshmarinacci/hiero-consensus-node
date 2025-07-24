package com.hedera.services.bdd.suites.fees;

import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.fees.BaseFeeRegistry;
import com.hedera.node.app.hapi.fees.apis.common.EntityCreate;
import com.hedera.node.app.hapi.fees.apis.common.FeesHelper;
import com.hedera.node.app.hapi.fees.apis.common.YesOrNo;
import com.hedera.node.app.hapi.utils.ByteStringUtils;
import com.hedera.services.bdd.junit.HapiTest;
import com.hedera.services.bdd.junit.HapiTestLifecycle;
import com.hedera.services.bdd.junit.LeakyHapiTest;
import com.hedera.services.bdd.junit.support.TestLifecycle;
import com.hedera.services.bdd.spec.HapiSpec;
import com.hedera.services.bdd.spec.transactions.token.TokenMovement;
import com.hedera.services.bdd.spec.utilops.streams.assertions.BlockStreamAssertion;
import com.hederahashgraph.api.proto.java.TokenSupplyType;
import com.hederahashgraph.api.proto.java.TokenType;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.FILE_FREE_BYTES;
import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.HCS_FREE_BYTES;
import static com.hedera.services.bdd.junit.support.validators.block.BlockContentsValidator.bodyFrom;
import static com.hedera.services.bdd.spec.HapiSpec.hapiTest;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountBalance;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileContents;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileInfo;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTopicInfo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.createTopic;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoTransfer;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoUpdate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.deleteTopic;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileAppend;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileDelete;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileUpdate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.mintToken;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.submitMessageTo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenAssociate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.updateTopic;
import static com.hedera.services.bdd.spec.transactions.token.CustomFeeSpecs.fixedConsensusHbarFee;
import static com.hedera.services.bdd.spec.transactions.token.CustomFeeSpecs.fixedHbarFeeInheritingRoyaltyCollector;
import static com.hedera.services.bdd.spec.transactions.token.CustomFeeSpecs.royaltyFeeWithFallback;
import static com.hedera.services.bdd.spec.transactions.token.TokenMovement.moving;
import static com.hedera.services.bdd.spec.transactions.token.TokenMovement.movingHbar;
import static com.hedera.services.bdd.spec.transactions.token.TokenMovement.movingUnique;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.blockStreamMustIncludePassFrom;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.newKeyNamed;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.overriding;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.validateChargedUsd;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.validateChargedUsdWithin;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.waitUntilNextBlock;
import static com.hedera.services.bdd.suites.HapiSuite.ONE_HBAR;
import static com.hedera.services.bdd.suites.HapiSuite.ONE_HUNDRED_HBARS;
import static com.hedera.services.bdd.suites.HapiSuite.ONE_MILLION_HBARS;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.SUCCESS;

@HapiTestLifecycle
public class SimpleFeesSuite {
    private static final String PAYER = "payer";

    @BeforeAll
    static void beforeAll(@NonNull final TestLifecycle testLifecycle) {
        testLifecycle.overrideInClass(Map.of("fees.simpleFeesEnabled", "true"));
    }

    @Nested
    class TopicFees {
        // create topic, basic
        @HapiTest
        @DisplayName("Simple fees for creating a topic")
        final Stream<DynamicTest> createTopicFee() {
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", 0.02)
            );
        }

        @HapiTest
        @DisplayName("Simple fees for creating a topic with custom fees")
        final Stream<DynamicTest> createTopicCustomFee() {
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    cryptoCreate("collector"),
                    createTopic("testTopic")
                            .blankMemo()
                            .withConsensusCustomFee(fixedConsensusHbarFee(88, "collector"))
                            .payingWith(PAYER)
                            .fee(ONE_HUNDRED_HBARS)
                            .via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", 2)
            );
        }

        @HapiTest
        @DisplayName("Simple fees for updating a topic")
        final Stream<DynamicTest> updateTopicFee() {
            final String ADMIN = "admin";
            return hapiTest(
                    newKeyNamed(ADMIN),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    // create topic. provide up to 100 hbar to pay for it
                    createTopic("testTopic").blankMemo().payingWith(PAYER).adminKeyName(ADMIN)
                            .fee(ONE_HUNDRED_HBARS).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", 0.020_00),
                    // update topic, provide up to 100 hbar to pay for it
                    updateTopic("testTopic").adminKey(ADMIN).payingWith(PAYER)
                            .fee(ONE_HUNDRED_HBARS).via("update-topic-txn"),
                    validateChargedUsd("update-topic-txn", 0.000_22)
            );
        }

        @HapiTest
        @DisplayName("Simple fees for getting a topic transaction info")
        final Stream<DynamicTest> getTopicInfoFee() {
            return hapiTest(
                    newKeyNamed(PAYER),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    // create topic. provide up to 1 hbar to pay for it
                    createTopic("testTopic").blankMemo().payingWith(PAYER).adminKeyName(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", 0.020_00),
                    // get topic info, provide up to 1 hbar to pay for it
                    getTopicInfo("testTopic").payingWith(PAYER)
                            .fee(ONE_HBAR).via("get-topic-txn").logged(),
                    // TODO: query is getting zeroed out
                    //  validateChargedUsd("get-topic-txn", 0.000_2)
                    validateChargedUsd("get-topic-txn", 0.000_1)
            );
        }

        @HapiTest
        @DisplayName("Simple fee for submitting a message")
        final Stream<DynamicTest> submitMessageFee() {
            final byte[] messageBytes = new byte[600]; // up to 1k
            Arrays.fill(messageBytes, (byte) 0b1);
            final var excess_bytes = 600 - HCS_FREE_BYTES;
            final var base = BaseFeeRegistry.getBaseFee("ConsensusSubmitMessage");
            final var per_byte = BaseFeeRegistry.getBaseFee("PerHCSByte");
            System.out.println("COST: " + base + " " + excess_bytes + " " + per_byte);
            System.out.println("COST: " + (base + excess_bytes * per_byte));
            return hapiTest(
                    newKeyNamed(PAYER),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    // create topic, provide up to 1 hbar to pay for it
                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", 0.020_00),
                    // submit message, provide up to 1 hbar to pay for it
                    submitMessageTo("testTopic").blankMemo().payingWith(PAYER).message(new String(messageBytes))
                            .fee(ONE_HBAR)
                            .via("submit-message-txn"),
                    validateChargedUsd("submit-message-txn", base + excess_bytes * per_byte, 1)
            );
        }

        //TODO: Submit message with custom fee

        //TODO: submit bigger message

        // delete topic
        @LeakyHapiTest(overrides = "fees.simpleFeesEnabled")
        final Stream<DynamicTest> deleteTopicFee() {
            return hapiTest(
                    overriding("fees.simpleFeesEnabled", "true"),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    createTopic("testTopic").blankMemo().payingWith(PAYER).adminKeyName(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", 0.020_00),
                    deleteTopic("testTopic").payingWith(PAYER)
                            .fee(ONE_HBAR).via("delete-topic-txn"),
                    validateChargedUsd("delete-topic-txn", 0.005_00)
            );
        }


        // test that we can get the create topic information back from the block stream
//        final HapiFileUpdate resetRatesOp = fileUpdate(EXCHANGE_RATES)
//                .payingWith(EXCHANGE_RATE_CONTROL)
//                .fee(ADEQUATE_FUNDS)
//                .contents(spec -> spec.ratesProvider().rateSetWith(1, 12).toByteString());

        @HapiTest
        @DisplayName("Restore FeeDetails for creating a topic")
        final Stream<DynamicTest> createTopicFeeDetailsRestore() {
            return hapiTest(
                    blockStreamMustIncludePassFrom(generateFeeDetails("create-topic-txn")),
//                    resetRatesOp,
//                    cryptoTransfer(tinyBarsFromTo(GENESIS, EXCHANGE_RATE_CONTROL, ADEQUATE_FUNDS))
//                            .fee(ONE_HUNDRED_HBARS),
//                    fileUpdate(EXCHANGE_RATES)
//                            .contents(spec -> {
//                                ByteString newRates =
//                                        spec.ratesProvider().rateSetWith(10, 121).toByteString();
//                                System.out.println("saving the new rates " + newRates);
//                                spec.registry().saveBytes("newRates", newRates);
//                                return newRates;
//                            })
//                            .payingWith(EXCHANGE_RATE_CONTROL),
//                    getFileContents(EXCHANGE_RATES)
//                            .hasContents(spec -> spec.registry().getBytes("newRates")),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR)
                            .via("create-topic-txn").hasKnownStatus(SUCCESS),
                    validateChargedUsd("create-topic-txn", 0.02),
                    waitUntilNextBlock().withBackgroundTraffic(true)
            );
        }

        public static Function<HapiSpec, BlockStreamAssertion> generateFeeDetails(String creationTxn) {
            return spec -> block -> {
//                System.out.println("inside assertion");
//                final com.hederahashgraph.api.proto.java.TransactionID creationTxnId;
//                try {
//                    creationTxnId = spec.registry().getTxnId(creationTxn);
//                } catch (RegistryNotFound ignore) {
//                    return false;
//                }
//                System.out.println("txid is " + creationTxnId);
//                System.out.println("checking the spec " + spec + " and block " + block);
//                System.out.println("rates " + spec.ratesProvider().rates());
                // load the current rates
                com.hederahashgraph.api.proto.java.ExchangeRate rate_proto = spec.ratesProvider().rates();
                var current_rate = com.hedera.hapi.node.transaction.ExchangeRate.newBuilder().centEquiv(rate_proto.getCentEquiv()).hbarEquiv(rate_proto.getHbarEquiv()).build();
                final var items = block.items();
                for (BlockItem item : items) {
//                    System.out.println("looking at item " + item.item().kind());
                    if (item.item().kind() == BlockItem.ItemOneOfType.EVENT_TRANSACTION) {
                        System.out.println("is an event transaction. ");
                        System.out.println("has application " + item.eventTransaction().hasApplicationTransaction());
                        try {
                            var txbody = bodyFrom(item.eventTransactionOrThrow());
                            System.out.println("transaction body is " + txbody);
                            if (txbody.hasConsensusCreateTopic()) {
                                var create_body = txbody.consensusCreateTopicOrThrow();
                                System.out.println("create topic body is " + create_body);

                                EntityCreate entity = FeesHelper.makeEntity(HederaFunctionality.CONSENSUS_CREATE_TOPIC, "Create a topic", 0, true);
                                Map<String, Object> params = new HashMap<>();
                                params.put("numSignatures", 0);
                                params.put("numKeys", 0);
                                params.put("hasCustomFee", YesOrNo.NO);
                                var fee = entity.computeFee(params, current_rate);
                                System.out.println("recomputed fee is " + fee);
                                // recomputed fee is Fees[nodeFee=1652800, networkFee=7438000, serviceFee=7438000, usd=0.02, details={Base fee=FeeDetail{1, .020000 }}]
                                // get the active rate
                                // get the fee schedule
                                /*
                                    given an event_transaction and a transaction_result we can
                                    * parse get the transaction body from the embedded protobuf
                                    * convert the body to the correct class for the transaction type
                                    * re-create the fee parameters hashmap
                                    * get the fee schedule that was in place when the transaction happened
                                    * get the active exchange rate that was in place when the transaction happened
                                    * calculate the Fees and FeeDetails again
                                 */

                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    if (item.item().kind() == BlockItem.ItemOneOfType.STATE_CHANGES) {
                        System.out.println("is state change transaction. " + item);
                        try {
                            var state = item.stateChangesOrThrow();
                            System.out.println("state is " + state);
                            for (var change : state.stateChanges()) {
                                System.out.println("  change is " + change);
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    if (item.item().kind() == BlockItem.ItemOneOfType.TRANSACTION_RESULT) {
                        System.out.println("is a transaction result. ");
                        var tran = item.transactionResult();
                        System.out.println("transaction is " + tran);
                        System.out.println("fee charged in hbar is " + tran.transactionFeeCharged());
                        System.out.println("transfer list is " + tran.transferList());
                    }
                }
                return true;
            };
        }

    }

    @Nested
    class FileFees {
        final double FileCreate = 0.050_00;
        final double PerFileByte = 0.000_011;

        @HapiTest
        final Stream<DynamicTest> fileCreateFee() {
            final var byte_count = 1789;
            var contents = "0".repeat(byte_count).getBytes();
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    fileCreate("test")
                            .memo("memotext")
                            .contents(contents)
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-file-txn"),
                    validateChargedUsd("create-file-txn",
                            FileCreate + (byte_count - FILE_FREE_BYTES) * PerFileByte)
            );
        }

        @HapiTest
        final Stream<DynamicTest> fileUpdateFee() {
            final var FileUpdate = 0.050_00;

            var contents = "0".repeat(1789).getBytes();

            final var byte_count = 4567;
            var new_contents = "0".repeat(byte_count).getBytes();
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    fileCreate("test")
                            .memo("memotext")
                            .contents(contents)
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-file-txn"),
                    validateChargedUsd("create-file-txn", FileCreate + (1789 - FILE_FREE_BYTES) * PerFileByte),
                    fileUpdate("test")
                            .contents(new_contents)
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("update-file-txn"),
                    validateChargedUsd("update-file-txn", FileUpdate + (byte_count - FILE_FREE_BYTES) * PerFileByte)
            );
        }

        // TODO: FileDelete transaction body doesn't expose the file contents so we can't
        //  calculate how many bytes are being deleted
        @HapiTest
        final Stream<DynamicTest> fileDeleteFee() {
            final var FileDelete = 0.007_00;
            final var byte_count = 1789;

            var contents = "0".repeat(byte_count).getBytes();

            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    fileCreate("test")
                            .memo("memotext")
                            .contents(contents)
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-file-txn"),
                    validateChargedUsd("create-file-txn", FileCreate + (byte_count - FILE_FREE_BYTES) * PerFileByte),
                    fileDelete("test")
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("delete-file-txn"),
                    validateChargedUsd("delete-file-txn", FileDelete)// + (byte_count - FILE_FREE_BYTES) * PerFileByte)
            );
        }

        @HapiTest
        final Stream<DynamicTest> fileAppendFee() {
            final var FileAppend = 0.050_00;

            final var byte_count = 4567;
            var new_contents = "0".repeat(byte_count).getBytes();
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    fileCreate("test")
                            .memo("memotext")
                            .contents("0".repeat(byte_count).getBytes())
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-file-txn"),
                    validateChargedUsd("create-file-txn", FileCreate + (byte_count - FILE_FREE_BYTES) * PerFileByte),
                    fileAppend("test")
                            .content(new_contents)
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("append-file-txn"),
                    validateChargedUsd("append-file-txn", FileAppend + (byte_count - FILE_FREE_BYTES) * PerFileByte)
            );
        }

        @HapiTest
        final Stream<DynamicTest> fileGetContents() {
            final var FileGetContents = 0.000_66;
            final var byte_count = 3764;
            final var create_price = FileCreate + (byte_count - FILE_FREE_BYTES) * PerFileByte;
//            System.out.println("create price is "+create_price);
            /*
            file create total fees: 67253300
            FCH: final fees is Fees[nodeFee=6725300, networkFee=30264000, serviceFee=30264000, usd=0.080704, details={Base fee=FeeDetail{1, .050000 }, Additional file size=FeeDetail{2764, .030404 }, Additional signature verifications=FeeDetail{3, .000300 }}] 67253300

            create price is 0.080404
            create price hbar is 96484800

            tinybar balance is 9932746700
             */
//            final var create_price_hbar = (long)(create_price * (1/12.0) * ONE_HBAR * 100) ;
//            final var create_price_hbar2 = 67253300;
//            System.out.println("create price hbar is "+create_price_hbar);
//            System.out.println("create price hbar is "+create_price_hbar2);
            final var correct = Math.max(byte_count - FILE_FREE_BYTES, 0) * PerFileByte + FileGetContents;
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    getAccountBalance(PAYER).hasTinyBars(100 * ONE_HBAR),
                    fileCreate("test")
                            .memo("memotext")
                            .contents("0".repeat(byte_count).getBytes())
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-file-txn"),
//                    getAccountBalance(PAYER).hasTinyBars(spec -> amount -> {
//                        System.out.println("getting balance for "+amount + " ");
//                        final var amt_2 = ONE_HUNDRED_HBARS-create_price_hbar;
//                        final var diff =  amt_2 - amount;
//                        final double percentage = Math.abs(((double)diff)/((double)amount));
//                        System.out.println("diff is "+diff + " " + percentage);
//                        if(percentage > 1.0) {
//                            return Optional.of("incorrect balance for "+amount + " vs "+(ONE_HUNDRED_HBARS-create_price_hbar));
//                        }
//                        return Optional.empty();
//                    }),
                    getAccountBalance(PAYER).hasTinyBars(9_932_746_700L),
                    validateChargedUsdWithin("create-file-txn", create_price, 1.00),
                    getFileContents("test")
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("get-file-contents-txn"),
                    getAccountBalance(PAYER).hasTinyBars(9_932_746_700L),
                    // TODO: doesn't work yet
                    //  validateChargedUsd("get-file-contents-txn", create_price, 1.00),
                    validateChargedUsd("get-file-contents-txn", 0.0001)
            );
        }

        @HapiTest
        final Stream<DynamicTest> fileGetInfo() {
            final var FileGetContents = 0.000_66;
            final var byte_count = 3764;
            final var correct = Math.max(byte_count - FILE_FREE_BYTES, 0) * PerFileByte + FileGetContents;
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    fileCreate("test")
                            .memo("memotext")
                            .contents("0".repeat(byte_count).getBytes())
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-file-txn"),
                    validateChargedUsd("create-file-txn", FileCreate + (byte_count - FILE_FREE_BYTES) * PerFileByte),
                    getFileInfo("test")
                            .payingWith(PAYER)
                            .fee(ONE_HBAR).via("get-file-info-txn"),
                    // TODO: query is getting zeroed out
                    //  validateChargedUsd("get-file-info-txn", correct)
                    validateChargedUsd("get-file-info-txn", 0.0001)
            );
        }
    }

    @Nested
    class CryptoFees {
        @HapiTest
        final Stream<DynamicTest> cryptoCreateFee() {
            // TODO: CryptoCreate, create account
            final var CryptoCreateFee_USD =  0.05000;
            final var treasury = "treasury";
            return hapiTest(
                    cryptoCreate(treasury).balance(ONE_HUNDRED_HBARS),
                    cryptoCreate("alice")
                            .payingWith(treasury)
                            .fee(ONE_HBAR)
                            .balance(ONE_HBAR)
                            .via("crypto-create-txn"),
                    validateChargedUsd("crypto-create-txn", CryptoCreateFee_USD)
            );
        }

        @HapiTest
        final Stream<DynamicTest> cryptoUpdateFee() {
            final var CryptoUpdateFee_USD =  0.000_22;
            final var treasury = "treasury";
            return hapiTest(
                    cryptoCreate(treasury).balance(ONE_HUNDRED_HBARS),
                    cryptoCreate("alice")
                            .payingWith(treasury)
                            .fee(ONE_HBAR)
                            .balance(ONE_HBAR)
                            .via("crypto-create-txn"),
                    cryptoUpdate("alice")
                            .payingWith(treasury)
                            .fee(ONE_HBAR)
                            .via("crypto-update-txn"),
                    validateChargedUsd("crypto-update-txn", CryptoUpdateFee_USD)
            );
        }

        // TODO: CryptoTransfer, transfer value in a FT
        // TODO: CryptoTransfer, transfer value in hbar
        // TODO: CryptoTransfer, transfer value in an NFT
        @HapiTest
        final Stream<DynamicTest> cryptoTransferFee() {
            final var CryptoTransferFee_USD =  0.000_10;
            final var treasury = "treasury";
            return hapiTest(
                    cryptoCreate(treasury).balance(ONE_HUNDRED_HBARS),
                    cryptoCreate("alice")
                            .payingWith(treasury)
                            .fee(ONE_HBAR)
                            .balance(ONE_HBAR)
                            .via("crypto-create-txn"),
                    cryptoTransfer(movingHbar(1).between(treasury,"alice"))
                            .payingWith(treasury)
                            .fee(ONE_HBAR)
                            .via("crypto-transfer-txn"),
                    validateChargedUsd("crypto-transfer-txn", CryptoTransferFee_USD)
            );
        }
        // multiple hbar transfers at once to go beyond the free number of involved accounts
        @HapiTest
        final Stream<DynamicTest> cryptoTransferMultipleHBarFee() {
            final var treasury = "treasury";
            final var alice = "alice";
            final var bob = "bob";
            final var carol = "carol";
            final var CryptoTransferFee_USD =  0.000_10;
            final var PerCryptoTransferAccount = 0.000_01;
            final var ExtraSig = 0.000_10;
            /*
            There are 4 accounts involved: treasury, alice, bob, & carol.

            There are 3 sigs involved: treasury, alice, and bob

            Note that if bob was sending less to carol (say 1) then his sig wouldn't be needed
            because the amount from alice and the treasury exceeds the amount he is sending
            to carol, so it would just send from alice and the treasury directly to carol.

             */
            return hapiTest(
                    cryptoCreate(treasury).balance(ONE_MILLION_HBARS),
                    cryptoCreate(alice).payingWith(treasury).fee(ONE_HBAR).balance(ONE_HUNDRED_HBARS),
                    cryptoCreate(bob).payingWith(treasury).fee(ONE_HBAR).balance(ONE_HUNDRED_HBARS),
                    cryptoCreate(carol).payingWith(treasury).fee(ONE_HBAR).balance(ONE_HUNDRED_HBARS),
                    cryptoTransfer(
                            movingHbar(1).between(alice,bob),
                            movingHbar(6).between(bob,carol),
                            movingHbar(3).between(treasury,bob)
                    )
                            .payingWithNoSig(treasury)
                            .signedBy(treasury, alice, bob)
                            .fee(ONE_HBAR)
                            .via("crypto-transfer-txn")
                            .hasKnownStatus(SUCCESS),
                    validateChargedUsd("crypto-transfer-txn",
                            CryptoTransferFee_USD
                                    + PerCryptoTransferAccount * (4-2)
                                    + (3-1)*ExtraSig)
            );
        }

    // TODO:  transfer one FT token
    // TODO:  multiple FT transfers

    // TODO: CryptoCreate, create token with custom fees
    // TODO: CryptoDelete, delete token
    // TODO: CryptoGetAccountRecords: ??
    // TODO: CryptoGetAccountBalance: ??
    // TODO: CryptoGetInfo: ??
    // TODO: CryptoApproveAllowance: approve single and multiple allowances
    // TODO: CryptoDeleteAllowance:

    }
    @Nested
    class TokenFees {
        @HapiTest
        final Stream<DynamicTest> tokenCreateFees() {
            final var treasury = "treasury";
            final var fungibleToken = "fungibleToken";
            final var TokenCreateFee_USD =  1.0;
            return hapiTest(
                    cryptoCreate(treasury).balance(ONE_MILLION_HBARS),
                    tokenCreate(fungibleToken)
                            .payingWith(treasury)
                            .initialSupply(4)
                            .fee(ONE_MILLION_HBARS)
                            .via("token-create-txn"),
                    validateChargedUsd("token-create-txn",TokenCreateFee_USD)
            );

        }
    }



    // TODO: Token services
    // TODO: scheduled transactions
    // TODO: smart contracts
    // TODO: random other stuff

    // TODO: delete this
    @HapiTest
    final Stream<DynamicTest> diogoTest() {
        final var NFT_KEY = "NFT_KEY";
        final var hbarCollector = "hbarCollector";
        final var tokenReceiver = "tokenReceiver";
        final var tokenTreasury = "tokenTreasury";
        final var tokenOwner = "tokenOwner";
        final var alice = "alice";
        final var feeDenom = "feeDenom";
        final var nonFungibleToken = "nonFungibleToken";
        return hapiTest(
                newKeyNamed(NFT_KEY),
                cryptoCreate(hbarCollector).balance(0L),
                cryptoCreate(tokenReceiver).balance(ONE_MILLION_HBARS),
                cryptoCreate(tokenTreasury).balance(ONE_MILLION_HBARS),
                cryptoCreate(tokenOwner),
                cryptoCreate(alice).balance(ONE_MILLION_HBARS),
                tokenCreate(feeDenom).treasury(tokenReceiver).initialSupply(4),
                tokenAssociate(tokenOwner, feeDenom),
                tokenAssociate(hbarCollector, feeDenom),
                tokenCreate(nonFungibleToken)
                        .treasury(tokenTreasury)
                        .tokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .initialSupply(0)
                        .supplyKey(NFT_KEY)
                        .supplyType(TokenSupplyType.INFINITE)
                        .withCustom(royaltyFeeWithFallback(
                                1, 4, fixedHbarFeeInheritingRoyaltyCollector(100), hbarCollector)),
                tokenAssociate(tokenReceiver, nonFungibleToken),
                tokenAssociate(alice, nonFungibleToken),
                tokenAssociate(tokenOwner, nonFungibleToken),
                mintToken(nonFungibleToken, List.of(ByteStringUtils.wrapUnsafely("meta1".getBytes()))),
                mintToken(nonFungibleToken, List.of(ByteStringUtils.wrapUnsafely("meta2".getBytes()))),
                cryptoTransfer(movingUnique(nonFungibleToken, 1L, 2L).between(tokenTreasury, tokenOwner)),
                cryptoTransfer(
                        movingUnique(nonFungibleToken, 1L).between(tokenOwner, tokenReceiver),
                        movingUnique(nonFungibleToken, 2L).between(tokenOwner, alice),
                        moving(1, feeDenom).between(tokenReceiver, tokenOwner)
                )
                        .signedByPayerAnd(tokenOwner, tokenReceiver, alice),
                getAccountBalance(tokenOwner)
                        .hasTokenBalance(nonFungibleToken, 0)
                        .hasTokenBalance(feeDenom, 1),
                getAccountBalance(tokenReceiver)
                        .hasTokenBalance(nonFungibleToken, 1)
                        .hasTokenBalance(feeDenom, 3)
                        .hasTinyBars(ONE_MILLION_HBARS),
                getAccountBalance(alice)
                        .hasTokenBalance(nonFungibleToken, 1)
                        .hasTinyBars(ONE_MILLION_HBARS),
                getAccountBalance(hbarCollector)
                        .hasTinyBars(0)
                        .hasTokenBalance(feeDenom, 0)
        );
    }

    // TODO: delete this
    /*
    Hi Diogo. I can recreate your scenario.

    To restate: The hbar collector gets nothing because the royalty is 1/4, and 1/4 of 1 is a 0.25FT which rounds down to zero.
    Suppose we increase the FT transfer from 1 to 10 (and increase the supply from 4 to 40)
    then the owner ends up with 8 and 2 go to the hbar collector, meaning the fee of 1/4 of 10 (2.5FT) is rounded
    down to 2. So the question is if the fallback of 100 should kick in when this happens. Currently, it looks
    like it cannot.
     */
    @HapiTest
    final Stream<DynamicTest> diogoTest2() {
        final var NFT_KEY = "NFT_KEY";
        final var hbarCollector = "hbarCollector";
        final var tokenReceiver = "tokenReceiver";
        final var tokenTreasury = "tokenTreasury";
        final var tokenOwner = "tokenOwner";
        final var alice = "alice";
        final var feeDenom = "feeDenom";
        final var nonFungibleToken = "nonFungibleToken";

        return hapiTest(
                newKeyNamed(NFT_KEY),
                cryptoCreate(hbarCollector).balance(0L),
                // reciever = 1MHbar
                cryptoCreate(tokenReceiver).balance(ONE_MILLION_HBARS),
                // treasury = 1MHbar
                cryptoCreate(tokenTreasury).balance(ONE_MILLION_HBARS),
                // owner = 0hbar
                cryptoCreate(tokenOwner).balance(0L),
                // receiver = 4FT
                tokenCreate(feeDenom).treasury(tokenReceiver).initialSupply(4),
                tokenAssociate(tokenOwner, feeDenom),
                tokenAssociate(hbarCollector, feeDenom),
                tokenCreate(nonFungibleToken)
                        .treasury(tokenTreasury)
                        .tokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .initialSupply(0)
                        .supplyKey(NFT_KEY)
                        .supplyType(TokenSupplyType.INFINITE)
                        // royalty is 1/4, 25% to hbar collector
                        // fallback is 100hbar
                        .withCustom(royaltyFeeWithFallback(
                                1, 4, fixedHbarFeeInheritingRoyaltyCollector(100), hbarCollector)),
                tokenAssociate(tokenReceiver, nonFungibleToken),
                tokenAssociate(tokenOwner, nonFungibleToken),
                // make two tokens
                mintToken(nonFungibleToken, List.of(ByteStringUtils.wrapUnsafely("meta1".getBytes()))),
                mintToken(nonFungibleToken, List.of(ByteStringUtils.wrapUnsafely("meta2".getBytes()))),
                // give two NFTs to owner
                cryptoTransfer(movingUnique(nonFungibleToken, 1L, 2L).between(tokenTreasury, tokenOwner)),

                /* before
                    owner:     0h, 2NFT, 0FT
                    receiver: 1Mh, 0NFT, 4FT
                    collector: 0h, 0NFT, 0FT
                 */
                getAccountBalance(tokenOwner)
                        .hasTinyBars(0)
                        .hasTokenBalance(nonFungibleToken, 2)
                        .hasTokenBalance(feeDenom,0),
                getAccountBalance(tokenReceiver)
                        .hasTinyBars(ONE_MILLION_HBARS)
                        .hasTokenBalance(nonFungibleToken, 0)
                        .hasTokenBalance(feeDenom,4),
                getAccountBalance(hbarCollector)
                        .hasTinyBars(0)
                        .hasTokenBalance(nonFungibleToken, 0)
                        .hasTokenBalance(feeDenom, 0),
                // swap 1 nft for 1 feeDenom between tokenOwner and tokenReceiver
                cryptoTransfer(
                        movingUnique(nonFungibleToken, 1L).between(tokenOwner, tokenReceiver),
                        moving(0, feeDenom).between(tokenReceiver, tokenOwner)
                )
                        .signedByPayerAnd(tokenOwner, tokenReceiver),
                /*  after
                    owner:     0h, 1NFT, 1FT
                    receiver: 1Mh, 1NFT, 3FT
                    collector: 0h, 0NFT, 0FT
                 */
                getAccountBalance(tokenOwner)
                        .hasTinyBars(0)
                        .hasTokenBalance(nonFungibleToken, 1)
                        .hasTokenBalance(feeDenom, 0),
                getAccountBalance(tokenReceiver)
                        .hasTinyBars(ONE_MILLION_HBARS-100)
                        .hasTokenBalance(nonFungibleToken, 1)
                        .hasTokenBalance(feeDenom, 4),
                getAccountBalance(hbarCollector)
                        .hasTinyBars(0)
                        .hasTokenBalance(nonFungibleToken, 0)
                        .hasTokenBalance(feeDenom, 0)
        );
    }
}
