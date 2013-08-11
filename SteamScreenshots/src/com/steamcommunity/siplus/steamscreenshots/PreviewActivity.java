package com.steamcommunity.siplus.steamscreenshots;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;

public class PreviewActivity extends Activity {
	static final String EXTRASTATE_ACCOUNT = Utility.PACKAGE + ".PreviewActivity.EXTRASTATE_ACCOUNT";
	static final String EXTRASTATE_CAPTION = Utility.PACKAGE + ".PreviewActivity.EXTRASTATE_CAPTION";
	static final String EXTRASTATE_GAME = Utility.PACKAGE + ".PreviewActivity.EXTRASTATE_GAME";
	static final String EXTRASTATE_SCREENSHOT = Utility.PACKAGE + ".PreviewActivity.EXTRASTATE_SCREENSHOT";
	static final String STATE_DELETE = Utility.PACKAGE + ".PreviewActivity.STATE_DELETE";

	SteamshotsAccount mAccount;
	UploadedCaption mCaption;
	boolean mFailedToLoad;
	AlertDialog mDelete;
	String mGame;
	int mScreenshot;

	TextView widgetCaption;
	TextView widgetFail;
	LinearLayout widgetInfo;
	TouchImageView widgetScreenshot;
	TextView widgetTime;

	void hideDelete() {
		if (mDelete != null) {
			mDelete.dismiss();
			mDelete = null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle;
		if (savedInstanceState != null) {
			bundle = savedInstanceState;
		} else {
			bundle = getIntent().getExtras();
			if (bundle == null) {
				finish();
				return;
			}
		}
		SteamshotsAccount account = bundle.getParcelable(EXTRASTATE_ACCOUNT);
		mCaption = bundle.getParcelable(EXTRASTATE_CAPTION);
		mGame = bundle.getString(EXTRASTATE_GAME);
		mScreenshot = bundle.getInt(EXTRASTATE_SCREENSHOT);
		if ((account == null) || (mGame == null) || (mScreenshot <= 0)) {
			finish();
			return;
		}
		mAccount = account;

		Utility.enableActionBarBack(this);

		setContentView(R.layout.view_preview);
		widgetCaption = (TextView)(findViewById(R.id.preview_caption));
		widgetFail = (TextView)(findViewById(R.id.preview_fail));
		widgetInfo = (LinearLayout)(findViewById(R.id.preview_info));
		widgetScreenshot = (TouchImageView)(findViewById(R.id.preview_screenshot));
		widgetTime = (TextView)(findViewById(R.id.preview_time));

		int screenshot = mScreenshot;
		String game = mGame;
		String name = ScreenshotName.nameToString(screenshot);
		Resources resources = getResources();
		long steamID = account.mSteamID;
		Bitmap bitmap = BitmapFactory.decodeFile(ScreenshotName.folderPath(steamID, game) + name);
		if (bitmap == null) {
			mFailedToLoad = true;
			widgetFail.setText(resources.getString(R.string.preview_fail, steamID, game, name));
			widgetFail.setVisibility(View.VISIBLE);
			widgetInfo.setVisibility(View.GONE);
			widgetScreenshot.setVisibility(View.GONE);
			return;
		}
		widgetScreenshot.setImageBitmap(bitmap);
		widgetTime.setText(DateFormat.getInstance().format(new Date(
			ScreenshotName.creationTime(screenshot, steamID, game))));
		widgetFail.setVisibility(View.GONE);
		widgetInfo.setVisibility(View.VISIBLE);
		widgetScreenshot.setVisibility(View.VISIBLE);

		if ((savedInstanceState != null) && (savedInstanceState.getBoolean(STATE_DELETE))) {
			showDelete();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mFailedToLoad) {
			return true;
		}
		ArrayList<UploadedCaption> captions = UploadedCaption.fromFileSorted(mAccount.mSteamID, mGame);
		if (captions != null) {
			UploadedCaption item;
			Iterator<UploadedCaption> iterator;
			long screenshot = mScreenshot;
			for (iterator = captions.iterator(); iterator.hasNext(); ) {
				item = iterator.next();
				if (item.mScreenshot == screenshot) {
					mCaption = item;
					break;
				}
			}
		}
		UploadedCaption caption = mCaption;
		if (caption == null) {
			getMenuInflater().inflate(R.menu.screenshots_new, menu);
			widgetCaption.setVisibility(View.GONE);
			return true;
		}
		getMenuInflater().inflate(R.menu.screenshots_uploaded, menu);
		((ShareActionProvider)(menu.findItem(R.id.action_screenshots_uploaded_share).getActionProvider()))
			.setShareIntent(caption.createShareIntent(mAccount.mSteamID));
		if (caption.mCaption.length() == 0) {
			widgetCaption.setVisibility(View.GONE);
			return true;
		}
		widgetCaption.setText(caption.mCaption);
		widgetCaption.setVisibility(View.VISIBLE);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		hideDelete();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.action_screenshots_new_delete:
		case R.id.action_screenshots_uploaded_delete:
			showDelete();
			return true;
		case R.id.action_screenshots_new_upload:
			startActivity(new Intent(getApplicationContext(), UploadActivity.class)
				.putExtra(UploadActivity.EXTRASTATE_ACCOUNT, mAccount)
				.putExtra(UploadActivity.EXTRASTATE_GAME, mGame)
				.putExtra(UploadActivity.EXTRASTATE_SCREENSHOTS, new int[] {mScreenshot}));
			return true;
		case R.id.action_screenshots_uploaded_view:
			mCaption.openInBrowser(this, mAccount.mSteamID);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		invalidateOptionsMenu();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRASTATE_ACCOUNT, mAccount);
		if (mCaption != null) {
			outState.putParcelable(EXTRASTATE_CAPTION, mCaption);
		}
		if (mDelete != null) {
			outState.putBoolean(STATE_DELETE, true);
		}
		outState.putString(EXTRASTATE_GAME, mGame);
		outState.putInt(EXTRASTATE_SCREENSHOT, mScreenshot);
	}

	void showDelete() {
		if (mDelete != null) {
			return;
		}
		PreviewDeleteOnClick listener = new PreviewDeleteOnClick(this);
		mDelete = (new AlertDialog.Builder(this))
			.setMessage(R.string.preview_delete)
			.setNegativeButton(R.string.delete_cancel, listener)
			.setOnCancelListener(new PreviewDeleteOnCancel(this))
			.setPositiveButton(R.string.delete_ok, listener)
			.show();
	}
}

class PreviewDeleteOnCancel implements DialogInterface.OnCancelListener {
	PreviewActivity mActivity;

	PreviewDeleteOnCancel(PreviewActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		mActivity.mDelete = null;
	}
}

class PreviewDeleteOnClick implements DialogInterface.OnClickListener {
	PreviewActivity mActivity;

	PreviewDeleteOnClick(PreviewActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		PreviewActivity activity = mActivity;
		activity.hideDelete();
		if (which == AlertDialog.BUTTON_POSITIVE) {
			ScreenshotName.deleteScreenshot(activity, activity.mScreenshot, activity.mAccount.mSteamID, activity.mGame);
			activity.finish();
		}
	}
}