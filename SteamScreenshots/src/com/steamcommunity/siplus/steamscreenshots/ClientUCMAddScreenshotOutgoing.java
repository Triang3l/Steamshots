package com.steamcommunity.siplus.steamscreenshots;

import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientUCMAddScreenshotProto;
import com.steamcommunity.siplus.steamscreenshots.proto.OutgoingProtos.ClientUCMAddScreenshotProto.ClientUCMAddScreenshotProtoTag;

public class ClientUCMAddScreenshotOutgoing extends Outgoing {
	String mCaption;
	String mFile;
	String mGame;
	String mGameID;
	int mHeight;
	String mLocation;
	boolean mSpoiler;
	String mThumb;
	int mTime;
	int mVisibility;
	int mWidth;

	ClientUCMAddScreenshotOutgoing(String game, String gameID, 
		String caption, String location, int visibility, boolean spoiler) {
		mCaption = caption;
		mGame = game;
		mGameID = gameID;
		mLocation = location;
		mSpoiler = spoiler;
		mVisibility = visibility;
	}

	@Override
	int getMessageType() {
		return 7301;
	}

	@Override
	byte[] serialize() {
		ClientUCMAddScreenshotProto.Builder builder = ClientUCMAddScreenshotProto.newBuilder()
			.setFilename(mFile)
			.setThumbname(mThumb)
			.setTimeCreated(mTime)
			.setWidth(mWidth)
			.setHeight(mHeight)
			.setPermissions(mVisibility)
			.setCaption(mCaption)
			.setShortcutName(mGame)
			.setSpoilerTag(mSpoiler);
		if (mLocation.length() != 0) {
			builder.addTag(ClientUCMAddScreenshotProtoTag.newBuilder()
				.setTagName("location")
				.setTagValue(mLocation));
		}
		return builder.build().toByteArray();
	}

	void setFile(int screenshot, int width, int height, long time, long job) {
		String game = mGameID;
		String name = ScreenshotName.nameToString(screenshot);
		mFile = game + "/screenshots/" + name;
		mThumb = game + "/screenshots/thumbnails/" + name;
		mWidth = width;
		mHeight = height;
		if (time < -0x80000000L) {
			mTime = -0x80000000;
		} else if (time > 0x7fffffffL) {
			mTime = 0x7fffffff;
		} else {
			mTime = (int)(time);
		}
	}
}
