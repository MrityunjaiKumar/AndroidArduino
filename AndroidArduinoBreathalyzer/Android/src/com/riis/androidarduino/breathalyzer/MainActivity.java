package com.riis.androidarduino.breathalyzer;

import java.io.IOException;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.riis.androidarduino.lib.BluetoothComm;

public class MainActivity extends Activity {
	private static String DEVICE_NAME = "AndroidArduinoBTRS232";
	private static final int MAX_ARRAY_SIZE = 1000;
	
	private Button connectButton;
	private Button disconnectButton;
	
	private ScrollView logScrollContainer;
	private TextView msgLog;
	
	private volatile boolean keepRunning;
	private boolean lastStatus;
	private Thread msgThread;
	
	private static Handler handler;

	private BluetoothComm btComm;
	private ArrayList<Float> readings;
	
	private Runnable msgUpdateThread = new Runnable() { 
		public void run() {
			while(keepRunning) {
				if(btComm.isConnected()) {
					if(!lastStatus) {
						lastStatus = true;
						appendMsgToMsgLog("Bluetooth Connected!");
					}
					
					if(btComm.isMessageReady()) {
						String data = btComm.readMessage();
						appendMsgToMsgLog("Sensor reading: " + data);
						addDataToGraph(data);
					}
		        	
		        } else {
		        	if(lastStatus) {
						lastStatus = false;
						appendMsgToMsgLog("Bluetooth Disconnected!");
						appendMsgToMsgLog("Waiting for Bluetooth connection...");
					}
		        }
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        keepRunning = true;
        lastStatus = false;
        
        readings = new ArrayList<Float>(MAX_ARRAY_SIZE);
        
        setUpGUI();
        
		appendMsgToMsgLog("Waiting for Bluetooth connection...");
    }
    
    private void setUpGUI() {
    	setUpConnectButton();
    	setUpDisconnectButton();
    	setupHandler();
    	setupMsgLog();
    	setUpGraph();
	}
    
    private void setUpConnectButton() {
    	connectButton = (Button)findViewById(R.id.connectButton);
    	connectButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				try {
						btComm.connect();
					} catch (IOException e) {
						Toast.makeText(MainActivity.this, "Couldn't connect!", Toast.LENGTH_SHORT).show();
					}
    			}
    		}
    	);
    }
    
    private void setUpDisconnectButton() {
    	disconnectButton = (Button)findViewById(R.id.disconnectButton);
    	disconnectButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				try {
						btComm.disconnect();
					} catch (IOException e) {
						Toast.makeText(MainActivity.this, "Couldn't disconnect!", Toast.LENGTH_SHORT).show();
					}
    			}
    		}
    	);
    }
    
    @SuppressLint("HandlerLeak")
	private void setupHandler() {
		handler = new Handler() {
			public void handleMessage(Message msg) {
				String taggedMessage = (String) msg.obj;
				String[] tokens = taggedMessage.split("~");
				
				String message = tokens[1];
				if(tokens[0].equals("LOG")) {
					msgLog.append(message + "\n");
			    	logScrollContainer.fullScroll(View.FOCUS_DOWN);
				} else if(tokens[0].equals("DATA")) {
					float reading = Float.parseFloat(message);
					if(readings.size() == MAX_ARRAY_SIZE)
						readings.remove(0);
					readings.add(reading);
					
					if(readings.size() % 10 == 0) {
						((LinearLayout)findViewById(R.id.graphContainer)).removeAllViews();
						setUpGraph();
					}
				}
			}
		};
	}
    
    private void setupMsgLog() {
    	logScrollContainer = (ScrollView)findViewById(R.id.scrollView);
    	msgLog = (TextView) findViewById(R.id.messageLog);
    	msgLog.append("Android Service Init...\n");
    	msgLog.setMovementMethod(new ScrollingMovementMethod());
    }
    
    private void setUpGraph() {
		String[] verlabels = new String[] { "Dead", "Wasted", "Drunk", "Tipsy", "Sober" };
		String[] horlabels = new String[] { "             Time ------------------->" };
		BreathalyzerView graphView = new BreathalyzerView(this, readings, "Breathalyzer Readings", horlabels, verlabels, BreathalyzerView.LINE);
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		graphView.setLayoutParams(params);
		
		((LinearLayout)findViewById(R.id.graphContainer)).addView(graphView);
    }
    
    private void appendMsgToMsgLog(String str) {
		Message msg = Message.obtain(handler);
		msg.obj = "LOG~" + str;
		handler.sendMessage(msg);
    }
    
    private void addDataToGraph(String data) {
    	Message msg = Message.obtain(handler);
		msg.obj = "DATA~" + data;
		handler.sendMessage(msg);
    }
    
    @Override
	public void onResume() {
		super.onResume();
		
		try {
			if(btComm == null) {
				btComm = new BluetoothComm(DEVICE_NAME);
			} else {
				btComm.resumeConnection();
			}
		} catch(IOException e) {
			Toast.makeText(MainActivity.this, "Couldn't connect!", Toast.LENGTH_SHORT).show();
		}
		
		btComm.shouldPrintLogMsgs(true);
		msgThread = new Thread(msgUpdateThread);
		msgThread.start();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		keepRunning = false;
		
		try {
			btComm.pauseConnection();
		} catch (IOException e) {
			Toast.makeText(MainActivity.this, "Couldn't disconnect!", Toast.LENGTH_SHORT).show();
		}
	}
}