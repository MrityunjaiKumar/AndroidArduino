package com.riis.androidarduino.can;
import java.util.ArrayList;
import java.util.Random;

import com.riis.androidarduino.lib.BluetoothComm;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static String DEVICE_NAME = "AndroidArduinoCANBT";
	private static final int MAX_ARRAY_SIZE = 1000;
	
	private Button btConnectButton;
	private Button startTrackingButton;
	private Button stopTrackingButton;
	private Button pauseTrackingButton;
	
	private TextView engineRunTime;
	private TextView airTemp;
	private TextView hybridBatteryPack;
	private TextView VIN;
	
	private ProgressBar oilTempBar;
	private TextView oilTempTxt;
	private ProgressBar coolantTempBar;
	private TextView coolantTempTxt;
	private ProgressBar throttlePosBar;
	private TextView throttlePosTxt;
	private ProgressBar absThrottleBBar;
	private TextView absThrottleBTxt;
	private ProgressBar absThrottleCBar;
	private TextView absThrottleCTxt;
	private ProgressBar absAccPosDBar;
	private TextView absAccPosDTxt;
	private ProgressBar absAccPosEBar;
	private TextView absAccPosETxt;
	private ProgressBar absAccPosFBar;
	private TextView absAccPosFTxt;
	
	private volatile boolean keepRunning;
	private boolean lastStatus;
	private Thread msgThread;
	
	private static Handler handler;
	private Context context;

	private BluetoothComm btComm;
	//private ArrayList<Float> readings;
	
	private Runnable msgUpdateThread = new Runnable() { 
		public void run() {
			while(keepRunning) {
				if(btComm.isConnected()) {
					if(!lastStatus) {
						lastStatus = true;
						changeConnectButtonText();
					}
					
					if(btComm.isMessageReady()) {
						String data = btComm.readMessage();
						parseCANData(data);
					}
		        	
		        } else {
		        	if(lastStatus) {
						lastStatus = false;
						changeConnectButtonText();
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
        
        context = this;
        //TODO Setup a set of arrays to track incoming data such as engine rpm, temp, vehicle speed, etc.
        //readings = new ArrayList<Float>(MAX_ARRAY_SIZE);
        
        setUpGUI();
    }
    
    private void setUpGUI() {
    	setUpConnectButton();
    	setUpStartTrackButton();
    	setUpPauseTrackButton();
    	setUpStopTrackButton();
    	setUpTempMonitors();
    	setUpThrottleMonitors();
    	setUpAcceleratorMonitors();
    	setUpBatteryMonitor();
    	setUpRuntimeMonitor();
    	setUpVIN();
    	setupHandler();
	}

	private void setUpConnectButton() {
    	btConnectButton = (Button)findViewById(R.id.connectButton);
    	btConnectButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				if(btComm.isConnected())
    					btComm.disconnect();
    				else
    					btComm.connect();
    			}
    		}
    	);
    }
	
	private void changeConnectButtonText() {
		if(btComm.isConnected())
			btConnectButton.setText("Disconnect");
		else
			btConnectButton.setText("Connect");
	}
    
    private void setUpStartTrackButton() {
    	startTrackingButton = (Button)findViewById(R.id.startTrackButton);
    	startTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				//TODO Start tracking data values
    			}
    		}
    	);
	}

	private void setUpPauseTrackButton() {
		pauseTrackingButton = (Button)findViewById(R.id.pauseTrackButton);
    	pauseTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				//TODO Pause tracking data values
    			}
    		}
    	);
	}

	private void setUpStopTrackButton() {
		stopTrackingButton = (Button)findViewById(R.id.stopTrackButton);
    	stopTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				//TODO Stop tracking data values
    			}
    		}
    	);
	}

	private void setUpTempMonitors() {
		oilTempBar = (ProgressBar)findViewById(R.id.oilTempBar);
		oilTempBar.setMax(255);
		oilTempBar.setProgress(40);
		oilTempTxt = (TextView)findViewById(R.id.oilTempTxt);
		oilTempTxt.append("0 C");
		
		coolantTempBar = (ProgressBar)findViewById(R.id.coolantTempBar);
		coolantTempBar.setMax(255);
		coolantTempBar.setProgress(40);
		coolantTempTxt = (TextView)findViewById(R.id.coolantTempTxt);
		coolantTempTxt.append("0 C");
		
		airTemp = (TextView)findViewById(R.id.airTemp);
		airTemp.append("0 C");
	}

	private void setUpThrottleMonitors() {
		throttlePosBar = (ProgressBar)findViewById(R.id.throttlePosBar);
		throttlePosBar.setMax(100);
		throttlePosTxt = (TextView)findViewById(R.id.throttlePosTxt);
		throttlePosTxt.append("0%");
		
		absThrottleBBar = (ProgressBar)findViewById(R.id.absoluteThrottleBBar);
		absThrottleBBar.setMax(100);
		absThrottleBTxt = (TextView)findViewById(R.id.absoluteThrottleBTxt);
		absThrottleBTxt.append("0%");
		
		absThrottleCBar = (ProgressBar)findViewById(R.id.absoluteThrottleCBar);
		absThrottleCBar.setMax(100);
		absThrottleCTxt = (TextView)findViewById(R.id.absoluteThrottleCTxt);
		absThrottleCTxt.append("0%");
	}

	private void setUpAcceleratorMonitors() {
		absAccPosDBar = (ProgressBar)findViewById(R.id.absAccPosDBar);
		absAccPosDBar.setMax(100);
		absAccPosDTxt = (TextView)findViewById(R.id.absAccPosDTxt);
		absAccPosDTxt.append("0%");
		
		absAccPosEBar = (ProgressBar)findViewById(R.id.absAccPosEBar);
		absAccPosEBar.setMax(100);
		absAccPosETxt = (TextView)findViewById(R.id.absAccPosETxt);
		absAccPosETxt.append("0%");
		
		absAccPosFBar = (ProgressBar)findViewById(R.id.absAccPosFBar);
		absAccPosFBar.setMax(100);
		absAccPosFTxt = (TextView)findViewById(R.id.absAccPosFTxt);
		absAccPosFTxt.append("0%");
	}

	private void setUpBatteryMonitor() {
		hybridBatteryPack = (TextView)findViewById(R.id.hybridBatteryPack);
	}
	
	private void setUpVIN() {
		VIN = (TextView)findViewById(R.id.VIN);
	}
	
	private void setUpRuntimeMonitor() {
		engineRunTime = (TextView)findViewById(R.id.engineRunTime);
		engineRunTime.append("0s");
	}
    
    private void setupHandler() {
		handler = new Handler() {
			public void handleMessage(Message msg) {
				String taggedMessage = (String) msg.obj;
				String[] tokens = taggedMessage.split("~");
				
				String message = tokens[1];
				if(tokens[0].equals("LOG")) {
					
				} else if(tokens[0].equals("DATA")) {
			    	if(tokens[1].charAt(0) == 0x02)
			    		setVIN(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x04)
			    		setEngineLoadVal(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x05)
			    		setEngineCoolantTemp(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x0C)
			    		setEngineRPM(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x0D)
			    		setVehicleSpeed(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x11)
			    		setThrottlePosition(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x1F)
			    		setEngineRunTime(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x2F)
			    		setFuelLevelInput(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x46)
			    		setAmbiantAirTemp(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x47)
			    		setAbsoluteThrottleB(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x48)
			    		setAbsoluteThrottleC(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x49)
			    		setAccPedalPosD(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x4A)
			    		setAccPedalPosE(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x4B)
			    		setAbsAccPedalPosF(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x5B)
			    		setHybridBatteryPackLife(tokens[1].substring(1));
			    	else if(tokens[1].charAt(0) == 0x5C)
			    		setEngineOilTemp(tokens[1].substring(1));
				}
			}
		};
	}
    
    private void setVIN(String vin) {
    	VIN.setText(R.string.VINPreface + vin);
	}

	private void setEngineLoadVal(String substring) {
		// TODO Auto-generated method stub
		
	}

	private void setEngineCoolantTemp(String coolantTempStr) {
    	int coolantTemp = Integer.parseInt(coolantTempStr);
		coolantTempTxt.setText(R.string.engineCoolantTempPreface + coolantTemp + " C");
		coolantTempBar.setProgress(coolantTemp+40);
	}

	private void setEngineRPM(String substring) {
		// TODO Auto-generated method stub
		
	}

	private void setVehicleSpeed(String substring) {
		// TODO Auto-generated method stub
		
	}

	private void setThrottlePosition(String substring) {
		// TODO Auto-generated method stub
		
	}

	private void setEngineRunTime(String timeStr) {
		engineRunTime.setText(R.string.engineRunTimePreface + timeStr + "s");
	}

	private void setFuelLevelInput(String substring) {
		// TODO Auto-generated method stub
		
	}

	private void setAmbiantAirTemp(String tempStr) {
		airTemp.setText(R.string.airTempPreface + tempStr + " C");
	}

	private void setAbsoluteThrottleB(String absThrottleBStr) {
		int absThrottleB = Integer.parseInt(absThrottleBStr);
		absThrottleBTxt.setText(R.string.absThrottleBPreface + absThrottleB + "%");
		absThrottleBBar.setProgress(absThrottleB);
	}

	private void setAbsoluteThrottleC(String absThrottleCStr) {
		int absThrottleC = Integer.parseInt(absThrottleCStr);
		absThrottleCTxt.setText(R.string.absThrottleCPreface + absThrottleC + "%");
		absThrottleCBar.setProgress(absThrottleC);
	}

	private void setAccPedalPosD(String absAccPosDStr) {
		int absAccPosD = Integer.parseInt(absAccPosDStr);
		absAccPosDTxt.setText(R.string.accPedalPosDPreface + absAccPosD + "%");
		absAccPosDBar.setProgress(absAccPosD);
	}

	private void setAccPedalPosE(String absAccPosEStr) {
		int absAccPosE = Integer.parseInt(absAccPosEStr);
		absAccPosETxt.setText(R.string.accPedalPosEPreface + absAccPosE + "%");
		absAccPosEBar.setProgress(absAccPosE);
	}

	private void setAbsAccPedalPosF(String absAccPosFStr) {
		int absAccPosF = Integer.parseInt(absAccPosFStr);
		absAccPosFTxt.setText(R.string.accPedalPosFPreface + absAccPosF + "%");
		absAccPosFBar.setProgress(absAccPosF);
	}

	private void setHybridBatteryPackLife(String batteryLifeStr) {
		int batteryLife = Integer.parseInt(batteryLifeStr);
		hybridBatteryPack.setText(R.string.hybridBatteryPackPreface + batteryLife + "%");
	}

	private void setEngineOilTemp(String oilTempStr) {
    	int oilTemp = Integer.parseInt(oilTempStr);
		oilTempTxt.setText(R.string.engineOilTempPreface + oilTemp + " C");
		oilTempBar.setProgress(oilTemp+40);
    }
    
    private void parseCANData(String data) {
    	Message msg = Message.obtain(handler);
		msg.obj = "DATA~" + data;
		handler.sendMessage(msg);
    }
    
    @Override
	public void onResume() {
		super.onResume();
		
		if(btComm == null) {
			btComm = new BluetoothComm(this, DEVICE_NAME);
		} else {
			btComm.resumeConnection();
		}
		
		btComm.shouldPrintLogMsgs(true);
		msgThread = new Thread(msgUpdateThread);
		msgThread.start();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		keepRunning = false;
		btComm.pauseConnection();
	}
    
    public BluetoothComm getBlueToothComm() {
    	return btComm;
    }
}