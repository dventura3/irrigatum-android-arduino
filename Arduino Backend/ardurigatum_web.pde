#include "SPI.h"
#include "Ethernet.h"
//#include "EthernetUdp.h"
#include "SdFat.h"

#define PORT 80
#define BUFSIZE 100
#define NAMELEN 15
// declare a static string
#define P(name)   static const prog_uchar name[] PROGMEM

static byte mac[6] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
static byte ip[4] = { 192, 168, 0, 90 };
static byte timeServer[4] = {192, 43, 244, 18};
static byte gatewayIp[4] = {192, 168, 0, 1}; // Only if useDhcp is false
static byte dnsIp[4] = {208, 67, 220, 220}; // Only if useDhcp is false
static byte subnet[4] = {255, 255, 255, 0}; // Only if useDhcp is false

EthernetServer server(80);
P(httpSuccessP) = "HTTP/1.1 200 OK\nContent-Type: text/html\n\n\n";
P(gifP) = "HTTP/1.1 200 OK\nContent-Type: image/gif\n\n\n";
//P(http404P) = "HTTP/1.1 404 Not Found\nContent-Type: text/html\n\n\n<h1>Vaccateli!!</h1>";
P(errJSON1P) = "{\"result\":\"FAIL\",\"err\": 1}";
P(okJSONrtP) = "{\"result\":\"OK\",\"t\":\"";
P(okJSONP) = "{\"result\":\"OK\"}";


// Orario (secondi dalla mezzanotte)
long unsigned daytime = 0;
// Alba 7.00 = 25200 secondi dalla mezzanotte
#define SUNRISE_TIME 25200

//Tramonto 19.00 = 68400 secondi dalla mezzanotte
#define SUNSET_TIME 68400

// Orario dell'ultimo cambio di stato degli attuatori
//long unsigned changetime = 0;

// Intervallo di aggiornamento NTP, default 30 minuti (1800 sec)
#define NTP_TIME 600
#define TIMEZONE_SECS 7200
#define NTPPORT 8888
//EthernetUDP Udp;
#define NTP_PACKET_SIZE 48

// SdFatLib
SdFat sd;

// Session Token: Utilizzato per verificare la sessione in corso;
char *SessionToken = NULL;
// Indica se è in corso una sezione attiva.
bool SessionAlive = false;
// Timer sessione
long unsigned sessionTimer = 0; 
// Tempo di vita massima di una sessione, default 3 ore
#define SESSION_LIVE_TIME 10800 

	template<class T>
inline Print &operator <<(Print &obj, T arg)
{ obj.print(arg); return obj; }

void printCP(const prog_uchar *str, EthernetClient client)
{
	char c;
	while((c = pgm_read_byte(str++)))
		client.print(c);
}

void ethBegin()
{
	Ethernet.begin(mac, ip, dnsIp, gatewayIp, subnet);
	server.begin();
}
void login(EthernetClient client, char *url_tail, bool isAuthorized)
{
	char name[NAMELEN];
	char value[NAMELEN];

	char *fine = strchr(url_tail, ' ');
	*fine = 0;

	if (strlen(url_tail))
	{
		//Serial.print(url_tail);
		//Serial.print(strlen(url_tail));
		while (strlen(url_tail))
		{
			if ( nextURLparam(&url_tail, name, NAMELEN, value, NAMELEN) != 0 ) {
				if(!strcmp(name, "new")) {
					char key[9];
					if (checkUser(value, key, 9)) {// Se l'utente non esiste
						if(!createUser(value, key, 9))
							readWebPage("user.cfg", client);
						else
							printCP(errJSON1P, client); // Invia JSON con err:3 SD error
					}
					else
						printCP(errJSON1P, client); // Invia JSON con err:2 utente esistente
				}
				//if (!strcmp(name, "check")) {
				//	char key[9];
				//	PrintMemory("\nCHECK");
				//	if (!checkUser(value, key, 9)) {
				//		printCP(okJSONP, client);
				//		Serial << "\nKey: " << key;
				//	}
				//	else
				//		printCP(errJSON1P, client); // Invia JSON con err:1 autenticazione fallita
				//}
				if (!strcmp(name, "user")) {
					char key[9];
					char rt[9];
					if (!checkUser(value, key, 9) && SessionToken == NULL) {
						genRandomKey(rt, 9); //Genera randomKey temporanea (RT)
						printCP(okJSONrtP, client);
						client << rt << "\"}"; // Invia JSON OK, con RT
						encrypt(rt, key, 9); // Cifra random key temporanea (k(RT))
						SessionToken = (char *) malloc (9);
						strcpy(SessionToken, rt);
						SessionToken[9] = 0; // Setta SessionToken alla RT ( ST = k(RT))
						Serial << "\n RT: " << SessionToken;
					} else
						printCP(errJSON1P, client); // Invia JSON con err:1 autenticazione fallita
				} else {
					if (!strcmp(name, "rt")) {
						char tmp[9];
						if (!strcmp(value, SessionToken)) {
							genRandomKey(tmp, 9);
							strcpy(SessionToken, tmp);
							printCP(okJSONrtP, client);
							client << SessionToken << "\"}"; // Invia JSON OK, con RT
							Serial << "\n ST: " << SessionToken;
							SessionAlive = true;
						} else
							printCP(errJSON1P, client);
					} else {
						//if (!strcmp(name, "end") && SessionAlive) { // Close Session
						if (!strcmp(name, "end") && isAuthorized && SessionAlive) { // Close Session
							closeSession();
							printCP(okJSONP, client);
							break;
						}
						else
							printCP(errJSON1P, client);
							break;
					}
				}

			}
		}
	//json(client);
	}
}

