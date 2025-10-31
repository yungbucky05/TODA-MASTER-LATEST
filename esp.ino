#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <WiFiClientSecure.h>
#include <time.h>
#include <Wire.h>
#include <SPI.h>
#include <Adafruit_PN532.h>
#include <LiquidCrystal_I2C.h>

// WiFi credentials
#define WIFI_SSID "George"
#define WIFI_PASSWORD "Aixroch!092601"

// Firebase credentials
#define API_KEY "AIzaSyAbEJ8tzm5RIeXvKhlxZl71a_tBX4WmN4E"
#define DATABASE_URL "https://toda-contribution-system-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL "test@example.com"
#define USER_PASSWORD "test123456"

// I2C LCD configuration
#define LCD_SDA 21
#define LCD_SCL 22
#define LCD_ADDRESS 0x27  // Common I2C address for LCD, may need to change to 0x3F

// PN532 SPI configuration
#define PN532_SCK  18
#define PN532_MOSI 23
#define PN532_SS   5
#define PN532_MISO 19

// Serial communication to Arduino (TX only)
#define ARDUINO_TX 17

// Coin pulse input pin (coin acceptor signal wire connected directly to ESP32)
#define COIN_PULSE_PIN 32

// Create instances
Adafruit_PN532 nfc(PN532_SCK, PN532_MISO, PN532_MOSI, PN532_SS);
LiquidCrystal_I2C lcd(LCD_ADDRESS, 16, 2);  // 16x2 LCD display

// Firebase objects
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// System state
bool coinSlotEnabled = false;
bool nfcEnabled = true;
unsigned long lastNFCCheck = 0;
const unsigned long NFC_CHECK_INTERVAL = 100;
unsigned long totalSavings = 0;

// Coin pulse detection (direct on ESP32)
volatile unsigned long coinLastPulseUs = 0;
// Count only VALID pulses based on width
volatile uint16_t coinValidPulses = 0;
// Active level configuration: runtime-tunable (true = active LOW, false = active HIGH)
volatile bool coinActiveLow = true;
// Track active state to measure pulse width
volatile bool coinActive = false;
volatile unsigned long coinActiveStartUs = 0;
volatile unsigned long lastValidPulseWidthUs = 0;
volatile uint32_t coinRawEdges = 0; // counts all CHANGE edges for diagnostics

// Timing thresholds (runtime-tunable)
volatile unsigned long coinGapUs = 350000UL;            // end of burst when quiet > 350ms
volatile unsigned long coinPowerStabilizeMs = 4000UL;   // wait 4s after power on
volatile unsigned long minPulseUs = 2000UL;             // min active width = 2ms (filter spikes)
volatile unsigned long maxPulseUs = 200000UL;           // max active width = 200ms (filter long noise)
volatile uint8_t minBurstPulses = 1;                    // require N valid pulses before accepting a coin
unsigned long coinSlotPowerOnTime = 0;

// Add RFID debounce variables
String lastScannedUID = "";
unsigned long lastScanTime = 0;
const unsigned long SCAN_DEBOUNCE_TIME = 3000; // 3 seconds between same card scans
bool isProcessingRFID = false;

// Add Firebase timeout variables
unsigned long firebaseTimeout = 10000; // 10 seconds timeout

// Driver information structure
struct Driver {
    String rfidUID;
    String todaNumber;
    String driverName;
    bool isRegistered;
};

// Global variable to store current driver info
Driver currentDriver;

// Queue management
String firstDriverQueueKey = "";

// Function forward declarations
void checkForRFIDCard();
void checkDriverRegistration(String rfidUID);
void enableCoinSlot(Driver driver);
void processCoinContribution();
void processContributionAndQueue();
void recordContribution();
void addDriverToQueue();
void showError(String message);
void updateLCDDisplay(String todaNumber, String message);
void clearLCDDisplay();
String getCurrentDate();
String getCurrentTime();

// ISR for coin pulse
void IRAM_ATTR coinISR() {
    int level = digitalRead(COIN_PULSE_PIN);
    unsigned long nowUs = micros();

    coinRawEdges++;

    bool isActive = coinActiveLow ? (level == LOW) : (level == HIGH);

    if (isActive) {
        if (!coinActive) {
            coinActive = true;
            coinActiveStartUs = nowUs;
        }
    } else { // inactive edge
        if (coinActive) {
            unsigned long width = nowUs - coinActiveStartUs;
            coinActive = false;
            // Validate pulse width
            if (width >= minPulseUs && width <= maxPulseUs) {
                coinValidPulses++;
                coinLastPulseUs = nowUs;
                lastValidPulseWidthUs = width;
            }
        }
    }
}

