#define WEBDUINO_FAVICON_DATA ""

#include "SPI.h"
#include "Ethernet.h"
#include "WebServer.h"
#include "EthernetUdp.h"

	template<class T>
inline Print &operator <<(Print &obj, T arg)
{ obj.print(arg); return obj; }

// Parametri da settare in base alla configurazione di rete //
static uint8_t mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };

static uint8_t ip[] = { 192, 168, 0, 91 };
IPAddress timeServer(192, 43, 244, 18); // time.nist.gov NTP server
// IPAddress timeServer(217, 194, 13, 90); // server 0.it.pool.ntp.org
IPAddress gatewayIp(192, 168, 0, 1); // Only if useDhcp is false
IPAddress dnsIp(208, 67, 220, 220); // Only if useDhcp is false
IPAddress subnet(255, 255, 255, 0); // Only if useDhcp is false

#define PREFIX ""

WebServer webserver(PREFIX, 80);
#define CONNECTION_BUFFER_SIZE 100
//char buffer[CONNECTION_BUFFER_SIZE];
//int buffer_size = CONNECTION_BUFFER_SIZE;

// Valori di controllo
short cmdtemp[2] = {-99, -99};
short cmdmois[2] = {-1, -1};
short cmdrad[2] = {-1, -1};


// Porte lettura dei sensori
#define TEMP_SENS 5
#define MOIS_SENS 1
#define RAD_SENS 2

#define MOIS_SENS_VOLT_1 6
#define MOIS_SENS_VOLT_2 6
#define FLIP_TIME 1 //secondi

// Porte Attuatori
#define PUMP1 0
#define PUMP2 1
#define BULB1 2
#define BULB2 3

// Stato dei device
bool senstemp = true;
bool sensmois = false;
bool sensrad = false;
bool attpump1 = false;
bool attpump2 = false;
bool attbulb1 = false;
bool attbulb2 = false;

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

long unsigned lightperiod[2] = {SUNRISE_TIME, SUNSET_TIME};

// Tempo di irraggiamento attuale
long unsigned radtime = 0;

// Intervallo di aggiornamento NTP, default 30 minuti (1800 sec)
#define NTP_TIME 600 
#define TIMEZONE_SECS 7200
#define NTPPORT 8888
EthernetUDP Udp;
#define NTP_PACKET_SIZE 48

short readTemperature()
{
	int t = 0;
	if(!senstemp)
		return -1;
	
	return analogRead(TEMP_SENS) * 0.48828125;
}

short readMoistness()
{
	/* IN ATTESA DEL SENSORE */
	return -1;
	/*
	   if (!sensmois)
	   return -1;
	   setSensorPolarity(true);
	//delay(FLIP_TIME);
	timeddelay(FLIP_TIME);
	int val1 = analogRead(MOIS_SENS);
	timeddelay(FLIP_TIME);  
	setSensorPolarity(false);
	timeddelay(FLIP_TIME);
	// invert the reading
	int val2 = 1023 - analogRead(sensorPin);
	return average(val1,val2);
	 */

}

short readRadiation()
{
	/* IN ATTESA DEL SENSORE */
	/*
	   if (!sensrad)
	   return -1;
	 */
	return -1;
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
		else {http://192.168.0.90/json
			strncpy(to, str + start, len);
			to[len] = '\0';
			array[index] = strtoul(to, NULL, 10);
			start = i + 1;
			len = 0;
			index++;
		}
	}
	force_mod = false; // Disabilita il controllo diretto degli attuatori
	return index;
}

void printArray(WebServer &server, short array[], short len, char *arrayname)
{
	server << "\"" << arrayname << "\": [ "; // INIZIO ARRAY
	for (short i = 0; i < len; i++)
	{
		server << array[i];
		if (i != len -1)
			server << " , ";
	}
	server << " ]";
}

/* Funzione associata alla pagina "json". Fornisce al richiedente informazioni sullo stato di arduino,
 * delle sue variabili interne e degli attuatori. Il formato utilizzato per l'output è JSON */
void json(WebServer &server, WebServer::ConnectionType type, char *url_tail, bool tail_complete)
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

	server << "\"temp\": " << temperature << ", ";
	server << "\"mois\": " << moistness << ", ";
	server << "\"rad\": " << radiation << ", ";
	server << "\"pump1\": " << pump1 << ", ";
	server << "\"pump2\": " << pump2 << ", ";
	server << "\"bulb1\": " << bulb1 << ", ";
	server << "\"bulb2\": " << bulb2 << ", ";
	server << "\"daytime\": " << daytime << ", ";
	server << "\"radtime\": " << radtime << ", ";
	//printArray(server, array, arrayLen, "array");
	printArray(server, cmdtemp, 2, "cmdtemp");
	server << ", ";
	printArray(server, cmdmois, 2, "cmdmois");
	server << ", ";
	printArray(server, cmdrad, 2, "cmdrad");

	server << " }"; // FINE JSON
}

#define NAMELEN 20

