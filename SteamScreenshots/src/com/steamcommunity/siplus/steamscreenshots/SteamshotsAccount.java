package com.steamcommunity.siplus.steamscreenshots;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

public class SteamshotsAccount implements Parcelable {
	public static final Creator<SteamshotsAccount> CREATOR = new Creator<SteamshotsAccount>() {
		@Override
		public SteamshotsAccount createFromParcel(Parcel source) {
			return new SteamshotsAccount(source);
		}

		@Override
		public SteamshotsAccount[] newArray(int size) {
			return new SteamshotsAccount[size];
		}
	};
	static final String DATA_AVATAR = Utility.PACKAGE + ".SteamshotsAccount.DATA_AVATAR";
	static final String DATA_AVATAR_ADDRESS = Utility.PACKAGE + ".SteamshotsAccount.DATA_AVATAR_ADDRESS";
	static final String DATA_GUARD_HASH = Utility.PACKAGE + ".SteamshotsAccount.DATA_GUARD_HASH";
	static final String DATA_LOGIN_KEY = Utility.PACKAGE + ".SteamshotsAccount.DATA_LOGIN_KEY";
	static final String DATA_NEXT_AVATAR_UPDATE = Utility.PACKAGE + ".SteamshotsAccount.DATA_NEXT_AVATAR_UPDATE";
	static final String DATA_STEAM_ID = Utility.PACKAGE + ".SteamshotsAccount.DATA_STEAM_ID";

	Bitmap mAvatar;
	String mAvatarAddress;
	byte[] mGuardHash;
	String mLoginKey;
	String mName;
	long mNextAvatarUpdate;
	long mSteamID;
	boolean mValid;

	SteamshotsAccount(AccountManager accountManager, Account account) {
		String name = account.name;
		if (name == null) {
			mName = "";
			return;
		}
		mName = name;
		String hash = accountManager.getUserData(account, DATA_GUARD_HASH);
		if (hash != null) {
			if (hash.length() != 28) {return;}
			try {
				mGuardHash = Base64.decode(hash, Base64.NO_WRAP);
			} catch (IllegalArgumentException e) {
				return;
			}
			if (mGuardHash.length != 20) {
				return;
			}
		}
		mLoginKey = accountManager.getUserData(account, DATA_LOGIN_KEY);
		if (mLoginKey == null) {
			return;
		}
		String avatarBase64 = accountManager.getUserData(account, DATA_AVATAR);
		if (avatarBase64 != null) {
			try {
				byte[] avatar = Base64.decode(avatarBase64, Base64.NO_WRAP);
				mAvatar = BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
			} catch (IllegalArgumentException e) {}
		}
		try {
			mNextAvatarUpdate = Long.parseLong(accountManager.getUserData(account, DATA_NEXT_AVATAR_UPDATE));
		} catch (NumberFormatException e) {}
		try {
			mSteamID = Long.parseLong(accountManager.getUserData(account, DATA_STEAM_ID));
		} catch (NumberFormatException e) {
			return;
		}
		mValid = true;
	}

	SteamshotsAccount(Parcel parcel) {
		mName = parcel.readString();
		int flags = parcel.readByte();
		mValid = flags != 0;
		if (!mValid) {
			return;
		}
		if (flags == 3)  {
			mGuardHash = new byte[20];
			parcel.readByteArray(mGuardHash);
		}
		mLoginKey = parcel.readString();
		mSteamID = parcel.readLong();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		if (!mValid) {
			dest.writeByte((byte)(0));
			return;
		}
		if (mGuardHash != null) {
			dest.writeByte((byte)(3));
		} else {
			dest.writeByte((byte)(1));
		}
		if (mGuardHash != null) {
			dest.writeByteArray(mGuardHash);
		}
		dest.writeString(mLoginKey);
		dest.writeLong(mSteamID);
	}
}