void closeSession()
{
	free(SessionToken);
	SessionToken = NULL;
	SessionAlive = false;
	sessionTimer = 0;
}

/* Funzione associata alla pagina "get". Contiene il codice per leggere i parametri passati
 * mediante GET ad arduino. Leggere la documentazione per maggiori informazioni sui parametri */
/*
void get(EthernetClient client, char *url_tail)
{
	char name[NAMELEN];
	char value[NAMELEN];

	char *fine = strchr(url_tail, ' ');
	*fine = 0;

	if (strlen(url_tail))
	{
		//Serial.print(url_tail);
		//Serial.print(strlen(url_tail));
		while (strlen(url_tail))
		{
			if ( nextURLparam(&url_tail, name, NAMELEN, value, NAMELEN) != 0 ) {
				//Serial << "\n" << name << " = " << value;
				if (!strcmp(name, "temp"))
					temperature = strtoul(value, NULL, 10);
				if (!strcmp(name, "mois"))
					moistness = strtoul(value, NULL, 10);
				if (!strcmp(name, "rad"))
					radiation = strtoul(value, NULL, 10);
				if(!strcmp(name, "pump1"))
					force_att(&pump1, PUMP1, strtoul(value, NULL, 10), true);
				if(!strcmp(name, "pump2"))
					force_att(&pump2, PUMP2, strtoul(value, NULL, 10), true);
				if(!strcmp(name, "bulb2"))
					force_att(&bulb2, BULB2, strtoul(value, NULL, 10), true);
				if(!strcmp(name, "bulb1"))
					force_att(&bulb1, BULB1, strtoul(value, NULL, 10), true);
				if (!strcmp(name, "cmdtemp"))
					if(getArray(value, cmdtemp, 2) != 2)
					{
						 Se viene inserito un solo valore
						 * L'array viene resettato 
						//Serial.print("\n\nerr:array\n");
						cmdtemp[0] = -99;
						cmdtemp[1] = -99;
					}
					else
					{
						force_mod = false;
						goto end;
					}

				if (!strcmp(name, "cmdmois"))
					if(getArray(value, cmdmois, 2) != 2)
					{
						 Se viene inserito un solo valore
						  L'array viene resettato 
						//Serial.print("\n\nerr:array\n");
						cmdmois[0] = -1;
						cmdmois[1] = -1;
					}
					else
					{
						force_mod = false;
						goto end;
					}

				if (!strcmp(name, "cmdrad"))
					if(getArray(value, cmdrad, 2) != 2)
					{
						 Se viene inserito un solo valore
						 * L'array viene resettato 
						//Serial.print("\n\nerr:array\n");
						cmdrad[0] = -1;
						cmdrad[1] = -1;
					}
					else
					{
						force_mod = false;
						goto end;
					}

				if (!strcmp(name, "daytime"))
				{
					short timearray[2] = {0, 0};
					if(getArray(value, timearray, 2) == 2)
					{
						daytime = ((long unsigned)timearray[0]) * 3600 + ((long unsigned)timearray[1]) * 60;
						goto end;
					}
				}

				   if (!strcmp(name, "senstemp"))
				   senstemp = set_device_var(value);

				   if (!strcmp(name, "sensmois"))
				   sensmois = set_device_var(value);

				   if (!strcmp(name, "sensrad"))
				   sensrad = set_device_var(value);

				   if(!strcmp(name, "attpump1"))
				   attpump1 = set_device_var(value);

				   if(!strcmp(name, "attpump2"))
				   attpump2 = set_device_var(value);

				   if(!strcmp(name, "attbulb1"))
				   attbulb1 = set_device_var(value);

				   if(!strcmp(name, "attbulb2"))
				   attbulb2 = set_device_var(value);

				//if(!strcmp(name, "discovery"))
				//   sdisc = true;

				if(!strcmp(name, "resradt"))
					radtime = 0;

				if(!strcmp(name, "save"))
					config("me.cfg", 'S');
				if(!strcmp(name, "load"))
					config("me.cfg", 'L');

			}
end:
			;
		}
		//if (sdisc)
		//	discovery(server, type, url_tail, tail_complete);
		//else
		//	json(server, type, url_tail, tail_complete);
		json(client);
	}
	PrintMemory("GET");
	logSD(url_tail, "j.log");
}
*/

