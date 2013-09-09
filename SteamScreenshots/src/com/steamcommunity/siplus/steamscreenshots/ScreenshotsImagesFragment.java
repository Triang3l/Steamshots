package com.steamcommunity.siplus.steamscreenshots;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;

public class ScreenshotsImagesFragment extends Fragment {
	static final String FRAGMENT_TAG = Utility.PACKAGE + ".ScreenshotsImagesFragment";
	static final String STATE_SELECTED = FRAGMENT_TAG + ".STATE_SELECTED";

	SteamshotsAccount mAccount;
	ScreenshotsActivity mActivity;
	ScreenshotsImagesAdapter mAdapter;
	ArrayList<UploadedCaption> mCaptions;
	UploadedCaption[] mCaptionsAtPositions;
	String mGame;
	Resources mResources;
	int[] mScreenshots;
	ArrayList<Integer> mSelected;

	GridView widgetGrid;
	TextView widgetSelect;

	// Assuming that the data is sorted
	void filterDeadSelected() {
		ArrayList<Integer> old = mSelected;
		if (old.isEmpty()) {
			return;
		}
		int current;
		int i = 0;
		Iterator<Integer> iterator;
		ArrayList<Integer> selected = new ArrayList<Integer>(mSelected.size());
		int screenshot;
		int[] screenshots = mScreenshots;
		int length = screenshots.length;
		for (iterator = old.iterator(); iterator.hasNext(); ) {
			current = iterator.next();
			for (; i < length; ++i) {
				screenshot = screenshots[i];
				if (screenshot == current) {
					selected.add(current);
					++i;
					break;
				}
				if (screenshot < current) {
					break;
				}
			}
			if (i > length) {
				break;
			}
		}
		mSelected = selected;
		mAdapter.notifyDataSetChanged();
		mActivity.refreshActionBar();
	}

	void loadScreenshots() {
		long steamID = mAccount.mSteamID;
		File[] files = (new File(ScreenshotName.folderPath(steamID, mGame))).listFiles(new ScreenshotFileFilter(0));
		int length;
		if (files != null) {
			length = files.length;
		} else {
			length = 0;
		}
		int i;
		int[] screenshots = new int[length];
		for (i = 0; i < length; ++i) {
			screenshots[i] = ScreenshotName.nameToInt(files[i].getName());
		}
		UploadedCaption[] captionsArray = new UploadedCaption[length];
		mCaptionsAtPositions = captionsArray;
		mScreenshots = screenshots;

		if (length == 0) {
			mActivity.deselectGame();
			mAdapter.notifyDataSetChanged();
			return;
		}

		Arrays.sort(screenshots);
		Utility.reverseArray(screenshots);

		ArrayList<UploadedCaption> captions = UploadedCaption.fromFileSorted(steamID, mGame);
		mCaptions = captions;
		if (captions != null) {
			// Because the list and the array are sorted, instead of doing search in array from 0 every time,
			// we're searching in the array from the (last offset + 1).
			UploadedCaption caption;
			int captionScreenshot;
			i = 0;
			Iterator<UploadedCaption> iterator;
			int screenshot;
			for (iterator = captions.iterator(); iterator.hasNext(); ) {
				caption = iterator.next();
				captionScreenshot = caption.mScreenshot;
				for (; i < length; ++i) {
					screenshot = screenshots[i];
					if (screenshot == captionScreenshot) {
						captionsArray[i++] = caption;
						break;
					}
					if (screenshot < captionScreenshot) {
						break;
					}
				}
				if (i >= length) {
					break;
				}
			}
		}

		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ScreenshotsActivity activity = (ScreenshotsActivity)(getActivity());
		mActivity = activity;

		mAccount = activity.mAccount;
		mGame = activity.mGame;
		mResources = activity.getResources();

		View view = getView();
		widgetGrid = (GridView)(view.findViewById(R.id.fragment_screenshots_images_grid));
		widgetSelect = (TextView)(view.findViewById(R.id.fragment_screenshots_images_select));

		if (mGame == null) {
			widgetGrid.setVisibility(View.GONE);
			widgetSelect.setVisibility(View.VISIBLE);
			return;
		}

		mAdapter = new ScreenshotsImagesAdapter(this);
		loadScreenshots();

		if (savedInstanceState != null) {
			mSelected = savedInstanceState.getIntegerArrayList(STATE_SELECTED);
			filterDeadSelected();
		}
		if (mSelected == null) {
			mSelected = new ArrayList<Integer>(mScreenshots.length);
		}
		activity.refreshActionBar();

		widgetGrid.setAdapter(mAdapter);
		widgetGrid.setOnItemClickListener(new ScreenshotsImagesOnItemClick(this));
		widgetGrid.setOnItemLongClickListener(new ScreenshotsImagesOnItemLongClick(this));
		widgetGrid.setVisibility(View.VISIBLE);
		widgetSelect.setVisibility(View.GONE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_screenshots_images, container, false);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntegerArrayList(STATE_SELECTED, mSelected);
	}

