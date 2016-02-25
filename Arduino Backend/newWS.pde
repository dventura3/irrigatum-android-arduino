#include <SdFatUtil.h>
#include <Ethernet.h>
#include <SPI.h>

#define PORT 80
#define BUFSIZE 100

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
P(http404P) = "HTTP/1.1 404 Not Found\nContent-Type: text/html\n\n\n<h1>Vaccateli!!</h1>";

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

#define NAMELEN 10
void get(char *url_tail)
{
	char name[NAMELEN];
	char value[NAMELEN];

	if (strlen(url_tail))
	{
		Serial.println("GET");
		//Serial.println(url_tail);
		//Serial.println(strlen(url_tail));
		while (strlen(url_tail))
		{
			if ( nextURLparam(&url_tail, name, NAMELEN, value, NAMELEN) == 0 )
				Serial.print("/n");
			else {
				Serial << "\n" << name << " = " << value;
			}
		}
	}
	PrintMemory("GET");
}


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
			case 0:
				s--;  // Back up to point to terminating NUL
				// Fall through to "stop the scan" code
			case '&':
				/* that's end of pair, go away */
				keep_scanning = false;
				need_value = false;
				break;
			case '+':
				ch = ' ';
				break;
			case '%':
				/* handle URL encoded characters by converting back
				 * to original form */
				if ((hex[0] = *s++) == 0)
				{
					s--;        // Back up to NUL
					keep_scanning = false;
					need_value = false;
				}
				else
				{
					if ((hex[1] = *s++) == 0)
					{
						s--;  // Back up to NUL
						keep_scanning = false;
						need_value = false;
					}
					else
					{
						hex[2] = 0;
						ch = strtoul(hex, NULL, 16);
					}
				}
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
				case 0:
					s--;  // Back up to point to terminating NUL
					// Fall through to "stop the scan" code
				case '&':
					/* that's end of pair, go away */
					keep_scanning = false;
					need_value = false;
					break;
				case '+':
					ch = ' ';
					break;
				case '%':
					/* handle URL encoded characters by converting back to original form */
					if ((hex[0] = *s++) == 0)
					{
						s--;  // Back up to NUL
						keep_scanning = false;
						need_value = false;
					}
					else
					{
						if ((hex[1] = *s++) == 0)
						{
							s--;  // Back up to NUL
							keep_scanning = false;
							need_value = false;
						}
						else
						{
							hex[2] = 0;
							ch = strtoul(hex, NULL, 16);
						}

					}
					break;
			}


			// check against 1 so we don't overwrite the final NUL
			if (keep_scanning && (valueLen > 1))
			{
				*value++ = ch;
				--valueLen;
			}
			else
				result = (result == 1) ?
					3 :
					5;
		}
	}
	*tail = s;
	return result;
}

void processConnection()
{
	char clientline[BUFSIZE];
	int index = 0;

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
				Serial.println(clientline);

				// Look for substring such as a request to get the root file
				if (strstr(clientline, "GET / ") != 0) {
					// send a standard http response header
					printCP(httpSuccessP, client);
					// print all the files, use a helper to keep it clean
					client.println("<h1>Default page</h1>");
					PrintMemory("def");

				}
				else if (strstr(clientline, "GET /get") != 0) {
					printCP(httpSuccessP, client);
					client.println("<h1>GET page</h1>");
					Serial.println(clientline + 9);
					get(clientline + 9);
					PrintMemory("get");

				} else {
					printCP(http404P, client);
				}
				break;
			}
		}
		// give the web browser time to receive the data
		delay(1);
		client.stop();
	}
}


void setup()
{
	ethBegin();
	Serial.begin(9600);
}

void loop()
{
	processConnection();

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
