#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

ESP8266WebServer server(80);

const char* apSsid = "sakht";
const char* apPassword = "12345678";
const int relayPin = D8;
const int buttonPin = D2;

bool relayState = false;
bool buttonState = false;
unsigned long lastButtonPressTime = 0;
const unsigned long debounceDelay = 50;
unsigned long lastServerUpdate = 0;
const unsigned long serverUpdateInterval = 5000;


void setup() {
  // Initialize relay pin
  pinMode(relayPin, OUTPUT);
  digitalWrite(relayPin, LOW);

  Serial.begin(115200);
  Serial.println();
  
  // Start ESP8266 AP
  WiFi.mode(WIFI_AP);
  WiFi.softAP(apSsid, apPassword);

  IPAddress apIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(apIP);

  // Start web server
  server.on("/", HTTP_GET, handleRoot);
  server.on("/on", HTTP_GET, handleOn);
  server.on("/off", HTTP_GET, handleOff);
  server.on("/wifi", HTTP_POST, handleWifiSetup);

  server.begin();
}

void loop() {
  server.handleClient();
}

void handleRoot() {
  String html = "<html><body>";
  html += "<h1>ESP8266 Relay Control</h1>";
  html += "<p>Click <a href='/on'>here</a> to turn the relay ON.</p>";
  html += "<p>Click <a href='/off'>here</a> to turn the relay OFF.</p>";
  html += "<p>Click <a href='/wifi'>here</a> to configure WiFi.</p>";
  html += "</body></html>";
  server.send(200, "text/html", html);
}

void handleOn() {
  digitalWrite(relayPin, HIGH);
  server.send(200, "text/plain", "Relay is ON");
}

void handleOff() {
  digitalWrite(relayPin, LOW);
  server.send(200, "text/plain", "Relay is OFF");
}

void handleWifiSetup() {
  String ssid = server.arg("ssid");
  String password = server.arg("password");

  if (ssid.length() > 0 && password.length() > 0) {
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid.c_str(), password.c_str());
    Serial.println("Connecting to WiFi...");
    
    while (WiFi.status() != WL_CONNECTED) {
      delay(1000);
      Serial.print(".");
    }
    
    Serial.println("\nConnected to WiFi");
    
    server.send(200, "text/plain", "WiFi configured. You can now disconnect and reconnect to the new network.");
  } else {
    server.send(400, "text/plain", "Invalid input. Please provide SSID and password.");
  }
}


void updateButtonState() {
  unsigned long currentMillis = millis();

  if (digitalRead(buttonPin) == LOW) {
    if (!buttonState && currentMillis - lastButtonPressTime >= debounceDelay) {
      buttonState = true;
      lastButtonPressTime = currentMillis;
      relayState = !relayState;
      digitalWrite(relayPin, relayState);
    }
  } else {
    buttonState = false;
  }
}



void updateServer() {
  unsigned long currentMillis = millis();

  if (currentMillis - lastServerUpdate >= serverUpdateInterval) {
    lastServerUpdate = currentMillis;

    // Send current state to the server
    String serverUrl = "http://mikaeel";
    String postData = "state=" + String(relayState);
    httpClient.begin(serverUrl);
    httpClient.addHeader("Content-Type", "application/x-www-form-urlencoded");
    int httpResponseCode = httpClient.POST(postData);
    httpClient.end();

    // Receive updates from the server
    if (httpResponseCode == HTTP_CODE_OK) {
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