byte encrypt(char str[], char key[], byte strLen)
{
	//strncpy(encrypted, plain, plainLen);
	return 0;
}

//byte decrypt(char plain[], char encrypted[], char key[], byte encLen)
//{
//	strncpy(plain, encrypted, encLen);
//	return 0;
//}



byte nextURLparam(char **tail, char *name, int nameLen,
		char *value, int valueLen)
{
	// assume name is at current place in stream
	char ch, hex[3];
	byte result = 1;
	char *s = *tail;
	bool keep_scanning = true;
	bool need_value = true;

	// clear out name and value so they'll be NUL terminated
	memset(name, 0, nameLen);
	memset(value, 0, valueLen);

	if (*s == 0)
		return 0; // Fine dei parametri
	// Read the keyword name
	while (keep_scanning)
	{
		ch = *s++;
		switch (ch)
		{
			//case ' ': // Stesse operazioni del \0
			case 0:
				s--;  // Back up to point to terminating NUL
				// Fall through to "stop the scan" code
			case '&':
				/* that's end of pair, go away */
				keep_scanning = false;
				need_value = false;
				break;
			case '=':
				/* that's end of name, so switch to storing in value */
				keep_scanning = false;
				break;
		}


		// check against 1 so we don't overwrite the final NUL
		if (keep_scanning && (nameLen > 1))
		{
			*name++ = ch;
			--nameLen;
		}
		else
			result = 2; //URLPARAM_NAME_OFLO;
	}

	if (need_value && (*s != 0))
	{
		keep_scanning = true;
		while (keep_scanning)
		{
			ch = *s++;
			switch (ch)
			{
				//case ' ':
				case 0:
					s--;  // Back up to point to terminating NUL
					// Fall through to "stop the scan" code
				case '&':
					/* that's end of pair, go away */
					keep_scanning = false;
					need_value = false;
					break;
			}


			// check against 1 so we don't overwrite the final NUL
			if (keep_scanning && (valueLen > 1))
			{
				*value++ = ch;
				--valueLen;
			}
			else
				result = (result == 1) ? 3 : 5;
		}
	}
	*tail = s;
	return result;
}

void processConnection()
{
	char clientline[BUFSIZE];
	bool isAuthorized = false;
	//char token[9];
	byte index = 0;
	//byte it = 0;
	//bool readingToken = false;
	//bool TokenComplete = false;

	EthernetClient client = server.available();
	if (client) {
		// an http request ends with a blank line
		boolean current_line_is_blank = true;

		// reset the input buffer
		index = 0;

		while (client.connected()) {
			if (client.available()) {
				char c = client.read();

				// If it isn't a new line, add the character to the buffer
				if (c != '\n' && c != '\r') {
					clientline[index] = c;
					index++;
					// are we too big for the buffer? start tossing out data
					if (index >= BUFSIZE)
						index = BUFSIZE -1;

					// continue to read more data!
					continue;
				}

				// got a \n or \r new line, which means the string is done
				clientline[index] = 0;

				// Print it out for debugging
				Serial.print(clientline);

				if (strstr(clientline, SessionToken) != 0) {
					isAuthorized = true;
					Serial << "\nLogged " << SessionToken;
				}

				// Look for substring such as a request to get the root file
				if (strstr(clientline, "GET /?t=") != 0 && isAuthorized && SessionAlive) {
					// send a standard http response header
					printCP(httpSuccessP, client);
					//json(client);
					break;
					// print all the files, use a helper to keep it clean
				}
				else if (strstr(clientline, "GET /get") != 0 && isAuthorized && SessionAlive) {
					printCP(httpSuccessP, client);
					//Serial.print(clientline + 9);
					//get(client, clientline + 9);
					PrintMemory("get");
					break;
				}
				else if (strstr(clientline, "GET /login") != 0) {
					printCP(httpSuccessP, client);
					//Serial.print(clientline + 11);
					login(client, clientline + 11, isAuthorized);
					break;
				} 
				else if(strstr(clientline, "GET /ard.gif") != 0) {
						printCP(gifP, client);
						readWebPage("ard.gif", client);
				} else
					printCP(errJSON1P, client);
				break;
			}
		}
		// give the web browser time to receive the data
		delay(1);
		client.stop();
	}
}

