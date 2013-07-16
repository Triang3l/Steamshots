package com.steamcommunity.siplus.steamscreenshots;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Fragment;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ScreenshotsGamesFragment extends Fragment {
	static final String FRAGMENT_TAG = Utility.PACKAGE + ".ScreenshotsGamesFragment";

	SteamshotsAccount mAccount;
	ScreenshotsActivity mActivity;
	ScreenshotsGamesAdapter mAdapter;
	PackageManager mPackageManager;

	TextView widgetEmpty;
	ListView widgetList;

	void fillGamesList() {
		ScreenshotsActivity activity = mActivity;

		String path = String.format("%s/Steamshots/%d",
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(),
			mAccount.mSteamID);
		File gamesDirectory = new File(path);
		if (!Utility.makeDirectories(gamesDirectory)) {
			return;
		}

		File[] games = gamesDirectory.listFiles(new ScreenshotsGamesFileFilter());
		String gameNames[] = new String[games.length];
		int i;
		for (i = 0; i < games.length; ++i) {
			gameNames[i] = games[i].getName();
		}
		Arrays.sort(gameNames, new ScreenshotsGamesComparator(mPackageManager));
		activity.mGames = gameNames;

		mAdapter.notifyDataSetChanged();

		if (gameNames.length == 0) {
			widgetEmpty.setVisibility(View.VISIBLE);
			widgetList.setVisibility(View.GONE);
		} else {
			widgetEmpty.setVisibility(View.GONE);
			widgetList.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_screenshots_games, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ScreenshotsActivity activity = (ScreenshotsActivity)(getActivity());
		mActivity = activity;
		mAccount = activity.mAccount;
		mPackageManager = activity.getPackageManager();

		View view = getView();
		widgetEmpty = (TextView)(view.findViewById(R.id.fragment_screenshots_games_empty));
		widgetList = (ListView)(view.findViewById(R.id.fragment_screenshots_games_list));

		mAdapter = new ScreenshotsGamesAdapter(activity);
		fillGamesList();
		widgetList.setAdapter(mAdapter);
		widgetList.setOnItemClickListener(new ScreenshotsGamesOnItemClick(activity));
	}
}

class ScreenshotsGamesAdapter extends BaseAdapter {
	ScreenshotsActivity mActivity;
	LayoutInflater mLayoutInflater;

	ScreenshotsGamesAdapter(ScreenshotsActivity activity) {
		mActivity = activity;
		mLayoutInflater = mActivity.getLayoutInflater();
	}

	@Override
	public int getCount() {
		return mActivity.mGames.length;
	}

	@Override
	public Object getItem(int position) {
		return mActivity.mGames[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ScreenshotsActivity activity = mActivity;
		PackageManager packageManager = activity.getPackageManager();
		String game = activity.mGames[position];
		Drawable icon;
		CharSequence label;
		try {
			ApplicationInfo info = packageManager.getApplicationInfo(game, 0);
			icon = packageManager.getApplicationIcon(info);
			label = packageManager.getApplicationLabel(info);
			if (label == null) {
				label = game;
			}
		} catch (NameNotFoundException e) {
			icon = activity.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
			label = game;
		}
		View view = Utility.inflateImageTextListItem(mLayoutInflater, parent, icon, label);
		if (game.equals(activity.mGame)) {
			view.setBackgroundResource(android.R.color.holo_blue_dark);
		}
		return view;
	}
}

class ScreenshotsGamesComparator implements Comparator<String> {
	PackageManager mPackageManager;

	ScreenshotsGamesComparator(PackageManager packageManager) {
		mPackageManager = packageManager;
	}

	@Override
	public int compare(String lhs, String rhs) {
		return Utility.applicationLabel(mPackageManager, lhs).compareToIgnoreCase(
			Utility.applicationLabel(mPackageManager, rhs));
	}
}

class ScreenshotsGamesFileFilter implements FileFilter {
	@Override
	public boolean accept(File pathname) {
		if (!(pathname.isDirectory() && pathname.getName().matches("\\A[\\.0-9A-Z_a-z]+\\z"))) {
			return false;
		}
		return pathname.listFiles(new ScreenshotFileFilter(0)).length != 0;
	}
}

class ScreenshotsGamesOnItemClick implements OnItemClickListener {
	ScreenshotsActivity mActivity;

	ScreenshotsGamesOnItemClick(ScreenshotsActivity activity) {
		mActivity = activity;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mActivity.selectGame(mActivity.mGames[position]);
	}
}