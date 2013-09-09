package com.steamcommunity.siplus.steamscreenshots;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ShareActionProvider;

public final class ScreenshotsActivity extends Activity {
	static final String EXTRASTATE_ACCOUNT = Utility.PACKAGE + ".GamesActivity.EXTRASTATE_ACCOUNT";
	static final String STATE_DELETE = Utility.PACKAGE + ".GamesActivity.STATE_DELETE";
	static final String STATE_GAME = Utility.PACKAGE + ".GamesActivity.STATE_GAME";

	SteamshotsAccount mAccount;
	ActionMode mActionMode;
	Context mApplicationContext;
	AlertDialog mDelete;
	FragmentManager mFragmentManager;
	String mGame;
	String[] mGames;
	boolean mNeedDelete;
	String mOnlineAddress;
	Uri mOnlineURI;
	Resources mResources;
	Bundle mSavedInstanceState;
	boolean mTablet;

	void deselectGame() {
		mGame = null;
		FragmentManager fragmentManager = mFragmentManager;
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (mTablet) {
			transaction.replace(R.id.view_screenshots_fragment_images, new ScreenshotsImagesFragment(),
				ScreenshotsImagesFragment.FRAGMENT_TAG);
			Fragment fragment = fragmentManager.findFragmentByTag(ScreenshotsGamesFragment.FRAGMENT_TAG);
			if (fragment != null) {
				((ScreenshotsGamesFragment)(fragment)).fillGamesList();
			}
		} else {
			fragmentManager.popBackStack();
			transaction.replace(android.R.id.content, new ScreenshotsGamesFragment(), ScreenshotsGamesFragment.FRAGMENT_TAG);
		}
		transaction
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
			.commit();
		refreshActionBar();
	}

	ScreenshotsImagesFragment getImagesFragment() {
		if (!mTablet && (mGame == null)) {
			return null;
		}
		Fragment fragment = mFragmentManager.findFragmentByTag(ScreenshotsImagesFragment.FRAGMENT_TAG);
		if (fragment == null) {
			return null;
		}
		ScreenshotsImagesFragment imagesFragment = (ScreenshotsImagesFragment)(fragment);
		if (imagesFragment.mGame == null) {
			return null;
		}
		return imagesFragment;
	}

	void hideDelete() {
		if (mDelete != null) {
			mDelete.dismiss();
			mDelete = null;
		}
	}

