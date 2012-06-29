package com.riis.androidarduino.lib;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelFileDescriptor;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class UsbComm extends SerialComm {
	public static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	
	private BroadcastReceiver usbBroadcastReceiver;
	private UsbManager manager;
	private UsbAccessory accessory;
	private ParcelFileDescriptor fileDescriptor;
	private PendingIntent permissionIntent;
	private boolean permissionRequestPending;
	
	
	public UsbComm(Activity parentActivity) {
		super(parentActivity);
		setupBroadcastReceiver();
		accessory = (UsbAccessory) parentActivity.getLastNonConfigurationInstance();
		connect();
	}
	
	private void setupBroadcastReceiver() {
		usbBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (ACTION_USB_PERMISSION.equals(action)) {
					synchronized (this) {
						UsbAccessory accessory = UsbManager.getAccessory(intent);
						if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
							openAccessory(accessory);
						} else {
							log("USB permission denied");
						}
					}
				} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (accessory != null && accessory.equals(accessory)) {
						log("USB Detached");
						disconnect();
					}
				}
			}
		};
	}
	
	@Override
	public void connect() {
		manager = UsbManager.getInstance(context);
		permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		registerReceiver();

		if (accessory != null) {
			openAccessory(accessory);
		}
	}
	
	private void registerReceiver() {
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		context.registerReceiver(usbBroadcastReceiver, filter);
	}
	
	private void openAccessory(UsbAccessory accessory) {
		fileDescriptor = manager.openAccessory(accessory);
		if (fileDescriptor != null) {
			this.accessory = accessory;
			FileDescriptor fd = fileDescriptor.getFileDescriptor();
			inputStream = new FileInputStream(fd);
			outputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "UsbCommWrapperLoop");
			thread.start();
			log("Attached");
		} else {
			log("openAccessory: accessory open failed");
		}
	}
	
	@Override
	public void disconnect() {
		try {
			if (fileDescriptor != null) {
				fileDescriptor.close();
				log("Dettached");
			}
		} catch (IOException e) {
		} finally {
			fileDescriptor = null;
			accessory = null;
		}
	}
	
	public void pauseConnection() {
		unregisterReceiver();
	}
	
	public void resumeConnection() {
		registerReceiver();
		
		if (inputStream != null && outputStream != null) {
			log("Resuming: streams were not null");
			return;
		}
		log("Resuming: streams were null");
		UsbAccessory[] accessories = manager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (manager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (usbBroadcastReceiver) {
					if (!permissionRequestPending) {
						manager.requestPermission(accessory, permissionIntent);
						permissionRequestPending = true;
					}
				}
			}
		} else {
			log("onResume: accessory is null");
		}
	}
	
	public void run() {
		byte[] buffer = new byte[256];

		while (true) { // keep reading messages forever.
			try {
				checkAndHandleMessages(buffer);
			} catch (IOException e) {
				break;
			}
		}
	}
	
	
	
	public void unregisterReceiver() {
		try {
			context.unregisterReceiver(usbBroadcastReceiver);
		} catch(IllegalArgumentException e) {
			//Do nothing and keep the exception from surfacing, because
			//the receiver was already unregistered
		}
	}
	
	public UsbAccessory getAccessory() {
		return accessory;
	}
}
