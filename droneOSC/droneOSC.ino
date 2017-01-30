#include <OSCMessage.h>
#include <WiFiUdp.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <Wire.h>
#include <Adafruit_PWMServoDriver.h>
#include <WiFiManager.h>          //https://github.com/tzapu/WiFiManager WiFi Configuration Magic

const IPAddress outIp(192,168,0,99);        // remote IP of your computer
const unsigned int outPort = 9900;          // remote port to receive OSC
const unsigned int localPort = 9901;        // local port to listen for OSC packets (actually not used for sending)

int counter = 0;
const char* ssid     = "ABANCA-INNOVA";
const char* password = "56K37GPS193FWA74";
const char* host = "192.168.1.254"; // IP address for HTTP request
const int httpPort = 8090;         // Port address for HTTP request
const char* ver = "0.5.0";

#define SERVOMIN  150 // this is the 'minimum' pulse length count (out of 4096)
#define SERVOMAX  600 // this is the 'maximum' pulse length count (out of 4096)

WiFiClient client;
WiFiUDP Udp;                                // A UDP instance to let us send and receive packets over UDP
WiFiManager wifiManager;
char incomingPacket[255];  // buffer for incoming packets
Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver();

char cmd[100]; // stores the command chars received from RoboRemo
int cmdIndex;
int chPin[] = {0, 1}; // ESP pins: GPIO 0, 2, 14, 12
int chVal[] = {1500, 1500}; // default value (middle)

void connectToAp(){
    IPAddress _ip = IPAddress(192, 168, 1, 254);
    IPAddress _gw = IPAddress(192, 168, 1, 1);
    IPAddress _sn = IPAddress(255, 255, 255, 0);
    wifiManager.setSTAStaticIPConfig(_ip, _gw, _sn);

    if (!wifiManager.autoConnect("ABANCA-INNOVA", "56K37GPS193FWA74")) {
      Serial.println("failed to connect, we should reset as see if it connects");
      delay(3000);
      ESP.reset();
      delay(5000);
    }

  //if you get here you have connected to the WiFi

  Serial.println("connected...yeey :)");

  Serial.println("local ip");

  Serial.println(WiFi.localIP());
    
}

boolean cmdStartsWith(const char *st) { // checks if cmd starts with st
  for(int i=0; ; i++) {
    if(st[i]==0) return true;
    if(cmd[i]==0) return false;
    if(cmd[i]!=st[i]) return false;;
  }
  return false;
}
void exeCmd() { // executes the command from cmd

  
  if( cmdStartsWith("ch") ) {
    int ch = cmd[2] - '0';
    int i = (int)atof(cmd+4);
    
    
    if(ch>=0 && ch<=9 && cmd[3]==' ') {
      chVal[ch] = (int)atof(cmd+4);
      Serial.println(chVal[ch]);
      
      if(ch==0){
       // float rudder = map(chVal[ch], 0, 180, SERVOMIN, SERVOMAX);
        pwm.setPWM(ch, 0, chVal[ch]);
      }else if (ch == 1){
        float esc = map(chVal[ch], 0, 180, SERVOMIN, SERVOMAX);
        pwm.setPWM(ch, 0, esc);

        client.flush();
      }
    
    }
  }
}
void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  delay(10);

  connectToAp();


  Serial.println("Starting UDP");
  Udp.begin(localPort);
  Serial.print("Local port: ");
  Serial.println(Udp.localPort());

  Wire.pins(2, 14);
  pwm.begin();
  pwm.setPWMFreq(60);  // Analog servos run at ~60 Hz updates
  
}

void loop() {
  // put your main code here, to run repeatedly:
  int packetSize = Udp.parsePacket();
  if (packetSize)
  {
    // receive incoming UDP packets
    //Serial.printf("Received %d bytes from %s, port %d\n", packetSize, Udp.remoteIP().toString().c_str(), Udp.remotePort());
    //int len = Udp.read(incomingPacket, 255);

    char packetBuffer[packetSize];
    
    Udp.read(packetBuffer,packetSize); // read char from client (RoboRemo app)

    char ultimo = packetBuffer[packetSize-1];

    if(ultimo == '*'){
      //Serial.println("paquete completo");  
      String str(packetBuffer);
      str.remove(packetSize-1);
      //Serial.println(str);  

      
       
      String dVal = getValue(str, ';', 0);
      String gVal = getValue(str, ';', 1);

      float esc = map( gVal.toInt(), 0, 180, SERVOMIN, SERVOMAX);
      pwm.setPWM(0, 0, dVal.toInt());
      pwm.setPWM(1, 0, gVal.toInt());

     // Serial.println(gVal);
     // Serial.println(esc);
    }
  }

}


String getValue(String data, char separator, int index)
{
    int found = 0;
    int strIndex[] = { 0, -1 };
    int maxIndex = data.length() - 1;

    for (int i = 0; i <= maxIndex && found <= index; i++) {
        if (data.charAt(i) == separator || i == maxIndex) {
            found++;
            strIndex[0] = strIndex[1] + 1;
            strIndex[1] = (i == maxIndex) ? i+1 : i;
        }
    }
    return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}
