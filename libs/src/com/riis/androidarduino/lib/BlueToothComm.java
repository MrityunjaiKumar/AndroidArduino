package com.riis.androidarduino.lib;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.widget.Toast;

public class BlueToothComm extends SerialComm implements Runnable {
	private BluetoothAdapter adapter;
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private String deviceName;
	
	public BlueToothComm(Activity parentActivity, String deviceName) {
		super(parentActivity);
		this.deviceName = deviceName;
		connect();
	}
	
	@Override
	public void connect() {
		findDevice(deviceName);
		connectSocket();
		
		Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();
	}
	
	private void findDevice(String deviceName) {
		boolean gotDevice = false;
    	
    	adapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

    	if (pairedDevices.size() > 0) {
		    for (BluetoothDevice searchDevice : pairedDevices) {
		    	if(searchDevice.getName().equals(deviceName)) {
		    		device = searchDevice;
		    		gotDevice = true;
		    		break;
		    	}
		    }
		    
		    if(gotDevice) {
		    	Toast.makeText(context, "Found device " + deviceName, Toast.LENGTH_SHORT).show();
		    } else {
	    		Toast.makeText(context, "Couldn't find device, is it paired?", Toast.LENGTH_SHORT).show();
		    }
		}
	}
	
	private void connectSocket() {
		try {
			socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));			
			socket.connect();
		
			inputStream = socket.getInputStream();														
			outputStream = socket.getOutputStream();		
		} catch (IOException e) {
			Toast.makeText(context, "Couldn't connect to device", Toast.LENGTH_SHORT).show();
			return ;
		}
	}
	
	public void disconnect() {
		try {
			socket.close();
			inputStream.close();
			outputStream.close();
		} catch (IOException e) { }
		
		Toast.makeText(context, "Disconnected!", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void run() {
		int msgLen = 0;
		byte[] buffer = new byte[256];
		int i;

		while (true) { // keep reading messages forever.
			try {
				msgLen = inputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < msgLen) {
				int len = msgLen - i;
				if (len >= 2) {
					Message m = Message.obtain(handler);
					int value = Util.composeInt(buffer[i], buffer[i + 1]);
					m.obj = new ValueMsg('a', value);
					handler.sendMessage(m);
				}
				i += 2;
			}
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {	}
		}
	}

	@Override
	public void pauseConnection() {
		disconnect();
		
	}

	@Override
	public void resumeConnection() {
		connect();
	}
}
