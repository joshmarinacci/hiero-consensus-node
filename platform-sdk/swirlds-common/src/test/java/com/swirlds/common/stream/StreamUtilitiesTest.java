// SPDX-License-Identifier: Apache-2.0
package com.swirlds.common.stream;

import static com.swirlds.common.stream.HashCalculatorTest.PAY_LOAD_SIZE_4;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.computeEntireHash;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.computeMetaHash;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.convertInstantToStringWithPadding;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.generateStreamFileNameFromInstant;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.getFileExtension;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.getPeriod;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.getTimeStampFromFileName;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.readFirstIntFromFile;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.readHashesFromStreamFile;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.readStartRunningHashFromStreamFile;
import static com.swirlds.common.stream.StreamTypeTest.EVENT_FILE_NAME;
import static com.swirlds.common.stream.StreamTypeTest.EVENT_SIG_FILE_NAME;
import static com.swirlds.common.stream.internal.StreamValidationResult.CALCULATED_END_HASH_NOT_MATCH;
import static com.swirlds.common.stream.internal.StreamValidationResult.INVALID_ENTIRE_SIGNATURE;
import static com.swirlds.common.stream.internal.StreamValidationResult.OK;
import static com.swirlds.common.stream.internal.StreamValidationResult.PARSE_SIG_FILE_FAIL;
import static com.swirlds.common.stream.internal.StreamValidationResult.PARSE_STREAM_FILE_FAIL;
import static com.swirlds.common.stream.internal.StreamValidationResult.SIG_HASH_NOT_MATCH_FILE;
import static com.swirlds.common.stream.internal.StreamValidationResult.STREAM_FILE_EMPTY;
import static com.swirlds.common.stream.internal.StreamValidationResult.STREAM_FILE_MISS_OBJECTS;
import static com.swirlds.common.stream.internal.StreamValidationResult.STREAM_FILE_MISS_START_HASH;
import static com.swirlds.common.test.fixtures.stream.ObjectForTestStream.getRandomObjectForTestStream;
import static com.swirlds.common.test.fixtures.stream.TestStreamType.TEST_STREAM;
import static com.swirlds.logging.legacy.LogMarker.EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.swirlds.base.utility.Pair;
import com.swirlds.common.constructable.ConstructableRegistry;
import com.swirlds.common.constructable.ConstructableRegistryException;
import com.swirlds.common.crypto.Cryptography;
import com.swirlds.common.crypto.CryptographyProvider;
import com.swirlds.common.crypto.Signature;
import com.swirlds.common.stream.internal.InvalidStreamFileException;
import com.swirlds.common.stream.internal.LinkedObjectStreamValidateUtils;
import com.swirlds.common.stream.internal.SingleStreamIterator;
import com.swirlds.common.stream.internal.StreamValidationResult;
import com.swirlds.common.test.fixtures.RandomUtils;
import com.swirlds.common.test.fixtures.io.InputOutputStream;
import com.swirlds.common.test.fixtures.stream.ObjectForTestStream;
import com.swirlds.common.test.fixtures.stream.StreamObjectWorker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.hiero.consensus.model.crypto.Hash;
import org.hiero.consensus.model.crypto.RunningHashable;
import org.hiero.consensus.model.io.SelfSerializable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * the object stream files used for testing are generated by running StreamObjectTest.generateFileRestartTest()
 * with the last line `clearDir();` be commented out
 */
