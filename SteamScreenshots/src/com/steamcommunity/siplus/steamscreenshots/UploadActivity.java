package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.io.Files;
import com.google.common.primitives.UnsignedLongs;
import com.google.protobuf.ByteString;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public final class UploadActivity extends Activity {
	static final String EXTRASTATE_ACCOUNT = Utility.PACKAGE + ".UploadActivity.EXTRASTATE_ACCOUNT";
	static final String EXTRASTATE_GAME = Utility.PACKAGE + ".UploadActivity.EXTRASTATE_GAME";
	static final String EXTRASTATE_SCREENSHOTS = Utility.PACKAGE + ".UploadActivity.EXTRASTATE_SCREENSHOTS";
	static final String STATE_ERROR = Utility.PACKAGE + ".UploadActivity.STATE_ERROR";
	static final String STATE_SIZE = Utility.PACKAGE + ".UploadActivity.STATE_SIZE";
	static final String STATE_TASK = Utility.PACKAGE + ".UploadActivity.STATE_TASK";
	static final String STATE_UPLOADED = Utility.PACKAGE + ".UploadActivity.STATE_UPLOADED";

	SteamshotsAccount mAccount;
	int mError;
	boolean mFromApp;
	String mGame;
	String mGameName;
	ProgressDialog mProgress;
	Resources mResources;
	int[] mScreenshots;
	long mSize;
	UploadTask mTask;
	int mUploaded;

	EditText widgetCaption;
	TextView widgetError;
	LinearLayout widgetErrorFrame;
	EditText widgetLocation;
	TextView widgetSize;
	CheckBox widgetSpoiler;
	Button widgetUploadButton;
	Spinner widgetVisibility;

	void refreshHeader() {
		if (AsyncTaskWithRunID.isRunning(mTask)) {
			widgetSize.setText(R.string.upload_uploading);
			widgetUploadButton.setEnabled(false);
			return;
		}
		int i;
		String file;
		String path = ScreenshotName.folderPath(mAccount.mSteamID, mGame);
		int[] screenshots = mScreenshots;
		int length = screenshots.length;
		long size = 0;
		for (i = 0; i < length; ++i) {
			file = ScreenshotName.nameToString(screenshots[i]);
			size += (new File(path, file)).length() + (new File(path, file + ScreenshotName.THUMB_SUFFIX)).length();
		}
		mSize = size;
		widgetSize.setText(mResources.getQuantityString(R.plurals.upload_header, length,
			length, formatSize(mSize), mGameName));
		widgetUploadButton.setEnabled(true);
	}

	CharSequence formatSize(long size) {
		int resid;
		int value;
		if (size < 1024L) {
			resid = R.plurals.upload_size_bytes;
			value = (int)size;
		} else if (size < 1048576L) {
			resid = R.plurals.upload_size_kilobytes;
			value = (int)(size / 1024L);
		} else if (size < 1073741824L) {
			resid = R.plurals.upload_size_megabytes;
			value = (int)(size / 1048576L);
		} else {
			resid = R.plurals.upload_size_gigabytes;
			value = (int)(size / 1073741824L);
		}
		return mResources.getQuantityString(resid, value, value);
	}

	void hideUploadProgress() {
		if (mProgress == null) {
			return;
		}
		mProgress.dismiss();
		mProgress = null;
	}

	public void listenerUploadOnClick(View view) {
		if ((mScreenshots.length == 0) || AsyncTaskWithRunID.isRunning(mTask)) {
			return;
		}
		if (!Utility.isConnected(this)) {
			setErrorText(R.string.error_unconnected);
			return;
		}
		setErrorText(0);
		mTask = new UploadTask(this);
		mTask.execute((Void)(null));
		refreshHeader();
		showUploadProgress();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle;
		if (savedInstanceState != null) {
			bundle = savedInstanceState;
			mSize = savedInstanceState.getLong(STATE_SIZE);
			mUploaded = savedInstanceState.getInt(STATE_UPLOADED);
		} else {
			bundle = getIntent().getExtras();
			if (bundle == null) {
				finish();
				return;
			}
		}
		mAccount = bundle.getParcelable(EXTRASTATE_ACCOUNT);
		mGame = bundle.getString(EXTRASTATE_GAME);
		mScreenshots = bundle.getIntArray(EXTRASTATE_SCREENSHOTS);
		if ((mAccount == null) || (mGame == null) || (mScreenshots == null)) {
			finish();
			return;
		}

		mResources = getResources();

		mGameName = Utility.applicationLabel(getPackageManager(), mGame);

		Utility.enableActionBarBack(this);

		setContentView(R.layout.view_upload);
		widgetCaption = (EditText)(findViewById(R.id.view_upload_caption));
		widgetError = (TextView)(findViewById(R.id.view_upload_error));
		widgetErrorFrame = (LinearLayout)(findViewById(R.id.view_upload_error_frame));
		widgetLocation = (EditText)(findViewById(R.id.view_upload_location));
		widgetSize = (TextView)(findViewById(R.id.view_upload_size));
		widgetSpoiler = (CheckBox)(findViewById(R.id.view_upload_spoiler));
		widgetUploadButton = (Button)(findViewById(R.id.view_upload_button));
		widgetVisibility = (Spinner)(findViewById(R.id.view_upload_visibility));

		if (savedInstanceState != null) {
			setErrorText(savedInstanceState.getInt(STATE_ERROR));
			mTask = (UploadTask)(AsyncTaskWithRunID.findRun(savedInstanceState.getInt(STATE_TASK)));
			if (mTask != null) {
				mTask.mActivity = this;
				if (!mTask.isCancelled()) {
					showUploadProgress();
				}
			}
		} else {
			setErrorText(0);
		}
		refreshHeader();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		hideUploadProgress();
		if (isChangingConfigurations()) {
			return;
		}
		if (AsyncTaskWithRunID.isRunning(mTask)) {
			mTask.mActivity = null;
			mTask.cancel(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home) {
			return super.onOptionsItemSelected(item);
		}
		finish();
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRASTATE_ACCOUNT, mAccount);
		outState.putInt(STATE_ERROR, mError);
		outState.putString(EXTRASTATE_GAME, mGame);
		outState.putIntArray(EXTRASTATE_SCREENSHOTS, mScreenshots);
		outState.putLong(STATE_SIZE, mSize);
		if (AsyncTaskWithRunID.isRunning(mTask)) {
			outState.putInt(STATE_TASK, mTask.mRun);
		}
		outState.putInt(STATE_UPLOADED, mUploaded);
	}

	void setErrorText(int resid) {
		mError = resid;
		if (resid == 0) {
			widgetErrorFrame.setVisibility(View.GONE);
			return;
		}
		widgetError.setText(resid);
		widgetErrorFrame.setVisibility(View.VISIBLE);
	}

	void showUploadProgress() {
		ProgressDialog dialog = new ProgressDialog(this);
		mProgress = dialog;
		dialog.setOnCancelListener(new UploadProgressOnCancel(this));
		dialog.setMax(mScreenshots.length);
		dialog.setProgress(mUploaded);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setTitle(R.string.upload_progress);
		dialog.show();
	}

	void updateProgress(int progress) {
		mUploaded = progress;
		if (mProgress != null) {
			mProgress.setProgress(progress);
		}
	}

	void uploadCancelled() {
		int uploaded = mUploaded;
		if (uploaded != 0) {
			int[] screenshots = mScreenshots;
			int length = screenshots.length - uploaded;
			mScreenshots = new int[length];
			System.arraycopy(screenshots, uploaded, mScreenshots, uploaded, length);
		}
		refreshHeader();
	}

	void uploadFailed() {
		if (mUploaded == mScreenshots.length) {
			uploadSucceeded();
			return;
		}
		hideUploadProgress();
		setErrorText(mTask.mError);
		uploadCancelled();
	}

	void uploadSucceeded() {
		hideUploadProgress();
		finish();
	}
}

