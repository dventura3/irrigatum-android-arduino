/*
   Software serial multple serial test

   Receives from the hardware serial, sends to software serial.
   Receives from software serial, sends to hardware serial.

   The circuit: 
 * RX is digital pin 2 (connect to TX of other device)
 * TX is digital pin 3 (connect to RX of other device)

 created back in the mists of time
 modified 9 Apr 2012
 by Tom Igoe
 based on Mikal Hart's example

 This example code is in the public domain.

 */
#define _SS_MAX_RX_BUFF 200
#include <SoftwareSerial.h>

SoftwareSerial mySerial(2, 3); // RX, TX
template<class T>
inline Print &operator <<(Print &obj, T arg)
{ obj.print(arg); return obj; }

void setup()  
{
	// Open serial communications and wait for port to open:
	Serial.begin(9600);
	while (!Serial) {
		; // wait for serial port to connect. Needed for Leonardo only
	}


	Serial.println("Start test json");

	// set the data rate for the SoftwareSerial port
	mySerial.begin(4800);
}

void loop() // run over and over
{
	char data = 0;
	delay(1000);
	mySerial.print("json#");
	Serial << "ASDASd\n";
	Serial << "\n" << _SS_MAX_RX_BUFF;
	while(mySerial.available()) {
		char data = mySerial.read();
		Serial << data;
	}
}
