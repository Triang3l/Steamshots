package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.os.SystemClock;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.MultiProto;

public class Connection {
	// THIS IS NOT A PERSISTENT CONNECTION!!!
	// It doesn't send keep alive messages, so the app must send heartbeats (using sendHeartbeatIfNeeded) in other places.
	// It sends keep alive messages when waiting for new messages, however.
	// sendHeartbeatIfNeeded works only if needHeartbeat is set.

	static final String CM_ADDRESS = "cm0.steampowered.com";
	static final int CM_PORT = 27017;
	static final byte[] ENCRYPT_RESPONSE_HEADER = {
		-92, 0, 0, 0, // Size = 164
		86, 84, 48, 49, // VT01
		24, 5, 0, 0, // Type = ChannelEncryptResponse
		-1, -1, -1, -1, -1, -1, -1, -1, // Target job ID = -1
		-1, -1, -1, -1, -1, -1, -1, -1, // Source job ID = -1.
		1, 0, 0, 0, // Protocol version = 1
		-128, 0, 0, 0 // Key size = 128
	};
	static final byte[] RSA_KEY = {
		48, -127, -99, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 
		5, 0, 3, -127, -117, 0, 48, -127, -121, 2, -127, -127, 0, -33, -20, 26, 
		-42, 44, 16, 102, 44, 23, 53, 58, 20, -80, 124, 89, 17, 127, -99, -45, 
		-40, 43, 122, -29, -32, 21, -51, 25, 30, 70, -24, 123, -121, 116, -94, 24, 
		70, 49, -87, 3, 20, 121, -126, -114, -23, 69, -94, 73, 18, -87, 35, 104, 
		115, -119, -49, 105, -95, -79, 97, 70, -67, -63, -66, -65, -42, 1, 27, -40, 
		-127, -44, -36, -112, -5, -2, 79, 82, 115, 102, -53, -107, 112, -41, -59, -114, 
		-70, 28, 122, 51, 117, -95, 98, 52, 70, -69, 96, -73, -128, 104, -6, 19, 
		-89, 122, -118, 55, 75, -98, -58, -12, 93, 95, 58, -103, -7, -98, -60, 58, 
		-23, 99, -94, -69, -120, 25, 40, -32, -25, 20, -64, 66, -119, 2, 1, 17
	};
	static final long TIMEOUT = 30000L;
	static final int VT01 = 0x31305456;

	Cipher mAES;
	boolean mAESInitialized;
	Cipher mAESIV;
	SecretKeySpec mAESKeySpec;
	ClientHeartBeatOutgoing mHeartbeat = new ClientHeartBeatOutgoing();
	boolean mHeartbeatWhenWaiting;
	LinkedList<byte[]> mMulti;
	int mSessionID;
	Socket mSocket;
	long mSteamID = 0x110000100000000L;

	void cloneSessionData(Incoming message) {
		MessageHeader header = message.mHeader;
		mSessionID = header.mSessionID;
		mSteamID = header.mSteamID;
	}

	void connect(String dstName, int dstPort) throws ConnectionException {
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {}
			mSocket = null;
		}		

		try {
			mSocket = new Socket(dstName, dstPort);
		} catch (UnknownHostException e) {
			throw new ConnectionException();
		} catch (IOException e) {
			throw new ConnectionException();
		}

		mMulti = new LinkedList<byte[]>();

		try {
			mSocket.setSoTimeout(5000);
		} catch (SocketException e) {
			disconnectThrow();
		}

		IncomingData data;

		try {
			data = waitForMessage(ChannelEncryptRequestIncoming.MESSAGE);
			if (data == null) {
				disconnectThrow();
			}
			ChannelEncryptRequestIncoming requestMessage = new ChannelEncryptRequestIncoming(data);
			if ((requestMessage.mProtocolVersion != 1) || (requestMessage.mUniverse != 1)) {
				disconnectThrow();
			}
		} catch (IncomingException e) {
			disconnectThrow();
		}

