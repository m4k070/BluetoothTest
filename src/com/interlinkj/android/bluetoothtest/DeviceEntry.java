package com.interlinkj.android.bluetoothtest;

import android.bluetooth.BluetoothDevice;

public class DeviceEntry {
	
	public DeviceEntry(BluetoothDevice device) {
		mName = device.getName();
		mMAC = device.getAddress();
	}
	
	public DeviceEntry(String title, String content) {
		mName = title;
		mMAC = content;
	}
	
	public String getName() {
		return mName;
	}
	protected void setName(String title) {
		mName = title;
	}
	
	public String getMAC() {
		return mMAC;
	}
	protected void setMAC(String uri) {
		mMAC = uri;
	}
		
	private String mName;
	private String mMAC;
}
