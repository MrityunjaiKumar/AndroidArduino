package com.riis.androidarduino.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public abstract class SerialComm implements Communication, Runnable {
	protected boolean shouldLog;

	protected InputStream inputStream;
	protected OutputStream outputStream;
	
	protected Context context;
	protected Handler handler;
	
	protected boolean isConnected;
	
	public SerialComm(Activity parentActivity) {
		this.context = parentActivity.getApplicationContext();
		
		shouldLog = false;
		isConnected = false;
		
		setupHandler();
	}
	
	protected void setupHandler() {
		handler = new Handler() {
			public void handleMessage(Message msg) {
				ValueMsg t = (ValueMsg) msg.obj;
				log("Usb Accessory sent: " + t.getFlag() + " " + t.getReading());
			}
		};
	}
	
	public void sendString(String str) {
		byte[] messageBytes = Util.stringToByteArray(str);
		for(int i = 0; i < messageBytes.length; ++i) {
			sendByteWithFlag('S', messageBytes[i]);
		}
		//Send the null terminator to signify the end of the string.
		sendByteWithFlag('N', (byte) 0);
	}
	
	public void sendByteWithFlag(char flag, byte value) {
		log("Sending Byte '" + value + "' to Usb Accessory");
		
		sendByte((byte) flag);
		sendByte(value);
	}
	
	public void sendByte(byte value) {
		byte buffer[] = new byte[1];
		buffer[0] = value;
		
		if (outputStream != null) {
			try {
				outputStream.write(buffer);
			} catch (IOException e) {
				log("Send failed: " + e.getMessage());
			}
		}
		else {
			log("Send failed: outputStream was null");
		}
	}
	
	
	public void shouldPrintLogMsgs(boolean shouldLog) {
		this.shouldLog = shouldLog;
	}
	
	protected void checkAndHandleMessages(byte[] buffer) throws IOException {
		int msgLen = 0;
		msgLen = inputStream.read(buffer);

		for(int i = 0; i < msgLen; i += 2) {
			int len = msgLen - i;
			if (len >= 2) {
				Message m = Message.obtain(handler);
				int value = Util.composeInt(buffer[i], buffer[i + 1]);
				m.obj = new ValueMsg('a', value);
				handler.sendMessage(m);
			}
		}
	}
	
	public boolean isConnected() {
		return isConnected;
	}

	protected void log(String string) {
		if(shouldLog) {
			Log.v("UsbCommWrapper", string);
		}
	}
}
