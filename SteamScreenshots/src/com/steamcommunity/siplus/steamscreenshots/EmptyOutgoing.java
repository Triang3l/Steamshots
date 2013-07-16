package com.steamcommunity.siplus.steamscreenshots;

public class EmptyOutgoing extends Outgoing {
	static final byte[] EMPTY = new byte[0];

	@Override
	int getMessageType() {
		return 0;
	}

	@Override
	byte[] serialize() {
		return EMPTY;
	}
}
