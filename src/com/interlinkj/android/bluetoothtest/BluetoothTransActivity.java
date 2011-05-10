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
import android.widget.TextView;

public class BluetoothTransActivity extends Activity {
	private static final int BUFFER_SIZE = 512;
    private static final int MESSAGE_READ = 0;
	private static final UUID SERIAL_PORT_PROFILE = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private BluetoothAdapter mAdapter;
	private BluetoothSocket mSocket;
	private InputStream mInStream;
	private OutputStream mOutStream;
	private AtomicBoolean mClosed = new AtomicBoolean();
	private String mLogText = null;
	private Handler mHandler;
	private TextView mTextView;
	private ConnectedThread mConnectedThread;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.trans);
		
		Intent i = getIntent();
		String address = i.getStringExtra("MACAddress");
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothDevice device = mAdapter.getRemoteDevice(address);
		
		mTextView = (TextView)findViewById(R.id.textView1);
		
		ConnectThread connectThread = new ConnectThread(device);
		connectThread.run();
	
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == MESSAGE_READ) {
					StringBuilder sb = new StringBuilder();
					sb.append(mTextView.getText());
					sb.append("\r\n");
					byte[] data = (byte[])msg.obj;
					sb.append(data);
					mTextView.setText(sb.toString());
				}
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

		TextView textView = (TextView)findViewById(R.id.textView1);
		textView.setText(receive());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			mSocket.close();
		} catch(IOException e) {
			
		}
	}

	public String receive() {
		int count = 0;
		byte[] buffer = new byte[BUFFER_SIZE];

		try {
			count = mInStream.read(buffer);
		} catch(IOException e) {

		}
		String str = new String(buffer, 0, count);

		return str;
	}

	public void manageConnectedSocket(BluetoothSocket mmSocket) {
		mSocket = mmSocket;
		mConnectedThread = new ConnectedThread(mSocket);
		mConnectedThread.run();
	}
	
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;

	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;

	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(SERIAL_PORT_PROFILE);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }

	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        mAdapter.cancelDiscovery();

	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }

	        // Do work to manage the connection (in a separate thread)
	        manageConnectedSocket(mmSocket);
	    }

	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
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

	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }

	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }

	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI Activity
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
	                        .sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }

	    /* Call this from the main Activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }

	    /* Call this from the main Activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
}
