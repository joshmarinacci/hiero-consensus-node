// SPDX-License-Identifier: Apache-2.0
package com.hedera.node.app.blocks.impl.streaming;

import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.pbj.runtime.Codec;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import io.grpc.MethodDescriptor;
import io.helidon.grpc.core.MarshallerSupplier;
import java.io.IOException;
import java.io.InputStream;

public class RequestResponseMarshaller<T> implements MethodDescriptor.Marshaller<T> {
    private final Codec<T> codec;

    RequestResponseMarshaller(Class<T> clazz) {
        if (clazz == PublishStreamRequest.class) {
            this.codec = (Codec<T>) PublishStreamRequest.PROTOBUF;
        } else if (clazz == PublishStreamResponse.class) {
            this.codec = (Codec<T>) PublishStreamResponse.PROTOBUF;
        } else {
            throw new IllegalArgumentException("Unsupported class: " + clazz);
        }
    }

    @Override
    public InputStream stream(T obj) {
        return codec.toBytes(obj).toInputStream();
    }

    @Override
    public T parse(InputStream inputStream) {
        try {
            return codec.parse(Bytes.wrap(inputStream.readAllBytes()));
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A {@link MarshallerSupplier} implementation that supplies
     * instances of {@link RequestResponseMarshaller}.
     */
    public static class Supplier implements MarshallerSupplier {
        @Override
        public <T> MethodDescriptor.Marshaller<T> get(Class<T> clazz) {
            return new RequestResponseMarshaller<>(clazz);
        }
    }
}
