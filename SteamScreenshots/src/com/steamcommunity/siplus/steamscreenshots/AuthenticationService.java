package com.steamcommunity.siplus.steamscreenshots;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public final class AuthenticationService extends Service {
	Authenticator mAuthenticator;

	@Override
	public void onCreate() {
		mAuthenticator = new Authenticator(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mAuthenticator.getIBinder();
	}
}
