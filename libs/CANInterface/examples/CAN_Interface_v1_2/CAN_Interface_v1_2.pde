#include <SdFat.h>
#include <SdFatUtil.h>
#include <NewSoftSerial.h>
#include <stdlib.h>
#include <SPI.h>
#include <mcp2515_defs.h>
#include <CANInterface.h>


/*SD card Init*/
Sd2Card card;
SdVolume volume;
SdFile root;
SdFile file;

/*LCD Screen Init*/
NewSoftSerial sLCD = NewSoftSerial(5,6); /* Serial LCD is connected to digital pin 6 */ 
#define COMMAND 0xFE
#define CLEAR   0x01
#define LINE0   0x80
#define LINE1   0xC0
#define SCREEN  32

/*"Joystick" Init*/
#define UP     A1    /*Analog Pin 1*/
#define DOWN   A3    /*Analog Pin 3*/
#define LEFT   A5    /*Analog Pin 5*/
#define RIGHT  A2    /*Analog Pin 2*/
#define CLICK  A4    /*Analog Pin 4*/
#define JSP    0  /*Joystick Pressed*/
#define JSNP   1  /*Joystick Not Pressed*/

/*Status LED's*/
#ifndef LED_2
int LED_2 = 8;
#endif
#ifndef LED_3
int LED_3 = 7;
#endif

int SDCS = 9; /* SPI Enable for SD Card */
int CANCS = 53;  /* SPI Enable for CAN */

int inputchar [32];

/*file name*/
char name[] = "DATA00.txt";


// store error strings in flash to save RAM
#define error(s) error_P(PSTR(s))

void error_P(const char* str) {
  PgmPrint("error: ");
  SerialPrintln_P(str);
  if (card.errorCode()) {
    PgmPrint("SD error: ");
    Serial.print(card.errorCode(), HEX);
    Serial.print(',');
    Serial.println(card.errorData(), HEX);
  }
  while(1);
}

void init_SPI_CS(void)
{
  pinMode(SDCS, OUTPUT);    /* Initialize the SD card SPI CS Pin*/
  digitalWrite(SDCS, HIGH);  /* Turns Sd Card communication off */
  pinMode(CANCS,OUTPUT);    /* Initialize the CAN bus card SPI CS Pin*/
  digitalWrite(CANCS, HIGH); /* Turns the CAN bus card communication off*/
  pinMode(10,INPUT);   /* all these need to be set to inputs cuz the card isn't compatible with the Arduino Mega....*/
  digitalWrite(10,HIGH);
  pinMode(11, INPUT);    
  digitalWrite(11, LOW);
  pinMode(12, INPUT);    
  digitalWrite(12, LOW);
  pinMode(13, INPUT);    
  digitalWrite(13, LOW);
}  

void init_Status_LED(void)
{
  pinMode(LED_2, OUTPUT); /*Status LED*/
  pinMode(LED_3, OUTPUT); /*communication LED*/
  digitalWrite(LED_2,HIGH);
}

/*converts char to decimal number*/
int char2num(int chr)
{
  switch(chr){
   case 48:
   return 0;
   break;
   case 49:
   return 1;
   break;
   case 50:
   return 2;
   break;
   case 51:
   return 3;
   break;
   case 52:
   return 4;
   break;
   case 53:
   return 5;
   break;
   case 54:
   return 6;
   break;
   case 55:
   return 7;
   break;
   case 56:
   return 8;
   break;
   case 57:
   return 9;
   break;
   case 65:
   return 10;
   break;
   case 66:
   return 11;
   break;
   case 67:
   return 12;
   break;
   case 68:
   return 13;
   break;
   case 69:
   return 14;
   break;
   case 70:
   return 15;
   break;   
   default:
  return 0; 
  }
  
}

void init_JoyStick(void)
{
  pinMode(UP,INPUT);
  pinMode(DOWN,INPUT);
  pinMode(LEFT,INPUT);
  pinMode(RIGHT,INPUT);
  pinMode(CLICK,INPUT);
  digitalWrite(UP, HIGH);  /*enabled input pull-up resistor*/
  digitalWrite(DOWN, HIGH);  /*enabled input pull-up resistor*/
  digitalWrite(LEFT, HIGH);  /*enabled input pull-up resistor*/
  digitalWrite(RIGHT, HIGH);  /*enabled input pull-up resistor*/
  digitalWrite(CLICK, HIGH);  /*enabled input pull-up resistor*/
}

