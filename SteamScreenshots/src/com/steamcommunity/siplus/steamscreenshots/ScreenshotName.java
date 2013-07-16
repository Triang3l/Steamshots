package com.steamcommunity.siplus.steamscreenshots;

import java.io.File;
import java.util.Calendar;

import android.content.Context;
import android.os.Environment;
import android.provider.MediaStore;

public final class ScreenshotName {
	static final int DAY_MASK = 31;
	static final int DAY_SHIFT = 17;
	static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
	static final int MONTH_MASK = 15;
	static final int MONTH_SHIFT = 22;
	static final int NUMBER_MASK = 131071;
	static final String THUMB_SUFFIX = ".thumb";
	static final int YEAR_SHIFT = 26;

	static long creationTime(int name, long steamID, String packageName) {
		Calendar calendar = Calendar.getInstance();
		File file = new File(folderPath(steamID, packageName), nameToString(name));
		if (file.isFile()) {
			calendar.setTimeInMillis(file.lastModified());
		}
		if (isValid(name)) {
			calendar.set(Calendar.YEAR, (name >> YEAR_SHIFT) + 2006);
			calendar.set(Calendar.MONTH, ((name >> MONTH_SHIFT) & MONTH_MASK) - 1);
			calendar.set(Calendar.DAY_OF_MONTH, (name >> DAY_SHIFT) & DAY_MASK);
		}
		return calendar.getTimeInMillis();
	}

	static void deleteScreenshot(Context context, int name, long steamID, String packageName) {
		String path = folderPath(steamID, packageName);
		String nameString = nameToString(name);
		(new File(path, nameString)).delete();
		(new File(path, nameString + THUMB_SUFFIX)).delete();
		context.getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
			MediaStore.Images.ImageColumns.DATA + "=?", new String[] {path + nameString});
		File folder = new File(path);
		String[] files = folder.list();
		if ((files != null) && (files.length == 0)) {
			folder.delete();
		}
	}

	static String folderPath(long steamID, String packageName) {
		return String.format("%s/Steamshots/%d/%s/",
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
			steamID, packageName);
	}

	static int nameToInt(String name) {
		if (!name.matches("\\A20[0-3][0-9]-[0-1][0-9]-[0-3][0-9]_[0-9]{5}\\.jpg\\z")) {
			return 0;
		}
		int year = Integer.parseInt(name.substring(2, 4)) - 6;
		if ((year < 0) || (year > 31)) {
			return 0;
		}
		int month = (name.charAt(5) - 48) * 10 + (name.charAt(6) - 48);
		if ((month == 0) || (month > 12)) {
			return 0;
		}
		int day = (name.charAt(8) - 48) * 10 + (name.charAt(9) - 48);
		if (day == 0) {
			return 0;
		}
		int days = DAYS_IN_MONTH[month - 1];
		if ((month == 2) && ((year & 3) == 2)) {
			++days;
		}
		if (day > days) {
			return 0;
		}
		int number = Integer.parseInt(name.substring(11, 16));
		if (number == 0) {
			return 0;
		}
		return number | (day << DAY_SHIFT) | (month << MONTH_SHIFT) | (year << YEAR_SHIFT);
	}

	static String nameToString(int name) {
		return String.format("20%02d-%02d-%02d_%05d.jpg",
			(name >> YEAR_SHIFT) + 6,
			(name >> MONTH_SHIFT) & MONTH_MASK,
			(name >> DAY_SHIFT) & DAY_MASK,
			name & NUMBER_MASK);
	}

	static boolean isDayAndMonthValid(int day, int month, int year) {
		if ((day == 0) || (month == 0) || (month > 12)) {
			return false;
		}
		int days = DAYS_IN_MONTH[month - 1];
		if ((month == 2) && ((year & 3) == 2)) {
			++days;
		}
		return day <= days;
	}

	static boolean isValid(int name) {
		if (name <= 0) {
			return false;
		}
		int i = name & NUMBER_MASK;
		if ((i == 0) || (i > 99999)) {
			return false;
		}
		int month = (name >> MONTH_SHIFT) & MONTH_MASK;
		if ((month == 0) || (month > 12)) {
			return false;
		}
		int day = (name >> DAY_SHIFT) & DAY_MASK;
		if (day == 0) {
			return false;
		}
		i = DAYS_IN_MONTH[month - 1];
		if ((month == 2) && (((name >> YEAR_SHIFT) & 3) == 2)) {
			++i;
		}
		return day <= i;
	}
}
