package com.steamcommunity.siplus.steamscreenshots;

public final class ClientLogOffOutgoing extends EmptyOutgoing {
	@Override
	int getMessageType() {
		return 706;
	}
}