void LCD_init(void)
{
  sLCD.begin(9600);
  sLCD.print(COMMAND,BYTE);
  sLCD.print(CLEAR,BYTE);
}

/*LCD_print(const char *str, int line, int pos, int clrscrn)
str is the pointer to the characters to be written to the screen

line is the line number that the user wants to have the characters
posted to. Mind you screen used in this demo had only two lines, there is
no support for going past the available number of lines.

pos is the position relative to the start of the line

clrscrn will clear the screen if it is 1 and do nothing if it is anything else*/

int LCD_print(const char *str, int line, int pos, int clrscrn)  /* assumes 2*16 screen size*/
{
  int counter = 0;
  if(line == 0)
  {
    line = LINE0 + pos;
    if(clrscrn == 1)
    {
    sLCD.print(COMMAND,BYTE);
    sLCD.print(CLEAR,BYTE);
    }
    sLCD.print(COMMAND,BYTE);
    sLCD.print(line,BYTE);
    while(*str)
    {
    sLCD.print(*str++,BYTE);  /*not the most efficient but it works...*/
    counter++;
    if(counter >= SCREEN)  /* doesn't allow overlap onto first screen*/
    {
      break;
    }
    }
  }
  else if (line == 1)
  {
    line = LINE1 + pos;
    if(clrscrn == 1)
    {
    sLCD.print(COMMAND,BYTE);
    sLCD.print(CLEAR,BYTE);
    }
    sLCD.print(COMMAND,BYTE);
    sLCD.print(line,BYTE);
    while(*str)
    {
    sLCD.print(*str++,BYTE);  /*not the most efficient but it works...*/
    counter++;
    if(counter >= (SCREEN/2)) /*doesn't allow overlap onto first screen*/
    {
      break;
    }
    }
  }
  else
  {
    return -1;
  }
}  


/*special data writing functions*/
void writeCRLF(SdFile& f) {
  f.write((uint8_t*)"\r\n", 2);
}
/*
 * Write an unsigned number to a file
 */
void writeNumber(SdFile& f, uint32_t n) {
  uint8_t buf[10];
  uint8_t i = 0;
  do {
    i++;
    buf[sizeof(buf) - i] = n%10 + '0';
    n /= 10;
  } while (n);
  f.write(&buf[sizeof(buf) - i], i);
}
/*
 * Write a string to a file
 */
void writeString(SdFile& f, char *str) {
   uint8_t n;
  for (n = 0; str[n]; n++);
  f.write((uint8_t *)str, n);
}

