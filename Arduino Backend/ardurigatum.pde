#include "SPI.h"
#include "Ethernet.h"
//#include "EthernetUdp.h"
#include "SdFat.h"

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
P(jsonP) = "GET /?t="; //len 9
P(getP) = "GET /get"; // len 9
P(loginP) = "GET /login"; // len 11
P(adminP) = "GET /admin"; // len 11
P(gettitleP) = "GET /title.gif"; // len 11
P(httpSuccessP) = "HTTP/1.1 200 OK\nContent-Type: text/html\n\n\n";
P(gifP) = "HTTP/1.1 200 OK\nContent-Type: image/gif\n";
//P(http404P) = "HTTP/1.1 404 Not Found\nContent-Type: text/html\n\n\n<h1>Vaccateli!!</h1>";
P(tempP) = "\"temp\":";
P(moisP) = "\"mois\":";
P(radP) = "\"rad\":";
P(pump1P) = "\"pump1\":";
P(pump2P) = "\"pump2\":";
P(bulb1P) = "\"bulb1\":";
P(bulb2P) = "\"bulb2\":";
P(daytimeP) = "\"daytime\":";
P(radtimeP) = "\"radtime\":";
P(changetimeP) = "\"changetime\":";
P(cmdtempP) = "\"cmdtemp\":[";
P(cmdmoisP) = "\"cmdmois\":[";
P(cmdradP) = "\"cmdrad\":[";
P(resultP) = "\"result\":";
P(errJSON1P) = "{\"result\":\"FAIL\",\"err\": 1}";
P(okJSONrtP) = "{\"result\":\"OK\",\"t\":\"";
P(okJSONP) = "{\"result\":\"OK\"}";
P(htmlP) = "</body></html>";
P(userfileP) = "user.cfg";
P(titleP) = "title.gif";
// Valori di controllo
short cmdtemp[2] = {-99, -99};
short cmdmois[2] = {-1, -1};
short cmdrad[2] = {-1, -1};


// Porte ANALOGICHE lettura dei sensori
#define TEMP_SENS 5
#define MOIS_SENS 1
#define RAD_SENS 2

// Porte digitali necessarie alla lettura dell'umidità
#define MOIS_SENS_VOLT_1 8
#define MOIS_SENS_VOLT_2 7
#define FLIP_TIME 1 //secondi

// Porte DIGITALI Attuatori
#define PUMP1 2
#define PUMP2 3
#define BULB1 6
#define BULB2 5

// Stato dei device
bool senstemp = false;
bool sensmois = false;
bool sensrad = false;
bool sens_flip = true;
//bool attpump1 = true;
//bool attpump2 = true;
//bool attbulb1 = true;
//bool attbulb2 = true;

// Controllo forzato
bool force_mod = false;

// Varibili interne
short temperature = -99; // temp
short moistness = -1;   // mois
short radiation = -1;   // rad

// Stato delle pompe
bool pump1 = false;
bool pump2 = false;

// Stato delle lampade
bool bulb1 = false;
bool bulb2 = false;

// Orario (secondi dalla mezzanotte)
long unsigned daytime = 0;
// Alba 7.00 = 25200 secondi dalla mezzanotte
#define SUNRISE_TIME 25200

//Tramonto 19.00 = 68400 secondi dalla mezzanotte
#define SUNSET_TIME 68400

//long unsigned lightperiod[2] = {SUNRISE_TIME, SUNSET_TIME};

// Tempo di irraggiamento attuale
long unsigned radtime = 0;

// Orario dell'ultimo cambio di stato degli attuatori
//long unsigned changetime = 0;

// Intervallo di aggiornamento NTP, default 30 minuti (1800 sec)
#define NTP_TIME 600
#define TIMEZONE_SECS 7200
#define NTPPORT 8888
//EthernetUDP Udp;
#define NTP_PACKET_SIZE 48

#define USERNAME "admin"
#define PASSWORD "saro"

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

char *getStringP(const prog_uchar *str, char **out_str, byte l)
{
	free(*out_str);
	*out_str = (char *) malloc (l + 1);
	byte i = 0;
	char c;
	if (out_str != NULL) {
		while((c = pgm_read_byte(str++)))
			(*out_str)[i++] = c;
		(*out_str)[l] = 0;
		return *out_str;
	} else
		return NULL;
}

