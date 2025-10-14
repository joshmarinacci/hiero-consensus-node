package com.hedera.services.bdd.suites.fees;


import com.hedera.services.bdd.junit.HapiTest;
import com.hedera.services.bdd.junit.HapiTestLifecycle;
import com.hedera.services.bdd.junit.LeakyHapiTest;
import com.hedera.services.bdd.junit.support.TestLifecycle;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static com.hedera.services.bdd.spec.HapiSpec.hapiTest;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileContents;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTopicInfo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.createTopic;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.deleteTopic;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.submitMessageTo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.updateTopic;
import static com.hedera.services.bdd.spec.transactions.token.CustomFeeSpecs.fixedConsensusHbarFee;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.newKeyNamed;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.overriding;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.validateChargedFee;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.validateChargedUsd;
import static com.hedera.services.bdd.suites.HapiSuite.GENESIS;
import static com.hedera.services.bdd.suites.HapiSuite.ONE_HBAR;
import static com.hedera.services.bdd.suites.HapiSuite.ONE_HUNDRED_HBARS;
import static com.hedera.services.bdd.suites.HapiSuite.SIMPLE_FEE_SCHEDULE;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeExtraDef;

@HapiTestLifecycle
public class SimpleFeesSuite {
    private static final String PAYER = "payer";
    private static FeeSchedule simpleSchedule;

    @BeforeAll
    static void beforeAll(@NonNull final TestLifecycle testLifecycle) {
        testLifecycle.overrideInClass(Map.of("fees.simpleFeesEnabled", "true"));
        simpleSchedule = FeeSchedule.DEFAULT
                .copyBuilder()
                .extras(makeExtraDef(Extra.KEYS, 1))
                .build();

    }

    @Nested
    class BeforeAfterTests {
        @LeakyHapiTest(overrides = {"fees.simpleFeesEnabled"})
        final Stream<DynamicTest> createTopicBeforeAfter() {
            // 0.01 is one cent or 10^8th tiny cents 100_000_000
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),

                    overriding("fees.simpleFeesEnabled", "false"),

                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn",0.0100),

                    overriding("fees.simpleFeesEnabled", "true"),

                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn",0.0100)
            );
        }
    }

    static double cents_to_USD(long amount) {
        return amount / 100.0;
    }

    @Nested
    class TopicFees {

        // create topic, basic
        @HapiTest
        @DisplayName("Simple fees for creating a topic")
        final Stream<DynamicTest> createTopicFee() {
            return hapiTest(
                    getFileContents(SIMPLE_FEE_SCHEDULE).payingWith(GENESIS),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", cents_to_USD(
                                    1 // base fee for create topic
                                    + 0 // 1024 bytes are included for free
                                    + 1 // node fee
                                    + 2 // network fee
                                    ))
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
                    validateChargedUsd("create-topic-txn", cents_to_USD(
                            1 // base fee for create topic
                                    + 1 // custom fee
                                    + 1 // node fee
                                    + 2 // network fee
                    ))
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
                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .adminKeyName(ADMIN)
                            .fee(ONE_HUNDRED_HBARS).via("create-topic-txn"),
                    // create topic should be base:19 + key:(2-1), node:(base:1, sig:1) * 3 to include network
                    validateChargedUsd("create-topic-txn", cents_to_USD(
                            1 // base fee for create topic
                            + 0 // 1024 bytes are included for free
                            + 1 // one extra sig
                            + (1 +1)*3 // extra sig in node fee, x3 to include network fee
                             )),
                    // update topic, provide up to 100 hbar to pay for it
                    // update topic is base:19 + key(1-1), node:(base:1,sig:1)*3 to include network
                    updateTopic("testTopic").adminKey(ADMIN).payingWith(PAYER)
                            .fee(ONE_HUNDRED_HBARS).via("update-topic-txn"),
                    validateChargedUsd("update-topic-txn", cents_to_USD(
                            1 // base fee for update topic
                            + 0 // 1024 bytes are included for free
                            + 1 // one extra sig
                            + (1 + 1)*3 // 1 extra sig (3) for node fee, x3 to include network fee
                            ))
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
                    validateChargedUsd("create-topic-txn", cents_to_USD( 1 + 3)),
                    // get topic info, provide up to 1 hbar to pay for it
                    getTopicInfo("testTopic").payingWith(PAYER)
                            .fee(ONE_HBAR).via("get-topic-txn").logged(),
                    validateChargedUsd("get-topic-txn", 0.0001)
            );
        }

        @HapiTest
        @DisplayName("Simple fee for submitting a message, all bytes included")
        final Stream<DynamicTest> submitMessageFeeWithIncludedBytes() {
            // 100 is less than the free size, so there's no per byte charge
            final var byte_size = 100;
            final byte[] messageBytes = new byte[byte_size]; // up to 1k
            Arrays.fill(messageBytes, (byte) 0b1);
            return hapiTest(
                    newKeyNamed(PAYER),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    // create topic, provide up to 1 hbar to pay for it
                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", cents_to_USD(1 + 1 + 2)),
                    // submit message, provide up to 1 hbar to pay for it
                    submitMessageTo("testTopic").blankMemo().payingWith(PAYER).message(new String(messageBytes))
                            .fee(ONE_HBAR)
                            .via("submit-message-txn"),
                    validateChargedUsd("submit-message-txn", cents_to_USD(
                            1 // base fee
                            + 1 + 2 // node + network fee
                            ))
            );
        }

        @HapiTest
        @DisplayName("Simple fee for submitting a large message")
        final Stream<DynamicTest> submitBiggerMessageFee() {
            // 256 included + an extra 500
            final var byte_size = 500+256;
            final byte[] messageBytes = new byte[byte_size]; // up to 1k
            Arrays.fill(messageBytes, (byte) 0b1);
            return hapiTest(
                    newKeyNamed(PAYER),
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    // create topic, provide up to 1 hbar to pay for it
                    createTopic("testTopic").blankMemo().payingWith(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", cents_to_USD(1 + 1 + 2)),
                    // submit message, provide up to 1 hbar to pay for it
                    submitMessageTo("testTopic").blankMemo().payingWith(PAYER).message(new String(messageBytes))
                            .fee(ONE_HBAR)
                            .via("submit-message-txn"),
                    validateChargedUsd("submit-message-txn", cents_to_USD(
                            1 // base fee for submit message
                            + 5 // for the extra 500 bytes
                            + 1 + 2 // node + network fee
                    ))
            );
        }

        // delete topic
        @HapiTest()
        @DisplayName("Simple fee for deleting a topic")
        final Stream<DynamicTest> deleteTopicFee() {
            return hapiTest(
                    cryptoCreate(PAYER).balance(ONE_HUNDRED_HBARS),
                    createTopic("testTopic").blankMemo().payingWith(PAYER).adminKeyName(PAYER)
                            .fee(ONE_HBAR).via("create-topic-txn"),
                    validateChargedUsd("create-topic-txn", cents_to_USD(
                            1 // base fee for create topic
                            + 1 +2 // node + network fee
                            )),
                    deleteTopic("testTopic").payingWith(PAYER)
                            .fee(ONE_HBAR).via("delete-topic-txn"),
                    validateChargedUsd("delete-topic-txn", cents_to_USD(
                            1 // base fee for delete topic
                            + 1 + 2 // node + network fee
                    ))
            );
        }
    }
}
