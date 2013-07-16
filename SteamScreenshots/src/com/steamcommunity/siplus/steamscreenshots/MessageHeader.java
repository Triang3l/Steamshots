package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.CommonProtos.HeaderProto;

public class MessageHeader {
	long mJobSource = -1;
	long mJobTarget = -1;
	boolean mLoggedOn;
	int mSessionID;
	long mSteamID;

	MessageHeader() {}

	MessageHeader(byte[] header, boolean protobuf, boolean extended) throws IncomingException {
		if (protobuf) {
			fromProtobuf(header);
			return;
		}
		if (extended) {
			fromRawExtended(header);
		} else {
			fromRaw(header);
		}
	}

	void fromProtobuf(byte[] header) throws IncomingException {
		HeaderProto proto;
		try {
			proto = HeaderProto.parseFrom(header);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		mJobSource = proto.getJobIDSource();
		mSessionID = proto.getClientSessionID();
		mSteamID = proto.getSteamID();
	}

	void fromRaw(byte[] header) throws IncomingException {
		try {
			@SuppressWarnings("resource")
			LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(header));
			mJobSource = stream.readLong();
		} catch (IOException e) {
			throw new IncomingException();
		}
	}

	void fromRawExtended(byte[] header) throws IncomingException {
		try {
			@SuppressWarnings("resource")
			LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(header));
			if (stream.readShort() != 2) {
				throw new IncomingException();
			}
			stream.readLong();
			mJobSource = stream.readLong();
			if (stream.readByte() != -17) {
				throw new IncomingException();
			}
			mSteamID = stream.readLong();
			mSessionID = stream.readInt();
		} catch (IOException e) {
			throw new IncomingException();
		}
	}

	byte[] serialize() {
		HeaderProto.Builder builder = HeaderProto.newBuilder()
			.setSteamID(mSteamID)
			.setClientSessionID(mSessionID);
		if (mJobTarget != -1) {
			builder.setJobIDTarget(mJobTarget);
		}
		return builder.build().toByteArray();
	}
}