byte config(char *filename, char op)
{
	SdFile myFile;
	byte i;
	char buf[7];
	char data;


	// if the file opened okay, write to it:
	if (!myFile.open(filename, O_RDWR | O_CREAT)) {
		//Serial.print("\nSD ERR");
		return -1;
	}

	/*
	if (op == 'S') { // Salva Configurazione sul file
		myFile << cmdtemp[0] << " " << cmdtemp[1] << " ";
		myFile << cmdmois[0] << " " << cmdmois[1] << " ";
		myFile << cmdrad[0] << " " << cmdrad[1] << " \0";
	} else {
		i = 0;
		int j = 0;
		short *p;
		while (( data = myFile.read()) > 0 && j < 6) {
			if (data != ' ')
				buf[i++] = data;
			else {
				buf[i] = 0;
				switch(j++)
				{
					case 0:
						p  = &cmdtemp[0];
						break;
					case 1:
						p  = &cmdtemp[1];
						break;
					case 2:
						p  = &cmdmois[0];
						break;
					case 3:
						p  = &cmdmois[1];
						break;
					case 4:
						p  = &cmdrad[0];
						break;
					case 5:
						p  = &cmdrad[1];
						break;
				}
				*p = strtoul(buf, NULL, 10);
				i = 0;
			}
		}
	}
	*/
	return 0;
}


byte readWebPage(char *filename, EthernetClient client)
{
	SdFile myFile;
	char buf[8];

	if (!myFile.open(filename, O_READ))
		return -1;
	int16_t data;
	byte i;
	while ((data = myFile.read()) >= 0) {
		buf[i++] = data;
		if (i == 8) {
			myFile.write(buf, 8);
			i = 0;
		}
	}
	if (i > 0)
		myFile.write(buf, i);
	return 0;
}

byte checkUser(char user[], char key[], byte keyLen)
{
	SdFile myFile;
	char name[9];
	char k[9];
	char val;
	bool isName = true;
	byte i;

	// if the file opened okay, write to it:
	Serial.print("\nSD");
	if (!myFile.open("user.cfg", O_READ)) {
		return -1;
	}
	i = 0;
	//Serial << "\n START!";
	while ((val = myFile.read()) > 0) {
		switch(val)
		{
			case ' ':
				isName = false;
				name[i] = 0;
				i = 0;
				break;
			case '\n':
				isName = true;
				i = 0;
				if (!strcmp(name, user)) {
					strncpy(key, k, keyLen - 1);
					key[keyLen - 1] = 0;
					return 0;
				}
				break;
			default:
				if (isName)
					name[i++] = val;
				else
					k[i++] = val;
				i = i % keyLen;
		}
	}
	Serial << "\nNO USER";
	return -2;
}

byte createUser(char user[], char key[], byte keyLen)
{
	// Prima di creare l'utente fare il check
	//if (false); //checkUser(user, key);
	//	return -2;
	genRandomKey(key, keyLen);
	//Serial << "\n" << user << " " << key;

	SdFile myFile;

	// if the file opened okay, write to it:
	//Serial.print("\nSD");
	if (!myFile.open("user.cfg", O_WRITE | O_CREAT | O_AT_END))
		return -1;
	myFile << user << " " << key << "\n";
	return 0;
}



byte logSD(char *str, char *filename)
{
	SdFile myFile;

	// if the file opened okay, write to it:
	//Serial.print("\nSD");
	if (!myFile.open(filename, O_RDWR | O_CREAT | O_AT_END))
		return -1;
	byte h = daytime / 3600;
	byte m = (daytime % 3600) / 60;
	byte s = daytime % 60;
	myFile << "\n[" << h << ":" << m << ":" << s << "]  " << str;
	// close the file:
	myFile.close();
	PrintMemory("SD");
	return 0;
}

