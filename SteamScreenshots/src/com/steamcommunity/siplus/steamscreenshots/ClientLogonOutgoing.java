package com.steamcommunity.siplus.steamscreenshots;

import java.util.Random;

import android.content.Context;
import android.provider.Settings.Secure;

import com.google.protobuf.ByteString;
import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientLogonProto;

public final class ClientLogonOutgoing extends Outgoing {
	String mGuardCode;
	ByteString mGuardHash;
	String mGuardName;
	String mLoginKey;
	static ByteString mMachineID;
	String mName;
	String mPassword;

	ClientLogonOutgoing(String name, String password, String guardCode, String guardName) {
		mName = name;
		mPassword = password;
		if (guardCode != null) {
			mGuardCode = guardCode;
			if (guardName.length() != 0) {
				mGuardName = guardName;
			}
		}
	}

	ClientLogonOutgoing(String name, String loginKey, byte[] guardHash) {
		mName = name;
		mLoginKey = loginKey;
		if (guardHash != null) {
			mGuardHash = ByteString.copyFrom(guardHash);
		}
	}

	@Override
	int getMessageType() {
		return 5514;
	}

	static void initializeMachineID(Context context) {
		if (mMachineID != null) {
			return;
		}
		String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		byte[] bytes;
		ByteString machineID = null;
		if (androidID != null) {
			bytes = Utility.shaHash(androidID.getBytes());
			if (bytes != null) {
				machineID = ByteString.copyFrom(bytes);
			}
		}
		if (machineID == null) {
			bytes = new byte[20];
			Random random = new Random();
			random.nextBytes(bytes);
			machineID = ByteString.copyFrom(bytes);
		}
		mMachineID = machineID;
	}

	@Override
	byte[] serialize() {
		ClientLogonProto.Builder builder = ClientLogonProto.newBuilder()
			.setProtocolVersion(65575)
			// We don't need real IP to upload screenshots,
			// so let's use 127.0.0.1 (XORed with 0xbaadf00d).
			.setObfuscatedPrivateIP(-1146228622)
			.setClientPackageVersion(2125)
			.setClientLanguage("english")
			.setClientOSType(-199)
			.setShouldRememberPassword(true)
			.setMachineID(mMachineID)
			.setAccountName(mName);
		if (mPassword != null) {
			builder.setPassword(mPassword);
		} else {
			builder.setLoginKey(mLoginKey);
		}
		if (mGuardHash != null) {
			builder
				.setShaSentryFile(mGuardHash)
				.setEresultSentryFile(EResult.OK);
		} else {
			builder.setEresultSentryFile(EResult.FILE_NOT_FOUND);
			if (mGuardCode != null) {
				builder.setAuthCode(mGuardCode);
				if (mGuardName != null) {
					builder.setMachineNameUserChosen(mGuardName);
				}
			}
		}
		return builder.build().toByteArray();
	}
}