void setup() {
    Serial.begin(115200);
    // Initialize Serial2 as TX-only (rxPin = -1) to command Arduino, no RX wire connected
    Serial2.begin(9600, SERIAL_8N1, -1, ARDUINO_TX);

    // Initialize coin pulse input
    pinMode(COIN_PULSE_PIN, INPUT_PULLUP);
    // Use CHANGE to measure pulse width accurately
    attachInterrupt(digitalPinToInterrupt(COIN_PULSE_PIN), coinISR, CHANGE);

    // Initialize I2C for LCD with custom pins
    Wire.begin(LCD_SDA, LCD_SCL);

    // Initialize LCD
    lcd.init();
    lcd.backlight();
    lcd.clear();

    // Display startup message
    lcd.setCursor(0, 0);
    lcd.print("TODA System");
    lcd.setCursor(0, 1);
    lcd.print("Starting...");

    Serial.println("ESP32 ready: TODA Contribution & Queueing System");
    Serial.println("Serial2 initialized: TX=17 (TX-only, no RX)");
    Serial.print("LCD initialized: SDA=Pin");
    Serial.print(LCD_SDA);
    Serial.print(", SCL=Pin");
    Serial.println(LCD_SCL);
    Serial.println("Coin pulse input on pin 16 with interrupt (CHANGE)");
    Serial.print("Coin polarity: active-");
    Serial.println(coinActiveLow ? "LOW" : "HIGH");

    // Initialize WiFi
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(300);
        Serial.print(".");
    }
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());

    // Configure time (needed for Firebase)
    configTime(8 * 3600, 0, "pool.ntp.org", "time.nist.gov");  // GMT+8 Philippines

    // Wait for time to be set
    Serial.print("Waiting for NTP time sync");
    time_t now = time(nullptr);
    while (now < 8 * 3600 * 2) {
        delay(500);
        Serial.print(".");
        now = time(nullptr);
    }
    Serial.println();
    Serial.println("Time synchronized");

    // Initialize Firebase
    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;

    // Use email/password authentication
    Serial.println("Initializing Firebase with email authentication...");

    // Set user credentials
    auth.user.email = USER_EMAIL;
    auth.user.password = USER_PASSWORD;

    // Assign the callback function for the long running token generation task
    config.token_status_callback = tokenStatusCallback;

    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

    // Set timeout
    fbdo.setResponseSize(4096);

    Serial.println("Firebase configuration complete");

    // Wait for Firebase authentication
    Serial.print("Waiting for Firebase authentication");
    unsigned long startTime = millis();
    while (!Firebase.ready() && (millis() - startTime < 30000)) {
        delay(1000);
        Serial.print(".");
    }

    if (Firebase.ready()) {
        Serial.println();
        Serial.println("✓ Firebase authenticated and ready!");

        // Test database connection
        if (Firebase.RTDB.get(&fbdo, "/drivers")) {
            Serial.println("✓ Database connection successful!");
        } else {
            Serial.println("Database test result: " + fbdo.errorReason());
        }
    } else {
        Serial.println();
        Serial.println("✗ Firebase authentication failed");
    }

    // Initialize PN532
    nfc.begin();
    uint32_t versiondata = nfc.getFirmwareVersion();
    if (!versiondata) {
        Serial.println("PN532 not found");
    } else {
        Serial.print("Found PN532 with firmware version: ");
        Serial.println((versiondata >> 16) & 0xFF, HEX);
        nfc.SAMConfig();
        Serial.println("PN532 ready, waiting for driver RFID...");
    }


    Serial.println("System ready: Please scan driver RFID card to start contribution process");

    // Update LCD with system ready message
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("System Ready");
    lcd.setCursor(0, 1);
    lcd.print("Scan RFID Card");
}

