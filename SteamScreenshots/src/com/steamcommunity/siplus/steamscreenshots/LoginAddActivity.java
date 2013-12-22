package com.steamcommunity.siplus.steamscreenshots;

import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class LoginAddActivity extends Activity {
	static final String EXTRASTATE_FROM_APP = Utility.PACKAGE + ".LoginAddActivity.EXTRASTATE_FROM_APP";
	static final String STATE_ERROR = Utility.PACKAGE + ".LoginAddActivity.STATE_ERROR";
	static final String STATE_GUARD_EMAIL = Utility.PACKAGE + ".LoginAddActivity.STATE_GUARD_EMAIL";
	static final String STATE_OLD_ACCOUNT = Utility.PACKAGE + ".LoginAddActivity.STATE_OLD_ACCOUNT";
	static final String STATE_TASK = Utility.PACKAGE + ".LoginAddActivity.STATE_TASK";

	AccountManager mAccountManager;
	ProgressDialog mCancelling;
	int mError;
	boolean mFromApp;
	String mGuardEmail;
	String mOldAccount;
	ProgressDialog mProgress;
	LoginAddTask mTask;

	EditText widgetAccount;
	Button widgetAddButton;
	TextView widgetError;
	LinearLayout widgetErrorFrame;
	LinearLayout widgetGuard;
	EditText widgetGuardCode;
	TextView widgetGuardInfo;
	EditText widgetGuardName;
	EditText widgetPassword;

	void doneLoggingIn() {
		hideLoginProgress();
		LoginAddTask task = mTask;
		mTask = null;
		ClientLogonResponseIncoming logonResponse = task.mLogonResponse;
		if (logonResponse == null) {
			setErrorText(R.string.error_connection);
			return;
		}
		if (logonResponse.mEResult == EResult.OK) {
			if (task.mLoginKey == null) {
				setErrorText(R.string.error_connection);
				return;
			}
		}
		switch (logonResponse.mEResult) {
		case EResult.OK:
			if (task.mLoginKey == null) {
				setErrorText(R.string.error_connection);
				return;
			}
			Bundle userdata = new Bundle();
			if (task.mGuardHash != null) {
				userdata.putString(SteamshotsAccount.DATA_GUARD_HASH, Base64.encodeToString(task.mGuardHash, Base64.NO_WRAP));
			}
			userdata.putString(SteamshotsAccount.DATA_LOGIN_KEY, task.mLoginKey);
			userdata.putString(SteamshotsAccount.DATA_STEAM_ID, Long.toString(logonResponse.mHeader.mSteamID));
			mAccountManager.addAccountExplicitly(new Account(task.mName, Utility.PACKAGE), null, userdata);
			finish();
			return;
		case EResult.INVALID_PASSWORD:
		case EResult.ILLEGAL_PASSWORD:
		case EResult.CANNOT_USE_OLD_PASSWORD:
			setErrorText(R.string.login_add_error_password);
			return;
		case EResult.LOGGED_IN_ELSEWHERE:
		case EResult.ALREADY_LOGGED_IN_ELSEWHERE:
			setErrorText(R.string.error_elsewhere);
			return;
		case EResult.ACCOUNT_DISABLED:
			setErrorText(R.string.login_add_error_disabled);
			return;
		case EResult.ACCOUNT_LOGON_DENIED:
			mGuardEmail = logonResponse.mEmailDomain;
			if (mGuardEmail == null) {
				mGuardEmail = "[unknown]";
			}
			refreshGuardInfo();
			return;
		case EResult.INVALID_LOGIN_AUTH_CODE:
		case EResult.EXPIRED_LOGIN_AUTH_CODE:
			setErrorText(R.string.login_add_error_guard_code);
			return;
		}
		setErrorText(R.string.login_add_error);
	}

	void hideLoginProgress() {
		if (mProgress == null) {
			return;
		}
		mProgress.dismiss();
		mProgress = null;
	}

	public void listenerAddOnClick(View view) {
		if (AsyncTaskWithRunID.isRunning(mTask)) {
			mCancelling = Utility.showCancellingProgress(this, new LoginAddCancellingOnCancel(this));
			return;
		}
		if (!Utility.isConnected(this)) {
			setErrorText(R.string.error_unconnected);
			return;
		}
		String name = widgetAccount.getText().toString().toLowerCase();
		Account[] accounts = mAccountManager.getAccountsByType(Utility.PACKAGE);
		int i;
		for (i = 0; i < accounts.length; ++i) {
			if (name.equals(accounts[i].name)) {
				setErrorText(R.string.login_add_error_exists);
				return;
			}
		}
		setErrorText(0);
		String guardCode;
		String guardName;
		if (mGuardEmail != null) {
			guardCode = widgetGuardCode.getText().toString();
			guardName = widgetGuardName.getText().toString();
		} else {
			guardCode = guardName = null;
		}
		mTask = new LoginAddTask(this, name,
			widgetPassword.getText().toString(),
			guardCode, guardName);
		mTask.execute((Void)(null));
		showLoginProgress();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAccountManager = AccountManager.get(this);

		setContentView(R.layout.view_login_add);
		widgetAccount = (EditText)(findViewById(R.id.view_login_add_account));
		widgetAddButton = (Button)(findViewById(R.id.view_login_add_button));
		widgetError = (TextView)(findViewById(R.id.view_login_add_error));
		widgetErrorFrame = (LinearLayout)(findViewById(R.id.view_login_add_error_frame));
		widgetGuard = (LinearLayout)(findViewById(R.id.view_login_add_guard));
		widgetGuardCode = (EditText)(findViewById(R.id.view_login_add_guard_code));
		widgetGuardInfo = (TextView)(findViewById(R.id.view_login_add_guard_info));
		widgetGuardName = (EditText)(findViewById(R.id.view_login_add_guard_name));
		widgetPassword = (EditText)(findViewById(R.id.view_login_add_password));

		if (savedInstanceState != null) {
			setErrorText(savedInstanceState.getInt(STATE_ERROR));
			mFromApp = savedInstanceState.getBoolean(EXTRASTATE_FROM_APP);
			mGuardEmail = savedInstanceState.getString(STATE_GUARD_EMAIL);
			mOldAccount = savedInstanceState.getString(STATE_OLD_ACCOUNT, "");
			mTask = (LoginAddTask)(AsyncTaskWithRunID.findRun(savedInstanceState.getInt(STATE_TASK)));
			if (mTask != null) {
				mTask.mActivity = this;
				if (!mTask.isCancelled()) {
					showLoginProgress();
				}
			}
		} else {
			setErrorText(0);
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				mFromApp = extras.getBoolean(EXTRASTATE_FROM_APP);
			}
			mOldAccount = "";
		}

		if (mFromApp) {
			Utility.enableActionBarBack(this);
		}

		refreshGuardInfo();

		widgetAccount.addTextChangedListener(new LoginAddAccountWatcher(this));
		LoginAddTextWatcher textWatcher = new LoginAddTextWatcher(this);
		widgetGuardCode.addTextChangedListener(textWatcher);
		widgetPassword.addTextChangedListener(textWatcher);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		hideLoginProgress();
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_ERROR, mError);
		outState.putBoolean(EXTRASTATE_FROM_APP, mFromApp);
		outState.putString(STATE_GUARD_EMAIL, mGuardEmail);
		outState.putString(STATE_OLD_ACCOUNT, widgetAccount.getText().toString());
		if (AsyncTaskWithRunID.isRunning(mTask)) {
			outState.putInt(STATE_TASK, mTask.mRun);
		}
	}

	void refreshGuardInfo() {
		toggleButtonEnabled();
		if (mGuardEmail == null) {
			widgetGuard.setVisibility(View.GONE);
			return;
		}
		widgetGuardInfo.setText(getResources().getString(R.string.login_add_guard_info, mGuardEmail));
		widgetGuard.setVisibility(View.VISIBLE);
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

	void showLoginProgress() {
		ProgressDialog dialog = new ProgressDialog(this);
		mProgress = dialog;
		dialog.setOnCancelListener(new LoginAddProgressOnCancel(this));
		dialog.setMax(mTask.mNeedGuard ? 7 : 5);
		dialog.setProgress(mTask.mStages);
		dialog.setProgressNumberFormat(null);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setTitle(R.string.login_add_progress);
		dialog.show();
	}

	void toggleButtonEnabled() {
		if ((widgetAccount.length() == 0) || (widgetPassword.length() == 0)) {
			widgetAddButton.setEnabled(false);
			return;
		}
		if (mGuardEmail != null) {
			if (widgetGuardCode.length() != 5) {
				widgetAddButton.setEnabled(false);
				return;
			}
		}
		widgetAddButton.setEnabled(true);
	}

	void updateLoginProgress(int progress) {
		if ((mProgress != null) && AsyncTaskWithRunID.isRunning(mTask)) {
			mProgress.setProgress(progress);
		}
	}
}

