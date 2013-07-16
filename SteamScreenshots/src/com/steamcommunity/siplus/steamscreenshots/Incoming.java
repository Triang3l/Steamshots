package com.steamcommunity.siplus.steamscreenshots;

public abstract class Incoming {
	MessageHeader mHeader;

	Incoming(IncomingData data) throws IncomingException {
		mHeader = data.mHeader;
		if (data.mProtobuf) {
			fromProtobuf(data.mData);
		} else {
			fromRaw(data.mData);
		}
	}

	abstract void fromProtobuf(byte[] data) throws IncomingException;
	abstract void fromRaw(byte[] data) throws IncomingException;
}
