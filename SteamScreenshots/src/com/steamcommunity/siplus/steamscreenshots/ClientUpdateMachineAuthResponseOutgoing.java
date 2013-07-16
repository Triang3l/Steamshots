package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.ByteString;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientUpdateMachineAuthProto;
import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientUpdateMachineAuthResponseProto;

public final class ClientUpdateMachineAuthResponseOutgoing extends Outgoing {
	ClientUpdateMachineAuthIncoming mIncoming;

	ClientUpdateMachineAuthResponseOutgoing(ClientUpdateMachineAuthIncoming incoming) {
		mIncoming = incoming;
		mHeader.mJobTarget = incoming.mHeader.mJobSource;
	}

	@Override
	int getMessageType() {
		return 5538;
	}

	@Override
	byte[] serialize() {
		ClientUpdateMachineAuthProto proto = mIncoming.mProto;
		ClientUpdateMachineAuthResponseProto.Builder builder = ClientUpdateMachineAuthResponseProto.newBuilder()
			.setFilename(proto.getFilename())
			.setEresult(EResult.OK)
			.setFilesize(proto.getFile().size())
			.setShaFile(ByteString.copyFrom(mIncoming.mGuardHash))
			.setLasterror(0)
			.setOffset(proto.getOffset())
			.setCubWrote(proto.getCubToWrite())
			.setOtpType(proto.getOtpType())
			.setOtpValue(0);
		if (proto.hasFilename()) {
			builder.setFilenameBytes(proto.getFilenameBytes());
		}
		if (proto.hasOtpIdentifier()) {
			builder.setOtpIdentifierBytes(proto.getOtpIdentifierBytes());
		}
		return builder.build().toByteArray();
	}
}
