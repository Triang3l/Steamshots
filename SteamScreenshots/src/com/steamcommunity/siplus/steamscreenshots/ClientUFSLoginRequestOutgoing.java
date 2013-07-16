package com.steamcommunity.siplus.steamscreenshots;

import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientUFSLoginRequestProto;

public class ClientUFSLoginRequestOutgoing extends Outgoing {
	long mSessionToken;

	ClientUFSLoginRequestOutgoing(long sessionToken) {
		mHeader.mJobSource = 1L;
		mSessionToken = sessionToken;
	}

	@Override
	int getMessageType() {
		return 5213;
	}

	@Override
	byte[] serialize() {
		return ClientUFSLoginRequestProto.newBuilder()
			.setProtocolVersion(65575)
			.setAmSessionToken(mSessionToken)
			.addApps(760)
			.build().toByteArray();
	}
}
