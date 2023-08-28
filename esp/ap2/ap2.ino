#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ESP8266HTTPClient.h>
#include <WiFiClient.h>
#include <EEPROM.h>

#define SSID_ADDRESS 0     // Address to store SSID in EEPROM
#define PASS_ADDRESS 32    // Address to store password in EEPROM
#define MAX_SSID_LENGTH 32 
#define MAX_PASS_LENGTH 64

ESP8266WebServer server(80);

const char* apSsid = "sakht";
const char* apPassword = "12345678";
const int relayPin = D8;
const int buttonPin = D7;

bool relayState = false;
bool buttonState = false;
unsigned long lastButtonPressTime = 0;
const unsigned long debounceDelay = 1000;
unsigned long lastServerUpdate = 0;
const unsigned long serverUpdateInterval = 1000;


void setup() {
  // Initialize relay pin
  pinMode(relayPin, OUTPUT);
  pinMode(buttonPin, INPUT);
  digitalWrite(relayPin, LOW);

  Serial.begin(115200);
  Serial.println();

  // Initialize EEPROM
  EEPROM.begin(512);

  connectToWifi();

  IPAddress apIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(apIP);

  // Start web server
  server.on("/", HTTP_GET, handleRoot);
  server.on("/state", HTTP_GET, handleState);
  server.on("/on", HTTP_GET, handleOn);
  server.on("/off", HTTP_GET, handleOff);
  server.on("/wifi", HTTP_POST, handleWifiSetup);

  server.begin();
}

void loop() {
  server.handleClient();
  updateButtonState();
  updateServer();
}

void handleRoot() {
  String html = "<html><body>";
  html += "<h1>ESP8266 Relay Control</h1>";
  html += "<p>Click <a href='/state'>here</a> to get currect state.</p>";
  html += "<p>Click <a href='/on'>here</a> to turn the relay ON.</p>";
  html += "<p>Click <a href='/off'>here</a> to turn the relay OFF.</p>";
  html += "<p>Click <a href='/wifi'>here</a> to configure WiFi.</p>";
  html += "</body></html>";
  server.send(200, "text/html", html);
}


void handleState() {
  server.send(200, "text/plain", (relayState?"Relay is OFF" : "Relay is ON"));
}

void handleOn() {
  digitalWrite(relayPin, LOW);
  server.send(200, "text/plain", "Relay is ON");
}

void handleOff() {
  digitalWrite(relayPin, HIGH);
  server.send(200, "text/plain", "Relay is OFF");
}

void handleWifiSetup() {
  String ssid = server.arg("ssid");
  String password = server.arg("password");

  if (ssid.length() > 0 && password.length() > 0) {
    EEPROM.put(SSID_ADDRESS, ssid.c_str());
    EEPROM.put(PASS_ADDRESS, password.c_str());
    EEPROM.commit();
    
    connectToWifi();
    server.send(200, "text/plain", "WiFi configured. You can now disconnect and reconnect to the new network.");
  } else {
    server.send(400, "text/plain", "Invalid input. Please provide SSID and password.");
  }
}

void hotspot(){
    // Start ESP8266 AP
    WiFi.mode(WIFI_AP);
    WiFi.softAP(apSsid);
    Serial.println("\nHotspot");
}

void connectToWifi(){
    // Load SSID and password from EEPROM
    char storedSSID[MAX_SSID_LENGTH];
    char storedPass[MAX_PASS_LENGTH];
    EEPROM.get(SSID_ADDRESS, storedSSID);
    EEPROM.get(PASS_ADDRESS, storedPass);
  
    if(strlen(storedSSID)==0){
      hotspot();
      return;
    }

    WiFi.mode(WIFI_STA);
    WiFi.begin(storedSSID, storedPass);
    Serial.println("Connecting to WiFi...");

    int limit=10;
    while (WiFi.status() != WL_CONNECTED && limit>0) {
      delay(1000);
      Serial.print(".");
      limit--;
    }

    if(WiFi.status() != WL_CONNECTED){
      hotspot();
      delay(10000);
      Serial.println("\nStart:");
    }else{
      Serial.println("\nConnected to WiFi");
    }
}


void updateButtonState() {
  unsigned long currentMillis = millis();


  if (digitalRead(buttonPin) == HIGH) {
    if (!buttonState && currentMillis - lastButtonPressTime >= debounceDelay) {
      buttonState = true;
      lastButtonPressTime = currentMillis;
      relayState = !relayState;
      Serial.println(relayState);
      digitalWrite(relayPin, relayState);
    }
  } else {
    buttonState = false;
  }
}



void updateServer() {
  unsigned long currentMillis = millis();

  if (currentMillis - lastServerUpdate >= serverUpdateInterval) {
    Serial.println("I want to update server");
    lastServerUpdate = currentMillis;

    // Send current state to the server
    String serverUrl = "http://192.168.4.2:8000/socket_status?status=" + String(relayState);
    Serial.println(serverUrl);

    String postData = "status=" + String(relayState);
    lastServerUpdate = currentMillis;

    WiFiClient client;

    HTTPClient httpClient;

    httpClient.begin(client, serverUrl);
    httpClient.addHeader("Content-Type", "application/json");
    int httpResponseCode = httpClient.POST(postData);
    httpClient.end();

    Serial.println(httpResponseCode);
    // Receive updates from the server
    Serial.println(httpClient.getString());
    if (httpResponseCode == HTTP_CODE_OK) {
      Serial.println("update server successfully");
      String response = httpClient.getString();
      if (response == "ON") {
        relayState = true;
        digitalWrite(relayPin, HIGH);
      } else if (response == "OFF") {
        relayState = false;
        digitalWrite(relayPin, LOW);
      }
    }
  }
}
