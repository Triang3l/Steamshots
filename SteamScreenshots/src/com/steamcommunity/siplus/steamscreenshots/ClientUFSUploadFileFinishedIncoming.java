package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientUFSUploadFileFinishedProto;

public class ClientUFSUploadFileFinishedIncoming extends Incoming {
	static final int MESSAGE = 5205;

	int mEResult;

	ClientUFSUploadFileFinishedIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientUFSUploadFileFinishedProto proto;
		try {
			proto = ClientUFSUploadFileFinishedProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		mEResult = proto.getEresult();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		throw new IncomingException();
	}
}
