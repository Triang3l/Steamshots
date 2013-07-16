package com.steamcommunity.siplus.steamscreenshots;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public final class Authenticator extends AbstractAccountAuthenticator {
	Context currentContext;

	public Authenticator(Context context) {
		super(context);
		currentContext = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
		String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, new Intent(currentContext, LoginAddActivity.class)
			.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response));
		return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
		throws NetworkErrorException {
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,Account account, String authTokenType, Bundle options)
		throws NetworkErrorException {
		return null;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
		throws NetworkErrorException {
		return null;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
		throws NetworkErrorException {
		return null;
	}
}