class StreamUtilitiesTest {
    private static final Logger logger = LogManager.getLogger(StreamUtilitiesTest.class);
    private static final Cryptography CRYPTOGRAPHY = CryptographyProvider.getInstance();
    private static final Marker LOGM_OBJECT_STREAM = MarkerManager.getMarker("OBJECT_STREAM");
    private static final Marker LOGM_EXCEPTION = MarkerManager.getMarker("EXCEPTION");
    private static final int logPeriodMs = 500;
    private static final File readStreamDirFile = getResourceFile("stream/readDir/");
    private static final File singleStreamFile = getResourceFile("stream/readDir/2021-01-13T17_10_59.050701000Z.test");
    private static final File singleStreamSigFile =
            getResourceFile("stream/readDir/2021-01-13T17_10_59.050701000Z.test_sig");
    private static final File recordFile = new File("stream/readDir/2020-11-19T23_04_37.400026000Z.rcd");
    private static final String CONVERT_INSTANT_TO_STRING_WITH_PADDING_ERROR_MSG =
            "converted result doesn't match " + "expected";
    private static final String GENERATE_STREAM_FILE_NAME_FROM_INSTANT_ERROR_MSG =
            "generated stream file name " + "doesn't" + " " + "match expected";
    private static final String GET_TIMESTAMP_FROM_FILE_NAME_ERROR_MSG =
            "extracted timestamp doesn't " + "match expected";
    private static final String GET_PERIOD_ERROR_MSG = "getPeriod() didn't return expected value";
    private static final String GET_EXTENSION_ERROR_MSG = "getFileExtension() didn't return expected value";

    static {
        System.setProperty("log4j.configurationFile", "log4j2ForTest.xml");
    }

    @BeforeAll
    static void setUp() throws ConstructableRegistryException {
        final ConstructableRegistry registry = ConstructableRegistry.getInstance();
        registry.registerConstructables("com.swirlds.common");
        registry.registerConstructables("org.hiero.consensus.model.crypto");
    }

    private static File getResourceFile(final String path) {
        URL resource = null;
        try {
            ClassLoader classLoader = StreamUtilitiesTest.class.getClassLoader();
            resource = classLoader.getResource(path);
            return new File(resource.getPath());
        } catch (Exception ex) {
            logger.error(EXCEPTION.getMarker(), "resource: {}", resource, ex);
            return null;
        }
    }

    /**
     * write Hash and objects to a stream;
     * then parse the stream;
     * check whether objects are as expected
     *
     * @throws IOException
     */
    @Test
    void parseStreamTest() throws Exception {
        InputOutputStream io = new InputOutputStream();
        Hash initialHash = RandomUtils.randomHash();

        StreamObjectWorker streamObjectWorker =
                new StreamObjectWorker(50, 50, initialHash, Instant.now(), io.getOutput());

        // a list of objects we expect to read from the stream
        Deque<SelfSerializable> objects = streamObjectWorker.getAddedObjects();
        objects.addFirst(initialHash);
        streamObjectWorker.work();
        // add expected endRunningHash to objects
        final RunningHashable lastObject = (RunningHashable) objects.getLast();
        objects.add(lastObject.getRunningHash().getFutureHash().get());

        io.startReading();
        Iterator<SelfSerializable> parsed = new SingleStreamIterator<>(io.getInput());
        Iterator<SelfSerializable> expected = objects.iterator();
        int i = 0;
        while (parsed.hasNext()) {
            SelfSerializable object = parsed.next();
            assertEquals(expected.next(), object, "SelfSerializable doesn't match expected");
            i++;
        }
        assertEquals(i, objects.size(), "the number of objects read from the stream doesn't match expected number");
    }

    @Test
    void parseFileTest() {
        Iterator<SelfSerializable> iterator =
                LinkedObjectStreamUtilities.parseStreamFile(singleStreamFile, TEST_STREAM);
        checkIterator(iterator, 12);
    }