void ethBegin()
{
	Ethernet.begin(mac, ip, dnsIp, gatewayIp, subnet);
	server.begin();
}

bool admin(EthernetClient client, char *url_tail)
{
	char name[NAMELEN];
	char value[NAMELEN];
	bool isAdmin = false;
	bool isPassword = false;

	char *fine = strchr(url_tail, ' ');
	*fine = 0;

	if (strlen(url_tail))
	{
		//Serial.print(url_tail);
		//Serial.print(strlen(url_tail));
		while (strlen(url_tail))
		{
			if ( nextURLparam(&url_tail, name, NAMELEN, value, NAMELEN) != 0 ) {
				if (!strcmp(name, "user"))
				{
					if(!strcmp(value, USERNAME))
						isAdmin =  true;
				}
				if (!strcmp(name, "pass") && isAdmin)
				{
					if (!strcmp(value, PASSWORD))
						isPassword = true;
				}
			}

		}
	}
	if (isPassword)
		return true;
	else
		return false;
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
						if(!createUser(value, key, 9)) {
							char *str;
							readWebPage(getStringP(userfileP, &str, 8), client);
							free(str);
						}
						else
							printCP(errJSON1P, client); // Invia JSON con err:3 SD error
					}
					else
						printCP(errJSON1P, client); // Invia JSON con err:2 utente esistente
					break;
				}
				//if (!strcmp(name, "check")) {
				//	char key[9];
				//	//PrintMemory("\nCHECK");
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
						//Serial << "\n RT: " << SessionToken;
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
							//Serial << "\n ST: " << SessionToken;
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
						/* Se viene inserito un solo valore
						 * L'array viene resettato */
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
						/* Se viene inserito un solo valore
						 * L'array viene resettato */
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
						/* Se viene inserito un solo valore
						 * L'array viene resettato */
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

				/*
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
				 */

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
	//PrintMemory("GET");
	//logSD(url_tail, "j.log");
}

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
	char *str = NULL;
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
					//Serial << "\nLogged " << SessionToken;
				}

				// Look for substring such as a request to get the root file
				if (strstr(clientline, getStringP(jsonP, &str, 8))!= 0 && isAuthorized && SessionAlive) {
					// send a standard http response header
					printCP(httpSuccessP, client);
					json(client);
					break;
					// print all the files, use a helper to keep it clean
				}
				else if (strstr(clientline, getStringP(getP, &str, 8)) != 0 && isAuthorized && SessionAlive) {
					printCP(httpSuccessP, client);
					//Serial.print(clientline + 9);
					get(client, clientline + 9);
					//PrintMemory("get");
					break;
				}
				else if (strstr(clientline, getStringP(loginP, &str, 10)) != 0) {
					printCP(httpSuccessP, client);
					//Serial.print(clientline + 11);
					login(client, clientline + 11, isAuthorized);
					break;
				} 
				else if(strstr(clientline, getStringP(adminP, &str, 10)) != 0) {
					printCP(httpSuccessP, client);
					readWebPage("index", client);
					if(admin(client, clientline + 11)) {
						readWebPage("user1", client);
						readWebPage(getStringP(userfileP, &str, 8), client);
						printCP(htmlP, client);
					}
					break;
				}
				else if(strstr(clientline, getStringP(gettitleP, &str, 15)) != 0) {
					printCP(gifP, client);
					readWebPage(getStringP(titleP, &str, 10), client);
					break;
				}
				else {
					printCP(httpSuccessP, client);
					printCP(errJSON1P, client);
				}
				break;
			}
		}
		// give the web browser time to receive the data
		free(str);
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
	return 0;
}


