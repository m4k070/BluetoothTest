package com.interlinkj.android.bluetoothtest;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class ReceiveTask extends AsyncTask<String, String, String> {
	Handler mHandler;

	@Override
	protected String doInBackground(String... arg0) {
		// TODO Auto-generated method stub
		Message msg = new Message();
		mHandler.sendMessage(msg);
		return null;
	}

}