void loop() {
    // DIRECT COIN PULSE HANDLING (no Serial2 RX)
    if (coinSlotEnabled) {
        // Check if a pulse burst has finished
        noInterrupts();
        uint16_t pulses = coinValidPulses;
        unsigned long lastUs = coinLastPulseUs;
        bool activeNow = coinActive;
        unsigned long lastWidth = lastValidPulseWidthUs;
        interrupts();

        // Only act after initial power stabilization
        if (millis() - coinSlotPowerOnTime < coinPowerStabilizeMs) {
            // Clear any early pulses as noise during stabilization
            if (pulses > 0 || activeNow) {
                noInterrupts();
                coinValidPulses = 0;
                coinActive = false;
                interrupts();
            }
        } else {
            // When we have a sufficient number of valid pulses and quiet period, process
            if (pulses >= minBurstPulses && (micros() - lastUs > coinGapUs) && !activeNow) {
                Serial.print("Coin burst validated - pulses: ");
                Serial.print(pulses);
                Serial.print(", last width(us): ");
                Serial.println(lastWidth);

                // Reset counters before processing to avoid re-entry
                noInterrupts();
                coinValidPulses = 0;
                coinActive = false;
                interrupts();

                processCoinContribution();
            }
        }
    } else {
        // If disabled, clear any accumulated pulses silently
        if (coinValidPulses > 0 || coinActive) {
            noInterrupts();
            coinValidPulses = 0;
            coinActive = false;
            interrupts();
        }
    }

    // Check for NFC cards (only when enabled)
    if (nfcEnabled && (millis() - lastNFCCheck >= NFC_CHECK_INTERVAL)) {
        lastNFCCheck = millis();
        checkForRFIDCard();
    }

    // Debug system status every 10 seconds
    static unsigned long lastSystemStatus = 0;
    if (millis() - lastSystemStatus > 10000) {
        Serial.println("System Status - NFC: " + String(nfcEnabled ? "ON" : "OFF") +
                       ", CoinSlot: " + String(coinSlotEnabled ? "ON" : "OFF") +
                       ", ProcessingRFID: " + String(isProcessingRFID ? "YES" : "NO"));
        lastSystemStatus = millis();
    }

    // Check for manual commands via Serial Monitor (for debugging)
    if (Serial.available()) {
        String command = Serial.readString();
        command.trim();
        command.toLowerCase();

        if (command == "enable") {
            Serial.println("Manually enabling coin slot...");
            Driver testDriver;
            testDriver.rfidUID = "MANUAL";
            testDriver.driverName = "Manual Test";
            testDriver.todaNumber = "TEST";
            testDriver.isRegistered = true;
            enableCoinSlot(testDriver);
        } else if (command == "disable") {
            Serial.println("Manually disabling coin slot...");
            coinSlotEnabled = false;
            nfcEnabled = true;
            Serial2.write((uint8_t)201);
            Serial2.flush();
            // Clear any pending coin pulses
            noInterrupts();
            coinValidPulses = 0;
            coinActive = false;
            interrupts();
            Serial.println("Coin slot disabled, NFC re-enabled");
        } else if (command == "status") {
            Serial.println("\n=== SYSTEM STATUS ===");
            Serial.println("NFC Enabled: " + String(nfcEnabled ? "true" : "false"));
            Serial.println("Coin Slot Enabled: " + String(coinSlotEnabled ? "true" : "false"));
            Serial.println("Processing RFID: " + String(isProcessingRFID ? "true" : "false"));
            Serial.println("WiFi Status: " + String(WiFi.status() == WL_CONNECTED ? "Connected" : "Disconnected"));
            Serial.println("Firebase Ready: " + String(Firebase.ready() ? "true" : "false"));
            Serial.println("==================");
        } else if (command == "test200") {
            Serial.println("Sending command 200 to Arduino...");
            Serial2.write((uint8_t)200);
            Serial2.flush();
            Serial.println("Command 200 sent!");
        } else if (command == "test201") {
            Serial.println("Sending command 201 to Arduino...");
            Serial2.write((uint8_t)201);
            Serial2.flush();
            Serial.println("Command 201 sent!");
        } else if (command == "queue") {
            Serial.println("Checking driver queue status...");
            checkQueueStatus();

        } else if (command.startsWith("coinpol")) {
            if (command.indexOf("high") != -1) {
                coinActiveLow = false;
                Serial.println("Coin polarity set to ACTIVE-HIGH");
            } else if (command.indexOf("low") != -1) {
                coinActiveLow = true;
                Serial.println("Coin polarity set to ACTIVE-LOW");
            } else {
                Serial.println("Usage: coinpol low|high");
            }
        } else if (command == "coinstat") {
            noInterrupts();
            uint16_t pulses = coinValidPulses;
            bool activeNow = coinActive;
            unsigned long lastWidth = lastValidPulseWidthUs;
            uint32_t edges = coinRawEdges;
            int pinLevel = digitalRead(COIN_PULSE_PIN);
            interrupts();
            Serial.println("=== COIN INPUT STATUS ===");
            Serial.print("Polarity: ACTIVE-"); Serial.println(coinActiveLow ? "LOW" : "HIGH");
            Serial.print("Pin level: "); Serial.println(pinLevel == HIGH ? "HIGH" : "LOW");
            Serial.print("Valid pulses (since enable): "); Serial.println(pulses);
            Serial.print("Raw edges (since boot): "); Serial.println(edges);
            Serial.print("Last valid pulse width (us): "); Serial.println(lastWidth);
            Serial.print("Active now: "); Serial.println(activeNow ? "YES" : "NO");
            Serial.print("Millis since power-on of slot: "); Serial.println(millis() - coinSlotPowerOnTime);
            Serial.println("=========================");
        } else if (command.startsWith("coinpull")) {
            if (command.indexOf("up") != -1) {
                detachInterrupt(digitalPinToInterrupt(COIN_PULSE_PIN));
                pinMode(COIN_PULSE_PIN, INPUT_PULLUP);
                attachInterrupt(digitalPinToInterrupt(COIN_PULSE_PIN), coinISR, CHANGE);
                Serial.println("Coin pin mode: INPUT_PULLUP");
            } else if (command.indexOf("down") != -1) {
                detachInterrupt(digitalPinToInterrupt(COIN_PULSE_PIN));
                pinMode(COIN_PULSE_PIN, INPUT_PULLDOWN);
                attachInterrupt(digitalPinToInterrupt(COIN_PULSE_PIN), coinISR, CHANGE);
                Serial.println("Coin pin mode: INPUT_PULLDOWN");
            } else if (command.indexOf("float") != -1) {
                detachInterrupt(digitalPinToInterrupt(COIN_PULSE_PIN));
                pinMode(COIN_PULSE_PIN, INPUT);
                attachInterrupt(digitalPinToInterrupt(COIN_PULSE_PIN), coinISR, CHANGE);
                Serial.println("Coin pin mode: INPUT (floating)");
            } else {
                Serial.println("Usage: coinpull up|down|float");
            }
        } else if (command.startsWith("coinset")) {
            // coinset min 500 | max 300000 | gap 400000 | stab 3000 | burst 1
            // parse simple tokens
            if (command.indexOf("min") != -1) {
                long v = command.substring(command.lastIndexOf(" ") + 1).toInt();
                if (v > 50 && v < 1000000) { minPulseUs = (unsigned long)v; Serial.print("minPulseUs="); Serial.println(minPulseUs); }
                else Serial.println("min must be 50..1000000 us");
            } else if (command.indexOf("max") != -1) {
                long v = command.substring(command.lastIndexOf(" ") + 1).toInt();
                if (v > 100 && v < 2000000) { maxPulseUs = (unsigned long)v; Serial.print("maxPulseUs="); Serial.println(maxPulseUs); }
                else Serial.println("max must be 100..2000000 us");
            } else if (command.indexOf("gap") != -1) {
                long v = command.substring(command.lastIndexOf(" ") + 1).toInt();
                if (v > 1000 && v < 3000000) { coinGapUs = (unsigned long)v; Serial.print("coinGapUs="); Serial.println(coinGapUs); }
                else Serial.println("gap must be 1000..3000000 us");
            } else if (command.indexOf("stab") != -1) {
                long v = command.substring(command.lastIndexOf(" ") + 1).toInt();
                if (v >= 0 && v < 30000) { coinPowerStabilizeMs = (unsigned long)v; Serial.print("coinPowerStabilizeMs="); Serial.println(coinPowerStabilizeMs); }
                else Serial.println("stab must be 0..30000 ms");
            } else if (command.indexOf("burst") != -1) {
                long v = command.substring(command.lastIndexOf(" ") + 1).toInt();
                if (v >= 1 && v <= 10) { minBurstPulses = (uint8_t)v; Serial.print("minBurstPulses="); Serial.println(minBurstPulses); }
                else Serial.println("burst must be 1..10");
            } else {
                Serial.println("Usage: coinset min|max|gap|stab|burst <value>");
            }
        } else if (command == "coinshow") {
            Serial.println("=== COIN SETTINGS ===");
            Serial.print("Polarity: ACTIVE-"); Serial.println(coinActiveLow ? "LOW" : "HIGH");
            Serial.print("Pin mode: ");
            // can't directly read mode; show suggestion
            Serial.println("see last coinpull command");
            Serial.print("minPulseUs="); Serial.println(minPulseUs);
            Serial.print("maxPulseUs="); Serial.println(maxPulseUs);
            Serial.print("coinGapUs="); Serial.println(coinGapUs);
            Serial.print("coinPowerStabilizeMs="); Serial.println(coinPowerStabilizeMs);
            Serial.print("minBurstPulses="); Serial.println(minBurstPulses);
            Serial.println("=====================");
        }
    }
}