byte readWebPage(char *filename, EthernetClient client)
{
	SdFile myFile;
	char buf[8];

	if (!myFile.open(filename, O_READ))
		return -1;
	byte i = 0;
	/*
	while ((buf[i++] = myFile.read()) > 0) {
		if (i == 7) {
			buf[7] = 0;
			//client.write(buf, 8);
			client << buf;
			i = 0;
		}
	}
	if (i > 0) {
		buf[i - 1] = 0;
		//client.write(buf, i);
		client << buf;
	}
	*/
	int16_t c;
	while ((c = myFile.read()) >= 0) {
		// uncomment the serial to debug (slow!)
		//Serial.print((char)c);
		client.print((char)c);
	}
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
	//Serial.print("\nSD");
	char *str = NULL;
	if (!myFile.open(getStringP(userfileP, &str, 8), O_READ)) {
		free(str);
		return -1;
	}
	free(str);
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
	Serial << "\nNO U";
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
	char *str = NULL;
	if (!myFile.open(getStringP(userfileP, &str, 8), O_WRITE | O_CREAT | O_AT_END)) {
		free(str);
		return -1;
	}
	free(str);
	myFile << user << " " << key << "\n";
	return 0;
}


/*
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
	//PrintMemory("SD");
	return 0;
}
*/
short readTemperature()
{
	int t = 0;
	if(!senstemp)
		return -1;
	analogRead(TEMP_SENS);
	timeddelay(1, false);
	return analogRead(TEMP_SENS) * 0.48828125;
}

short readMoistness()
{
	if (!sensmois)
		return -1;
	setSensorPolarity(true);
	timeddelay(FLIP_TIME, false);
	int val1 = analogRead(MOIS_SENS);
	//Serial << "\n VAL1: " << val1 << "\n";
	timeddelay(FLIP_TIME, false);
	setSensorPolarity(false);
	timeddelay(FLIP_TIME, false);
	// invert the reading
	int val2 = 1023 - analogRead(MOIS_SENS);
	// Serial << "\n VAL2: " << val2 << "\n";
	// Serial << "\n AVG: " << ((val1 + val2) / 2)<< "\n";
	return abs(100 - ((val1 + val2) / 20));
}

short readRadiation()
{
	if (!sensrad)
		return -1;
	short val = (analogRead(RAD_SENS) - 600) / 4; // diciamo di leggere i valori associati al sensore
	if (val >= 100)
		val = 100;
	//Serial << "\n RAD: " << val << "\n"; // leggiamo i valori che ha il sensore
	return val;
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
	//PrintMemory("JSON");
	//logSD("json", "j.log");
}

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

bool set_device_var(char *str)
{
	if (strtoul(str, NULL, 10) > 0)
		return true;
	else
		return false;
}

/* Generica funzione creata per verificare se un valore (temp, umidità) si trova dentro, sopra o sotto
 * la soglia desiderata */
short check_range(short val, short cmd[], short *norm)
{
	short low = val - cmd[0];
	short high = val - cmd[1];
	if (low < 0)
		return low; // al di sotto dell'intervallo
	if (high > 0)
		return high; // al di sopra dell'intervallo
	if (norm != NULL)
		*norm = ((val - cmd[0]) / (cmd[1] - cmd[0])) * 100;
	return 0; // dentro l'intervallo
}

/* Funzione per il controllo dello stato di irraggiamento */
/* !!! DA RIVEDERE !!! */
// val : valore di irraggiamento
// currenttime: tempo di irraggiamento attuale (global radtime)
// cmdr[] ; array che contiene la soglia di luce e il periodo di luce richiesto (global cmdrad[])
short check_irraggiamento(short val, int unsigned currenttime, short cmdr[])
{
	/*
	   int diff = val - cmd[0];
	   if (diff < 0 && currenttime < cmdrad[1])
	   return abs(diff); // Luminosità al di sotto della soglia dentro il tempo di irraggiamento --> accendere
	   if (currenttime > cmd[1])
	   return -1; // Tempo di irraggiamento raggiunto --> spegnere
	   if (diff > 0)
	   return -2; // Livello di irraggiamento raggiunto --> spegnere
	   return 0;// Condizioni ottimali --> mantenere stato
	 */

	short diff = val - cmdr[0]; // Differenza tra la luce rilevata e la soglia
	// se luce < soglia && daytime è nel lightperiod e radtime è minore del periodo di luce
	if (diff < 0 && daytime > SUNRISE_TIME && currenttime < cmdr[1] * 3600)
		return abs(diff); // Accendi la luce

	if (currenttime > cmdr[1] * 3600)
		return -1; // spegnere le luci

	if (diff > 0)
		return -2; // spegnere le luci

	return 0; // Condizioni ottimali
}


