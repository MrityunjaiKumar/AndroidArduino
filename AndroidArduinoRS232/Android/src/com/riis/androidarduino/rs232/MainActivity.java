package com.riis.androidarduino.rs232;

import com.riis.androidarduino.lib.UsbComm;
import com.riis.androidarduino.rs232.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private EditText msgBox;
	private Button sendMsgButton;
	
	UsbComm usbComm;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        usbComm = new UsbComm(this);
        
        setUpGUI();
    }
    
    private void setUpGUI() {
    	setupMsgBox();
    	setupSendMsgButton();
	}
    
    private void setupMsgBox() {
    	msgBox = (EditText) findViewById(R.id.messageBox);
    }
    
    private void setupSendMsgButton() {
    	sendMsgButton = (Button) findViewById(R.id.sendButton);
    	sendMsgButton.setOnClickListener(
    	    		new OnClickListener(){
    					public void onClick(View v) {
    						usbComm.sendString(msgBox.getText().toString());
    						msgBox.setText("");
    					}
    	    		}
    	    	);
    }
    
	@Override
	public Object onRetainNonConfigurationInstance() {
		// In case the app is restarted, try to retain the usb accessory object
		// so that the connection to the device is not lost.
		if (usbComm.getAccessory() != null) {
			return usbComm.getAccessory();
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}
	
	@Override
	public void onResume() {
		Log.v("Arduino App", "Resuming");
		super.onResume();
		usbComm.resumeConnection();
	}

	@Override
	public void onPause() {
		Log.v("Arduino App", "Pausing");
		super.onPause();
		usbComm.pauseConnection();
	}

	@Override
	public void onDestroy() {
		Log.v("Arduino App", "Destroying");
		usbComm.unregisterReceiver();
		super.onDestroy();
	}
}