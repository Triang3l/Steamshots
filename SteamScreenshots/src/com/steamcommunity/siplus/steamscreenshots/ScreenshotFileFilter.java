package com.steamcommunity.siplus.steamscreenshots;

import java.io.File;
import java.io.FileFilter;

public class ScreenshotFileFilter implements FileFilter {
	int mHighest;
	int mHighestDate;

	ScreenshotFileFilter(int highestDate) {
		mHighestDate = highestDate >> ScreenshotName.DAY_SHIFT;
	}

	@Override
	public boolean accept(File pathname) {
		if (!pathname.isFile()) {
			return false;
		}
		String name = pathname.getName();
		int nameInt = ScreenshotName.nameToInt(name);
		if (nameInt == 0) {
			return false;
		}
		if (!((new File(pathname.getAbsolutePath() + ScreenshotName.THUMB_SUFFIX)).isFile())) {
			return false;
		}
		if ((nameInt >> ScreenshotName.DAY_SHIFT) == mHighestDate) {
			int number = nameInt & ScreenshotName.NUMBER_MASK;
			if (number > mHighest) {
				mHighest = number;
			}
		}
		return true;
	}
}
