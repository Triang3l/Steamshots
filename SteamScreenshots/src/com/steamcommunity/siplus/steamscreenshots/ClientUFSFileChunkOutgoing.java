package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.ByteString;
import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientUFSFileChunkProto;

public class ClientUFSFileChunkOutgoing extends Outgoing {
	static final int CHUNK_SIZE = 10240;

	ByteString mData;
	int mStart;
	ByteString mHash;

	ClientUFSFileChunkOutgoing(long job, ByteString hash) { // really long job
		mHash = hash;
		mHeader.mJobTarget = job;
	}

	@Override
	int getMessageType() {
		return 5204;
	}

	@Override
	byte[] serialize() {
		return ClientUFSFileChunkProto.newBuilder()
			.setShaFile(mHash)
			.setFileStart(mStart)
			.setData(mData)
			.build().toByteArray();
	}

	void setData(byte[] data, int start) {
		int length;
		if ((start + CHUNK_SIZE) > data.length) {
			length = data.length % CHUNK_SIZE;
		} else {
			length = CHUNK_SIZE;
		}
		mData = ByteString.copyFrom(data, start, length);
		mStart = start;
	}
}
