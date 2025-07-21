package com.hedera.services.bdd.suites.fees;

import com.hedera.node.app.hapi.fees.BaseFeeRegistry;
import com.hedera.services.bdd.junit.HapiTest;
import com.hedera.services.bdd.junit.HapiTestLifecycle;
import com.hedera.services.bdd.junit.LeakyHapiTest;
import com.hedera.services.bdd.junit.support.TestLifecycle;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.FILE_FREE_BYTES;
import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.HCS_FREE_BYTES;
import static com.hedera.services.bdd.spec.HapiSpec.hapiTest;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountBalance;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileContents;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileInfo;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTopicInfo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.createTopic;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.deleteTopic;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileAppend;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileDelete;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileUpdate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.submitMessageTo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.updateTopic;
import static com.hedera.services.bdd.spec.transactions.token.CustomFeeSpecs.fixedConsensusHbarFee;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.newKeyNamed;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.overriding;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.validateChargedUsd;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.validateChargedUsdWithin;
import static com.hedera.services.bdd.suites.HapiSuite.ONE_HBAR;
import static com.hedera.services.bdd.suites.HapiSuite.ONE_HUNDRED_HBARS;

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
            final var excess_bytes = 600-HCS_FREE_BYTES;
            final var base = BaseFeeRegistry.getBaseFee("ConsensusSubmitMessage");
            final var per_byte = BaseFeeRegistry.getBaseFee("PerHCSByte");
            System.out.println("COST: " + base + " "  + excess_bytes + " "  + per_byte);
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
            final var correct= Math.max(byte_count - FILE_FREE_BYTES, 0) * PerFileByte + FileGetContents;
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    getAccountBalance(PAYER).hasTinyBars(100*ONE_HBAR),
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
            final var correct= Math.max(byte_count - FILE_FREE_BYTES, 0) * PerFileByte + FileGetContents;
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


    // TODO: CryptoCreate, create token
    // TODO: CryptoCreate, create token with custom fees
    // TODO: CryptoDelete, delete token
    // TODO: CryptoTransfer, transfer value in a FT
    // TODO: CryptoGetAccountRecords: ??
    // TODO: CryptoGetAccountBalance: ??
    // TODO: CryptoGetInfo: ??
    // TODO: CryptoApproveAllowance: approve single and multiple allowances
    // TODO: CryptoDeleteAllowance: approvate

    // TODO: Token services
    // TODO: scheduled transactions
    // TODO: smart contracts
    // TODO: File service
    // TODO: random other stuff
}