/* LOGICA DI CONTROLLO !LAVORI IN CORSO! */
int logic_control()
{
	/* Informazioni:
	 *	Intervallo TEMP:
	 *	Se la temperatura scende oltre la soglia, si attivano le lampade
	 *	Se la temperatura sale oltre la soglia , si spengono le lampade
	 *		poi si attivano le pompe (rispettando il vincolo di umidita')
	 */
	Serial.print("\nlogic");
	short tempcheck = 0;
	short tempnorm = 0;
	if (cmdtemp[0] != -99 && cmdtemp[1] != -99) // Se il range non è settato la temperatura viene ignorata
		tempcheck = check_range(temperature, cmdtemp, &tempnorm);
	else
		tempnorm = -1;

	if (tempcheck < 0)
	{
		//Serial.print("\n\nTemp sotto soglia: accendere le lamp (se comp. con irraggiamento)\n");
		force_att(&bulb1, BULB1, 1, false);
		force_att(&bulb2, BULB2, 1, false);
	}

	if (tempcheck > 0)
	{
		//Serial.print("\n\nTemp sopra soglia: spegnere le lamp (comp. irraggiamento); attivare pompe (comp. humidità)\n");
		force_att(&bulb1, BULB1, 0, false);
		force_att(&bulb2, BULB2, 0, false);
	}

	if (tempcheck == 0 && tempnorm != -1)
	{
		//Serial.print("\n Temp dentro soglia: spegnere le lampadine se sono accese ");
		if (tempnorm <= 25)
		{
			force_att(&bulb1, BULB1, 1, false); // Se siamo sotto il 25% della temp accendiamo una lamp
			force_att(&bulb2, BULB2, 0, false);
		}
		else
		{
			force_att(&bulb1, BULB1, 0, false); // Se siamo sopra il 25% spegniamo entrmabe le luci
			force_att(&bulb2, BULB2, 0, false);
		}
	}

	/*	Intervallo MOIS:
	 *	Se l'umidita' scende sotto la soglia si attivano le pompe
	 *	Se l'umidita' sale oltre la soglia si attivano le lampade
	 *		(Rispettando i vincoli di irraggiamento e temperatura)
	 */

	short moischeck = 0;
	short moisnorm = 0;
	if (cmdmois[0] != -1 && cmdmois[1] != -1) // Verifico se è settato il range di umidità
		moischeck = check_range(moistness, cmdmois, &moisnorm);
	else
		moisnorm = -1;
	if (moischeck < 0)
	{
		//Serial.print("\n\nUmidità sotto soglia: attivare le pompe (no cond)\n");
		force_att(&pump1, PUMP1, 1, false);
		force_att(&pump2, PUMP2, 1, false);
	}
	if (moischeck > 0)
	{
		//Serial.print("\n\nUmidità sopra soglia: (spegnere le pompe) attivare le lampadine (comp. irraggiamento)\n");
		force_att(&pump1, PUMP1, 0, false);
		force_att(&pump2, PUMP2, 0, false);
	}

	if (moischeck == 0 && moisnorm != -1)
	{
		//Serial.print("\nUmidità dentro la soglia, gestione dell'umidità");
		if (moisnorm <= 25)
		{
			force_att(&pump1, PUMP1, 1, false); // Se siamo sotto il 25% della umidità attiviamo una pompa
			force_att(&pump2, PUMP2, 0, false);
		}
		else
		{
			force_att(&pump1, PUMP1, 0, false); // Se siamo sopra il 25% disattiviamo le pompe
			force_att(&pump2, PUMP2, 0, false);
		}
	}
	/* Tempo di irraggiamento:
	 *	Se la luminosita' scende sotto la soglia prima di aver raggiunto
	 *	il tempo completo di irraggiamento si attivano le lampade secondo la differenza
	 *	di irraggiamento.
	 */

	short radcheck = 0;
	if (cmdrad[0] != -1 && cmdrad[1] != -1)
		radcheck = check_irraggiamento(radiation, radtime, cmdrad);
	else
		radcheck = 0;

	if (radcheck > 0)
	{
		force_att(&bulb1, BULB1, 1, false);
		force_att(&bulb2, BULB2, 1, false);
	}
	if (radcheck < 0)
	{
		force_att(&bulb1, BULB1, 0, false);
		force_att(&bulb2, BULB2, 0, false);
	}
}