/* Questa funzione estrae gli elementi da una stringa del tipo "1,2,3$" per popolare un array.
str: stringa di ingresso
array[] : array da popolare
arrayLen : lunghezza dell'array che si vuole popolare
 */

short getArray(char *str, short array[], int arrayLen)
{
	short start = 0;
	short len = 0;
	short index = 0;
	char to[10];
	for (short i = 0; i < strlen(str) && index < arrayLen; i++)
	{
		if (str[i] != ',' && str[i] != '$')
			len++;
		else {
			strncpy(to, str + start, len);
			to[len] = '\0';
			array[index] = strtoul(to, NULL, 10);
			start = i + 1;
			len = 0;
			index++;
		}
	}
	return index;
}

void printArray(EthernetClient &client, short array[], short len, const prog_uchar *str)
{
	printCP(str, client);
	for (short i = 0; i < len; i++)
	{
		client << array[i];
		if (i != len -1)
			client << ",";
	}
	client << "]";
}

/* Funzione associata alla pagina "json". Fornisce al richiedente informazioni sullo stato di arduino,
 * delle sue variabili interne e degli attuatori. Il formato utilizzato per l'output è JSON */
/*
void json(EthernetClient client)
{
	client << "{"; // INIZIO JSON

	//client << "\"temp\":" << temperature << ",";
	printCP(tempP, client);
	client << "" << temperature << ",";
	printCP(moisP, client);
	client << moistness << ",";
	printCP(radP, client);
	client << radiation << ",";
	printCP(pump1P, client);
	client << pump1 << ",";
	printCP(pump2P, client);
	client << pump2 << ",";
	printCP(bulb1P, client);
	client << "" << bulb1 << ",";
	printCP(bulb2P, client);
	client << bulb2 << ",";
	printCP(daytimeP, client);
	client << daytime << ",";
	printCP(radtimeP, client);
	client << radtime << ",";
	//printCP(changetimeP, client);
	//client << changetime << ",";
	printCP(resultP, client);
	client << "\"OK\",";
	//printArray(client, array, arrayLen, "array");
	printArray(client, cmdtemp, 2, cmdtempP);
	client << ",";
	printArray(client, cmdmois, 2, cmdmoisP);
	client << ",";
	printArray(client, cmdrad, 2, cmdradP);

	client << "}"; // FINE JSON
	PrintMemory("JSON");
	//logSD("json", "j.log");
}
*/
/* Funzione associata alla pagina "discovery". Invia, mediante JSON, informazioni sui sensori
 * e attuatori attivi e utilizzati dal sistema. */

/*
   void discovery(WebServer &server, WebServer::ConnectionType type, char *url_tail, bool tail_complete)
   {
   if (type == WebServer::POST)
   {
   server.httpFail();
   return;
   }

   server.httpSuccess("application/json");

   if (type == WebServer::HEAD)
   return;

   server << "{ "; // INIZIO JSON
   server << "\"force_mod\": " << (force_mod ? 1 : 0) << ", ";
   server << "\"senstemp\": " << (senstemp ? TEMP_SENS : -1) << ", ";
   server << "\"sensmois\": " << (sensmois ? MOIS_SENS : -1) << ", ";
   server << "\"sensrad\": " << (sensrad ? RAD_SENS : -1);
//server << "\"sensrad\": " << (sensrad ? RAD_SENS : -1) << ", ";
//server << "\"attpump1\": " << (attpump1 ? PUMP1 : -1) << ", ";
//server << "\"attpump2\": " << (attpump2 ? PUMP2 : -1) << ", ";
//server << "\"attbulb1\": " << (attbulb1 ? BULB1 : -1) << ", ";
//server << "\"attbulb2\": " << (attbulb2 ? BULB2 : -1);
server << " }"; // FINE JSON
}
 */

void setup()
{

	if (!sd.begin(4, SPI_HALF_SPEED)) {
		Serial.print("\nSD ERR");
		return;
	}
	ethBegin();
	Serial.begin(9600);
	pinMode(10, OUTPUT);
	digitalWrite(10, HIGH);

	//Begin Utp connection to NTP server
	// run the memory test function and print the results to the serial port
	//Udp.begin(NTPPORT);
	//syncNTP();
	//logSD("test.txt", "TRY");
	// Init random system.
	randomSeed(analogRead(0) * analogRead(1) * analogRead(2));
	config("me.cfg", 'L');
	PrintMemory("END_SETUP");
}

