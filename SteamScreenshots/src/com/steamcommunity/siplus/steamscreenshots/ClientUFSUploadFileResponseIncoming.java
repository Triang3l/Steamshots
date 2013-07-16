package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientUFSUploadFileResponseProto;

public class ClientUFSUploadFileResponseIncoming extends Incoming {
	static final int MESSAGE = 5203;

	int mEResult;
	boolean mUnsupportedModes;

	ClientUFSUploadFileResponseIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientUFSUploadFileResponseProto proto;
		try {
			proto = ClientUFSUploadFileResponseProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		mEResult = proto.getEresult();
		mUnsupportedModes = proto.getUseHTTP() || proto.getUseHTTPS() || proto.getEncryptFile();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		throw new IncomingException();
	}
}
