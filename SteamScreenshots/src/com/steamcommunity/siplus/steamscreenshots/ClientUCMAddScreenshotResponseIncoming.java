package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientUCMAddScreenshotResponseProto;

public class ClientUCMAddScreenshotResponseIncoming extends Incoming {
	static final int MESSAGE = 7302;

	int mEResult;
	long mHandle;

	ClientUCMAddScreenshotResponseIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientUCMAddScreenshotResponseProto proto;
		try {
			proto = ClientUCMAddScreenshotResponseProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		mEResult = proto.getEresult();
		mHandle = proto.getScreenshotID();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		throw new IncomingException();
	}
}
