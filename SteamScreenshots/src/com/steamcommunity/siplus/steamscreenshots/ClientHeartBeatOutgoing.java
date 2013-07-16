package com.steamcommunity.siplus.steamscreenshots;

public final class ClientHeartBeatOutgoing extends EmptyOutgoing {
	@Override
	int getMessageType() {
		return 703;
	}
}
