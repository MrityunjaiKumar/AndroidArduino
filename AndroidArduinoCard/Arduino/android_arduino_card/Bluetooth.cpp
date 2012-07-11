#include "Bluetooth.h"

Bluetooth::Bluetooth(int RX, int TX, String deviceName, boolean shouldPrintLog)
    : bluetoothSerial(RX, TX, false)
{
    this->shouldPrintLog = shouldPrintLog;
    this->deviceName = deviceName;
    
    connectionState = '0';
    allowReading = false;

    pinMode(RX, INPUT);
    pinMode(TX, OUTPUT);
}

Bluetooth::~Bluetooth() { }

boolean Bluetooth::beginBluetooth()
{
    logMsg("Powering up the Bluetooth device...");
    if(sendSetupCommands())
    {
        logMsg("Bluetooth ready! Connect to " + deviceName + ".");
	return true;
    } else {
	logMsg("Bluetooth setup failed!");
        logMsg("Make sure it's connected properly and reset the Arduino board.");
	return false;
    }	
}

boolean Bluetooth::sendSetupCommands()
{
    bluetoothSerial.begin(38400);

    if(!sendCommand("\r\n+STWMOD=0\r\n")) {return false;}           //Set the Bluetooth to slave mode
    if(!sendCommand("\r\n+STNA=" + deviceName + "\r\n")) {return false;} //Set the Bluetooth device name
    if(!sendCommand("\r\n+STOAUT=1\r\n")) {return false;}           //Permit a paired device to connect
    if(!sendCommand("\r\n+STAUTO=0\r\n")) {return false;}           //No auto connection
	
    delay(2000);    
    if(!sendCommand("\r\n+INQ=1\r\n")) {return false;}              //Make the Bluetooth device inquirable 
	
    delay(2000);
    bluetoothSerial.flush();

    return true;
}

boolean Bluetooth::sendCommand(String command)
{
    bluetoothSerial.print(command);
    return waitForCommandOK();
}

boolean Bluetooth::waitForCommandOK()
{
    unsigned long startTime = millis();
    char msgChar = '\0';
    
    while(msgChar != 'O')
    {
        if(bluetoothSerial.available())
        {
            msgChar = bluetoothSerial.read();
        }
        
        if(millis() > startTime + 5000)
        {
            return false; //Timed out, return that the command failed
        }
    }
    
    while(msgChar != 'K')
    {
        if(bluetoothSerial.available())
        {
            msgChar = bluetoothSerial.read();
        }
        
        if(millis() > startTime + 5000)
        {
            return false; //Timed out, return that the command failed
        }
    }
 
    return true;
}

void Bluetooth::sendByte(byte value)
{
    bluetoothSerial.write(value);
}

void Bluetooth::sendStringRaw(String message)
{
    for(int i = 0; i < message.length(); i++)
    {
        sendByte(message[i]);
    }   
}

void Bluetooth::sendByteWithFlag(char flag, byte value)
{
    bluetoothSerial.write(flag);
    bluetoothSerial.write(value);
}

void Bluetooth::sendStringWithFlags(String message)
{
    for(int i = 0; i < message.length(); i++)
    {
        sendByteWithFlag('S', message[i]);
    }
    
    sendByteWithFlag('N', 0);
}

int Bluetooth::bytesAvailable()
{
    if(allowReading)
    {
        return inputBuffer.length();
    }
    else
    {
        return 0;   
    }
}

byte Bluetooth::readByte()
{
    if(allowReading)
    {
        byte toRead = (byte)inputBuffer[0];
        inputBuffer = inputBuffer.substring(1);
        return toRead;
    }
    else
    {
        return 0;
    }
}

void Bluetooth::process()
{
    int charsAvailable = bluetoothSerial.available();
    
    if(charsAvailable)
    {
        while(charsAvailable)
        {
            inputBuffer = inputBuffer + (char)bluetoothSerial.read();
            charsAvailable -= 1;
        }
    
        if((isStringWithinBuffer("\r\n+") >= 0) || (isStringWithinBuffer("CON") >= 0))
        {
            allowReading = false;
            
            if(isCompleteStatusMessageInBuffer())
            {
                allowReading = true;
                statusMessage = cutStatusMessageOutOfBuffer();
                parseStatusMessage();   
            }
            
            int conMsgStart = isStringWithinBuffer("CONNECT:OK");
            if(conMsgStart >= 0)
            {
                inputBuffer = inputBuffer.substring(0, conMsgStart) + inputBuffer.substring(conMsgStart + 10);
            }
        }
        else if(allowReading && inputBuffer.length() < 3)
        {
            allowReading = false;   
        }
        else if(!allowReading)
        {
            allowReading = true;
        }
    }
}

int Bluetooth::isStringWithinBuffer(String string)
{
    if(inputBuffer.length() < string.length())
    {
        return -1; 
    }
    
    for(int i = 0; i <= inputBuffer.length() - string.length(); i++)
    {
        String sampleSubstring = inputBuffer.substring(i, i + string.length());
        if(sampleSubstring == string)
        {
            return i;  
        } 
    }
    
    return -1;
}

boolean Bluetooth::isCompleteStatusMessageInBuffer()
{
    if(isStringWithinBuffer("\r\n+BTSTATE:") >= 0)
    {
        if(isStringWithinBuffer("0\r\n") >= 0 ||
           isStringWithinBuffer("1\r\n") >= 0 ||
           isStringWithinBuffer("2\r\n") >= 0 || 
           isStringWithinBuffer("3\r\n") >= 0 || 
           isStringWithinBuffer("4\r\n") >= 0)
        {
           return true;
        } 
    }
    
    return false;
}

String Bluetooth::cutStatusMessageOutOfBuffer()
{
    int messagePlace = isStringWithinBuffer("\r\n+BTSTATE:");
    if(messagePlace < 0)
    {
        return "";   
    }
    else
    {
        String message = inputBuffer.substring(messagePlace, messagePlace + 14);
        inputBuffer = inputBuffer.substring(0, messagePlace) + inputBuffer.substring(messagePlace + 14);
        return message;
    }
}

void Bluetooth::parseStatusMessage()
{
    if(statusMessage.length() < 14)
    {
        return;
    }
 
    connectionState = statusMessage[11];
}

boolean Bluetooth::isConnected()
{
    return (connectionState == '4');
}

void Bluetooth::logMsg(String message)
{
    if(shouldPrintLog)
    {
        Serial.println(message);
    }   
}
