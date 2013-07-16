package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;

public final class ChannelEncryptRequestIncoming extends Incoming {
	static final int MESSAGE = 1303;

	int mProtocolVersion;
	int mUniverse;

	ChannelEncryptRequestIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		throw new IncomingException();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		try {
			@SuppressWarnings("resource")
			LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
			mProtocolVersion = stream.readInt();
			mUniverse = stream.readInt();
		} catch (IOException e) {
			throw new IncomingException();
		}
	}
}
