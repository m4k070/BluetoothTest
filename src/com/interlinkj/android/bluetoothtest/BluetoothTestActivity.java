package com.interlinkj.android.bluetoothtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.*;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class BluetoothTestActivity extends ListActivity implements OnClickListener {
	private final int REQUEST_ENABLE_BLUETOOTH = 1;
	public static final String TAG = "BluetoothTest";

	private BluetoothAdapter mBluetooth;
	private List<BluetoothDevice> mBondedDevices; // ペアリング済みデバイスリスト
	private List<BluetoothDevice> mDevices; // 新規発見デバイスリスト
	private ListRowAdapter mListAdapter;
	private List<DeviceEntry> mEntries;
	private DiscoveryReceiver mDiscoveryListener;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		if(!ensureBluetooth()) {
			showAlert("");
		} else {
			if(!ensureEnabled()) {
				showAlert("");
			}
		}
		
		if(null == mBluetooth) {
			finish();
		}

		Button button = (Button)findViewById(R.id.button01);
		button.setOnClickListener(this);

		mEntries = new ArrayList<DeviceEntry>();
		mListAdapter = new ListRowAdapter(getApplicationContext(),
				R.layout.list_row, mEntries);
		setListAdapter(mListAdapter);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mDiscoveryListener);
	}

	@Override
	public void onResume() {
		super.onResume();
		mDiscoveryListener = new DiscoveryReceiver();
//		mDiscoveryListener.registerFor(getApplicationContext());
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mDiscoveryListener, filter);
	}

	private boolean ensureBluetooth() {
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
		if(null == ba) {
			return false;
		}

		mBluetooth = ba;
		return true;
	}

	private boolean ensureEnabled() {
		if(!mBluetooth.isEnabled()) {
			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(i, REQUEST_ENABLE_BLUETOOTH);
			return false;
		}
		return true;
	}

	private void showAlert(String str) {

	}

	private void onBluetoothEnabled() {

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case REQUEST_ENABLE_BLUETOOTH:
			if(resultCode == Activity.RESULT_OK) {
				onBluetoothEnabled();
				return;
			}
			showAlert("bluetooth is not enabled");
			break;
		}
	}

	public void onClick(View view) {
		// ペアリング済みデバイスの取得
		mBondedDevices = new ArrayList<BluetoothDevice>();
		Set<BluetoothDevice> devices = mBluetooth.getBondedDevices();
		for(BluetoothDevice device : devices) {
			mBondedDevices.add(device);
		}

		// デバイス検索の開始
		startDiscovery();
	}

	private void startDiscovery() {
		if(!mBluetooth.isDiscovering()) {
			mBluetooth.cancelDiscovery();
		}
		mDevices = new ArrayList<BluetoothDevice>();
		mBluetooth.startDiscovery();
	}

	public void setSearchResult() {
		List<DeviceEntry> entries = new ArrayList<DeviceEntry>();
		for(BluetoothDevice device : mBondedDevices) {
			entries.add(new DeviceEntry(device));
		}
		for(BluetoothDevice device : mDevices) {
			entries.add(new DeviceEntry(device));
		}
		mEntries = entries;

		mListAdapter.clear();
		for(DeviceEntry entry : mEntries) {
			mListAdapter.add(entry);
		}
	}

	private boolean isDeviceBonded(BluetoothDevice device) {
		boolean isBonded = mBondedDevices.contains(device);
		return isBonded;
	}

	private boolean isNameRetrieved(BluetoothDevice device) {
		boolean resolved = device.getName() != null;
		return resolved;
	}

	private final class DiscoveryReceiver extends BroadcastReceiver {
		public void registerFor(Context context) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothDevice.ACTION_FOUND);
			filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			context.registerReceiver(this, filter);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if(action.equals(BluetoothDevice.ACTION_FOUND)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(isDeviceBonded(device)) {
					return;
				}
				if(!isNameRetrieved(device)) {
					return;
				}
				mDevices.add(device);
				return;
			}
			if(action.equals(BluetoothDevice.ACTION_NAME_CHANGED)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(isDeviceBonded(device)) {
					return;
				}
				mDevices.add(device);
				return;
			}
			if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
				setProgressBarIndeterminateVisibility(true);
				return;
			}
			if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				setSearchResult();

				setProgressBarIndeterminateVisibility(false);
				return;
			}
		}
	}
	
	/**
	 * ListView用Adapter
	 * 
	 * @author Ito
	 * 
	 */
	public class ListRowAdapter extends ArrayAdapter<DeviceEntry> {

		LayoutInflater mInflater;
		List<DeviceEntry> items;
		private View mSourceView;

		public ListRowAdapter(Context context, int textViewResourceId,
				List<DeviceEntry> objects) {
			super(context, textViewResourceId, objects);
			mInflater = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			items = objects;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup parent) {
			View v = convertView;
			if(v == null) {
				v = mInflater.inflate(R.layout.list_row, null);
			}

			DeviceEntry row = (DeviceEntry)items.get(pos);

			TextView nameView = (TextView)v.findViewById(R.id.deviceName);
			nameView.setText(row.getName());
//			nameView.setOnTouchListener(mTouchListener);

			TextView addrView = (TextView)v.findViewById(R.id.MAC);
			addrView.setText(row.getMAC());
			addrView.setOnTouchListener(mTouchListener);
			
			return v;
		}

		private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					Log.v(TAG, "ACTION_DOWN");
					break;
				case MotionEvent.ACTION_UP:
					Log.v(TAG, "ACTION_UP");
					// インテント発行
					
					Intent i = new Intent();
					i.setClassName("com.interlinkj.android.bluetoothtest",
							"com.interlinkj.android.bluetoothtest.BluetoothTransActivity");
					i.putExtra("MACAddress", ((TextView)v).getText());
					startActivityIfNeeded(i, -1);
					
					
					break;
				case MotionEvent.ACTION_MOVE:
					Log.v(TAG, "ACTION_MOVE");
					break;
				case MotionEvent.ACTION_CANCEL:
					Log.v(TAG, "ACTION_CANCEL");
					break;
				default:
					Log.v(TAG, "unexcepted action");
					break;
				}
				return true;
			}
		};
	}
}