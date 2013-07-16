package com.steamcommunity.siplus.steamscreenshots;

import com.google.protobuf.ByteString;
import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientUFSUploadFileRequestProto;

public class ClientUFSUploadFileRequestOutgoing extends Outgoing {
	ByteString mHash;
	String mName;
	int mSize;
	long mTime;
	int mZipSize;

	ClientUFSUploadFileRequestOutgoing(long job, String name, int size, int zipSize, ByteString hash, long time) {
		mHash = hash;
		mHeader.mJobSource = job;
		mName = name;
		mSize = size;
		mTime = time;
		mZipSize = zipSize;
	}

	@Override
	int getMessageType() {
		return 5202;
	}

	@Override
	byte[] serialize() {
		return ClientUFSUploadFileRequestProto.newBuilder()
			.setAppID(760)
			.setFileSize(mZipSize)
			.setRawFileSize(mSize)
			.setShaFile(mHash)
			.setTimeStamp(mTime)
			.setFileName(mName)
			.setPlatformsToSync(0)
			.setCanEncrypt(false)
			.build().toByteArray();
	}
}
