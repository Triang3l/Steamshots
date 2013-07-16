package com.steamcommunity.siplus.steamscreenshots;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public final class LoginSelectActivity extends Activity {
	AccountManager mAccountManager;
	SteamshotsAccount[] mAccounts;
	LoginSelectAdapter mAdapter;
	LoginSelectAvatarTask mAvatarTask;
	Drawable mDefaultAvatar;

	LinearLayout widgetEmpty;
	ListView widgetList;

	void avatarLoaded(long steamID, String address, byte[] avatar) {
		int i;
		long time = System.currentTimeMillis() + 86400000L;
		SteamshotsAccount steamshotsAccount;

		Account account;
		AccountManager accountManager = mAccountManager;
		Account[] accounts = accountManager.getAccountsByType(Utility.PACKAGE);
		long accountSteamID;
		for (i = 0; i < accounts.length; ++i) {
			account = accounts[i];
			try {
				accountSteamID = Long.parseLong(accountManager.getUserData(account, SteamshotsAccount.DATA_STEAM_ID));
			} catch (NumberFormatException e) {
				continue;
			}
			if (accountSteamID != steamID) {
				continue;
			}
			if (address == null) { // Failed to get the address - just delay for a hour and don't update anything
				accountManager.setUserData(account, SteamshotsAccount.DATA_NEXT_AVATAR_UPDATE, Long.toString(time - 82800000L));
				break;
			}
			accountManager.setUserData(account, SteamshotsAccount.DATA_NEXT_AVATAR_UPDATE, Long.toString(time));
			if (address.equals(accountManager.getUserData(account, SteamshotsAccount.DATA_AVATAR_ADDRESS))) {
				break;
			}
			if (avatar == null) {
				accountManager.setUserData(account, SteamshotsAccount.DATA_AVATAR, null);
			} else {
				accountManager.setUserData(account, SteamshotsAccount.DATA_AVATAR,
					Base64.encodeToString(avatar, Base64.NO_WRAP));
			}
			accountManager.setUserData(account, SteamshotsAccount.DATA_AVATAR_ADDRESS, address);
			break;
		}

		SteamshotsAccount[] steamshotsAccounts = mAccounts;
		for (i = 0; i < steamshotsAccounts.length; ++i) {
			steamshotsAccount = steamshotsAccounts[i];
			if (steamshotsAccount.mSteamID != steamID) {
				continue;
			}
			// No need to set .mNextAvatarUpdate, because it's used only once when loading accounts and avatars
			if (address == null) {
				break;
			}
			if (avatar == null) {
				steamshotsAccount.mAvatar = null;
			} else {
				steamshotsAccount.mAvatar = BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
			}
			steamshotsAccount.mAvatarAddress = address;
			break;
		}

		mAdapter.notifyDataSetChanged();
	}

	void loadAccounts() {
		if (mAvatarTask != null) {
			mAvatarTask.cancel(false);
			return;
		}
		AccountManager accountManager = mAccountManager;
		Account[] accounts = accountManager.getAccountsByType(Utility.PACKAGE);
		int accountsLength = accounts.length;
		int i;
		SteamshotsAccount[] steamshotsAccounts = new SteamshotsAccount[accountsLength];
		mAccounts = steamshotsAccounts;
		for (i = 0; i < accountsLength; ++i) {
			steamshotsAccounts[i] = new SteamshotsAccount(accountManager, accounts[i]);
		}
		Arrays.sort(steamshotsAccounts, new LoginSelectComparator());
		if (Utility.isConnected(this)) {
			ArrayList<SteamshotsAccount> avatarsToLoad = new ArrayList<SteamshotsAccount>(accountsLength);
			SteamshotsAccount steamshotsAccount;
			long time = System.currentTimeMillis();
			for (i = 0; i < accountsLength; ++i) {
				steamshotsAccount = steamshotsAccounts[i];
				if (time > steamshotsAccount.mNextAvatarUpdate) {
					avatarsToLoad.add(steamshotsAccount);
				}
			}
			if (!avatarsToLoad.isEmpty()) {
				LoginSelectAvatarTask task = new LoginSelectAvatarTask(this, avatarsToLoad);
				mAvatarTask = task;
				task.execute((Void)(null));
			}
		}

		if (accountsLength == 0) {
			widgetEmpty.setVisibility(View.VISIBLE);
			widgetList.setVisibility(View.GONE);
		} else {
			widgetEmpty.setVisibility(View.GONE);
			mAdapter = new LoginSelectAdapter(this);
			widgetList.setAdapter(mAdapter);
			widgetList.setOnItemClickListener(new LoginSelectOnItemClick(this));
			widgetList.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		mAccountManager = AccountManager.get(this);
		mDefaultAvatar = getResources().getDrawable(R.drawable.avatar);

		setContentView(R.layout.view_login_select);
		widgetEmpty = (LinearLayout)(findViewById(R.id.view_login_select_empty));
		widgetList = (ListView)(findViewById(R.id.view_login_select_list));

		loadAccounts();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.login_select, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAvatarTask != null) {
			mAvatarTask.cancel(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != R.id.action_login_select_add) {
			return super.onOptionsItemSelected(item);
		}
		startActivity(new Intent(this, LoginAddActivity.class)
			.putExtra(LoginAddActivity.EXTRASTATE_FROM_APP, true));
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		loadAccounts();
	}
}

class LoginSelectAdapter extends BaseAdapter {
	LoginSelectActivity mActivity;
	float mDisabledAlpha;
	LayoutInflater mLayoutInflater;
	Resources mResources;

	LoginSelectAdapter(LoginSelectActivity activity) {
		mActivity = activity;
		{
			TypedValue outValue = new TypedValue();
			activity.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
			mDisabledAlpha = outValue.getFloat();
		}
		mLayoutInflater = activity.getLayoutInflater();
		mResources = activity.getResources();
	}

	@Override
	public int getCount() {
		return mActivity.mAccounts.length;
	}

	@Override
	public Object getItem(int position) {
		return mActivity.mAccounts[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LoginSelectActivity activity = mActivity;
		SteamshotsAccount account = activity.mAccounts[position];
		Drawable avatar;
		Bitmap avatarBitmap = account.mAvatar;
		if (avatarBitmap != null) {
			avatar = new BitmapDrawable(mResources, avatarBitmap);
		} else {
			avatar = activity.mDefaultAvatar;
		}
		View view = Utility.inflateImageTextListItem(mLayoutInflater, parent, avatar, account.mName);
		if (!account.mValid) {
			view.setAlpha(mDisabledAlpha);
		}
		return view;
	}
}

class LoginSelectAvatarTask extends AsyncTask<Void, Object, Void> {
	SteamshotsAccount[] mAccounts;
	LoginSelectActivity mActivity;
	String mAddressElement;

	LoginSelectAvatarTask(LoginSelectActivity activity, ArrayList<SteamshotsAccount> accounts) {
		mActivity = activity;

		mAccounts = new SteamshotsAccount[accounts.size()];
		accounts.toArray(mAccounts);

		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int density = metrics.densityDpi;
		if (density <= DisplayMetrics.DENSITY_LOW) {
			mAddressElement = "avatarIcon";
		} else if (density >= DisplayMetrics.DENSITY_XHIGH) {
			mAddressElement = "avatarFull";
		} else {
			mAddressElement = "avatarMedium";
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		int i;
		SteamshotsAccount account;
		String address;
		SteamshotsAccount[] accounts = mAccounts;
		byte[] avatar;
		long steamID;
		for (i = 0; i < accounts.length; ++i) {
			account = accounts[i];
			steamID = account.mSteamID;
			address = requestAddress(steamID);
			if (isCancelled()) {return null;}
			if (address == null) {
				publishProgress(steamID, null, null);
				continue;
			}
			if (address.indexOf("fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb") >= 0) {
				publishProgress(steamID, address, null);
				continue;
			}
			avatar = downloadFile(address, "image/jpeg");
			if (isCancelled()) {return null;}
			if (avatar == null) {
				publishProgress(steamID, null, null);
			} else {
				publishProgress(steamID, address, avatar);
			}
		}
		return null;
	}

	byte[] downloadFile(String address, String contentType) {
		URL url;
		try {
			url = new URL(address);
		} catch (MalformedURLException e) {
			return null;
		}
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection)(url.openConnection());
			connection.setInstanceFollowRedirects(true);
			connection.connect();
			if (isCancelled() ||
				(connection.getResponseCode() != HttpURLConnection.HTTP_OK) ||
				!contentType.equals(connection.getContentType())) {
				connection.disconnect();
				return null;
			}
			byte[] buffer = new byte[16384];
			InputStream inputStream = connection.getInputStream();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			int read;
			for (;;) {
				if (isCancelled()) {
					connection.disconnect();
					return null;
				}
				read = inputStream.read(buffer);
				if (read < 0) {
					break;
				}
				outputStream.write(buffer, 0, read);
			}
			connection.disconnect();
			return outputStream.toByteArray();
		} catch (IOException e) {
			if (connection != null) {
				connection.disconnect();
			}
			return null;
		}
	}

	@Override
	protected void onProgressUpdate(Object... progress) {
		if (!isCancelled()) {
			mActivity.avatarLoaded((Long)(progress[0]), (String)(progress[1]), (byte[])(progress[2]));
		}
	}

	String requestAddress(long steamID) {
		byte[] data = downloadFile(String.format("http://steamcommunity.com/profiles/%d?xml=1", steamID),
			"text/xml; charset=utf-8");
		if ((data == null) || isCancelled()) {
			return null;
		}
		try {
			XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
			parser.setInput(new ByteArrayInputStream(data), "UTF-8");
			String element = mAddressElement;
			int event;
			for (;;) {
				event = parser.next();
				if (event == XmlPullParser.END_DOCUMENT) {
					return null;
				}
				if ((event != XmlPullParser.START_TAG) || (!element.equals(parser.getName()))) {
					continue;
				}
				return parser.nextText();
			}
		} catch (IOException e) {
			return null;
		} catch (XmlPullParserException e) {
			return null;
		}
	}
}

class LoginSelectComparator implements Comparator<SteamshotsAccount> {
	@Override
	public int compare(SteamshotsAccount lhs, SteamshotsAccount rhs) {
		return lhs.mName.compareTo(rhs.mName);
	}
}

class LoginSelectOnItemClick implements OnItemClickListener {
	LoginSelectActivity mActivity;

	LoginSelectOnItemClick(LoginSelectActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		LoginSelectActivity activity = mActivity;
		SteamshotsAccount account = activity.mAccounts[position];
		if (!account.mValid) {
			Toast.makeText(activity, R.string.login_select_corrupted, Toast.LENGTH_LONG).show();
			return;
		}
		activity.startActivity(new Intent(activity, ScreenshotsActivity.class)
			.putExtra(ScreenshotsActivity.EXTRASTATE_ACCOUNT, account));
	}
}