	@Override
	public void onBackPressed() {
		if (mTablet || (mGame == null)) {
			super.onBackPressed();
		} else {
			deselectGame();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mAccount = savedInstanceState.getParcelable(EXTRASTATE_ACCOUNT);
			mGame = savedInstanceState.getString(STATE_GAME);
		}
		if (mAccount == null) {
			mAccount = getIntent().getParcelableExtra(EXTRASTATE_ACCOUNT);
		}
		if ((mAccount == null) || !mAccount.mValid) {
			finish();
			return;
		}
		mOnlineAddress = String.format("http://steamcommunity.com/profiles/%d/screenshots", mAccount.mSteamID);
		mOnlineURI = Uri.parse(mOnlineAddress);

		mApplicationContext = getApplicationContext();
		mFragmentManager = getFragmentManager();
		mResources = getResources();

		Utility.enableActionBarBack(this);

		setContentView(R.layout.view_screenshots);
		mTablet = findViewById(R.id.view_screenshots_fragment_images) != null;

		if (savedInstanceState == null) {
			FragmentTransaction transaction = mFragmentManager.beginTransaction();
			if (mTablet) {
				transaction.add(R.id.view_screenshots_fragment_games, new ScreenshotsGamesFragment(),
					ScreenshotsGamesFragment.FRAGMENT_TAG);
				transaction.add(R.id.view_screenshots_fragment_images, new ScreenshotsImagesFragment(),
					ScreenshotsImagesFragment.FRAGMENT_TAG);
			} else {
				transaction.add(android.R.id.content, new ScreenshotsGamesFragment(), ScreenshotsGamesFragment.FRAGMENT_TAG);
			}
			transaction.commit();

			startService(new Intent(mApplicationContext, TakeService.class)
				.putExtra(TakeService.EXTRA_ACCOUNT, mAccount));
		} else {
			mNeedDelete = savedInstanceState.getBoolean(STATE_DELETE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.screenshots, menu);
		((ShareActionProvider)(menu.findItem(R.id.action_screenshots_share).getActionProvider()))
			.setShareIntent((new Intent(Intent.ACTION_SEND))
				.setType("text/plain")
				.putExtra(Intent.EXTRA_TEXT, mOnlineAddress));
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		hideDelete();
		if (!isChangingConfigurations()) {
			stopService(new Intent(mApplicationContext, TakeService.class));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mTablet || (mGame == null)) {
				finish();
				return true;
			}
			deselectGame();
			return true;
		case R.id.action_screenshots_view:
			startActivity((new Intent(Intent.ACTION_VIEW)).setData(mOnlineURI));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		refreshEverything();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRASTATE_ACCOUNT, mAccount);
		if (mDelete != null) {
			outState.putBoolean(STATE_DELETE, true);
		}
		outState.putString(STATE_GAME, mGame);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mNeedDelete) {
			return;
		}
		mNeedDelete = false;
		hideDelete();
		showDelete();
	}

	void refreshActionBar() {
		int selected = selectedCount();
		if (selected == 0) {
			if (mActionMode == null) {
				return;
			}
			mActionMode.finish();
			mActionMode = null;
			return;
		}
		if (mActionMode != null) {
			mActionMode.invalidate();
		} else {
			mActionMode = startActionMode(new ScreenshotsActionMode(this));
		}
		mActionMode.setTitle(Integer.toString(selected));
	}

	void refreshEverything() {
		ScreenshotsImagesFragment imagesFragment = getImagesFragment();
		if (imagesFragment != null) {
			imagesFragment.loadScreenshots();
			imagesFragment.filterDeadSelected();
		}
		if (mTablet || (mGame == null)) {
			Fragment fragment = mFragmentManager.findFragmentByTag(ScreenshotsGamesFragment.FRAGMENT_TAG);
			if (fragment != null) {
				((ScreenshotsGamesFragment)(fragment)).fillGamesList();
			}
		}
		if (mDelete != null) {
			hideDelete();
			showDelete();
		}
	}

	int selectedCount() {
		if (mGame == null) {
			return 0;
		}
		ScreenshotsImagesFragment fragment = getImagesFragment();
		if (fragment == null) {
			return 0;
		}
		return fragment.mSelected.size();
	}

	void selectGame(String game) {
		if (game == mGame) {
			return;
		}
		mGame = game;
		FragmentManager fragmentManager = mFragmentManager;
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (mTablet) {
			transaction.replace(R.id.view_screenshots_fragment_images, new ScreenshotsImagesFragment(),
				ScreenshotsImagesFragment.FRAGMENT_TAG);
		} else {
			transaction
				.addToBackStack(null)
				.replace(android.R.id.content, new ScreenshotsImagesFragment(), ScreenshotsImagesFragment.FRAGMENT_TAG);
		}
		transaction
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			.commit();
		if (mTablet) {
			Fragment fragment = fragmentManager.findFragmentByTag(ScreenshotsGamesFragment.FRAGMENT_TAG);
			if (fragment != null) {
				((ScreenshotsGamesFragment)(fragment)).mAdapter.notifyDataSetChanged();
			}
		}
	}

	void showDelete() {
		if (mDelete != null) {
			return;
		}
		ScreenshotsImagesFragment fragment = getImagesFragment();
		if (fragment == null) {
			return;
		}
		ArrayList<Integer> selected = fragment.mSelected;
		if (selected.isEmpty()) {
			return;
		}
		ScreenshotsDeleteOnClick listener = new ScreenshotsDeleteOnClick(this);
		AlertDialog.Builder builder = (new AlertDialog.Builder(this))
			.setNegativeButton(R.string.delete_cancel, listener)
			.setOnCancelListener(new ScreenshotsDeleteOnCancel(this))
			.setPositiveButton(R.string.delete_ok, listener);
		if (selected.size() == 1) {
			builder.setMessage(R.string.screenshots_images_delete_single);
		} else {
			builder.setMessage(R.string.screenshots_images_delete);
		}
		mDelete = builder.show();
	}
}

class ScreenshotsActionMode implements ActionMode.Callback {
	ScreenshotsActivity mActivity;
	MenuInflater mMenuInflater;