/* Funzione associata alla pagina "discovery". Invia, mediante JSON, informazioni sui sensori
 * e attuatori attivi e utilizzati dal sistema. */
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
	server << "\"sensrad\": " << (sensrad ? RAD_SENS : -1) << ", ";
	server << "\"attpump1\": " << (attpump1 ? PUMP1 : -1) << ", ";
	server << "\"attpump2\": " << (attpump2 ? PUMP2 : -1) << ", ";
	server << "\"attbulb1\": " << (attbulb1 ? BULB1 : -1) << ", ";
	server << "\"attbulb2\": " << (attbulb2 ? BULB2 : -1);
	server << " }"; // FINE JSON
}

/* Funzione associata alla pagina "get". Contiene il codice per leggere i parametri passati
 * mediante GET ad arduino. Leggere la documentazione per maggiori informazioni sui parametri */
void get(WebServer &server, WebServer::ConnectionType type, char *url_tail, bool tail_complete)
{
	URLPARAM_RESULT rc;
	char name[NAMELEN];
	char value[NAMELEN];
	bool sdisc = false;

	if (strlen(url_tail))
	{
		Serial.println(url_tail);
		Serial.println(strlen(url_tail));
		while (strlen(url_tail))
		{
			rc = server.nextURLparam(&url_tail, name, NAMELEN, value, NAMELEN);
			if (rc == URLPARAM_EOS)
				//server.print("/n");
				Serial.print("/n");
			else
			{
				Serial.print("\n");
				Serial.print(name);
				Serial.print(" : ");
				Serial.print(value);
				Serial.print("\n");

				// Modifica valore variabile
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
						Serial.print("\nerr:array\n");
						cmdtemp[0] = -99;
						cmdtemp[1] = -99;
					}

				if (!strcmp(name, "cmdmois"))
					if(getArray(value, cmdmois, 2) != 2)
					{
						/* Se viene inserito un solo valore
						 * L'array viene resettato */
						Serial.print("\nerr:array\n");
						cmdmois[0] = -1;
						cmdmois[1] = -1;
					}

				if (!strcmp(name, "cmdrad"))
					if(getArray(value, cmdrad, 2) != 2)
					{
						/* Se viene inserito un solo valore
						 * L'array viene resettato */
						Serial.print("\nerr:array\n");
						cmdrad[0] = -1;
						cmdrad[1] = -1;
					}

				if (!strcmp(name, "daytime"))
				{
					short timearray[2] = {0, 0};
					if(getArray(value, timearray, 2) != 2)
					{
						/* Se viene inserito un solo valore
						 * L'array viene resettato */
						Serial.print("\nerr:array\n");
						cmdrad[0] = 0;
						cmdrad[1] = 0;
					}
					else
					{
						Serial.println("time: ");
						Serial.print((short unsigned)timearray[0]);
						Serial.print(" : ");
						Serial.print((short unsigned)timearray[1]);
						daytime = ((long unsigned)timearray[0]) * 3600 + ((long unsigned)timearray[1]) * 60;
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
				 
				   if(!strcmp(name, "discovery"))
					   sdisc = true;

				   if(!strcmp(name, "resetradtime"))
					   radtime = 0;

			}

		}
		if (sdisc)
			discovery(server, type, url_tail, tail_complete);
		else
			json(server, type, url_tail, tail_complete);
	}
	PrintMemory();
}

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
		*norm = (val / (cmd[1] - cmd[0])) * 100;
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
	if (diff < 0 && daytime > lightperiod[1] && currenttime < cmdr[1])
		return abs(diff); // Accendi la luce

	if (currenttime > cmdr[1])
		return -1; // spegnere le luci

	if (diff > 0)
		return -2; // spegnere le luci

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
	Serial.println("Start logic control");
	short tempcheck = 0;
	short tempnorm = 0;
	if (cmdtemp[0] != -99 && cmdtemp[1] != -99)
		tempcheck = check_range(temperature, cmdtemp, &tempnorm);
	else
		tempnorm = -1;
	
	if (tempcheck < 0)
	{
		Serial.print("\nTemp sotto soglia: accendere le lamp (se comp. con irraggiamento)\n");
		force_att(&bulb1, BULB1, 1, false);
		force_att(&bulb2, BULB2, 1, false);
	}

	if (tempcheck > 0)
	{
		Serial.print("\nTemp sopra soglia: spegnere le lamp (comp. irraggiamento); attivare pompe (comp. humidità)\n");
		force_att(&bulb1, BULB1, 0, false);
		force_att(&bulb2, BULB2, 0, false);
	}

	if (tempcheck == 0 && tempnorm != -1)
	{
		Serial.println(" Temp dentro soglia: spegnere le lampadine se sono accese ");
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
		Serial.print("\nUmidità sotto soglia: attivare le pompe (no cond)\n");
		force_att(&pump1, PUMP1, 1, false);
		force_att(&pump2, PUMP2, 1, false);
	}
	if (moischeck > 0)
	{
		Serial.print("\nUmidità sopra soglia: (spegnere le pompe) attivare le lampadine (comp. irraggiamento)\n");
		force_att(&pump1, PUMP1, 0, false);
		force_att(&pump2, PUMP2, 0, false);
	}
	
	if (moischeck == 0 && moisnorm != -1)
	{
		Serial.println("Umidità dentro la soglia, gestione dell'umidità");
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
	/*
	short radcheck = check_irraggiamento(radiation, radtime, cmdrad);
	if (radcheck > 0)
	{
		Serial.print("\n Accendere");
		force_att(&bulb1, BULB1, 1, false);
		force_att(&bulb2, BULB2, 1, false);
	}
	if (radcheck < 0)
	{
		Serial.print("\n Spegnere \n");
		force_att(&bulb1, BULB1, 0, false);
		force_att(&bulb2, BULB2, 0, false);
	}
	*/
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

/* Funzione per generare un approssimazione della media di due interi.
 * Usata nella funzione di lettura del sensore di umidità */
short average(short val1,short val2){

	short avg = (val1 + val2) / 2;

	String msg = "avg: ";
	msg += avg;
	Serial.println(msg);
	return avg;
}

void setup()
{

	Ethernet.begin(mac, ip, dnsIp, gatewayIp, subnet);
	webserver.begin();
	Serial.begin(9600);

	webserver.setDefaultCommand(&json);
	//webserver.addCommand("json", &json);
	webserver.addCommand("get", &get);
	//webserver.addCommand("discovery", &discovery);
	//webserver.addCommand("setdevice", &setupdevices);

	// Stato dei sensori e attuatori
	senstemp = true;
	sensmois = false;
	sensrad = false;
	attpump1 = false;
	attpump2 = false;
	attbulb1 = false;
	attbulb2 = false;

	// Set pin per il sensore di Umidità
	if (sensmois)
	{
		pinMode(MOIS_SENS_VOLT_1, OUTPUT);
		pinMode(MOIS_SENS_VOLT_2, OUTPUT);
		pinMode(MOIS_SENS, INPUT);
	}

        if (senstemp)
        {
                pinMode(TEMP_SENS, INPUT);
        }

	//Begin Utp connection to NTP server
	// run the memory test function and print the results to the serial port
	PrintMemory();
	Udp.begin(NTPPORT);
	syncNTP();
	PrintMemory();

}

void syncNTP()
{
	byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming and outgoing packets 

	sendNTPpacket(timeServer, packetBuffer); // Invia la richiesta al server NTP
	// Attesa dell'arriva del pacchetto
	timeddelay(1, false);  // !! RIVEDERE !!//
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
		daytime = (epoch  % 86400L) + TIMEZONE_SECS;
		Serial.print("\n ");
		Serial.print(daytime / 3600);
		Serial.print(" : ");
		Serial.print((daytime % 3600) / 60);
	}
	else
		Serial.print("\n NO PACKAGE");
}


// send an NTP request to the time server at the given address 
unsigned long sendNTPpacket(IPAddress& address, byte packetBuffer[])
{
	// set all bytes in the buffer to 0
	Serial.print("\n Send packet\n");
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

/* Funzione che permette di forzare un attuatore collegato ad una porta digitale */
bool force_att(bool *att_state, int att_digital, int state, bool mod)
{
	if (mod)
		force_mod = true; // Abilita il controllo forzato degli attuatori
	if (state == 0)
	{
		*att_state = false;
		digitalWrite(att_digital, LOW);
		return false;
	}
	else
	{
		*att_state =  true;
		digitalWrite(att_digital, HIGH);
		return true;
	}
}


void loop()
{
	timeddelay(1, true);
	// Il webserver processa le connessioni una alla volta.
	char buffer[CONNECTION_BUFFER_SIZE];
	int buffer_size = CONNECTION_BUFFER_SIZE;
	webserver.processConnection(buffer, &buffer_size);
	//Serial.print("a\n");

	// Fase di lettura dai sensori
	if(senstemp)
		temperature = readTemperature();
	if(sensmois)
		moistness = readMoistness();
	if(sensrad)
		radiation = readRadiation();

	if (!force_mod) // Se il sistema è sotto il controllo forzato dell'utente
			// non viene effettuato nessun controllo logico sulle variabili
		logic_control();

	PrintMemory();
}

/* Questa funzione permette di aggiungere delay nel codice mantenendo la consistenza
   dei timer deputati al conteggio delle ore del giorno e delle ore di luce */
void timeddelay(int sec, bool ntp)
{
	for (int i = 0; i < sec; i++)
	{
		delay(1000);
		daytime++;
		if (daytime == 86400)
			daytime = 0;
		if (daytime % NTP_TIME == 0 && ntp)
		{
			// Lancia processo di sync con NTP
			Serial.println("USO NTP!!");
			syncNTP();
		}
		radtime++; // Aggiungere controllo luminosità
	}
}

// this function will return the number of bytes currently free in RAM
extern int __bss_end;
extern void *__brkval;

void PrintMemory()
{
	int free_memory;

	if((int)__brkval == 0)
		free_memory = ((int)&free_memory) - ((int)&__bss_end);
	else
		free_memory = ((int)&free_memory) - ((int)__brkval);

	Serial.println("MEM: ");
	Serial.print(free_memory);
	Serial.print("\n");
}
