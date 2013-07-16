package com.steamcommunity.siplus.steamscreenshots;

import java.util.Iterator;
import java.util.LinkedList;

import android.os.AsyncTask;

public abstract class AsyncTaskWithRunID extends AsyncTask<Void, Integer, Boolean> {
	static int mNextRun;
	int mRun;
	static LinkedList<AsyncTaskWithRunID> mRuns = new LinkedList<AsyncTaskWithRunID>();

	static AsyncTaskWithRunID findRun(int run) {
		if (run <= 0) {
			return null;
		}
		Iterator<AsyncTaskWithRunID> iterator;
		AsyncTaskWithRunID task;
		for (iterator = mRuns.iterator(); iterator.hasNext(); ) {
			task = iterator.next();
			if (task.mRun == run) {
				return task;
			}
		}
		return null;
	}

	static boolean isRunning(AsyncTaskWithRunID task) {
		return (task != null) && (task.mRun > 0);
	}

	@Override
	protected void onCancelled(Boolean result) {
		removeSelfFromRunsList();
		onCancelledWithRunID(result);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		removeSelfFromRunsList();
		onPostExecuteWithRunID(result);
	}

	@Override
	protected void onPreExecute() {
		mRun = (mNextRun++) + 1;
		mNextRun &= 0x3fffffff;
		mRuns.add(this);
		onPreExecuteWithRunID();
	}

	void removeSelfFromRunsList() {
		int oldRun = mRun;
		mRun = 0;
		Iterator<AsyncTaskWithRunID> iterator;
		for (iterator = mRuns.iterator(); iterator.hasNext(); ) {
			if (iterator.next().mRun == oldRun) {
				iterator.remove();
				return;
			}
		}
	}

	abstract void onCancelledWithRunID(boolean result);
	abstract void onPostExecuteWithRunID(boolean result);
	abstract void onPreExecuteWithRunID();
}