void setup(void)
{  
  init_SPI_CS();
  init_JoyStick();
  init_Status_LED();
  LCD_init();
  LCD_print("CAN Monitor",0,0,1);
  LCD_print("John Schmotzer",1,0,0);
  delay(1000);
  Serial.begin(9600);  /* Setup Serial communication to Computer*/
  LCD_print("Press Up to",0,0,1);
  LCD_print("Begin",1,0,0);
  while (digitalRead(UP));
  SPI.begin();
  /*Set CANSPEED to have baud rate for 95 kbps GMLAN medium speed baudrate.*/
  if(CAN.Init(7))/*SPI must be configured first  new 95 kbps...*/
  {
    LCD_print("CAN Running",0,0,1);
    Serial.println("CAN Running");
  }
  else
  {
    LCD_print("CAN Failure",0,0,1);
  }
    digitalWrite(CANCS, HIGH);
    digitalWrite(SDCS, LOW); 
   delay(500); 
  // initialize the SD card at SPI_HALF_SPEED to avoid bus errors with
  // breadboards.  use SPI_FULL_SPEED for better performance.
  if (!card.init(SPI_FULL_SPEED)) error("card.init failed");
  
  // initialize a FAT volume
  if (!volume.init(&card)) error("volume.init failed");
  
  // open the root directory
  if (!root.openRoot(&volume)) error("openRoot failed");

  // create a new file
  //name[] = "WRITE00.TXT";
  for (uint8_t i = 0; i < 100; i++) {
    name[4] = i/10 + '0';
    name[5] = i%10 + '0';
    if (file.open(&root, name, O_CREAT | O_EXCL | O_WRITE)) break;
  }
  if (!file.isOpen()) error ("file.create");
  LCD_print("Writing to: ",0,0,1);
  LCD_print(name,1,0,0);
  
  writeString(file,"time (us),");
  writeString(file,"ID,");
  writeString(file,"Data,");
  writeCRLF(file);
  file.close();
  digitalWrite(SDCS,HIGH);
  digitalWrite(CANCS,LOW);
  
  
  while(1)
  {
    digitalWrite(CANCS,HIGH);
    digitalWrite(SDCS,LOW);
    while(!file.open(&root,name, O_CREAT | O_WRITE | O_APPEND));
    digitalWrite(SDCS,HIGH);
    digitalWrite(CANCS,LOW);
  /*CAN Structure*/
  tCAN message;

  /*CAN Data setup*/
  float engine_data;
  int timeout = 0;
  char message_ok = 0;
  char buffer[32];
  unsigned long timestamp=0;
  
  /*digitalWrite(CANCS,HIGH);
  digitalWrite(SDCS,LOW);
  file.print(15,HEX);
  writeString(file,"Happy");
  file.println();
  file.close();
  delay(1000);*/  
 CAN.bit_modify(CANCTRL, (1<<REQOP2)|(1<<REQOP1)|(1<<REQOP0), 0);
 
 if (CAN.check_message())
	{
        if (CAN.get_message(&message, &timestamp))
	    {
             digitalWrite(CANCS,HIGH);
             digitalWrite(SDCS,LOW);
             file.print(timestamp,DEC);
             Serial.print(timestamp,DEC);
             writeString(file,",");
             Serial.print(',');
             file.print(message.id,HEX);
             Serial.print(message.id,HEX);
             writeString(file,",");
             Serial.print(',');
             for(int i = 0; i < 8; i++)
             {
               file.print(message.data[i],HEX);
               Serial.print(message.data[i],HEX);
               writeString(file,"  ");
               Serial.print(' ');
             }
             Serial.println();
             file.println();             
            } 
        }
    
  file.close();
  digitalWrite(SDCS,HIGH);
  digitalWrite(CANCS,LOW);
  if(Serial.available() >= 30)
  {
    if(Serial.peek() == 'W')
     {
       /*Serial write format looks like this:
       "W 0x000 0x0 0x0000000000000000"
       do that (With the spaces) and you are good, otherwise we have a problem, no quotes.
       There is no format error detection so be careful.
       Note that its "W ID Data Length Payload" In HEX NOT Decimal....this is also key, no "15" enter F
       Its case sensitive, so no 'f' either, don't complain, fix it if you like code is above, scroll up.*/
     for(uint8_t i = 0;i < 30; i++)
       {
         inputchar[i] = Serial.read();
       }
       message.id = (uint16_t)(char2num(inputchar[4])<<8)+(char2num(inputchar[5])<<4)+(char2num(inputchar[6]));
       message.header.length = char2num(inputchar[10]);  
       message.header.rtr = 0;
       for(uint8_t i = 0;i < message.header.length;i++)
       {
         /*29....Yes this is effed up. but basically, there are 30 elements in the array so 0-29
         are the indices.  14th position is MSB of payload, 29th is LSB and all that needs
         to go into message.data to be sent over the friendly CAN bus....*/
         message.data[i] = (uint8_t)((char2num(inputchar[29-(i*2+1)])<<4)+(char2num(inputchar[29-(i*2)])));
       }
       CAN.send_message(&message);
   } 
  }
  delay(10);
  if(!digitalRead(DOWN))
  {
    break;
  }
  }
  
  digitalWrite(CANCS,HIGH);
  digitalWrite(SDCS,LOW);
  if(file.open(&root,name, O_READ))
  {
    LCD_print("Reading:",0,0,1);
    LCD_print(name,1,0,0);
    int16_t n;
    uint8_t buf[7];// nothing special about 7, just a lucky number.
    while ((n = file.read(buf, sizeof(buf))) > 0) 
    {
      for (uint8_t i = 0; i < n; i++) Serial.print(buf[i]);
    }
  }
  else
  {
    LCD_print("Fail Read",0,0,1);
  }
  
  delay(5000);
  LCD_print("Program Done",0,0,1);
  
  
}

void loop(void)
{
  
}
