package com.steamcommunity.siplus.steamscreenshots;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public final class Utility {
	static final String PACKAGE = "com.steamcommunity.siplus.steamscreenshots";

	static String applicationLabel(PackageManager packageManager, String packageName) {
		try {
			ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
			return (String)(packageManager.getApplicationLabel(info));
		} catch (NameNotFoundException e) {
			return packageName;
		}
	}

	static void closeCloseable(Closeable closeable) {
		try {
			closeable.close();
		} catch (IOException e) {}
	}

	static void enableActionBarBack(Activity activity) {
		ActionBar actionBar = activity.getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
	}

	static View inflateImageTextListItem(LayoutInflater inflater, ViewGroup parent, Drawable image, CharSequence text) {
		View view = inflater.inflate(R.layout.list_imagetext, parent, false);
		((ImageView)(view.findViewById(R.id.list_imagetext_image))).setImageDrawable(image);
		((TextView)(view.findViewById(R.id.list_imagetext_text))).setText(text);
		return view;
	}

	static boolean isConnected(Context context) {
		NetworkInfo networkInfo = ((ConnectivityManager)(context.getSystemService(Context.CONNECTIVITY_SERVICE)))
			.getActiveNetworkInfo();
		return (networkInfo != null) && networkInfo.isConnectedOrConnecting();
	}

	static boolean makeDirectories(File path) {
		return path.isDirectory() || path.mkdirs();
	}

	static void readFromStream(InputStream stream, byte[] target) throws IOException {
		int read;
		int toRead = target.length;
		while (toRead > 0) {
			read = stream.read(target, target.length - toRead, toRead);
			if (read <= 0) {
				throw new IOException();
			}
			if (read > toRead) {
				throw new IOException();
			}
			toRead -= read;
		}
	}

	static byte[] shaHash(byte[] bytes) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		return digest.digest(bytes);
	}

	static ProgressDialog showCancellingProgress(Context context, DialogInterface.OnCancelListener listener) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setMessage(context.getResources().getString(R.string.cancelling_message));
		dialog.setOnCancelListener(listener);
		dialog.setTitle(R.string.cancelling_title);
		dialog.show();
		return dialog;
	}

	static void reverseArray(int[] array) {
		int i;
		int l = array.length;
		int t;
		int e;
		for (i = l >> 1; --i >= 0; ) {
			t = l - i - 1;
			e = array[t];
			array[t] = array[i];
			array[i] = e;
		}
	}
}