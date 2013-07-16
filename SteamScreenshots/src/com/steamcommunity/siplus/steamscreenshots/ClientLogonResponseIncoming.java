package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientLogonResponseProto;

public final class ClientLogonResponseIncoming extends Incoming {
	static final int MESSAGE = 751;

	String mEmailDomain;
	int mEResult;

	ClientLogonResponseIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientLogonResponseProto proto;
		try {
			proto = ClientLogonResponseProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		if (proto.hasEmailDomain()) {
			mEmailDomain = proto.getEmailDomain();
		}
		mEResult = proto.getEresult();
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		try {
			mEResult = (new LittleEndianDataInputStream(new ByteArrayInputStream(data))).readInt();
		} catch (IOException e) {
			throw new IncomingException();
		}
	}
}
