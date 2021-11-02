package com.hedera.services.state.virtual;

import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import com.swirlds.jasperdb.SelfSerializableSupplier;

import java.io.IOException;

public class VirtualBlobKeySupplier implements SelfSerializableSupplier<VirtualBlobKey> {
	private static final long CLASS_ID = 0x17041622ecb49350L;
	private static final int CURRENT_VERSION = 1;

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
		/* No-op */
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		/* No-op */
	}

	@Override
	public long getClassId() {
		return CLASS_ID;
	}

	@Override
	public int getVersion() {
		return CURRENT_VERSION;
	}

	@Override
	public VirtualBlobKey get() {
		return new VirtualBlobKey();
	}
}