class LoginAddAccountWatcher implements TextWatcher {
	LoginAddActivity mActivity;

	LoginAddAccountWatcher(LoginAddActivity activity) {
		mActivity = activity;
	}

	@Override
	public void afterTextChanged(Editable s) {
		String account = s.toString();
		LoginAddActivity activity = mActivity;
		if (activity.mOldAccount.equals(account)) {
			return;
		}
		mActivity.mGuardEmail = null;
		mActivity.mOldAccount = account;
		mActivity.refreshGuardInfo();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
}

class LoginAddCancellingOnCancel implements DialogInterface.OnCancelListener {
	LoginAddActivity mActivity;

	LoginAddCancellingOnCancel(LoginAddActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		mActivity.mCancelling = null;
	}
}

class LoginAddProgressOnCancel implements DialogInterface.OnCancelListener {
	LoginAddActivity mActivity;

	LoginAddProgressOnCancel(LoginAddActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		mActivity.mProgress = null;
		LoginAddTask task = mActivity.mTask;
		if (AsyncTaskWithRunID.isRunning(task)) {
			task.cancel(false);
		}
	}
}

class LoginAddTask extends AsyncTaskWithRunID {
	LoginAddActivity mActivity;
	Connection mConnection;
	byte[] mGuardHash;
	boolean mLoggedOn;
	String mLoginKey;
	ClientLogonOutgoing mLogonOutgoing;
	ClientLogonResponseIncoming mLogonResponse;
	String mName;
	boolean mNeedGuard;
	int mStages;