		try {
			Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, (RSAPublicKey)(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(RSA_KEY))));
			SecureRandom random = new SecureRandom();
			byte[] aesKey = new byte[32];
			random.nextBytes(aesKey);
			byte[] encrypted = cipher.doFinal(aesKey);
			CRC32 crc = new CRC32();
			crc.update(encrypted);
			long checksum = crc.getValue();
			byte[] response = new byte[172];
			System.arraycopy(ENCRYPT_RESPONSE_HEADER, 0, response, 0, 36);
			System.arraycopy(encrypted, 0, response, 36, 128);
			response[164] = (byte)(checksum & 255L);
			response[165] = (byte)((checksum >> 8L) & 255L);
			response[166] = (byte)((checksum >> 16L) & 255L);
			response[167] = (byte)((checksum >> 24L) & 255L);
			OutputStream stream = mSocket.getOutputStream();
			stream.write(response);
			stream.flush();
			data = waitForMessage(ChannelEncryptResultIncoming.MESSAGE);
			if (data == null) {
				disconnectThrow();
			}
			ChannelEncryptResultIncoming resultMessage = new ChannelEncryptResultIncoming(data);
			if (resultMessage.mEResult != EResult.OK) {
				disconnectThrow();
			}
			mAES = Cipher.getInstance("AES/CBC/PKCS7Padding");
			mAESIV = Cipher.getInstance("AES/ECB/NoPadding");
			mAESKeySpec = new SecretKeySpec(aesKey, "AES");
			mAESInitialized = true;
		} catch (Exception e) {
			disconnectThrow();
		}
	}

	byte[] decryptMessage(byte[] data) throws IncomingException {
		try {
			mAESIV.init(Cipher.DECRYPT_MODE, mAESKeySpec);
			mAES.init(Cipher.DECRYPT_MODE, mAESKeySpec, new IvParameterSpec(mAESIV.doFinal(data, 0, 16)));
			return mAES.doFinal(data, 16, data.length - 16);
		} catch (Exception e) {
			disconnectThrowIncoming();
		}
		return null;
	}

	void disconnect() {
		if (mSocket == null) {
			return;
		}
		mMulti = null;
		try {
			mSocket.close();
		} catch (IOException e) {}
		mSocket = null;
	}

	void disconnectThrow() throws ConnectionException {
		disconnect();
		throw new ConnectionException();
	}

	void disconnectThrowIncoming() throws IncomingException {
		disconnect();
		throw new IncomingException();
	}

	void disconnectThrowOutgoing() throws OutgoingException {
		disconnect();
		throw new OutgoingException();
	}

	byte[] encryptMessage(int type, byte[] header, byte[] data, byte[] ivOutput) throws OutgoingException {
		try {
			SecureRandom random = new SecureRandom();
			byte[] iv = new byte[16];
			random.nextBytes(iv);
			mAES.init(Cipher.ENCRYPT_MODE, mAESKeySpec, new IvParameterSpec(iv));
			mAESIV.init(Cipher.ENCRYPT_MODE, mAESKeySpec);
			iv = mAESIV.doFinal(iv);
			System.arraycopy(iv, 0, ivOutput, 0, iv.length);
			ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream(8 + header.length + data.length);
			@SuppressWarnings("resource")
			LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(byteArrayStream);
			stream.writeInt((int)(type | 0x80000000L));
			stream.writeInt(header.length);
			stream.write(header);
			stream.write(data);
			return mAES.doFinal(byteArrayStream.toByteArray());

		} catch (Exception e) {
			disconnectThrowOutgoing();
		}
		return null;
	}

	IncomingData nextMessage() throws IncomingException {
		byte[] bytes;
		if (mMulti.isEmpty()) {
			bytes = requestNextMessage();
			if (bytes == null) {
				return null;
			}
		} else {
			bytes = mMulti.remove(0);
		}
		IncomingData data;
		try {
			data = new IncomingData(bytes);
		} catch (IncomingException e) {
			disconnect();
			throw e;
		}
		// Single message
		if (data.mType != 1) {
			return data;
		}
		// Multiple messages
		if (!data.mProtobuf) {
			disconnectThrowIncoming();
		}
		MultiProto proto = null;
		try {
			proto = MultiProto.parseFrom(data.mData);
		} catch (InvalidProtocolBufferException e) {
			disconnectThrowIncoming();
		}
		if (!proto.hasMessageBody()) {
			return null;
		}
		byte[] multi = proto.getMessageBody().toByteArray();
		int sizeUnzipped = proto.getSizeUnzipped();
		if (sizeUnzipped != 0) {
			if (sizeUnzipped < 0) {
				disconnectThrowIncoming();
			}
			ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(multi));
			try {
				ZipEntry zipEntry = zipInputStream.getNextEntry();
				if ((zipEntry == null) || (zipEntry.getSize() != sizeUnzipped)) {
					return null;
				}
				multi = new byte[sizeUnzipped];
				Utility.readFromStream(zipInputStream, multi);
			} catch (IOException e) {
				disconnectThrowIncoming();
			}
		}
		LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(multi));
		LinkedList<byte[]> newMultiMessages = new LinkedList<byte[]>();
		try {
			int multiLength;
			byte[] multiMessage;
			while (stream.available() > 4) {
				multiLength = stream.readInt();
				if ((multiLength < 5) || (multiLength > stream.available())) {
					disconnectThrowIncoming();
				}
				multiMessage = new byte[multiLength];
				Utility.readFromStream(stream, multiMessage);
				newMultiMessages.add(multiMessage);
			}
			if (stream.available() != 0) {
				disconnectThrowIncoming();
			}
		} catch (IOException e) {
			disconnectThrowIncoming();
		}
		newMultiMessages.addAll(mMulti);
		mMulti = newMultiMessages;
		if (newMultiMessages.isEmpty()) {
			return null;
		}
		bytes = newMultiMessages.get(0);
		if ((bytes[0] == 1) && (bytes[1] == 0) && (bytes[2] == 0)) {
			switch (bytes[3]) {
			case 0:
				disconnectThrowIncoming();
			case -128:
				return null;
			}
		}
		newMultiMessages.remove(0);
		try {
			return new IncomingData(bytes);
		} catch (IncomingException e) {
			disconnect();
			throw e;
		}
	}

	byte[] requestNextMessage() throws IncomingException {
		try {
			InputStream stream = mSocket.getInputStream();
			byte[] data = new byte[8];
			Utility.readFromStream(stream, data);
			int length = (data[0] & 255) | ((data[1] & 255) << 8) | ((data[2] & 255) << 16) | ((data[3] & 255) << 24);
			if ((length < 16) || (length > 65527)) {
				disconnectThrowIncoming();
			}
			if ((data[4] != 86) || (data[5] != 84) || (data[6] != 48) || (data[7] != 49)) {
				disconnectThrowIncoming();
			}
			data = new byte[length];
			Utility.readFromStream(stream, data);
			if (mAESInitialized) {
				data = decryptMessage(data);
			}
			return data;
		} catch (InterruptedIOException e) {
			return null;
		} catch (IOException e) {
			disconnectThrowIncoming();
		}
		return null;
	}

	void sendHeartbeat() {
		MessageHeader header = mHeartbeat.mHeader;
		header.mSessionID = mSessionID;
		header.mSteamID = mSteamID;
		try {
			sendMessage(mHeartbeat);
		} catch (OutgoingException e) {}
	}

	void sendMessage(Outgoing message) throws OutgoingException {
		MessageHeader header = message.mHeader;
		header.mSessionID = mSessionID;
		header.mSteamID = mSteamID;
		byte[] iv = new byte[16];
		byte[] encrypted = encryptMessage(message.getMessageType(), header.serialize(), message.serialize(), iv);
		try {
			@SuppressWarnings("resource")
			LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(mSocket.getOutputStream());
			stream.writeInt(encrypted.length + 16);
			stream.writeInt(VT01);
			stream.write(iv);
			stream.write(encrypted);
			stream.flush();
		} catch (IOException e) {
			disconnectThrowOutgoing();
		}
	}

	IncomingData waitForMessage(int message) throws IncomingException {
		IncomingData data;
		long start = SystemClock.elapsedRealtime();
		for (;;) {
			data = nextMessage();
			if ((data != null) && (data.mType == message)) {
				return data;
			}
			if ((SystemClock.elapsedRealtime() - start) > TIMEOUT) {
				throw new IncomingException();
			}
			if (mHeartbeatWhenWaiting) {
				sendHeartbeat();
			}
		}
	}
}
