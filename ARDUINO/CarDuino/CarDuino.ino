#include <ESP8266WiFi.h>
#include <WiFiUdp.h>


const char* ssid = "1234";
const char* password = "qwertz1234";

#define bufferSize 255
#define new_frequency 10  //use PWM frequency optimised for your hardware
#define PWMN 5            //forward, D2
#define PWMR 16           //backward, D1
#define PWML 0            //left, D3
#define PWMD 4            //right, D4
#define localUdpPort 4210 //right, D4


struct _pos;
typedef struct _pos* pos;
typedef struct _pos {
  size_t forward;
  size_t backward;
  size_t left;
  size_t right;  
} CarPostion;


WiFiUDP Udp;

char* incomingPacket;  // buffer for incoming packets


int packetSize;
int len;
pos data;

void setup()
{
  delay(1000);

  Serial.begin(115200);

  incomingPacket = NULL;
  data = NULL;

  incomingPacket = (char*)malloc(bufferSize * sizeof(char));
  data = (pos)malloc(sizeof(CarPostion));

  if (incomingPacket == NULL)
    Serial.printf("Failed to allocate memory.\n");
  if (data == NULL)
    Serial.printf("Failed to allocate memory.\n");

  analogWriteFreq(new_frequency);
  pinMode(PWMN, OUTPUT);
  pinMode(PWMR, OUTPUT);
  pinMode(PWML, OUTPUT);
  pinMode(PWMD, OUTPUT);

  analogWrite(PWMN, 0);
  analogWrite(PWMR, 0);
  analogWrite(PWML, 0);
  analogWrite(PWMD, 0);


  Serial.printf("Connecting to %s ", ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" connected");
  Udp.begin(localUdpPort);
  Udp.flush();
  Serial.printf("Now listening at IP %s, UDP port %d\n", WiFi.localIP().toString().c_str(), localUdpPort);

  
}


void loop()
{
  packetSize = Udp.parsePacket();
  if (packetSize)
  {
    len = Udp.read(incomingPacket, bufferSize);
    if (len > 0)
    {
      incomingPacket[len] = 0;
    }
    //Serial.printf("Packet size: %d\nPacket: %s\n",len,incomingPacket);
    sscanf(incomingPacket, "%u&%u:%u&%ue", &(data->forward), &(data->backward), &(data->left), &(data->right));
    changeOfDirection();
  }

  delay (10);
}

inline void changeOfDirection()
{
  if (data->forward != data->backward)
  {
    if (data->backward == 0) {
      analogWrite(PWMR, 0);
      analogWrite(PWMN, data->forward);
    }
    if (data->forward == 0) {
      analogWrite(PWMN, 0);
      analogWrite(PWMR, data->backward);
    }
  }
  else
  {
    analogWrite(PWMN, 0);
    analogWrite(PWMR, 0);
  }

  if (data->left != data->right)
  {
    if (data->right == 0) {
      analogWrite(PWMD, 0);
      analogWrite(PWML, data->left);
    }
    if (data->left == 0) {
      analogWrite(PWML, 0);
      analogWrite(PWMD, data->right);
    }
  }

  else
  {
    analogWrite(PWML, 0);
    analogWrite(PWMD, 0);
  }

}