	LoginAddTask(LoginAddActivity activity, String name, String password, String guardCode, String guardName) {
		mActivity = activity;
		ClientLogonOutgoing.initializeMachineID(activity);
		mLogonOutgoing = new ClientLogonOutgoing(name, password, guardCode, guardName);
		mName = name;
		mNeedGuard = guardCode != null;
	}

	void disconnect() {
		Connection connection = mConnection;
		if (connection == null) {
			return;
		}
		if (mLoggedOn) {
			try {
				connection.sendMessage(new ClientLogOffOutgoing());
			} catch (OutgoingException e) {}
		}
		connection.disconnect();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (isCancelled()) {return false;}
		Connection connection = new Connection();
		mConnection = connection;
		try {
			connection.connect(Connection.CM_ADDRESS, Connection.CM_PORT);
		} catch (ConnectionException e) {
			return false;
		}
		publishProgress(++mStages);
		if (isCancelled()) {return false;}
		try {
			connection.sendMessage(mLogonOutgoing);
			publishProgress(++mStages);
			if (isCancelled()) {return false;}
			mLogonResponse = new ClientLogonResponseIncoming(
				connection.waitForMessage(ClientLogonResponseIncoming.MESSAGE));
			if ((mLogonResponse.mEResult != EResult.OK) || isCancelled()) {
				return false;
			}
			publishProgress(++mStages);
			MessageHeader header = mLogonResponse.mHeader;
			connection.mSessionID = header.mSessionID;
			connection.mSteamID = header.mSteamID;
			connection.mHeartbeatWhenWaiting = true;
			if (mNeedGuard) {
				if (isCancelled()) {return false;}
				ClientUpdateMachineAuthIncoming updateMachineAuthIncoming = new ClientUpdateMachineAuthIncoming(
					connection.waitForMessage(ClientUpdateMachineAuthIncoming.MESSAGE));
				publishProgress(++mStages);
				mGuardHash = updateMachineAuthIncoming.mGuardHash;
				if (isCancelled()) {return false;}
				connection.sendMessage(new ClientUpdateMachineAuthResponseOutgoing(updateMachineAuthIncoming));
				publishProgress(++mStages);
			}
			if (isCancelled()) {return false;}
			ClientNewLoginKeyIncoming loginKeyIncoming = new ClientNewLoginKeyIncoming(
				connection.waitForMessage(ClientNewLoginKeyIncoming.MESSAGE));
			publishProgress(++mStages);
			mLoginKey = loginKeyIncoming.mLoginKey;
			if (isCancelled()) {return false;}
			connection.sendMessage(new ClientNewLoginKeyAcceptedOutgoing(loginKeyIncoming.mUniqueID));
		} catch (IncomingException e) {
			return false;
		} catch (OutgoingException e) {
			return false;
		}
		return true;
	}

	@Override
	void onCancelledWithRunID(boolean result) {
		disconnect();
		LoginAddActivity activity = mActivity;
		if ((activity == null) || (activity.mCancelling == null)) {
			return;
		}
		activity.mCancelling.dismiss();
		activity.mCancelling = null;
		activity.listenerAddOnClick(null);
	}

	@Override
	void onPostExecuteWithRunID(boolean result) {
		disconnect();
		if (mActivity != null) {
			mActivity.doneLoggingIn();
		}
	}

	@Override
	void onPreExecuteWithRunID() {}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		if (mActivity != null) {
			mActivity.updateLoginProgress(progress[0]);
		}
	}
}

class LoginAddTextWatcher implements TextWatcher {
	LoginAddActivity mActivity;

	LoginAddTextWatcher(LoginAddActivity activity) {
		mActivity = activity;
	}

	@Override
	public void afterTextChanged(Editable s) {
		mActivity.toggleButtonEnabled();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
}
