#include <SoftwareSerial.h>

// ESP32 -> Arduino command serial (RX on pin 4, TX on pin 3 not used)
SoftwareSerial espSerial(4, 3);

const byte COINSLOT_POWER_PIN = 8;
const unsigned long POWER_ON_DELAY_MS = 3000UL; // Increased for better stability

bool coinSlotPowered = false;
unsigned long coinSlotPowerOnTime = 0;

// Debugging - only when connected to PC
unsigned long lastDebugTime = 0;
const unsigned long DEBUG_INTERVAL = 5000; // 5 seconds

// Power-on initialization flag
bool systemInitialized = false;
unsigned long systemStartTime = 0;

void setup() {
    // CRITICAL: Extra delay for 12V power supply stabilization
    // When powered by barrel jack, the voltage regulator needs time to stabilize
    delay(2000); // 2 second delay before ANY initialization

    // Record system start time immediately after power stabilization
    systemStartTime = millis();

    // Initialize hardware pins FIRST (before any serial communication)
    pinMode(COINSLOT_POWER_PIN, OUTPUT);
    digitalWrite(COINSLOT_POWER_PIN, LOW); // Ensure coin slot starts OFF

    // Add built-in LED for status indication (pin 13 on most Arduinos)
    pinMode(LED_BUILTIN, OUTPUT);
    digitalWrite(LED_BUILTIN, LOW);

    // For 12V barrel jack operation, we need to be more careful with serial init
    // Only initialize Serial if we detect it's actually needed/connected
    // This prevents hanging when running standalone

    // Initialize ESP32 communication FIRST (this is most critical)
    espSerial.begin(9600);
    espSerial.setTimeout(50); // Shorter timeout for faster response

    // Initialize USB Serial more carefully for 12V operation
    Serial.begin(9600);
    Serial.setTimeout(50); // Shorter timeout

    // Additional stabilization delay after serial init
    delay(500);

    // Flash LED pattern to indicate 12V power mode startup
    // Different pattern than before to show it's in 12V mode
    for (int i = 0; i < 2; i++) {
        digitalWrite(LED_BUILTIN, HIGH);
        delay(100);
        digitalWrite(LED_BUILTIN, LOW);
        delay(100);
    }
    delay(300);
    for (int i = 0; i < 2; i++) {
        digitalWrite(LED_BUILTIN, HIGH);
        delay(100);
        digitalWrite(LED_BUILTIN, LOW);
        delay(100);
    }

    // Try to send initial message, but don't block if Serial isn't connected
    // Use a very short timeout to detect if PC is connected
    unsigned long serialStart = millis();
    bool pcConnected = false;

    // Quick check if Serial is actually connected (non-blocking approach)
    if (Serial) {
        Serial.println("Arduino UNO with 12V power ready!");
        Serial.println("Coin slot power pin: 8 (LOW=OFF, HIGH=ON)");
        Serial.println("ESP32 communication on pins 4(RX), 3(TX)");
        Serial.println("System optimized for 12V barrel jack operation");
        pcConnected = true;
    }

    // Send a test signal to ESP32 to indicate Arduino is ready
    // This helps ESP32 know Arduino is alive and ready for commands
    delay(100);
    espSerial.write((uint8_t)255); // Special "Arduino Ready" signal
    espSerial.flush();

    if (pcConnected) {
        Serial.println("Ready signal sent to ESP32");
    }

    // Mark system as NOT initialized yet - will be set after POWER_ON_DELAY_MS
    systemInitialized = false;
}

void loop() {
    // System initialization delay (critical for standalone operation)
    if (!systemInitialized) {
        if (millis() - systemStartTime >= POWER_ON_DELAY_MS) {
            systemInitialized = true;

            // Blink LED to indicate system is fully ready
            for (int i = 0; i < 5; i++) {
                digitalWrite(LED_BUILTIN, HIGH);
                delay(100);
                digitalWrite(LED_BUILTIN, LOW);
                delay(100);
            }

            if (Serial) {
                Serial.println("System initialization complete - ready for commands");
            }
        } else {
            // During initialization, just blink LED slowly to show it's alive
            if ((millis() / 500) % 2 == 0) {
                digitalWrite(LED_BUILTIN, HIGH);
            } else {
                digitalWrite(LED_BUILTIN, LOW);
            }
            return; // Skip command processing during initialization
        }
    }

    // Periodic debug - only if Serial is connected (non-blocking)
    if (Serial && (millis() - lastDebugTime > DEBUG_INTERVAL)) {
        Serial.print("Status - CoinSlot: ");
        Serial.print(coinSlotPowered ? "ON" : "OFF");
        Serial.print(", Uptime(s): ");
        Serial.println(millis() / 1000);
        lastDebugTime = millis();
    }

    // Handle incoming serial commands from ESP32 (THIS IS THE CRITICAL PART)
    if (espSerial.available()) {
        uint8_t command = espSerial.read();

        if (command == 200) {
            // ENABLE coin slot power
            digitalWrite(COINSLOT_POWER_PIN, HIGH);
            coinSlotPowered = true;
            coinSlotPowerOnTime = millis();

            // Visual feedback - LED on when coin slot is powered
            digitalWrite(LED_BUILTIN, HIGH);

            if (Serial) {
                Serial.println("Command 200 received: ENABLE coin slot power");
            }

        } else if (command == 201) {
            // DISABLE coin slot power
            digitalWrite(COINSLOT_POWER_PIN, LOW);
            coinSlotPowered = false;

            // Visual feedback - LED off when coin slot is powered off
            digitalWrite(LED_BUILTIN, LOW);

            if (Serial) {
                Serial.println("Command 201 received: DISABLE coin slot power");
            }

        } else {
            if (Serial) {
                Serial.print("Unknown command ignored: ");
                Serial.println(command);
            }
        }
    }

    // Watchdog-like functionality - ensure coin slot doesn't stay on indefinitely
    if (coinSlotPowered && (millis() - coinSlotPowerOnTime > 300000)) { // 5 minutes max
        digitalWrite(COINSLOT_POWER_PIN, LOW);
        coinSlotPowered = false;
        digitalWrite(LED_BUILTIN, LOW);

        if (Serial) {
            Serial.println("Safety timeout: Coin slot auto-disabled after 5 minutes");
        }
    }

    // Small delay to prevent excessive loop cycling
    delay(10);
}