void checkQueueStatus() {
    Serial.println("\n=== DRIVER QUEUE STATUS ===");

    if (Firebase.RTDB.get(&fbdo, "/queue")) {
        if (fbdo.dataType() == "json") {
            String jsonStr = fbdo.jsonString();

            if (jsonStr == "null" || jsonStr.length() == 0) {
                Serial.println("Queue is empty");
            } else {
                Serial.println("Queue data:");

                // Parse and display queue entries with readable timestamps
                int driverCount = 0;
                int pos = 0;
                while ((pos = jsonStr.indexOf("\"driverName\"", pos)) != -1) {
                    driverCount++;

                    // Find the driver name
                    int nameStart = jsonStr.indexOf("\"", pos + 12) + 1;
                    int nameEnd = jsonStr.indexOf("\"", nameStart);
                    String driverName = jsonStr.substring(nameStart, nameEnd);

                    // Find the timestamp (look backwards for the key)
                    int keyEnd = jsonStr.lastIndexOf("\":", pos);
                    int keyStart = jsonStr.lastIndexOf("\"", keyEnd - 1) + 1;
                    String queueKey = jsonStr.substring(keyStart, keyEnd);

                    // Find the readable timestamp
                    int timestampPos = jsonStr.indexOf("\"timestamp\":", pos);
                    String readableTime = "";
                    if (timestampPos != -1 && timestampPos < jsonStr.indexOf("\"driverName\"", pos + 1)) {
                        int timeStart = jsonStr.indexOf("\"", timestampPos + 12) + 1;
                        int timeEnd = jsonStr.indexOf("\"", timeStart);
                        readableTime = jsonStr.substring(timeStart, timeEnd);
                    }

                    Serial.println("Driver #" + String(driverCount) + ": " + driverName +
                                   " (Queue: " + queueKey + ", Time: " + readableTime + ")");

                    pos++;
                }
                Serial.println("Total drivers in queue: " + String(driverCount));
            }
        } else {
            Serial.println("Queue data type: " + fbdo.dataType());
        }
    } else {
        // Firebase removes empty nodes, so "path not exist" means no drivers in queue
        if (fbdo.errorReason().indexOf("path not exist") != -1 ||
            fbdo.errorReason().indexOf("not found") != -1 ||
            fbdo.httpCode() == 404) {
            Serial.println("No drivers available in queue");
        } else {
            Serial.println("✗ Failed to get queue: " + fbdo.errorReason());
        }
    }

    Serial.println("===========================\n");
}