	ScreenshotsActionMode(ScreenshotsActivity activity) {
		mActivity = activity;
		if (activity.mGame == null) {
			return;
		}
		mMenuInflater = activity.getMenuInflater();
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		ScreenshotsActivity activity = mActivity;
		ScreenshotsImagesFragment fragment = activity.getImagesFragment();
		if (fragment == null) {
			return false;
		}
		ArrayList<Integer> selected = fragment.mSelected;
		int length = selected.size();
		if (length == 0) {
			return false;
		}
		switch (item.getItemId()) {
		case R.id.action_screenshots_new_delete:
		case R.id.action_screenshots_uploaded_delete:
		case R.id.action_screenshots_uploaded_multi_delete:
			activity.showDelete();
			return true;
		case R.id.action_screenshots_new_upload:
			{
				int i = 0;
				Iterator<Integer> iterator;
				int[] screenshots = new int[length];
				for (iterator = selected.iterator(); iterator.hasNext(); ) {
					screenshots[i++] = iterator.next();
				}
				activity.startActivity(new Intent(activity.mApplicationContext, UploadActivity.class)
					.putExtra(UploadActivity.EXTRASTATE_ACCOUNT, activity.mAccount)
					.putExtra(UploadActivity.EXTRASTATE_GAME, activity.mGame)
					.putExtra(UploadActivity.EXTRASTATE_SCREENSHOTS, screenshots));
				return true;
			}
		case R.id.action_screenshots_uploaded_view:
			{
				ArrayList<UploadedCaption> captions = fragment.mCaptions;
				if (captions == null) {
					return true;
				}
				UploadedCaption caption;
				Iterator<UploadedCaption> iterator;
				int screenshot = selected.get(0);
				for (iterator = captions.iterator(); iterator.hasNext(); ) {
					caption = iterator.next();
					if (caption.mScreenshot == screenshot) {
						caption.openInBrowser(activity, activity.mAccount.mSteamID);
						return true;
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		ScreenshotsActivity activity = mActivity;
		activity.mActionMode = null;
		if (activity.mGame == null) {
			return;
		}
		ScreenshotsImagesFragment fragment = activity.getImagesFragment();
		if (fragment == null) {
			return;
		}
		fragment.mSelected = new ArrayList<Integer>(fragment.mScreenshots.length);
		GridView grid = fragment.widgetGrid;
		int i;
		int length = grid.getChildCount();
		View view;
		for (i = 0; i < length; ++i) {
			view = grid.getChildAt(i);
			view.setBackgroundResource(0);
			view.findViewById(R.id.grid_screenshot_selected).setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		ScreenshotsImagesFragment fragment = mActivity.getImagesFragment();
		if (fragment == null) {
			return true;
		}

		ArrayList<UploadedCaption> captions = fragment.mCaptions;
		if (captions == null) {
			mMenuInflater.inflate(R.menu.screenshots_new, menu);
			return true;
		}

		int item;
		ArrayList<Integer> selected = fragment.mSelected;

		if (selected.size() == 1) {
			UploadedCaption caption;
			item = selected.get(0);
			Iterator<UploadedCaption> captionIterator;
			for (captionIterator = captions.iterator(); captionIterator.hasNext(); ) {
				caption = captionIterator.next();
				if (caption.mScreenshot != item) {
					continue;
				}
				mMenuInflater.inflate(R.menu.screenshots_uploaded, menu);
				((ShareActionProvider)(menu.findItem(R.id.action_screenshots_uploaded_share).getActionProvider()))
					.setShareIntent(caption.createShareIntent(mActivity.mAccount.mSteamID));
				return true;
			}
			mMenuInflater.inflate(R.menu.screenshots_new, menu);
			return true;
		}

		UploadedCaption[] captionsArray = fragment.mCaptionsAtPositions;
		int i = 0;
		Iterator<Integer> iterator;
		int[] screenshots = fragment.mScreenshots;
		int length = screenshots.length;
		for (iterator = selected.iterator(); iterator.hasNext(); ) {
			item = iterator.next();
			for (; i < length; ++i) {
				if (screenshots[i] != item) {
					continue;
				}
				if (captionsArray[i] != null) {
					mMenuInflater.inflate(R.menu.screenshots_uploaded_multi, menu);
					return true;
				}
				++i;
				break;
			}
			if (i >= length) {
				break;
			}
		}
		mMenuInflater.inflate(R.menu.screenshots_new, menu);
		return true;
	}
}

class ScreenshotsDeleteOnCancel implements DialogInterface.OnCancelListener {
	ScreenshotsActivity mActivity;

	ScreenshotsDeleteOnCancel(ScreenshotsActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		mActivity.mDelete = null;
	}
}

class ScreenshotsDeleteOnClick implements DialogInterface.OnClickListener {
	ScreenshotsActivity mActivity;

	ScreenshotsDeleteOnClick(ScreenshotsActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		ScreenshotsActivity activity = mActivity;
		activity.hideDelete();
		if (which != AlertDialog.BUTTON_POSITIVE) {
			return;
		}
		ScreenshotsImagesFragment fragment = activity.getImagesFragment();
		if (fragment == null) {
			return;
		}
		ArrayList<Integer> selected = fragment.mSelected;
		if (selected.isEmpty()) {
			return;
		}
		String game = activity.mGame;
		Iterator<Integer> iterator;
		long steamID = activity.mAccount.mSteamID;
		for (iterator = selected.iterator(); iterator.hasNext(); ) {
			ScreenshotName.deleteScreenshot(activity, iterator.next(), steamID, game);
		}
		activity.refreshEverything();
	}
}