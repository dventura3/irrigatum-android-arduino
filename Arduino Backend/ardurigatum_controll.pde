#include "SoftwareSerial.h"

#define P(name)   static const prog_uchar name[] PROGMEM
#define RX 2
#define TX 3

SoftwareSerial mySerial(RX, TX); // RX, TX

#define BUFSIZE 100
#define NAMELEN 15


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
#define PUMP1 11
#define PUMP2 12
#define BULB1 6
#define BULB2 5

// Stato dei device
bool senstemp = false;
bool sensmois = false;
bool sensrad = false;

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

	template<class T>
inline Print &operator <<(Print &obj, T arg)
{ obj.print(arg); return obj; }

void printCP(const prog_uchar *str, SoftwareSerial mySerial)
{
	char c;
	while((c = pgm_read_byte(str++)))
		mySerial.print(c);
}

byte serialServer()
{
	char buf[100];
	byte i = 0;
	char data;
	while (mySerial.available()) {
		data = (char) mySerial.read();
		if (data != '#')
			buf[i++] = data;
		else {
			buf[i] = 0;
			if (!strcmp(buf, "json"))
				mySerial.flush();
				json();
			if (!strcmp(buf, "get")) {
				i = 0;
				while(mySerial.available()) {
					data = mySerial.read();
					if (data != '#')
						buf[i++] = data;
				}
			}
				
		}
		Serial.print(buf[i - 1]);
	}
}
/* Funzione associata alla pagina "get". Contiene il codice per leggere i parametri passati
 * mediante GET ad arduino. Leggere la documentazione per maggiori informazioni sui parametri */
void get(SoftwareSerial mySerial, char *url_tail)
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
			}
end:
			;
		}
		//if (sdisc)
		//	discovery(server, type, url_tail, tail_complete);
		//else
		//	json();
		json();
	}
	PrintMemory("GET");
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
	Serial << "\nR:" << analogRead(RAD_SENS);
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

void printArray(SoftwareSerial mySerial, short array[], short len, const prog_uchar *str)
{
	printCP(str, mySerial);
	for (short i = 0; i < len; i++)
	{
		mySerial << array[i];
		if (i != len -1)
			mySerial << ",";
	}
	mySerial << "]";
}

/* Funzione associata alla pagina "json". Fornisce al richiedente informazioni sullo stato di arduino,
 * delle sue variabili interne e degli attuatori. Il formato utilizzato per l'output è JSON */
void json()
{
	mySerial << "{"; // INIZIO JSON
	printCP(tempP, mySerial);
	mySerial << "" << temperature << ",";
	printCP(moisP, mySerial);
	mySerial << moistness << ",";
	printCP(radP, mySerial);
	mySerial << radiation << ",";
	printCP(pump1P, mySerial);
	mySerial << pump1 << ",";
	printCP(pump2P, mySerial);
	mySerial << pump2 << ",";
	printCP(bulb1P, mySerial);
	mySerial << "" << bulb1 << ",";
	printCP(bulb2P, mySerial);
	mySerial << bulb2 << ",";
	printCP(daytimeP, mySerial);
	mySerial << daytime << ",";
	printCP(radtimeP, mySerial);
	mySerial << radtime << ",";
	//printCP(changetimeP, mySerial);
	//mySerial << changetime << ",";
	printCP(resultP, mySerial);
	mySerial << "\"OK\",";
	//printArray(mySerial, array, arrayLen, "array");
	printArray(mySerial, cmdtemp, 2, cmdtempP);
	mySerial << ",";
	printArray(mySerial, cmdmois, 2, cmdmoisP);
	mySerial << ",";
	printArray(mySerial, cmdrad, 2, cmdradP);

	mySerial << "}"; // FINE JSON
	PrintMemory("JSON");
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

	Serial.begin(9600);
	mySerial.begin(4800);
	// Stato dei sensori e attuatori
	senstemp = true;
	sensmois = true;
	sensrad = true;
	pinMode(PUMP1, OUTPUT);
	pinMode(PUMP2, OUTPUT);
	pinMode(BULB1, OUTPUT);
	pinMode(BULB2, OUTPUT);

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

	PrintMemory("END_SETUP");
}

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
	serialServer();
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
		radtime++; // Aggiungere controllo luminosità
		if (daytime >= 86400) {
			daytime = 0;
			radtime = 0;
		}
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