void checkForRFIDCard() {
    // Don't check for RFID if we're already processing one
    if (isProcessingRFID) {
        return;
    }

    uint8_t uid[7];
    uint8_t uidLength;

    if (nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength)) {
        // Convert UID to string
        String uidString = "";
        for (uint8_t i = 0; i < uidLength; i++) {
            if (uid[i] < 0x10) uidString += "0";
            uidString += String(uid[i], HEX);
        }
        uidString.toUpperCase();

        // Check for debounce - prevent same card from being processed too quickly
        if (uidString == lastScannedUID && (millis() - lastScanTime < SCAN_DEBOUNCE_TIME)) {
            return; // Ignore duplicate scan within debounce time
        }

        Serial.print("Driver RFID scanned: ");
        Serial.println(uidString);

        // Update debounce variables
        lastScannedUID = uidString;
        lastScanTime = millis();
        isProcessingRFID = true;

        // Check driver registration in database
        checkDriverRegistration(uidString);
    }
}

void checkDriverRegistration(String rfidUID) {
    Serial.println("Checking driver registration using RFID index...");
    Serial.println("Looking up RFID: " + rfidUID);

    fbdo.clear();

    // Step 1: Use the new rfidUIDIndex for instant driver ID lookup
    String indexPath = "/rfidUIDIndex/" + rfidUID;

    if (Firebase.RTDB.getString(&fbdo, indexPath)) {
        String driverId = fbdo.stringData();

        if (driverId.length() == 0 || driverId == "null") {
            Serial.println("✗ RFID not found in index: " + rfidUID);
            showError("RFID not registered. Please register driver first.");
            delay(2000);
            isProcessingRFID = false;
            return;
        }

        Serial.println("✓ Driver ID found in index: " + driverId);

        // Step 2: Fetch the full driver data using the driver ID
        String driverPath = "/drivers/" + driverId;
        FirebaseData fbdo2; // Use separate FirebaseData object

        if (Firebase.RTDB.get(&fbdo2, driverPath)) {
            if (fbdo2.dataType() == "json") {
                String driverJson = fbdo2.jsonString();

                // Parse essential driver fields
                int nameStart = driverJson.indexOf("\"driverName\":\"") + 14;
                int nameEnd = (nameStart >= 14) ? driverJson.indexOf("\"", nameStart) : -1;
                String driverName = (nameStart >= 14 && nameEnd > nameStart) ? driverJson.substring(nameStart, nameEnd) : "";

                int todaStart = driverJson.indexOf("\"todaNumber\":\"") + 14;
                int todaEnd = (todaStart >= 14) ? driverJson.indexOf("\"", todaStart) : -1;
                String todaNumber = (todaStart >= 14 && todaEnd > todaStart) ? driverJson.substring(todaStart, todaEnd) : "";

                // Check if driver is active
                bool isActive = true;
                int activePos = driverJson.indexOf("\"isActive\":");
                if (activePos != -1) {
                    int vStart = activePos + 11;
                    String v = driverJson.substring(vStart, min((int)driverJson.length(), vStart + 5));
                    v.trim();
                    isActive = v.startsWith("true");
                }

                if (!isActive) {
                    Serial.println("✗ Driver is inactive");
                    showError("Driver account inactive. Please contact admin.");
                    delay(2000);
                    isProcessingRFID = false;
                    return;
                }

                if (driverName.length() > 0 && todaNumber.length() > 0) {
                    Driver driver;
                    driver.rfidUID = rfidUID;
                    driver.driverName = driverName;
                    driver.todaNumber = todaNumber;
                    driver.isRegistered = true;

                    Serial.println("✓ Driver found via RFID index:");
                    Serial.println("  Driver ID: " + driverId);
                    Serial.println("  Name: " + driver.driverName);
                    Serial.println("  TODA Number: " + driver.todaNumber);

                    enableCoinSlot(driver);
                    delay(1000);
                    isProcessingRFID = false;
                    return;
                } else {
                    Serial.println("✗ Incomplete driver data");
                    showError("Driver record incomplete. Please contact admin.");
                    delay(2000);
                    isProcessingRFID = false;
                    return;
                }
            } else {
                Serial.println("✗ Driver data is not JSON: " + fbdo2.dataType());
                showError("Invalid driver data format. Please contact admin.");
                delay(2000);
                isProcessingRFID = false;
                return;
            }
        } else {
            Serial.println("✗ Failed to fetch driver data: " + fbdo2.errorReason());
            showError("Failed to read driver data. Please try again.");
            delay(2000);
            isProcessingRFID = false;
            return;
        }
    } else {
        String error = fbdo.errorReason();
        Serial.println("✗ Failed to lookup RFID index: " + error);

        // Check if it's a "not found" error vs connection error
        if (error.indexOf("not exist") != -1 || error.indexOf("null") != -1) {
            showError("RFID not registered. Please register driver first.");
        } else {
            showError("Database connection error. Check WiFi.");
        }
        delay(2000);
        isProcessingRFID = false;
        return;
    }
}