    @Test
    void parseFileNegativeTest() {
        File file = new File("src/test/resources/stream/readDir/2020-08-26T01_29_04.121101Z.soc");
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> LinkedObjectStreamUtilities.parseStreamFile(file, EventStreamType.getInstance()),
                "should throw an exception when paring a invalid file");
        assertTrue(
                exception.getMessage().contains("extension doesn't match"),
                "should throw IllegalArgumentException when parsing a file which is not of given stream type");
    }

    @Test
    void parseSignatureTest() throws Exception {
        Pair<Pair<Hash, Signature>, Pair<Hash, Signature>> pairs =
                LinkedObjectStreamUtilities.parseSigFile(singleStreamSigFile, TEST_STREAM);
        assertNotNull(pairs, "Fail to parse the signature file");
        // check entireHash
        assertEquals(
                computeEntireHash(singleStreamFile),
                pairs.left().left(),
                "the EntireHash extracted from the signature file doesn't match the EntireHash calculated from the "
                        + "stream file");
        assertNotNull(pairs.left().right(), "Fail to extract EntireSignature");
        assertEquals(
                computeMetaHash(singleStreamFile, TEST_STREAM),
                pairs.right().left(),
                "the MetaHash extracted from the signature file doesn't match the MetaHash calculated from the "
                        + "stream file");
        assertNotNull(pairs.right().right(), "Fail to extract MetaSignature");

        Exception exception = assertThrows(
                InvalidStreamFileException.class,
                () -> LinkedObjectStreamUtilities.parseSigFile(singleStreamFile, TEST_STREAM),
                "should throw InvalidStreamFileException when calling parseSigFile() on a stream file");
        assertTrue(
                exception.getMessage().contains("extension doesn't match"),
                "the exception should contain expected message");
    }

    @Test
    void parseStreamDirOrFileTest() throws InvalidStreamFileException {
        Iterator<SelfSerializable> iterator =
                LinkedObjectStreamUtilities.parseStreamDirOrFile(singleStreamFile, TEST_STREAM);
        checkIterator(iterator, 12);

        assertThrows(
                InvalidStreamFileException.class,
                () -> LinkedObjectStreamUtilities.parseStreamDirOrFile(recordFile, TEST_STREAM),
                "should throw an exception when type not match");
    }

    @Test
    void parseDirTest() throws InvalidStreamFileException {
        Iterator<ObjectForTestStream> iterator =
                LinkedObjectStreamUtilities.parseStreamDirOrFile(readStreamDirFile, TEST_STREAM);
        checkIterator(iterator, 102);
    }

    /**
     * parse stream files which contains ObjectForTestingStream, not real event stream files
     */
    @Test
    void validateEventsDirTest() throws Exception {
        Pair<StreamValidationResult, Hash> result =
                LinkedObjectStreamValidateUtils.validateDirOrFile(readStreamDirFile, TEST_STREAM);
        assertEquals(OK, result.left(), "the files are not valid, result: " + result.left());
    }

    /**
     * tests {@link LinkedObjectStreamUtilities#readStartRunningHashFromStreamFile(File, StreamType)} and
     * {@link LinkedObjectStreamUtilities#readHashesFromStreamFile(File, StreamType)},
     * verifies if extracted Hash matches expected
     */
    @Test
    void extractHashTest() throws InvalidStreamFileException {
        Iterator<SelfSerializable> iterator =
                LinkedObjectStreamUtilities.parseStreamFile(singleStreamFile, TEST_STREAM);
        Hash expectedStartHash = (Hash) iterator.next();
        Hash expectedEndHash = null;
        while (iterator.hasNext()) {
            SelfSerializable object = iterator.next();
            // the last one should be endRunningHash
            if (!iterator.hasNext()) {
                expectedEndHash = (Hash) object;
            }
        }
        assertNotNull(expectedEndHash, "Fail to extract EndRunningHash");

        Hash readStartHash = readStartRunningHashFromStreamFile(singleStreamFile, TEST_STREAM);
        assertEquals(expectedStartHash, readStartHash, "readStartRunningHash should match expected StartRunningHash");

        Pair<Hash, Hash> readHashPair = readHashesFromStreamFile(singleStreamFile, TEST_STREAM);
        assertEquals(
                expectedStartHash, readHashPair.left(), "readStartRunningHash should match expected StartRunningHash");

        assertEquals(expectedEndHash, readHashPair.right(), "The extracted EndRunningHash doesn't match expected");
    }

    /**
     * check if number of objects read from given iterator matches expected;
     * check if startRunningHash and endRunningHash is not null;
     * check if endRunningHash is valid;
     *
     * @param iterator
     * @param expectedCount
     * @return
     */
    private void checkIterator(Iterator<? extends SelfSerializable> iterator, int expectedCount) {
        Hash startRunningHash = null;
        Hash endRunningHash = null;
        Hash runningHash = null;
        int objectsCount = 0;
        while (iterator.hasNext()) {
            SelfSerializable object = iterator.next();
            if (objectsCount == 0) {
                startRunningHash = (Hash) object;
                runningHash = startRunningHash;
            } else if (object instanceof Hash) {
                endRunningHash = (Hash) object;
                // endRunningHash should be the last one in the iterator
                assertFalse(iterator.hasNext());
            } else {
                Hash objectHash = CRYPTOGRAPHY.digestSync(object);
                runningHash = CRYPTOGRAPHY.calcRunningHash(runningHash, objectHash);
            }
            objectsCount++;
            logger.info(LOGM_OBJECT_STREAM, "parsed object: {}", object);
        }
        // assertNotNull(startRunningHash);
        assertNotNull(endRunningHash, "endRunningHash should not be null");
        assertEquals(runningHash, endRunningHash, "endRunningHash should match calculated runningHash");
        assertEquals(expectedCount, objectsCount, "number of objects in this iterator doesn't match expected number");
    }

    /**
     * check if two iterators contain the same content
     *
     * @param iterator1
     * @param iterator2
     * @return
     */
    private void compareIterator(
            Iterator<? extends SelfSerializable> iterator1, Iterator<? extends SelfSerializable> iterator2) {
        int objectsCount = 0;
        int diffObjectsCount = 0;
        while (iterator1.hasNext() && iterator2.hasNext()) {
            SelfSerializable object1 = iterator1.next();
            SelfSerializable object2 = iterator2.next();
            if (!object1.equals(object2)) {
                diffObjectsCount++;
                logger.info(
                        LOGM_EXCEPTION,
                        "got different object. " + "from iterator1: {}, from iterator2: {}",
                        object1,
                        object2);
            }

            objectsCount++;
        }
        while (iterator1.hasNext()) {
            logger.info(LOGM_EXCEPTION, "only in iterator1: {}", iterator1.next());
            diffObjectsCount++;
        }
        while (iterator2.hasNext()) {
            logger.info(LOGM_EXCEPTION, "only in iterator2: {}", iterator2.next());
            diffObjectsCount++;
        }

        assertEquals(0, diffObjectsCount, "iterator1 and iterator2 should not contain different numbers of objects");
        logger.info(LOGM_OBJECT_STREAM, "objectsCount: {}", objectsCount);
        logger.info(LOGM_OBJECT_STREAM, "diffObjectsCount: {}", diffObjectsCount);
    }

    @Test
    void convertInstantToStringWithPaddingTest() {
        assertEquals(
                "2020-10-19T21_35_39.000000000Z",
                convertInstantToStringWithPadding(Instant.parse("2020-10-19T21:35:39Z")),
                CONVERT_INSTANT_TO_STRING_WITH_PADDING_ERROR_MSG);
        assertEquals(
                "2020-10-19T21_35_39.123000000Z",
                convertInstantToStringWithPadding(Instant.parse("2020-10-19T21:35:39.123Z")),
                CONVERT_INSTANT_TO_STRING_WITH_PADDING_ERROR_MSG);
        assertEquals(
                "2020-10-19T21_35_39.123456000Z",
                convertInstantToStringWithPadding(Instant.parse("2020-10-19T21:35:39.123456Z")),
                CONVERT_INSTANT_TO_STRING_WITH_PADDING_ERROR_MSG);
        assertEquals(
                "2020-10-19T21_35_39.123456789Z",
                convertInstantToStringWithPadding(Instant.parse("2020-10-19T21:35:39.123456789Z")),
                CONVERT_INSTANT_TO_STRING_WITH_PADDING_ERROR_MSG);
        assertEquals(
                "2020-10-19T21_35_39.000456000Z",
                convertInstantToStringWithPadding(Instant.parse("2020-10-19T21:35:39.000456Z")),
                CONVERT_INSTANT_TO_STRING_WITH_PADDING_ERROR_MSG);
    }

    @Test
    void generateStreamFileNameFromInstantTest() {
        assertEquals(
                "2020-10-19T21_35_39.000000000Z.evts",
                generateStreamFileNameFromInstant(Instant.parse("2020-10-19T21:35:39Z"), EventStreamType.getInstance()),
                GENERATE_STREAM_FILE_NAME_FROM_INSTANT_ERROR_MSG);
    }

    @Test
    void extractTimestampTest() {
        Instant expected = Instant.now();
        String fileName = convertInstantToStringWithPadding(expected);
        String eventFileNameWithExtension = generateStreamFileNameFromInstant(expected, EventStreamType.getInstance());
        assertEquals(expected, getTimeStampFromFileName(fileName), GET_TIMESTAMP_FROM_FILE_NAME_ERROR_MSG);
        assertEquals(
                expected, getTimeStampFromFileName(eventFileNameWithExtension), GET_TIMESTAMP_FROM_FILE_NAME_ERROR_MSG);

        // invalid because doesn't end with 'Z'
        String invalidTimestampString = "2020-10-19T21_35_39.123456789";
        assertNull(
                getTimeStampFromFileName(invalidTimestampString),
                "should return null when file name is not a valid timestamp string");
    }

    @Test
    void getPeriodTest() {
        Instant instant = Instant.now();
        Instant nextPeriodInstant = instant.plusMillis(logPeriodMs);
        Instant nextTwoPeriodInstant = instant.plusMillis(logPeriodMs * 2);
        Instant atPeriodBeginning = Instant.ofEpochSecond(1000);

        assertEquals(
                getPeriod(instant, logPeriodMs) + 1, getPeriod(nextPeriodInstant, logPeriodMs), GET_PERIOD_ERROR_MSG);

        assertEquals(
                getPeriod(instant, logPeriodMs) + 2,
                getPeriod(nextTwoPeriodInstant, logPeriodMs),
                GET_PERIOD_ERROR_MSG);

        assertEquals(
                getPeriod(atPeriodBeginning, logPeriodMs),
                getPeriod(atPeriodBeginning.plusMillis(logPeriodMs / 2), logPeriodMs),
                GET_PERIOD_ERROR_MSG);
    }

    @Test
    void getFileExtensionTest() {
        assertEquals(
                EventStreamType.getInstance().getExtension(),
                getFileExtension(new File(EVENT_FILE_NAME)),
                GET_EXTENSION_ERROR_MSG);
        assertEquals(
                EventStreamType.getInstance().getSigExtension(),
                getFileExtension(new File(EVENT_SIG_FILE_NAME)),
                GET_EXTENSION_ERROR_MSG);

        assertTrue(getFileExtension(new File("noExtensionFileName")).isEmpty(), GET_EXTENSION_ERROR_MSG);
    }

    @Test
    void getEntireHashTest() throws Exception {
        assertNotNull(computeEntireHash(singleStreamFile), "Fail to calculate EntireHash");

        assertThrows(
                FileNotFoundException.class,
                () -> computeEntireHash(new File("nonExistFile")),
                "should throw a FileNotFoundException when the file doesn't exist");
    }

    @Test
    void readFirstIntFromFileTest() throws IOException {
        final int VERSION_5 = 5;
        assertEquals(
                VERSION_5, readFirstIntFromFile(singleStreamFile), "the read first Int doesn't match expected value");
    }

    @Test
    void generateSigFilePathTest() {
        assertEquals(
                singleStreamSigFile.getAbsolutePath(),
                LinkedObjectStreamUtilities.generateSigFilePath(singleStreamFile),
                "generated signature file path should match expected path");
    }

    @Test
    void validateNullIteratorTest() {
        assertEquals(
                PARSE_STREAM_FILE_FAIL,
                LinkedObjectStreamValidateUtils.validateIterator(null).left(),
                "when iterator is null, validation result should be PARSE_STREAM_FILE_FAIL");
    }

    @Test
    void validateEmptyIteratorTest() {
        Iterator<SelfSerializable> emptyIterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public SelfSerializable next() {
                return null;
            }
        };
        assertEquals(
                STREAM_FILE_EMPTY,
                LinkedObjectStreamValidateUtils.validateIterator(emptyIterator).left(),
                "when iterator doesn't contain elements, validation result should be STREAM_FILE_EMPTY");
    }

    @Test
    void validateIteratorMissStartHashTest() {
        Iterator<SelfSerializable> missStartHashIterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public SelfSerializable next() {
                return mock(SelfSerializable.class);
            }
        };
        assertEquals(
                STREAM_FILE_MISS_START_HASH,
                LinkedObjectStreamValidateUtils.validateIterator(missStartHashIterator)
                        .left(),
                "when the first element is not Hash, validation result should be STREAM_FILE_MISS_START_HASH");
    }

    @Test
    void validateIteratorMissObjectsTest() {
        Iterator<SelfSerializable> missObjectsIterator = new Iterator<>() {
            // this iterator only returns two Hashes
            private static final int count = 2;
            private int id = 0;

            @Override
            public boolean hasNext() {
                return id < count;
            }

            @Override
            public SelfSerializable next() {
                id++;
                return mock(Hash.class);
            }
        };
        assertEquals(
                STREAM_FILE_MISS_OBJECTS,
                LinkedObjectStreamValidateUtils.validateIterator(missObjectsIterator)
                        .left(),
                "when the file only contains two Hash, validation result should be STREAM_FILE_MISS_OBJECTS");
    }

    @Test
    void validateIteratorEndHashNotMatchTest() {
        Iterator<SelfSerializable> endHashNotMatchIterator = new Iterator<>() {
            private static final int FIRST_ID = 0;
            private static final int LAST_ID = 5;
            int id = 0;

            @Override
            public boolean hasNext() {
                return id <= LAST_ID;
            }

            @Override
            public SelfSerializable next() {
                SelfSerializable next;
                if (id == FIRST_ID || id == LAST_ID) {
                    next = RandomUtils.randomHash();
                } else {
                    next = getRandomObjectForTestStream(PAY_LOAD_SIZE_4);
                }
                id++;
                return next;
            }
        };
        assertEquals(
                CALCULATED_END_HASH_NOT_MATCH,
                LinkedObjectStreamValidateUtils.validateIterator(endHashNotMatchIterator)
                        .left(),
                "when the calculated endHash doesn't match the endHash in the iterator, validation result should be "
                        + "CALCULATED_END_HASH_NOT_MATCH");
    }

    @Test
    void validateFileAndSignatureWithInvalidKeyTest() {
        PublicKey invalidPubKey = mock(PublicKey.class);
        StreamValidationResult result = LinkedObjectStreamValidateUtils.validateFileAndSignature(
                singleStreamFile, singleStreamSigFile, invalidPubKey, TEST_STREAM);
        assertEquals(INVALID_ENTIRE_SIGNATURE, result, "should get INVALID_ENTIRE_SIGNATURE when pubKey is not valid");
    }

    @Test
    void validateFileAndSignatureWithInvalidStreamFileTest() {
        PublicKey invalidPubKey = mock(PublicKey.class);
        StreamValidationResult result = LinkedObjectStreamValidateUtils.validateFileAndSignature(
                recordFile, singleStreamSigFile, invalidPubKey, EventStreamType.getInstance());
        assertEquals(
                PARSE_STREAM_FILE_FAIL, result, "should fail to parse when stream file doesn't match the stream type");
    }

    @Test
    void validateSignatureStreamTypeNotMatchTest() {
        StreamType anotherType = mock(StreamType.class);
        given(anotherType.isStreamSigFile(any(File.class))).willReturn(false);
        given(anotherType.getSigExtension()).willReturn("AnotherType");
        StreamValidationResult result = LinkedObjectStreamValidateUtils.validateSignature(
                mock(Hash.class), singleStreamSigFile, mock(PublicKey.class), anotherType);
        assertEquals(
                PARSE_SIG_FILE_FAIL,
                result,
                "should fail to parse when stream signature file doesn't match the stream type");
    }

    @Test
    void validateSignatureEntireHashMismatchTest() {
        StreamValidationResult result = LinkedObjectStreamValidateUtils.validateSignature(
                mock(Hash.class), singleStreamSigFile, mock(PublicKey.class), TEST_STREAM);
        assertEquals(
                SIG_HASH_NOT_MATCH_FILE,
                result,
                "when the given EntireHash doesn't match the EntireHash read from signature file, should return "
                        + "SIG_HASH_NOT_MATCH_FILE");
    }
}