void genRandomKey(char key[], byte keylen)
{
	byte i, val;
	for (i = 0; i < keylen - 1; i++) {
		val = random(62);
		if (val < 10)
			val += 48;
		else if (val < 36)
			val += 55;
		else
			val += 61;
		key[i] = (char) val;
	}
	key[keylen - 1] = 0;
}


/*
void syncNTP()
{
	byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming and outgoing packets 

	sendNTPpacket(timeServer, packetBuffer); // Invia la richiesta al server NTP
	// Attesa dell'arriva del pacchetto
	timeddelay(2, false);  // !! RIVEDERE !!//
	if ( Udp.parsePacket() )
	{  
		// Leggiamo il pacchetto appena arrivato dal server NTP
		Udp.read(packetBuffer,NTP_PACKET_SIZE);  // Leggiamo nel packetBuffer

		// Il timestamp comincia al byte 40 ed è lungo 2 word
		unsigned long highWord = word(packetBuffer[40], packetBuffer[41]);
		unsigned long lowWord = word(packetBuffer[42], packetBuffer[43]);  

		// Combiniamo le due word per ottenere i secondi dal 1/1/1900
		unsigned long secsSince1900 = highWord << 16 | lowWord;  

		// Unix-time
		const unsigned long seventyYears = 2208988800UL;     
		unsigned long epoch = secsSince1900 - seventyYears;  

		// Otteniamo l'ora attuale
		// L'ora ottenuta sarà UTC in GMT. Quindi bisogna adattarla al nostro +2h (Ora legale)
		daytime = ((epoch  % 86400L) + TIMEZONE_SECS) % 86400;
		//Serial.print("\n\n ");
		//Serial.print(daytime / 3600);
		//Serial.print("\n : ");
		//Serial.print((daytime % 3600) / 60);
	}
	else
		Serial.print("\n\n NO PACKAGE");
}
*/

// send an NTP request to the time server at the given address 
/*
unsigned long sendNTPpacket(byte address[], byte packetBuffer[])
{
	// set all bytes in the buffer to 0
	Serial.print("\n\n Send packet\n");
	memset(packetBuffer, 0, NTP_PACKET_SIZE); 
	// Initialize values needed to form NTP request
	// (see URL above for details on the packets)
	packetBuffer[0] = 0b11100011;   // LI, Version, Mode
	packetBuffer[1] = 0;     // Stratum, or type of clock
	packetBuffer[2] = 6;     // Polling Interval
	packetBuffer[3] = 0xEC;  // Peer Clock Precision
	// 8 bytes of zero for Root Delay & Root Dispersion
	packetBuffer[12]  = 49; 
	packetBuffer[13]  = 0x4E;
	packetBuffer[14]  = 49;
	packetBuffer[15]  = 52;

	// all NTP fields have been given values, now
	// you can send a packet requesting a timestamp: 		   
	Udp.beginPacket(address, 123); //NTP requests are to port 123
	Udp.write(packetBuffer,NTP_PACKET_SIZE);
	Udp.endPacket(); 
}
*/

void loop()
{
	timeddelay(1, true);
	// Il webserver processa le connessioni una alla volta.
	processConnection();

	//useSD("loop", 'L');
	PrintMemory("LOOP");
}

/* Questa funzione permette di aggiungere delay nel codice mantenendo la consistenza
   dei timer deputati al conteggio delle ore del giorno e delle ore di luce */
void timeddelay(int sec, bool ntp)
{
	for (int i = 0; i < sec; i++)
	{
		delay(1000);
		daytime++;
		daytime = daytime % 86400;
		if (SessionAlive) {
			sessionTimer++;
			if (sessionTimer >= SESSION_LIVE_TIME)
				closeSession();
		}
		/*
		if (daytime % NTP_TIME <= 1 && ntp)
		{
			// Lancia processo di sync con NTP
			//Serial.print("\nUSO NTP!!");
			syncNTP();
		}
		*/
	}
}

// this function will return the number of bytes currently free in RAM

extern int __bss_end;
extern void *__brkval;
void PrintMemory(char *label)
{
	int free_memory;

	if((int)__brkval == 0)
		free_memory = ((int)&free_memory) - ((int)&__bss_end);
	else
		free_memory = ((int)&free_memory) - ((int)__brkval);

	Serial << "\n "<< label << " MEM: " << free_memory << "\n";
}