void enableCoinSlot(Driver driver) {
    Serial.println("\n=== ENABLING COIN SLOT ===");

    // FIRST: Check if driver is already in queue before enabling coin slot
    Serial.println("Checking if driver is already in queue before enabling coin slot...");

    if (Firebase.RTDB.get(&fbdo, "/queue")) {
        if (fbdo.dataType() == "json") {
            String queueJson = fbdo.jsonString();

            // Check if this driver's RFID is already in the queue with "waiting" status
            String searchPattern = "\"driverRFID\":\"" + driver.rfidUID + "\"";
            int rfidPos = queueJson.indexOf(searchPattern);

            if (rfidPos != -1) {
                // Found the driver's RFID, now check if they have "waiting" status
                int statusStart = queueJson.indexOf("\"status\":", rfidPos);
                if (statusStart != -1) {
                    int statusValueStart = queueJson.indexOf("\"", statusStart + 9) + 1;
                    int statusValueEnd = queueJson.indexOf("\"", statusValueStart);
                    String status = queueJson.substring(statusValueStart, statusValueEnd);

                    if (status == "waiting") {
                        Serial.println("✗ Driver " + driver.driverName + " is already in the queue!");
                        Serial.println("Coin slot will NOT be enabled. Please wait for your turn.");
                        showError("Already in queue! Please wait for your turn.");

                        // Display error on LCD
                        updateLCDDisplay(driver.todaNumber, "Already in Queue!");
                        delay(3000); // Show error for 3 seconds
                        clearLCDDisplay();

                        // Reset processing flag and re-enable NFC immediately
                        isProcessingRFID = false;
                        nfcEnabled = true;
                        coinSlotEnabled = false;
                        return; // Exit without enabling coin slot
                    }
                }
            }
        }
    }

    Serial.println("Driver not in queue, proceeding to enable coin slot...");

    // Disable NFC to prevent interference
    nfcEnabled = false;
    coinSlotEnabled = true;
    isProcessingRFID = false; // Allow processing to continue

    // Reset coin pulse counters and mark power-on time
    noInterrupts();
    coinValidPulses = 0;
    coinActive = false;
    interrupts();
    coinSlotPowerOnTime = millis();

    Serial.println("NFC disabled: " + String(nfcEnabled ? "false" : "true"));
    Serial.println("Coin slot enabled: " + String(coinSlotEnabled ? "true" : "false"));

    // Send command to Arduino to power on coin slot
    Serial.println("Sending command 200 to Arduino via Serial2...");
    Serial2.write((uint8_t)200);
    Serial2.flush(); // Ensure data is sent

    Serial.println("✓ Command sent to Arduino");
    Serial.println("RFID verified! PN532 disabled. Coin slot enabled - insert ₱5 contribution.");
    Serial.println("Driver: " + driver.driverName + " (TODA #" + driver.todaNumber + ")");

    // Store current driver info for contribution processing
    currentDriver = driver;

    // Display on LCD: TODA-001 INSERT 5
    updateLCDDisplay(driver.todaNumber, "INSERT 5");

    Serial.println("=== COIN SLOT READY ===\n");
}

