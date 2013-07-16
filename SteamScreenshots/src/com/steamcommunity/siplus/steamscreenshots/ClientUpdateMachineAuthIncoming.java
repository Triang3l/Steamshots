package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientUpdateMachineAuthProto;

public final class ClientUpdateMachineAuthIncoming extends Incoming {
	static final int MESSAGE = 5537;

	byte[] mGuardHash;
	ClientUpdateMachineAuthProto mProto;

	ClientUpdateMachineAuthIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		try {
			mProto = ClientUpdateMachineAuthProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		if (!mProto.hasFile()) {
			throw new IncomingException();
		}
		mGuardHash = Utility.shaHash(mProto.getFile().toByteArray());
		if (mGuardHash == null) {
			throw new IncomingException();
		}
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		throw new IncomingException();
	}
}
