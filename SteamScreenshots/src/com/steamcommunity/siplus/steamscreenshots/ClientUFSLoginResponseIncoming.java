package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientUFSLoginResponseProto;

public class ClientUFSLoginResponseIncoming extends Incoming {
	static final int MESSAGE = 5214;

	int mEResult;

	ClientUFSLoginResponseIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientUFSLoginResponseProto proto;
		try {
			proto = ClientUFSLoginResponseProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		mEResult = proto.getEresult();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		try {
			@SuppressWarnings("resource")
			LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
			mEResult = stream.readInt();
		} catch (IOException e) {
			throw new IncomingException();
		}
	}
}