void processCoinContribution() {
    totalSavings += 5;
    Serial.println("₱5 contribution received from " + currentDriver.driverName);
    Serial.println("Total today: ₱" + String(totalSavings));

    // Display success message on LCD
    updateLCDDisplay(currentDriver.todaNumber, "Payment Success!");
    delay(2000); // Show success message for 2 seconds

    // Send command to Arduino to power off coin slot
    Serial2.write((uint8_t)201);
    Serial2.flush();

    // Process contribution and add to queue
    processContributionAndQueue();

    // Re-enable NFC for next driver
    coinSlotEnabled = false;
    nfcEnabled = true;
    isProcessingRFID = false; // Make sure RFID processing flag is cleared

    // Clear coin pulses to avoid re-trigger
    noInterrupts();
    coinValidPulses = 0;
    coinActive = false;
    interrupts();

    // Clear LCD and show system ready message
    clearLCDDisplay();

    Serial.println("Contribution processed! PN532 re-enabled. Next driver can scan RFID.");
    Serial.println("System ready for next driver...");
}

void processContributionAndQueue() {
    Serial.println("Processing contribution and adding to queue...");

    // Record contribution in database
    recordContribution();

    // Add driver to queue
    addDriverToQueue();
}

void recordContribution() {
    // Create contribution record JSON
    String timestamp = String(time(nullptr));
    String contributionPath = "/contributions/" + timestamp;

    FirebaseJson contributionJson;
    contributionJson.set("driverRFID", currentDriver.rfidUID);
    contributionJson.set("driverName", currentDriver.driverName);
    contributionJson.set("todaNumber", currentDriver.todaNumber);
    contributionJson.set("amount", 5);
    contributionJson.set("timestamp", timestamp);
    contributionJson.set("date", getCurrentDate());

    if (Firebase.RTDB.setJSON(&fbdo, contributionPath, &contributionJson)) {
        Serial.println("✓ Contribution recorded in database");
    } else {
        Serial.println("✗ Failed to record contribution: " + fbdo.errorReason());
    }
}

