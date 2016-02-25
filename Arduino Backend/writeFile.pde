// Ported to SdFat from the native Arduino SD library example by Bill Greiman
// On the Ethernet Shield, CS is pin 4. SdFat handles setting SS
const int chipSelect = 4;
/*
   SD card read/write

   This example shows how to read and write data to and from an SD card file 	
   The circuit:
 * SD card attached to SPI bus as follows:
 ** MOSI - pin 11
 ** MISO - pin 12
 ** CLK - pin 13
 ** CS - pin 4

 created   Nov 2010
 by David A. Mellis
 updated 2 Dec 2010
 by Tom Igoe
 modified by Bill Greiman 11 Apr 2011
 This example code is in the public domain.

 */
#include <SdFat.h>
SdFat sd;
SdFile myFile;

template<class T>
inline Print &operator <<(Print &obj, T arg)
{ obj.print(arg); return obj; }

void setup() {
	Serial.begin(9600);

	// Initialize SdFat or print a detailed error message and halt
	// Use half speed like the native library.
	// change to SPI_FULL_SPEED for more performance.
	if (!sd.begin(chipSelect, SPI_HALF_SPEED)) sd.initErrorHalt();

	// open the file for write at end like the Native SD library
	if (!myFile.open("user1", O_RDWR | O_CREAT )) {
		sd.errorHalt("opening test.txt for write failed");
	}
	myFile.remove();
	myFile.close();

	if (!myFile.open("user1", O_RDWR | O_CREAT )) {
		sd.errorHalt("opening test.txt for write failed");
	}
	// if the file opened okay, write to it:
	Serial.print("Writing to test.txt...");
	myFile << "<html>\n";
	myFile << "<head>\n";
	myFile << "</head>\n";
	myFile << "<body>\n";
	myFile << "<form method=\"GET\" action=\"login\">\n";
	myFile << "Create a new User : <input type=\"text\" name=\"new\" size=\"15\" /><br />\n";
	myFile << "<div align=\"center\">\n";
	myFile << "<p><input type=\"submit\" value=\"Create\" /></p>\n";
	myFile << "</div></form>\n";
	myFile.close();
	Serial.println("done.");

	// re-open the file for reading:
	if (!myFile.open("user1", O_READ)) {
		sd.errorHalt("opening test.txt for read failed");
	}
	Serial.println("test.txt:");

	// read from the file until there's nothing else in it:
	int data;
	while ((data = myFile.read()) > 0) Serial.write((char) data);
	// close the file:
	myFile.close();
}

void loop() {
	// nothing happens after setup
}


