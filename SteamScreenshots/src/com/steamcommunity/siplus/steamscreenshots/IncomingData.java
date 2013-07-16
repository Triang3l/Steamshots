package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;

public class IncomingData {
	byte[] mData;
	MessageHeader mHeader;
	boolean mProtobuf;
	int mType;

	@SuppressWarnings("resource")
	IncomingData(byte[] data) throws IncomingException {
		try {
			LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
			boolean extendedHeader = false;
			int headerSize;
			int headerSizeSize;
			mType = stream.readInt();
			if (mType < 0) {
				mProtobuf = true;
				mType = (0x7fffffff + mType) + 1;
				headerSize = stream.readInt();
				if (headerSize < 0) {
					throw new IncomingException();
				}
				headerSizeSize = 8;
			} else {
				switch (mType) {
				case ChannelEncryptRequestIncoming.MESSAGE:
				case ChannelEncryptResultIncoming.MESSAGE:
					headerSize = 16;
					headerSizeSize = 4;
					break;
				default:
					extendedHeader = true;
					headerSize = stream.readByte();
					if (headerSize < 0) {
						headerSize += 256;
					}
					headerSize -= 5;
					if (headerSize < 0) {
						throw new IncomingException();
					}
					headerSizeSize = 5;
				}
			}
			byte[] header = new byte[headerSize];
			Utility.readFromStream(stream, header);
			mHeader = new MessageHeader(header, mProtobuf, extendedHeader);
			mData = new byte[data.length - headerSize - headerSizeSize];
			Utility.readFromStream(stream, mData);
		} catch (IOException e) {
			throw new IncomingException();
		}
	}
}