void addDriverToQueue() {
    Serial.println("Checking if driver is already in queue...");

    // First, check if driver is already in the queue
    if (Firebase.RTDB.get(&fbdo, "/queue")) {
        if (fbdo.dataType() == "json") {
            String queueJson = fbdo.jsonString();

            // Check if this driver's RFID is already in the queue with "waiting" status
            String searchPattern = "\"driverRFID\":\"" + currentDriver.rfidUID + "\"";
            int rfidPos = queueJson.indexOf(searchPattern);

            if (rfidPos != -1) {
                // Found the driver's RFID, now check if they have "waiting" status
                int statusStart = queueJson.indexOf("\"status\":", rfidPos);
                if (statusStart != -1) {
                    int statusValueStart = queueJson.indexOf("\"", statusStart + 9) + 1;
                    int statusValueEnd = queueJson.indexOf("\"", statusValueStart);
                    String status = queueJson.substring(statusValueStart, statusValueEnd);

                    if (status == "waiting") {
                        Serial.println("✗ Driver " + currentDriver.driverName + " is already in the queue!");
                        Serial.println("Please wait for your turn or check with the operator.");
                        showError("Already in queue! Please wait for your turn.");
                        return; // Exit without adding to queue
                    }
                }
            }
        }
    }

    Serial.println("Driver not in queue, adding to queue...");

    // Create queue entry JSON
    String timestamp = String(time(nullptr));
    String queuePath = "/queue/" + timestamp;

    FirebaseJson queueJson;
    queueJson.set("driverRFID", currentDriver.rfidUID);
    queueJson.set("driverName", currentDriver.driverName);
    queueJson.set("todaNumber", currentDriver.todaNumber);
    queueJson.set("queueTime", timestamp);
    queueJson.set("timestamp", getCurrentDate() + " " + getCurrentTime()); // Real-time readable timestamp
    queueJson.set("status", "waiting");
    queueJson.set("contributionPaid", true);

    if (Firebase.RTDB.setJSON(&fbdo, queuePath, &queueJson)) {
        Serial.println("✓ Driver added to queue successfully!");
        Serial.println("Driver " + currentDriver.driverName + " is now in the tricycle queue");
        Serial.println("Queue Time: " + getCurrentDate() + " " + getCurrentTime());
    } else {
        Serial.println("✗ Failed to add driver to queue: " + fbdo.errorReason());
    }
}

void showError(String message) {
    Serial.println("ERROR: " + message);
    Serial.println("Please try scanning the RFID card again...");
    // Here you could add LED indicators, buzzer, or display messages
}

String getCurrentDate() {
    time_t now = time(nullptr);
    struct tm* timeinfo = localtime(&now);

    char dateStr[11];
    sprintf(dateStr, "%04d-%02d-%02d",
            timeinfo->tm_year + 1900,
            timeinfo->tm_mon + 1,
            timeinfo->tm_mday);

    return String(dateStr);
}

String getCurrentTime() {
    time_t now = time(nullptr);
    struct tm* timeinfo = localtime(&now);

    char timeStr[9];
    sprintf(timeStr, "%02d:%02d:%02d",
            timeinfo->tm_hour,
            timeinfo->tm_min,
            timeinfo->tm_sec);

    return String(timeStr);
}

// LCD Display Functions
void updateLCDDisplay(String todaNumber, String message) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("TODA-" + todaNumber);
    lcd.setCursor(0, 1);
    lcd.print(message);

    Serial.println("LCD Display Updated:");
    Serial.println("Line 1: TODA-" + todaNumber);
    Serial.println("Line 2: " + message);
}

void clearLCDDisplay() {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("System Ready");
    lcd.setCursor(0, 1);
    lcd.print("Scan RFID Card");

    Serial.println("LCD Display Cleared - Ready for next scan");
}
