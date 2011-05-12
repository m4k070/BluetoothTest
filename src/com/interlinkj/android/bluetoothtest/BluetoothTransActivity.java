package com.interlinkj.android.bluetoothtest;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.*;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothTransActivity extends Activity {
	private static final int BUFFER_SIZE = 512;
	private static final int MESSAGE_READ = 0;
	private static final UUID SERIAL_PORT_PROFILE = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String TAG = "BluetoothTransTest";

	private BluetoothAdapter mAdapter;
	private BluetoothSocket mSocket;
	private InputStream mInStream;
	private OutputStream mOutStream;
	private AtomicBoolean mClosed = new AtomicBoolean();
	private String mLogText = null;
	private Handler mHandler;
	private TextView mTextView;
	private ConnectedThread mConnectedThread;
	private BluetoothDevice mDevice;
	private String mText;

	public byte[] mBuffer;

	public String getText() {
		return mText;
	}

	protected void setText(String s) {
		mText = s;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.trans);

		Intent i = getIntent();
		String address = i.getStringExtra("MACAddress");
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mDevice = mAdapter.getRemoteDevice(address);

		mTextView = (TextView)findViewById(R.id.textView1);

		mBuffer = new byte[1024];
	}

	@Override
	public void onResume() {
		super.onResume();

		mHandler = new ReceiveHandler();

		ConnectThread connectThread = new ConnectThread(mDevice);
		connectThread.run();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			mSocket.close();
		} catch(IOException e) {

		}
	}

	public void manageConnectedSocket(BluetoothSocket mmSocket) {
		mSocket = mmSocket;
		mConnectedThread = new ConnectedThread(mSocket);
		mConnectedThread.start();
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// BluetoothSocketの作成
			try {
				// UUIDを指定してrfcommのソケットを作成
				tmp = device
						.createRfcommSocketToServiceRecord(SERIAL_PORT_PROFILE);
			} catch(IOException e) {
			}
			mmSocket = tmp;
		}

		public void run() {
			// 通信開始前にデバイスの探索を中止させる
			mAdapter.cancelDiscovery();

			try {
				// ソケットを利用して通信を開始する
				mmSocket.connect();
			} catch(IOException connectException) {
				// 例外が発生した場合はソケットを閉じ処理を抜ける
				try {
					mmSocket.close();
				} catch(IOException closeException) {
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			manageConnectedSocket(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch(IOException e) {
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// ソケットからInputStreamとOutputStreamを取得する
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch(IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			final byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// 例外が発生するまで受信処理を続ける
			while(true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);

					// Send the obtained bytes to the UI Activity
					Message msg = mHandler.obtainMessage(MESSAGE_READ, bytes,
							-1, buffer);
					if(!mHandler.sendMessage(msg)) {
						Log.e(TAG, "sendMessage Failed.");
					}

					/*
					 * mHandler.post(new Runnable() { public void run() {
					 * StringBuilder sb = new StringBuilder();
					 * sb.append(mTextView); sb.append("\r\n"); sb.append(new
					 * String(buffer)); mTextView.setText(sb.toString()); } });
					 */
				} catch(IOException e) {
					break;
				}
			}
		}

		/* Call this from the main Activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch(IOException e) {
			}
		}

		/* Call this from the main Activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch(IOException e) {
			}
		}
	}

	// 受信処理のHandler
	public class ReceiveHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "handleMessage");
			if(msg.what == MESSAGE_READ) {
				/*
				StringBuilder sb = new StringBuilder();
				
				sb.append(mTextView.getText());
				byte[] data = (byte[])msg.obj;
				sb.append(new String(data));
				mTextView.setText(sb.toString());
				*/
				String tmp = (String)mTextView.getText();
				tmp += new String((byte[])msg.obj);
				mTextView.setText(tmp);
			}
		}
	}
}