class UploadProgressOnCancel implements DialogInterface.OnCancelListener {
	UploadActivity mActivity;

	UploadProgressOnCancel(UploadActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		UploadActivity activity = mActivity;
		activity.mProgress = null;
		UploadTask task = activity.mTask;
		if (AsyncTaskWithRunID.isRunning(task)) {
			task.cancel(false);
		}
		activity.uploadCancelled();
	}
}

class UploadTask extends AsyncTaskWithRunID {	
	SteamshotsAccount mAccount;
	UploadActivity mActivity;
	String mCaption;
	Connection mCM;
	int mError;
	int mHeight;
	long mJob;
	String mLocation;
	boolean mLoggedOn;
	String mGame;
	String mGameID;
	String mGameName;
	int[] mScreenshots;
	boolean mSpoiler;
	Connection mUFS;
	int mUploaded;
	int mVisibility;
	int mWidth;

	UploadTask(UploadActivity activity) {
		mActivity = activity;
		ClientLogonOutgoing.initializeMachineID(activity);
	}

	boolean connect() {
		if (isCancelled()) {return false;}

		Connection cm = new Connection();
		try {
			cm.connect(Connection.CM_ADDRESS, Connection.CM_PORT);
		} catch (ConnectionException e) {
			mError = R.string.error_connection;
			return false;
		}
		if (isCancelled()) {return false;}
		mCM = cm;
		SteamshotsAccount account = mAccount;
		try {
			cm.sendMessage(new ClientLogonOutgoing(account.mName, account.mLoginKey, account.mGuardHash));
		} catch (OutgoingException e) {
			mError = R.string.error_connection;
			return false;
		}
		if (isCancelled()) {return false;}
		long sessionToken;
		String ufsAddress;
		int ufsPort;
		try {
			ClientLogonResponseIncoming logonResponse = new ClientLogonResponseIncoming(
				cm.waitForMessage(ClientLogonResponseIncoming.MESSAGE));
			if (isCancelled()) {return false;}
			switch (logonResponse.mEResult) {
			case EResult.OK:
				break;
			case EResult.ALREADY_LOGGED_IN_ELSEWHERE:
			case EResult.LOGGED_IN_ELSEWHERE:
			case EResult.PASSWORD_REQUIRED_TO_KICK_SESSION:
				mError = R.string.error_elsewhere;
				return false;
			default:
				mError = R.string.upload_error_eresult;
				return false;
			}
			cm.cloneSessionData(logonResponse);
			ClientSessionTokenIncoming sessionTokenIncoming = new ClientSessionTokenIncoming(
				cm.waitForMessage(ClientSessionTokenIncoming.MESSAGE));
			if (isCancelled()) {return false;}
			sessionToken = sessionTokenIncoming.mToken;
			ClientServerListIncoming serverList = new ClientServerListIncoming(
				cm.waitForMessage(ClientServerListIncoming.MESSAGE));
			if (isCancelled()) {return false;}
			if (serverList.mUFSAddress != null) {
				ufsAddress = serverList.mUFSAddress;
				ufsPort = serverList.mUFSPort;
			} else {
				serverList = new ClientServerListIncoming(cm.waitForMessage(ClientServerListIncoming.MESSAGE));
				if (serverList.mUFSAddress == null) {
					mError = R.string.error_connection;
					return false;
				}
				ufsAddress = serverList.mUFSAddress;
				ufsPort = serverList.mUFSPort;
			}
		} catch (IncomingException e) {
			mError = R.string.error_connection;
			return false;
		}
		cm.sendHeartbeat();

		Connection ufs = new Connection();
		try {
			ufs.connect(ufsAddress, ufsPort);
		} catch (ConnectionException e) {
			mError = R.string.error_connection;
			return false;
		}
		if (isCancelled()) {return false;}
		mUFS = ufs;
		ufs.mSteamID = cm.mSteamID;
		try {
			ufs.sendMessage(new ClientUFSLoginRequestOutgoing(sessionToken));
		} catch (OutgoingException e) {
			mError = R.string.error_connection;
			return false;
		}
		if (isCancelled()) {return false;}
		cm.sendHeartbeat();
		try {
			ClientUFSLoginResponseIncoming ufsLoginResponse = new ClientUFSLoginResponseIncoming(
				ufs.waitForMessage(ClientUFSLoginResponseIncoming.MESSAGE));
			if (isCancelled()) {return false;}
			ufs.cloneSessionData(ufsLoginResponse);
			if (ufsLoginResponse.mEResult == EResult.OK) {
				return true;
			}
		} catch (IncomingException e) {}
		mError = R.string.error_connection;
		return false;
	}

