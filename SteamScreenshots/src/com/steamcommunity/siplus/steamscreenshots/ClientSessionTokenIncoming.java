package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientSessionTokenProto;

public final class ClientSessionTokenIncoming extends Incoming {
	static final int MESSAGE = 850;

	long mToken;

	ClientSessionTokenIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientSessionTokenProto proto;
		try {
			proto = ClientSessionTokenProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		if (!proto.hasToken()) {
			throw new IncomingException();
		}
		mToken = proto.getToken();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		throw new IncomingException();
	}
}
