package com.steamcommunity.siplus.steamscreenshots;

import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientNewLoginKeyAcceptedProto;

public final class ClientNewLoginKeyAcceptedOutgoing extends Outgoing {
	int mUniqueID;

	ClientNewLoginKeyAcceptedOutgoing(int uniqueID) {
		mUniqueID = uniqueID;
	}

	@Override
	int getMessageType() {
		return 5464;
	}

	@Override
	byte[] serialize() {
		return ClientNewLoginKeyAcceptedProto.newBuilder().setUniqueID(mUniqueID).build().toByteArray();
	}
}