	void disconnect() {
		if (mUFS != null) {
			mUFS.disconnect();
		}
		Connection cm = mCM;
		if (cm != null) {
			if (mLoggedOn) {
				try {
					cm.sendMessage(new ClientLogOffOutgoing());
				} catch (OutgoingException e) {}
			}
			cm.disconnect();
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (isCancelled() || !connect()) {
			return false;
		}
		Connection cm = mCM;
		int i;
		Connection ufs = mUFS;
		ClientUCMAddScreenshotOutgoing outgoing = new ClientUCMAddScreenshotOutgoing(
			mGameName, mGameID, mCaption, mLocation, mVisibility, mSpoiler);
		int[] screenshots = mScreenshots;
		for (i = 0; i < screenshots.length; ++i) {
			if (isCancelled()) {return false;}
			if (!uploadScreenshot(screenshots[i], outgoing)) {
				mError = R.string.upload_error;
				return false;
			}
			ufs.sendHeartbeat();
			cm.sendHeartbeat();
			publishProgress(++mUploaded);
		}
		return true;
	}

	@Override
	void onCancelledWithRunID(boolean result) {
		disconnect();
		if (mActivity != null) {
			mActivity.uploadFailed();
		}
	}

	@Override
	void onPostExecuteWithRunID(boolean result) {
		disconnect();
		UploadActivity activity = mActivity;
		if (activity == null) {
			return;
		}
		if (result) {
			activity.uploadSucceeded();
		} else {
			activity.uploadFailed();
		}
	}

	@Override
	void onPreExecuteWithRunID() {
		UploadActivity activity = mActivity;
		mAccount = activity.mAccount;
		String game = activity.mGame;
		mGame = game;
		int[] screenshots = activity.mScreenshots;
		mScreenshots = new int[screenshots.length];
		System.arraycopy(screenshots, 0, mScreenshots, 0, screenshots.length);

		mCaption = activity.widgetCaption.getText().toString();
		mLocation = activity.widgetLocation.getText().toString();
		mSpoiler = activity.widgetSpoiler.isChecked();
		int visibility = activity.widgetVisibility.getSelectedItemPosition();
		if (visibility < 0) {
			mVisibility = 8;
		} else {
			mVisibility = 8 >> visibility;
		}

		mError = 0;
		mJob = 1;
		mLoggedOn = false;
		mUploaded = 0;

		CRC32 crc = new CRC32();
		crc.update(game.getBytes());
		mGameID = UnsignedLongs.toString((crc.getValue() << 32L) | -0x7ffffffffe000000L);
		mGameName = Utility.applicationLabel(activity.getPackageManager(), mGame);
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		if (mActivity != null) {
			mActivity.updateProgress(progress[0]);
		}
	}

	boolean uploadFile(String local, String remote, long time, boolean querySize) {
		File localFile = new File(local);
		if (isCancelled() || !localFile.isFile()) {
			return false;
		}
		if (localFile.length() > 33554432L) { // 32 MiB. Just a random number.
			return false;
		}
		byte[] data;
		int dataLength;
		byte[] zip;
		try {
			data = Files.toByteArray(localFile);
			dataLength = data.length;
			ByteArrayOutputStream zipByteArrayStream = new ByteArrayOutputStream(dataLength);
			ZipOutputStream zipStream = new ZipOutputStream(zipByteArrayStream);
			zipStream.putNextEntry(new ZipEntry("z"));
			zipStream.write(data);
			zipStream.closeEntry();
			zipStream.finish();
			zip = zipByteArrayStream.toByteArray();
		} catch (IOException e) {
			return false;
		}
		if (querySize) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, options);
			mHeight = options.outHeight;
			mWidth = options.outWidth;
		}
		if (isCancelled()) {return false;}
		byte[] hashBytes = Utility.shaHash(data);
		if (hashBytes == null) {
			return false;
		}
		ByteString hash = ByteString.copyFrom(hashBytes);
		int zipLength = zip.length;
		try {
			Connection cm = mCM;
			Connection ufs = mUFS;
			if (isCancelled()) {return false;}
			ufs.sendMessage(new ClientUFSUploadFileRequestOutgoing(++mJob, remote, dataLength, zipLength, hash, time));
			cm.sendHeartbeat();
			if (isCancelled()) {return false;}
			ClientUFSUploadFileResponseIncoming response = new ClientUFSUploadFileResponseIncoming(
				ufs.waitForMessage(ClientUFSUploadFileResponseIncoming.MESSAGE));
			int eresult = response.mEResult;
			if (eresult == EResult.DUPLICATE_REQUEST) {
				return true; // Reuploading after cancelling
			}
			if (response.mUnsupportedModes || (response.mEResult != EResult.OK)) {
				return false;
			}
			cm.sendHeartbeat();
			ClientUFSFileChunkOutgoing chunk = new ClientUFSFileChunkOutgoing(response.mHeader.mJobSource, hash);
			int i;
			for (i = 0; i < zipLength; i += ClientUFSFileChunkOutgoing.CHUNK_SIZE) {
				if (isCancelled()) {return false;}
				chunk.setData(zip, i);
				ufs.sendMessage(chunk);
				cm.sendHeartbeat();
			}
			if (isCancelled()) {return false;}
			ClientUFSUploadFileFinishedIncoming finished = new ClientUFSUploadFileFinishedIncoming(
				ufs.waitForMessage(ClientUFSUploadFileFinishedIncoming.MESSAGE));
			cm.sendHeartbeat();
			return finished.mEResult == EResult.OK;
		} catch (IncomingException e) {
			e.printStackTrace();
			return false;
		} catch (OutgoingException e) {
			e.printStackTrace();
			return false;
		}
	}

	boolean uploadScreenshot(int screenshot, ClientUCMAddScreenshotOutgoing outgoing) {
		String game = mGame;
		long steamID = mAccount.mSteamID;
		ArrayList<UploadedCaption> captions = UploadedCaption.fromFileSorted(steamID, game);
		if (captions != null) {
			Iterator<UploadedCaption> iterator;
			for (iterator = captions.iterator(); iterator.hasNext(); ) {
				if (iterator.next().mScreenshot == screenshot) {
					return true; // Late cancel
				}
			}
		}
		String name = ScreenshotName.nameToString(screenshot);
		String cloudFile = mGameID + "/screenshots/" + name;
		String cloudThumb = mGameID + "/screenshots/thumbnails/" + name;
		String path = ScreenshotName.folderPath(steamID, game) + name;
		long time = ScreenshotName.creationTime(screenshot, steamID, game) / 1000L;
		if (!uploadFile(path, cloudFile, time, true)) {
			return false;
		}
		if (isCancelled()) {return false;}
		if (!uploadFile(path + ScreenshotName.THUMB_SUFFIX, cloudThumb, time, false)) {
			return false;
		}
		// Must not cancel here, because the upload has succeeded
		outgoing.setFile(screenshot, mWidth, mHeight, time, ++mJob);
		Connection cm = mCM;
		try {
			cm.sendMessage(outgoing);
			mUFS.sendHeartbeat();
			ClientUCMAddScreenshotResponseIncoming response = new ClientUCMAddScreenshotResponseIncoming(
				cm.waitForMessage(ClientUCMAddScreenshotResponseIncoming.MESSAGE));
			if (response.mEResult != EResult.OK) {
				return false;
			}
			if (captions == null) {
				captions = new ArrayList<UploadedCaption>(1);
			}
			captions.add(new UploadedCaption(screenshot, response.mHandle, mCaption));
			return UploadedCaption.toFile(captions, steamID, game);
		} catch (IncomingException e) {
			return false;
		} catch (OutgoingException e) {
			return false;
		}
	}
}