/* Funzione per l'impostazione della polarità del sensore di umidità */
void setSensorPolarity(boolean flip)
{
	if(flip)
	{
		digitalWrite(MOIS_SENS_VOLT_1, HIGH);
		digitalWrite(MOIS_SENS_VOLT_2, LOW);
	}
	else
	{
		digitalWrite(MOIS_SENS_VOLT_1, LOW);
		digitalWrite(MOIS_SENS_VOLT_2, HIGH);
	}
}

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
	// Stato dei sensori e attuatori
	senstemp = true;
	sensmois = true;
	sensrad = true;
	//attpump1 = false;
	//attpump2 = false;
	//attbulb1 = false;
	//attbulb2 = false;
	pinMode(PUMP1, OUTPUT);
	pinMode(PUMP2, OUTPUT);
	pinMode(BULB1, OUTPUT);
	pinMode(BULB2, OUTPUT);

	//digitalWrite(PUMP1, LOW);
	//digitalWrite(PUMP2, LOW);
	//digitalWrite(BULB1, LOW);
	//digitalWrite(BULB2, LOW);

	// Set pin per il sensore di Umidità
	if (sensmois)
	{
		pinMode(MOIS_SENS_VOLT_1, OUTPUT);
		pinMode(MOIS_SENS_VOLT_2, OUTPUT);
		pinMode(MOIS_SENS, INPUT);
	}

	if (sensrad)
		pinMode(RAD_SENS, INPUT);


	if (senstemp)
		pinMode(TEMP_SENS, INPUT);

	//Begin Utp connection to NTP server
	// run the memory test function and print the results to the serial port
	//Udp.begin(NTPPORT);
	//syncNTP();
	//logSD("test.txt", "TRY");
	// Init random system.
	randomSeed(analogRead(0) * analogRead(1) * analogRead(2));
	config("me.cfg", 'L');
	//PrintMemory("END_SETUP");
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

		// Unix-time
		unsigned long epoch = (highWord << 16 | lowWord) - 2208988800UL;  

		// Otteniamo l'ora attuale
		// L'ora ottenuta sarà UTC in GMT. Quindi bisogna adattarla al nostro +2h (Ora legale)
		daytime = ((epoch  % 86400L) + TIMEZONE_SECS) % 86400;
		//Serial.print("\n\n ");
		//Serial.print(daytime / 3600);
		//Serial.print("\n : ");
		//Serial.print((daytime % 3600) / 60);
	}
	else
		Serial.print("\nNOPAC");
}

// send an NTP request to the time server at the given address 
unsigned long sendNTPpacket(byte address[], byte packetBuffer[])
{
	// set all bytes in the buffer to 0
	Serial.print("\nSnd");
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
/* Funzione che permette di forzare un attuatore collegato ad una porta digitale */
bool force_att(bool *att_state, int att_digital, int state, bool mod)
{
	//changetime = daytime;
	if (mod)
		force_mod = true; // Abilita il controllo forzato degli attuatori
	if (state == 0)
	{
		//Serial.print("\nLOW");
		*att_state = false;
		digitalWrite(att_digital, LOW);
		return false;
	}
	else
	{
		//Serial.print("\nHIGH");
		*att_state =  true;
		digitalWrite(att_digital, HIGH);
		return true;
	}
}


void loop()
{
	timeddelay(1, true);
	// Il webserver processa le connessioni una alla volta.
	processConnection();
	//Serial.print("\na\n");

	// Fase di lettura dai sensori
	if (sens_flip)
	{
		if(senstemp)
			temperature = readTemperature();
		if(sensmois)
			moistness = readMoistness();
		if(sensrad)
			radiation = readRadiation();

		if (!force_mod) // Se il sistema è sotto il controllo forzato dell'utente
			// non viene effettuato nessun controllo logico sulle variabili
			logic_control();
		sens_flip = false;
	}
	else
		sens_flip = true;
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
		radtime++; // Aggiungere controllo luminosità
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