	void toggleSelection(int position) {
		int current;
		boolean deselected = false;
		Iterator<Integer> iterator;
		ArrayList<Integer> selected = mSelected;
		int screenshot = mScreenshots[position];
		for (iterator = selected.iterator(); iterator.hasNext(); ) {
			current = iterator.next();
			if (current == screenshot) {
				iterator.remove();
				mActivity.refreshActionBar();
				deselected = true;
				break;
			}
		}
		if (!deselected) {
			selected.add(screenshot);
			Collections.sort(selected);
			Collections.reverse(selected);
			mActivity.refreshActionBar();
		}
		View view = widgetGrid.getChildAt(position - widgetGrid.getFirstVisiblePosition());
		if (view != null) {
			if (deselected) {
				view.setBackgroundResource(0);
				view.findViewById(R.id.grid_screenshot_selected).setVisibility(View.GONE);
			} else {
				view.setBackgroundResource(android.R.color.holo_blue_dark);
				view.findViewById(R.id.grid_screenshot_selected).setVisibility(View.VISIBLE);
			}
		}
	}
}

class ScreenshotsImagesAdapter extends BaseAdapter {
	ScreenshotsImagesFragment mFragment;
	LayoutInflater mLayoutInflater;

	ScreenshotsImagesAdapter(ScreenshotsImagesFragment fragment) {
		mFragment = fragment;
		mLayoutInflater = fragment.mActivity.getLayoutInflater();
	}

	@Override
	public int getCount() {
		int[] screenshots = mFragment.mScreenshots;
		if (screenshots == null) {
			return 0;
		}
		return screenshots.length;
	}

	@Override
	public Object getItem(int position) {
		return getItemId(position);
	}

	@Override
	public long getItemId(int position) {
		int[] screenshots = mFragment.mScreenshots;
		if (screenshots == null) {
			return position;
		}
		return screenshots[position];
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ScreenshotsImagesFragment fragment = mFragment;
		int[] screenshots = fragment.mScreenshots;
		if (screenshots == null) {
			return null;
		}
		View view = mLayoutInflater.inflate(R.layout.grid_screenshot, parent, false);
		((ImageView)(view.findViewById(R.id.grid_screenshot_image))).setImageDrawable(
			new BitmapDrawable(fragment.mResources, ScreenshotName.folderPath(fragment.mAccount.mSteamID, fragment.mGame) +
				'/' + ScreenshotName.nameToString(fragment.mScreenshots[position]) + ScreenshotName.THUMB_SUFFIX));
		view.findViewById(R.id.grid_screenshot_uploaded).setVisibility(
			fragment.mCaptionsAtPositions[position] != null ? View.VISIBLE : View.GONE);
		Iterator<Integer> iterator;
		int screenshot = screenshots[position];
		int selected;
		for (iterator = fragment.mSelected.iterator(); iterator.hasNext(); ) {
			selected = iterator.next();
			if (selected != screenshot) {
				continue;
			}
			view.setBackgroundResource(android.R.color.holo_blue_dark);
			view.findViewById(R.id.grid_screenshot_selected).setVisibility(View.VISIBLE);
		}
		return view;
	}
}

class ScreenshotsImagesOnItemClick implements OnItemClickListener {
	ScreenshotsImagesFragment mFragment;

	ScreenshotsImagesOnItemClick(ScreenshotsImagesFragment fragment) {
		mFragment = fragment;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ScreenshotsImagesFragment fragment = mFragment;
		if (!fragment.mSelected.isEmpty()) {
			fragment.toggleSelection(position);
			return;
		}
		ScreenshotsActivity activity = fragment.mActivity;
		activity.startActivity(new Intent(activity.getApplicationContext(), PreviewActivity.class)
			.putExtra(PreviewActivity.EXTRASTATE_ACCOUNT, fragment.mAccount)
			.putExtra(PreviewActivity.EXTRASTATE_GAME, fragment.mGame)
			.putExtra(PreviewActivity.EXTRASTATE_SCREENSHOT, fragment.mScreenshots[position]));
	}
}

class ScreenshotsImagesOnItemLongClick implements OnItemLongClickListener {
	ScreenshotsImagesFragment mFragment;

	ScreenshotsImagesOnItemLongClick(ScreenshotsImagesFragment fragment) {
		mFragment = fragment;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		mFragment.toggleSelection(position);
		return true;
	}
}