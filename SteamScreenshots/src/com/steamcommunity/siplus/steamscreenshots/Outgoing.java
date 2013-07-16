package com.steamcommunity.siplus.steamscreenshots;

public abstract class Outgoing {
	MessageHeader mHeader;

	Outgoing() {
		mHeader = new MessageHeader();
	}

	abstract int getMessageType();
	abstract byte[] serialize();
}
