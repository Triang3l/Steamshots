package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientNewLoginKeyProto;

public final class ClientNewLoginKeyIncoming extends Incoming {
	static final int MESSAGE = 5463;

	String mLoginKey;
	int mUniqueID;

	ClientNewLoginKeyIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientNewLoginKeyProto proto;
		try {
			proto = ClientNewLoginKeyProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		if (!(proto.hasUniqueID() && proto.hasLoginKey())) {
			throw new IncomingException();
		}
		mLoginKey = proto.getLoginKey();
		if (mLoginKey.length() != 19) {
			throw new IncomingException();
		}
		mUniqueID = proto.getUniqueID();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		try {
			LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
			mUniqueID = stream.readInt();
			byte[] key = new byte[20];
			Utility.readFromStream(stream, key);
			mLoginKey = new String(key);
		} catch (IOException e) {
			throw new IncomingException();
		}
	}
}
