import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.net.*; 
import net.java.games.input.*; 
import org.gamecontrolplus.*; 
import org.gamecontrolplus.gui.*; 
import processing.serial.*; 
import hypermedia.net.*; 
import controlP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class droneCarUdp extends PApplet {







ControlIO control;
ControlDevice device;



UDP udp;  // define the UDP object
ControlButton buttonReverse;
ControlButton buttonFreno;
ControlButton marchaUp;
ControlButton marchaDown;
ControlHat hat;
ControlSlider direccion, gas;

Client c;
 
float direccionMap = 90;
float gasMap = 90;
float sonido = 1;
float jasBruto = 0.0f;
float rInter = 6.0f;
float trim = -4.5f;

Serial myPort;  // Create object from Serial class

int steeringMax = 501;
int steeringMin = 280;
int maxGas = 98;

int[] marchas={95,98,110,150,180};
int indexMarchas = 0;

boolean frenada = false;


ControlP5 cp5;

int marcha= 0;

int lastMarchaCambio = 0;
int lastSerialMsg = 0;
Knob knobMaxSpeed;

int cmd = 0;
int jmap = 0;


public void setup() {
  
  frameRate(60);
  //background(255);
   noStroke();
   
  cp5 = new ControlP5(this);
  cp5.addSlider("steeringMax").setPosition(20,30).setRange(150,600);
  cp5.addSlider("steeringMin").setPosition(20,60).setRange(150,600);
  cp5.addSlider("maxGas").setPosition(20,90).setRange(90,170);
  cp5.addSlider("trim").setPosition(20,120).setRange(-100,100);
  cp5.addSlider("marcha").setPosition(20,150).setRange(0,marchas.length).setValue(1);
  knobMaxSpeed = cp5.addKnob("maxSpeed").setRange(98,180).setValue(98).setPosition(20,200).setRadius(50).setDragDirection(Knob.VERTICAL);
               
  control = ControlIO.getInstance(this);
  device = control.getDevice("Thrustmaster T500 RS Racing wheel");
    
  direccion = device.getSlider(0);
  gas = device.getSlider(1);
  
  buttonReverse = device.getButton(1);
  buttonFreno = device.getButton(0);
  
  marchaUp = device.getButton(2);
  marchaDown = device.getButton(5);
  
  gas.setMultiplier(-10.0f);
  //gas.setTolerance(0.15);
  //direccion.setMultiplier(10.0);
  //direccion.setTolerance(0.0);
  
  //c = new Client(this, "192.168.1.80", 8080); // Connect to server on port 80
  //c = new Client(this, "192.168.1.254", 9901); // Connect to server on port 80
  udp = new UDP( this, 6000 );
  //udp.log( true );     // <-- printout the connection activity
  udp.listen( true );
  
  myPort = new Serial(this, "COM3", 9600);
  
  lastMarchaCambio = millis();
  lastSerialMsg = millis();
  

}

 
public void draw(){
  background(0);
  
  float direccionRaw = direccion.getValue();
  float exponential = ((1.0f-0.7f)*(direccionRaw*direccionRaw*direccionRaw))+(0.7f*direccionRaw);
  float jas = gas.getValue();
  
  direccionMap = map(exponential, -1.0f, 1.0f, steeringMax, steeringMin);
  sonido =  map(jas,0,10,1,4096);

  
  if(buttonFreno.pressed()){
     gasMap = map(jas*-1,0,-10.0f,90,0);
  }else if(buttonReverse.pressed()){
   // reverse
   gasMap = map(jas*-1,0,-10.0f,91,70);
    
 }else{
    gasMap = map(jas,0,10.0f,90,marchas[indexMarchas]);
 }
 
 if(marchaDown.pressed() && ((millis()-lastMarchaCambio)>400)){
   // bajamos la marcha

   lastMarchaCambio = millis();
   
   if(indexMarchas>0){
     indexMarchas--;
     cp5.getController("marcha").setValue(indexMarchas+1);
     cp5.getController("maxSpeed").setValue(marchas[indexMarchas]);
   }
 }
 
 if(marchaUp.pressed() && ((millis()-lastMarchaCambio)>400)){

   lastMarchaCambio = millis();
   // subimos la marcha
   
   if(indexMarchas<4){
     indexMarchas++;
     cp5.getController("marcha").setValue(indexMarchas+1);
     cp5.getController("maxSpeed").setValue(marchas[indexMarchas]);
   }
 }
  
  
  
    String message  = str( key );  // the message to send
    String ip       = "192.168.1.254";  // the remote IP address
    int port        = 9901;    // the destination port
    
    // formats the message for Pd
    message = "ch0 "+PApplet.parseInt(direccionMap+trim)+"\n";
    
    float j = map(gasMap,0,180,150,600);
    
    udp.send(PApplet.parseInt(direccionMap+trim)+ ";"+PApplet.parseInt(j) +"*", ip, port );
    //udp.send( "ch1:"+int(gasMap+trim)+"\n", ip, port );
  
  
    serialMsg();
}

public void serialMsg(){
  cmd = PApplet.parseInt(sonido);
  jmap = PApplet.parseInt(gasMap);



  myPort.write(cmd+"S");
  myPort.write(jmap+"J");
  myPort.write(indexMarchas+"D");
  lastSerialMsg = millis();
}
  public void settings() {  size(400, 400); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "droneCarUdp" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
