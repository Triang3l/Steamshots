package com.steamcommunity.siplus.steamscreenshots;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;

public class TakeService extends Service {
	static final String EXTRA_ACCOUNT = Utility.PACKAGE + ".TakeService.EXTRA_ACCOUNT";
	static final int NOTIFICATION_FAILURE = R.string.notification_failure;
	static final int NOTIFICATION_SUCCESS = R.string.notification_success;
	static final int NOTIFICATION_TAKE = R.string.notification_take;

	SteamshotsAccount mAccount;
	ActivityManager mActivityManager;
	Context mApplicationContext;
	ContentResolver mContentResolver;
	Cursor mCursor;
	NotificationManager mNotificationManager;
	TakeContentObserver mObserver;
	int mOldCount;
	PackageManager mPackageManager;
	Resources mResources;
	String mToBeUpdated;

	@SuppressWarnings("deprecation")
	void addScreenshot(String pngPath) {
		List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
		if (tasks.isEmpty()) {
			return;
		}
		String packageName = tasks.get(0).topActivity.getPackageName();
		if (packageName.equals(Utility.PACKAGE)) {
			return;
		}
		Notification.Builder builder = new Notification.Builder(this)
			.setSmallIcon(R.drawable.ic_stat_notify);
		String game = Utility.applicationLabel(mPackageManager, packageName);
		int name = addScreenshotInner(pngPath, packageName);
		Resources resources = mResources;
		mNotificationManager.cancel(NOTIFICATION_SUCCESS);
		if (name != 0) {
			builder
				.setContentTitle(resources.getString(R.string.notification_success))
				.setContentText(resources.getString(R.string.notification_success_info, game));
		} else {
			builder
				.setContentTitle(resources.getString(R.string.notification_failure))
				.setContentText(resources.getString(R.string.notification_failure_info, game));
		}
		Notification notification;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			notification = builder.build();
		} else {
			notification = builder.getNotification();
		}
		mNotificationManager.notify(NOTIFICATION_SUCCESS, notification);
	}

	@SuppressWarnings("resource")
	int addScreenshotInner(String pngPath, String packageName) {
		Calendar date = Calendar.getInstance();
		int dateYear = date.get(Calendar.YEAR) - 2006;
		if ((dateYear < 0) || (dateYear > 31)) {
			return 0;
		}

		long steamID = mAccount.mSteamID;
		String path = ScreenshotName.folderPath(steamID, packageName);
		File pathFile = new File(path);
		if (!Utility.makeDirectories(pathFile)) {
			return 0;
		}

		int dateInt = (date.get(Calendar.DAY_OF_MONTH) << ScreenshotName.DAY_SHIFT) |
			((date.get(Calendar.MONTH) + 1) << ScreenshotName.MONTH_SHIFT) |
			(dateYear << ScreenshotName.YEAR_SHIFT);
		ScreenshotFileFilter filter = new ScreenshotFileFilter(dateInt);
		pathFile.listFiles(filter);
		int highest = filter.mHighest;
		int highestUploaded = UploadedCaption.highestToDateSorted(
			UploadedCaption.fromFileSorted(steamID, packageName), dateInt);
		if (highestUploaded > highest) {
			highest = highestUploaded;
		}
		int targetNameInt = dateInt | ((highest + 1) % 10000);
		String targetName = ScreenshotName.nameToString(targetNameInt);
		String targetPath = path + targetName;
		Bitmap bitmap = BitmapFactory.decodeFile(pngPath);
		if (bitmap == null) {
			return 0;
		}

		FileOutputStream targetFile;
		try {
			targetFile = new FileOutputStream(targetPath);
		} catch (FileNotFoundException e) {
			bitmap.recycle();
			return 0;
		}
		int height;
		int width;
		try {
			height = bitmap.getHeight();
			width = bitmap.getWidth();
			if ((height == 0) || (width == 0)) {
				throw new IOException();
			}
			if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, targetFile)) {
				throw new IOException();
			}
			targetFile.flush();
			Utility.closeCloseable(targetFile);
		} catch (IOException e) {
			bitmap.recycle();
			Utility.closeCloseable(targetFile);
			return 0;
		}

		if ((height > 200) || (width > 200)) {
			Bitmap thumb;
			if (height > width) {
				width = (int)(width / (height / 200.0f));
				if (width == 0) {
					width = 1;
				}
				thumb = Bitmap.createScaledBitmap(bitmap, width, 200, true);
			} else {
				height = (int)(height / (width / 200.0f));
				if (height == 0) {
					height = 1;
				}
				thumb = Bitmap.createScaledBitmap(bitmap, 200, height, true);
			}
			bitmap.recycle();
			bitmap = thumb;
		}
		try {
			targetFile = new FileOutputStream(targetPath + ScreenshotName.THUMB_SUFFIX);
		} catch (FileNotFoundException e) {
			bitmap.recycle();
			return 0;
		}
		try {
			if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, targetFile)) {
				throw new IOException();
			}
			targetFile.flush();
			Utility.closeCloseable(targetFile);
		} catch (IOException e) {
			bitmap.recycle();
			Utility.closeCloseable(targetFile);
			return 0;
		}

		long imageDate = System.currentTimeMillis();
		ContentValues values = new ContentValues(7);
		values.put(MediaStore.Images.ImageColumns.DATA, targetPath);
		values.put(MediaStore.Images.ImageColumns.DATE_ADDED, imageDate);
		values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, imageDate);
		values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, imageDate);
		values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, targetName);
		values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.Images.ImageColumns.TITLE, targetName);
		values.put(MediaStore.Images.ImageColumns.SIZE, (new File(targetPath)).length());
		mContentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
			MediaStore.Images.ImageColumns.DATA + "=?", new String[] {targetPath}); // If deleted externally
		mContentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		return targetNameInt;
	}

	void mediaStoreChanged() {
		Cursor cursor = queryMediaStore();
		if (cursor == null) {
			return;
		}
		int oldCount = mOldCount;
		int count = cursor.getCount();
		mOldCount = count;
		if (count == 0) {
			mToBeUpdated = null;
			cursor.close();
			return;
		}

		String rowPath;
		int rowSize;

		// Responding to inserts
		if (count > oldCount) {
			cursor.moveToLast();
			rowPath = cursor.getString(0);
			if (rowPath == null) {
				cursor.close();
				return;
			}
			rowSize = cursor.getInt(1);
			if (rowSize <= 0) {
				// On 4.2.2, .insert is called with an empty image with 0 size,
				// and when the actual image is saved, .update is called with correct size,
				// so we wait for .update with that name.
				mToBeUpdated = rowPath;
				cursor.close();
				return;
			}
			cursor.close();
			addScreenshot(rowPath);
			return;
		}

		// Reponding to possible updates, from the last to the first
		String toBeUpdated = mToBeUpdated;
		if ((toBeUpdated != null) && (count == oldCount)) {
			for (cursor.moveToLast(); count-- > 0; cursor.moveToPrevious()) {
				rowPath = cursor.getString(0);
				if (!toBeUpdated.equals(rowPath)) {
					continue;
				}
				rowSize = cursor.getInt(1);
				if (rowSize <= 0) { // Not updated
					cursor.close();
					return;
				}
				mToBeUpdated = null;
				cursor.close();
				addScreenshot(rowPath);
				return;
			}
			mToBeUpdated = null; // The screenshot is gone from the MediaStore
		}

		cursor.close();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		if (mCursor != null) {
			mCursor.unregisterContentObserver(mObserver);
			mCursor.close();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle extras = intent.getExtras();
		if ((extras == null) || !extras.containsKey(EXTRA_ACCOUNT)) {
			stopSelf();
			return START_NOT_STICKY;
		}
		mAccount = extras.getParcelable(EXTRA_ACCOUNT);

		mContentResolver = getContentResolver();
		mCursor = queryMediaStore();
		if (mCursor == null) {
			stopSelf();
			return START_NOT_STICKY;
		}
		mOldCount = mCursor.getCount();

		mActivityManager = (ActivityManager)(getSystemService(ACTIVITY_SERVICE));
		mApplicationContext = getApplicationContext();
		mNotificationManager = (NotificationManager)(getSystemService(NOTIFICATION_SERVICE));
		mPackageManager = getPackageManager();
		Resources resources = getResources();
		mResources = resources;

		mObserver = new TakeContentObserver(this);
		mCursor.registerContentObserver(mObserver);

		Notification.Builder builder = new Notification.Builder(this)
			.setContentTitle(resources.getString(R.string.notification_take, mAccount.mName))
			.setContentText(resources.getString(R.string.notification_take_info))
			.setSmallIcon(R.drawable.ic_stat_notify)
			.setOngoing(true)
			.setWhen(0L);
		Notification notification;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			notification = builder.build();
		} else {
			notification = builder.getNotification();
		}
		startForeground(NOTIFICATION_TAKE, notification);

		return START_NOT_STICKY;
	}

	Cursor queryMediaStore() {
		return mContentResolver.query(
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
			new String[] {MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.SIZE},
			MediaStore.Images.ImageColumns.TITLE + " LIKE 'Screenshot\\_____-__-__-__-__-__.png' ESCAPE '\\'",
			null,
			MediaStore.Images.ImageColumns.DATE_ADDED + " ASC");
	}
}

class TakeContentObserver extends ContentObserver {
	TakeService mService;

	public TakeContentObserver(TakeService service) {
		super(new Handler());
		mService = service;
	}

	@Override
	public void onChange(boolean selfChange, Uri uri) {
		mService.mediaStoreChanged();
	}
}