package com.steamcommunity.siplus.steamscreenshots;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;

public class UploadedCaption implements Parcelable {
	public static final Creator<UploadedCaption> CREATOR = new Creator<UploadedCaption>() {
		@Override
		public UploadedCaption createFromParcel(Parcel source) {
			return new UploadedCaption(source);
		}

		@Override
		public UploadedCaption[] newArray(int size) {
			return new UploadedCaption[size];
		}
	};

	String mCaption;
	long mHandle;
	int mScreenshot;

	UploadedCaption(int screenshot, long handle, String caption) {
		mScreenshot = screenshot;
		mHandle = handle;
		mCaption = caption;
	}

	UploadedCaption(Parcel parcel) {
		mScreenshot = parcel.readInt();
		mHandle = parcel.readLong();
		mCaption = parcel.readString();
	}

	UploadedCaption(InputStream inputStream) throws IOException {
		@SuppressWarnings("resource")
		LittleEndianDataInputStream stream = new LittleEndianDataInputStream(inputStream);
		mScreenshot = stream.readInt();
		mHandle = stream.readLong();
		int length = stream.readByte();
		if (length == 0) {
			mCaption = "";
			return;
		}
		if (length < 0) {
			length += 256;
		}
		char[] caption = new char[length];
		int i = 0;
		while (length-- > 0) {
			caption[i++] = stream.readChar();
		}
		mCaption = new String(caption);
	}

	Intent createShareIntent(long steamID) {
		Intent intent = (new Intent(Intent.ACTION_SEND))
			.setType("text/plain");
		if (mCaption.length() == 0) {
			return intent.putExtra(Intent.EXTRA_TEXT, String.format("http://steamcommunity.com/profiles/%d/screenshot/%d",
				steamID, mHandle));
		}
		return intent.putExtra(Intent.EXTRA_TEXT, String.format("%s http://steamcommunity.com/profiles/%d/screenshot/%d",
			mCaption, steamID, mHandle));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	static String filePath(long steamID, String packageName) {
		return String.format("%s/Steamshots/%d/.%s.uploaded",
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				steamID, packageName);
	}

	static ArrayList<UploadedCaption> fromFileSorted(long steamID, String packageName) {
		FileInputStream stream;
		try {
			stream = new FileInputStream(filePath(steamID, packageName));
		} catch (FileNotFoundException e) {
			return null;
		}
		try {
			@SuppressWarnings("resource")
			int size = (new LittleEndianDataInputStream(stream)).readInt();
			if (size < 0) {
				Utility.closeCloseable(stream);
				return null;
			}
			ArrayList<UploadedCaption> list = new ArrayList<UploadedCaption>(size);
			while (size-- > 0) {
				list.add(new UploadedCaption(stream));
			}
			Utility.closeCloseable(stream);
			// Even though the list is sorted when saved, let's sort it again in case of manipulation/corruption
			Collections.sort(list, new UploadedCaptionComparator());
			return list;
		} catch (IOException e) {
			Utility.closeCloseable(stream);
			return null;
		}
	}

	static int highestToDateSorted(ArrayList<UploadedCaption> list, int date) {
		if (list == null) {
			return 0;
		}
		date >>= ScreenshotName.DAY_SHIFT;
		int current;
		Iterator<UploadedCaption> iterator;
		for (iterator = list.iterator(); iterator.hasNext(); ) {
			current = iterator.next().mScreenshot;
			if ((current >> ScreenshotName.DAY_SHIFT) == date) {
				return current & ScreenshotName.NUMBER_MASK;
			}
		}
		return 0;
	}

	void openInBrowser(Context context, long steamID) {
		context.startActivity((new Intent(Intent.ACTION_VIEW))
			.setData(Uri.parse(String.format(
				"http://steamcommunity.com/profiles/%d/screenshot/%d?forceMobileWebsitePresentation=desktop",
				steamID, mHandle))));
	}

	@SuppressWarnings("resource")
	static boolean toFile(ArrayList<UploadedCaption> list, long steamID, String packageName) {
		FileOutputStream stream;
		try {
			stream = new FileOutputStream(filePath(steamID, packageName));
		} catch (FileNotFoundException e) {
			return false;
		}
		@SuppressWarnings("unchecked")
		ArrayList<UploadedCaption> newList = (ArrayList<UploadedCaption>)(list.clone());
		Collections.sort(newList, new UploadedCaptionComparator());
		int size = newList.size();
		try {
			(new LittleEndianDataOutputStream(stream)).writeInt(size);
			Iterator<UploadedCaption> iterator = newList.iterator();
			while (size-- > 0) {
				iterator.next().writeToStream(stream);
			}
			stream.flush();
			Utility.closeCloseable(stream);
			return true;
		} catch (IOException e) {
			Utility.closeCloseable(stream);
			return false;
		}
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mScreenshot);
		dest.writeLong(mHandle);
		dest.writeString(mCaption);
	}

	void writeToStream(OutputStream outputStream) throws IOException {
		@SuppressWarnings("resource")
		LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(outputStream);
		stream.writeInt(mScreenshot);
		stream.writeLong(mHandle);
		stream.writeByte((byte)(mCaption.length()));
		stream.writeChars(mCaption);
	}
}

class UploadedCaptionComparator implements Comparator<UploadedCaption> {
	@Override
	public int compare(UploadedCaption lhs, UploadedCaption rhs) {
		return Integer.valueOf(rhs.mScreenshot).compareTo(lhs.mScreenshot);
	}
}