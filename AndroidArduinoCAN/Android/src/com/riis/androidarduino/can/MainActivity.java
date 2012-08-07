/*
 * 	
 * Things to fix
 * 		temperatures
 * 		make it not do anything until the bluetooth
 * 
 */

package com.riis.androidarduino.can;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.riis.androidarduino.lib.BluetoothComm;

public class MainActivity extends Activity {
	private static String DEVICE_NAME = "AndroidArduinoCANBT";
	private static final int MAX_ARRAY_SIZE = 1000;
	
	private Button btConnectButton;
	private Button startTrackingButton;
	private Button viewTripDataButton;
	private Button pauseTrackingButton;
	private boolean enableTracking;
	
	private double timeSinceTrackStart;
	private ArrayList<Double> avgRPM;
	private ArrayList<Double> avgSpeed;
	
	private GuageView guages;
	
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
	private ProgressBar fuelLevelBar;
	private TextView fuelLevelTxt;
	
	private volatile boolean keepRunning;
	private String lastStatus;
	private Thread msgThread;
	
	private static Handler handler;
	private Context context;

	private BluetoothComm btComm;
	//private ArrayList<double> readings;
	
	private Runnable msgUpdateThread = new Runnable() { 
		public void run() {
			while(keepRunning) {
				if(btComm.isConnected()) {
					if(btComm.isMessageReady()) {
						String data = btComm.readMessage();
						parseCANData(data);
					}
		        }
				else {
					if(btConnectButton.getText().equals("Disconnect")) {
						Message msg = Message.obtain(handler);
						msg.obj = getString(R.id.connectButton) + "~Connect";
						handler.sendMessage(msg);
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
        
        context = this;
        avgRPM = new ArrayList<Double>(MAX_ARRAY_SIZE);
        avgSpeed = new ArrayList<Double>(MAX_ARRAY_SIZE);
        enableTracking = false;
        
        setUpGUI();
    }
    
    private void setUpGUI() {
    	setUpConnectButton();
    	setUpStartTrackButton();
    	setUpPauseTrackButton();
    	setUpViewTripDataButton();
    	setUpTempMonitors();
    	setUpThrottleMonitors();
    	setUpBatteryMonitor();
    	setUpRuntimeMonitor();
    	setUpVIN();
    	setUpGuages();
    	setUpFuelLevelMonitor();
    	setupHandler();
    }

	private void setUpConnectButton() {
    	btConnectButton = (Button)findViewById(R.id.connectButton);
    	btConnectButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				if(btComm.isConnected())
						try {
							btComm.disconnect();
							btConnectButton.setText("Connect");
						} catch (IOException e) {
							Toast.makeText(context, "Could not disconnect from device!", Toast.LENGTH_LONG).show();
						}
					else
						try {
							btComm.connect();
							btConnectButton.setText("Disconnect");
						} catch (IOException e) {
							Toast.makeText(context, "Could not connect to device!", Toast.LENGTH_LONG).show();
						}
    			}
    		}
    	);
    }
    
    private void setUpStartTrackButton() {
    	startTrackingButton = (Button)findViewById(R.id.startTrackButton);
    	startTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				if(startTrackingButton.getText().toString().toLowerCase().contains("start")) {
	    				startTrackingButton.setText("Stop Tracking");
	    				enableTracking = true;
    				}
    				else {
    					startTrackingButton.setText("Start Tracking");
	    				enableTracking = false;
	    				avgRPM.clear();
	    				avgSpeed.clear();
    				}
    			}
    		}
    	);
	}

	private void setUpPauseTrackButton() {
		pauseTrackingButton = (Button)findViewById(R.id.pauseTrackButton);
    	pauseTrackingButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
    				enableTracking = false;
    			}
    		}
    	);
	}

	private void setUpViewTripDataButton() {
		viewTripDataButton = (Button)findViewById(R.id.viewTripDataButton);
    	viewTripDataButton.setOnClickListener(
    		new OnClickListener() {
    			public void onClick(View v) {
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
	
	private void setUpGuages() {
		guages = (GuageView) findViewById(R.id.guages);
	}
	
	private void setUpFuelLevelMonitor() {
		fuelLevelBar = (ProgressBar)findViewById(R.id.fuelLevelBar);
		fuelLevelBar.setMax(100);
		fuelLevelTxt = (TextView)findViewById(R.id.fuelLevelTxt);
		fuelLevelTxt.append("0%");
	}
    
    private void setupHandler() {
		handler = new Handler() {
			public void handleMessage(Message msg) {
				String taggedMessage = (String) msg.obj;
				String[] tokens = taggedMessage.split("~");
				
				if(tokens.length < 2)
					return;
				
				String message = tokens[1];
				if(tokens[0].equals(getString(R.id.connectButton))) {
					btConnectButton.setText(message);
				}
				if(tokens.length == 4 && tokens[0].equals("DATA")) {
					int pid;
					int dataA;
					int dataB;
					
					try {
						pid = Integer.parseInt(tokens[1], 16);
						dataA = Integer.parseInt(tokens[2], 16);
						dataB = Integer.parseInt(tokens[3], 16);
					} catch(NumberFormatException e) {
						return;
					}
						
			    	if(pid == 0x02)
			    		setVIN(dataA, dataB);
			    	else if(pid == 0x05)
			    		setEngineCoolantTemp(dataA, dataB);
			    	else if(pid == 0x0C)
			    		setEngineRPM(dataA, dataB);
			    	else if(pid == 0x0D)
			    		setVehicleSpeed(dataA, dataB);
			    	else if(pid == 0x11)
			    		setThrottlePosition(dataA, dataB);
			    	else if(pid == 0x1F)
			    		setEngineRunTime(dataA, dataB);
			    	else if(pid == 0x2F)
			    		setFuelLevel(dataA, dataB);
			    	else if(pid == 0x46)
			    		setAmbiantAirTemp(dataA, dataB);
			    	else if(pid == 0x47)
			    		setAbsoluteThrottleB(dataA, dataB);
			    	else if(pid == 0x5B)
			    		setHybridBatteryPackLife(dataA, dataB);
			    	else if(pid == 0x5C)
			    		setEngineOilTemp(dataA, dataB);
				}
			}
		};
	}
    
    private void setVIN(int dataA, int dataB) {
    	String vin = "";
    	VIN.setText(getString(R.string.VINPreface) + vin);
	}

	private void setEngineCoolantTemp(int dataA, int dataB) {
    	double coolantTemp = dataA - 40.0;
		coolantTempTxt.setText(getString(R.string.engineCoolantTempPreface) + coolantTemp + " C");
		coolantTempBar.setProgress((int)coolantTemp+40);
	}

	private void setEngineRPM(int dataA, int dataB) {
		double rpm = ((dataA * 256) + dataB) / 4;
		
		if(enableTracking) {
			if(avgRPM.size() == MAX_ARRAY_SIZE)
				avgRPM.remove(0);
			avgRPM.add(rpm);
		}
		
		double newAvg = getAvg(avgRPM);
		
		guages.setTach((float) rpm);
		
		// calculate angle from rpm
		// -turn into a %- rpm/maxVal
		// zeroAngle + (rpm*maxValAngle)
		// if(result < 0)
		//    result = 360+result;
		// calculate angle from newAvg
	}

	private void setVehicleSpeed(int dataA, int dataB) {
		double speed = dataA * 0.621371;
		
		if(enableTracking) {
			if(avgSpeed.size() == MAX_ARRAY_SIZE)
				avgSpeed.remove(0);
			avgSpeed.add(speed);
		}
		
		double newAvg = getAvg(avgSpeed);
		
		guages.setSpeed((float) speed);
		
		// calculate angle from speed
		// calculate angle from newAvg
	}

	private double getAvg(ArrayList<Double> dataList) {
		double avg = 0;
		for(double datum : dataList) {
			avg += datum;
		}
		avg /= dataList.size();
		return avg;
	}

	private void setThrottlePosition(int dataA, int dataB) {
		double throttlePos = (dataA / 256.0) * 100.0;
		throttlePosTxt.setText(getString(R.string.throttlePosPreface) + throttlePos + "%");
		throttlePosBar.setProgress((int)throttlePos);
	}

	private void setEngineRunTime(int dataA, int dataB) {
		double time = (dataA * 256.0) + dataB;
		engineRunTime.setText(getString(R.string.engineRunTimePreface) + time + "s");
	}

	private void setFuelLevel(int dataA, int dataB) {
		double fuelLevel = (dataA / 256.0) * 100.0;
		fuelLevelBar.setProgress((int) fuelLevel);
		fuelLevelTxt.setText(getString(R.string.fuelLevelPreface) + fuelLevel + "%");		
	}

	private void setAmbiantAirTemp(int dataA, int dataB) {
		double temp = dataA - 40.0;
		airTemp.setText(getString(R.string.airTempPreface) + temp + " C");
	}

	private void setAbsoluteThrottleB(int dataA, int dataB) {
		double absThrottleB = (dataA / 256.0) * 100.0;
		absThrottleBTxt.setText(getString(R.string.absThrottleBPreface) + absThrottleB + "%");
		absThrottleBBar.setProgress((int)absThrottleB);
	}

	private void setHybridBatteryPackLife(int dataA, int dataB) {
		double batteryLife = (dataA / 256.0) * 100.0;
		hybridBatteryPack.setText(getString(R.string.hybridBatteryPackPreface) + batteryLife + "%");
	}

	private void setEngineOilTemp(int dataA, int dataB) {
    	double oilTemp = dataA - 40.0;
		oilTempTxt.setText(getString(R.string.engineOilTempPreface) + oilTemp + " C");
		oilTempBar.setProgress((int)oilTemp+40);
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
			btComm = new BluetoothComm(DEVICE_NAME);
			try {
				btComm.connect();
				btConnectButton.setText("Disconnect");
			} catch (IOException e) {
				Toast.makeText(context, "Could not connect to device!", Toast.LENGTH_LONG).show();
			}
		} else {
			try {
				btComm.resumeConnection();
				btConnectButton.setText("Disconnect");
			} catch (IOException e) {
				Toast.makeText(context, "Could not reconnect to device!", Toast.LENGTH_LONG).show();
			}
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
			btConnectButton.setText("Connect");
		} catch (IOException e) {
			Toast.makeText(context, "Could not pause device connection!", Toast.LENGTH_LONG).show();
		}
	}
    
    public BluetoothComm getBlueToothComm() {
    	return btComm;
    }
}