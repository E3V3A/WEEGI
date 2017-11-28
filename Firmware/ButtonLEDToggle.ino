#include <DSPI.h>
#include <OBCI32_SD.h>
#include <EEPROM.h>
#include <OpenBCI_Wifi_Master_Definitions.h>
#include <OpenBCI_Wifi_Master.h>
#include <OpenBCI_32bit_Library.h>
#include <OpenBCI_32bit_Library_Definitions.h>

// Booleans Required for SD_Card_Stuff.ino
boolean addAccelToSD = false; // On writeDataToSDcard() call adds Accel data to SD card write
boolean addAuxToSD = false; // On writeDataToSDCard() call adds Aux data to SD card write
boolean SDfileOpen = false; // Set true by SD_Card_Stuff.ino on successful file open

// KRAK 11/19/2017 12:03 PM
// PROG pushbutton test variables
  int LED = 11;                       // alias for the blue LED
  int pushButton = 17;                // button is on pin D17
  int pushButtonValue;                // latest button state
  int previousPushButtonValue;        // previous button state
  boolean ledState = HIGH;            // used to toggle the on-board LED
  unsigned long previousMillis = 0;   // store how long LED has been in current state

void setup() {
  // Bring up the OpenBCI Board
  board.begin();
  // Bring up wifi
  wifi.begin(true, true);
  
  // KRAK 11/19/2017 12:08 PM
  // PROG pushbutton test setup
    pinMode(pushButton, INPUT);   // set the button pin direction
    pushButtonValue = previousPushButtonValue = digitalRead(pushButton);  // seed
    previousMillis = millis();  // get time since program started running
}

void loop() {
  // KRAK 11/19/2017 12:22 PM
  // PROG pushbutton test loop
    pushButtonValue = digitalRead(pushButton);  // feel the PROG button
    if (pushButtonValue != previousPushButtonValue && (millis() > previousMillis + 250))
    {
      previousMillis = millis();                // debounce the button
      if (pushButtonValue == HIGH)              // button was just pressed
        if (board.streaming)
          board.streamStop();
        else
          board.streamStart();
        
      previousPushButtonValue = pushButtonValue;
    }
    
    if (board.streaming == false)
      ledState = HIGH;
    else if (millis() > (previousMillis + 500))
    {
      ledState = !ledState;
      previousMillis = millis();
    }
    
    digitalWrite(LED, ledState); // toggle the LED
  
  if (board.streaming) {
    if (board.channelDataAvailable) {
      // Read from the ADS(s), store data, set channelDataAvailable flag to false
      board.updateChannelData();

      // Check to see if accel has new data
      if (board.curAccelMode == board.ACCEL_MODE_ON) {
        if(board.accelHasNewData()) {
          // Get new accel data
          board.accelUpdateAxisData();

          // Tell the SD_Card_Stuff.ino to add accel data in the next write to SD
          addAccelToSD = true; // Set false after writeDataToSDcard()
        }
      } else {
        addAuxToSD = true;
      }

      // Verify the SD file is open
      if(SDfileOpen) {
        // Write to the SD card, writes aux data
        writeDataToSDcard(board.sampleCounter);
      }

      board.sendChannelData();
    }
  }

  // Check serial 0 for new data
  if (board.hasDataSerial0()) {
    // Read one char from the serial 0 port
    char newChar = board.getCharSerial0();

    // Send to the sd library for processing
    sdProcessChar(newChar);

    // Send to the board library
    board.processChar(newChar);
  }

  if (board.hasDataSerial1()) {
    // Read one char from the serial 1 port
    char newChar = board.getCharSerial1();

    // Send to the sd library for processing
    sdProcessChar(newChar);

    // Read one char and process it
    board.processChar(newChar);
  }

  // Call the loop function on the board
  board.loop();

  // Call to wifi loop
  wifi.loop();

  if (wifi.hasData()) {
    // Read one char from the wifi shield
    char newChar = wifi.getChar();

    // Send to the sd library for processing
    sdProcessChar(newChar);

    // Send to the board library
    board.processCharWifi(newChar);
  }

  if (!wifi.sentGains) {
    if(wifi.present && wifi.tx) {
      wifi.sendGains(board.numChannels, board.getGains());
    }
  }
}