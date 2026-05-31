#include <stdbool.h> 
#include <Arduino.h>
#include <stdint.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include "DHTesp.h"
#include "freertos/timers.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "constantes.h"

// Clientes WiFi y MQTT
WiFiClient espClient;
PubSubClient client(espClient);

typedef enum {
    ESTADO_DETENIDO_MANUAL,
    ESTADO_ABRIENDO_MANUAL,
    ESTADO_CERRANDO_MANUAL,
    ESTADO_BLOQUEADO_MANUAL,
    ESTADO_DETENIDO_AUTO,
    ESTADO_ABRIENDO_AUTO,
    ESTADO_CERRANDO_AUTO,
    ESTADO_BLOQUEADO_AUTO
} estado_t;

typedef enum {
    EVT_ABRIR_M,
    EVT_CERRAR_M,
    EV_CHANGE_MODE_AUTO_SENSOR,
    EV_CHANGE_MODE_MANUAL,
    EVT_HUMEDAD,
    EVT_LLUVIA,
    EVT_PIR_ON,
    EVT_TIMEOUT_PIR,
    EVT_FC_ABIERTO, 
    EVT_FC_CERRADO, 
    EVT_EMRGENCE, 
    EVT_CONTINUE,
} tipo_evento_t;

// Definicion de variables globales
estado_t estado_actual = ESTADO_DETENIDO_MANUAL;
TimerHandle_t pirTimer;
short indice_sensor = 0;
bool lluvia = false;
bool humedad_alta = false;

QueueHandle_t queueEvents;
TaskHandle_t loopTaskHandler;
TaskHandle_t loopNewEventHandler;
DHTesp dht;

// Prototipos de funciones para el arreglo
tipo_evento_t verificarEmergencia(void);
tipo_evento_t verificarPIR(void);
tipo_evento_t verificarFC_Abierto(void);
tipo_evento_t verificarFC_Cerrado(void);
tipo_evento_t verificarLluvia(void);
tipo_evento_t verificarHumedad(void);
tipo_evento_t verificarComandosAndroid(void);

// Arreglo original de sensores
tipo_evento_t (*verificar_sensor[])(void) = {
  verificarEmergencia,
  verificarPIR,
  verificarFC_Abierto,
  verificarFC_Cerrado,
  verificarLluvia,
  verificarHumedad,
  verificarComandosAndroid
};

// Se calcula solo la cantidad para que no desborde la memoria
const int NUM_SENSORES = sizeof(verificar_sensor) / sizeof(verificar_sensor[0]);

// Funciones de hardware y sensores
int leer_humedad() {
  TempAndHumidity data = dht.getTempAndHumidity();
  
  if (dht.getStatus() == DHTesp::ERROR_NONE) {
      // 1. Reporte por Monitor Serie
      Serial.printf("[Sensor] Temperatura: %.1f °C | Humedad: %.1f %%\n", data.temperature, data.humidity);
      
      // 2. Reporte por MQTT (Solo si hay conexión)
      if (client.connected()) {
          String hum_str = String(data.humidity, 1);
          String temp_str = String(data.temperature, 1);
          
          client.publish(TOPIC_HUMEDAD, hum_str.c_str());
          client.publish(TOPIC_TEMP, temp_str.c_str());
      }
  } else {
      Serial.println("[Sensor] Error de lectura en el DHT11 (Ignorando ciclo)");
      return -1; // Código de error
  }
  
  return (int)data.humidity;
}

int leer_lluvia(){
  return analogRead(GPIO_Lluvia);
}

int leer_pir(){
  return digitalRead(GPIO_PIR);
}

void encender_led(){
  digitalWrite(GPIO_LED_IND,HIGH);
}

void apagar_led(){
  digitalWrite(GPIO_LED_IND,LOW);
}

void motor_control(int vel, int d1, int d2) {
  Serial.printf("[Motor] Velocidad: %d | Dir1: %d | Dir2: %d\n", vel, d1, d2);
  ledcWrite(motor1PWM, vel);
  digitalWrite(motor1Dir1 , d1);
  digitalWrite(motor1Dir2 , d2);
}

// Verificaciones de sensores
tipo_evento_t verificarEmergencia() {
  static int ultimo_estado = HIGH; 
  int estado_lectura = digitalRead(GPIO_EMERG);
  
  if (estado_lectura == LOW && ultimo_estado == HIGH) {
    Serial.printf("[Sensor] Emergencia detectada.\n");
    encender_led();
    ultimo_estado = estado_lectura;
    return EVT_EMRGENCE;
  } 
  else if (estado_lectura == HIGH && ultimo_estado == LOW) {
    apagar_led();
  }
  
  ultimo_estado = estado_lectura;
  return EVT_CONTINUE;
}

tipo_evento_t verificarLluvia() {
  int valor_actual = leer_lluvia();
  
  if (valor_actual > (LLUVIA_MIN_VALIDA + 100) && lluvia == false) {
    Serial.printf("[Sensor] Lluvia detectada. Valor: %d\n", valor_actual);
    lluvia = true;
    return EVT_LLUVIA;
  }
  else if (valor_actual <= (LLUVIA_MIN_VALIDA - 100) && lluvia == true) {
    Serial.printf("[Sensor] Lluvia detenida.\n");
    lluvia = false;
  }
  
  return EVT_CONTINUE;
}

tipo_evento_t verificarHumedad() {
  if (lluvia == true) {
    return EVT_CONTINUE;
  }
  
  static uint32_t ultima_lectura = RESET_LECTURA;
  
  if (millis() - ultima_lectura >= INTERVALO) {
    ultima_lectura = millis();
    
    int humedad_actual = leer_humedad(); 
    if (humedad_actual == -1) return EVT_CONTINUE; 

    // Análisis de umbrales para la máquina de estados
    if (humedad_actual > HUMEDAD_UMBRAL_MAX && humedad_alta == false) {
      Serial.printf("[FSM] Humedad superó el umbral (%d%%). Lanzando evento.\n", humedad_actual); 
      humedad_alta = true;
      return EVT_HUMEDAD;
    }
    else if (humedad_actual < HUMEDAD_UMBRAL_MAX && humedad_alta == true) {
      Serial.printf("[FSM] Humedad bajó del umbral (%d%%).\n", humedad_actual);
      humedad_alta = false;
    }
  }
  
  return EVT_CONTINUE;
}

tipo_evento_t verificarPIR() {
  static int ultimo_estado = LOW; 
  int estado_lectura = leer_pir();

  if (estado_lectura == HIGH && ultimo_estado == LOW) {
    Serial.printf("[Sensor] Movimiento detectado por PIR.\n");
    xTimerReset(pirTimer, NO_WAIT); 
    ultimo_estado = estado_lectura; 
    return EVT_PIR_ON;
  }
  
  ultimo_estado = estado_lectura; 
  return EVT_CONTINUE;
}

tipo_evento_t verificarFC_Abierto() {
  static int ultimo_estado = LOW; 
  int estado_lectura = digitalRead(GPIO_FC_A);

  if (estado_lectura == HIGH && ultimo_estado == LOW) {
    Serial.printf("[Sensor] Final de carrera APERTURA presionado.\n");
    ultimo_estado = estado_lectura;
    return EVT_FC_ABIERTO;
  }
  
  ultimo_estado = estado_lectura; 
  return EVT_CONTINUE;
}

tipo_evento_t verificarFC_Cerrado() {
  static int ultimo_estado= LOW;
  int estado_lectura = digitalRead(GPIO_FC_C);
  if (estado_lectura == HIGH && ultimo_estado == LOW) {
    Serial.printf("[Sensor] Final de carrera CIERRE presionado.\n");
    ultimo_estado = estado_lectura;
    return EVT_FC_CERRADO;
  }
  ultimo_estado= estado_lectura;
  return EVT_CONTINUE;
}

tipo_evento_t verificarComandosAndroid() {
  if (!Serial.available()) {
    return EVT_CONTINUE;
  }

  char c = tolower(Serial.read());
  switch (c) {
    case 'a': return EVT_ABRIR_M;
    case 'c': return EVT_CERRAR_M;
    case 'm': return EV_CHANGE_MODE_MANUAL;
    case 's': return EV_CHANGE_MODE_AUTO_SENSOR;
    default:  return EVT_CONTINUE;
  }
}

// Timer Callback de FreeRTOS
void pirTimeoutCallback(TimerHandle_t xTimer) {
    tipo_evento_t evt = EVT_TIMEOUT_PIR;
    xQueueSend(queueEvents, &evt, NO_WAIT);
    Serial.printf("[Timer] Callback de PIR disparado. Evento enviado.\n");
}

// Tarea de recolección de eventos
void vGetEventTask(void *pvParameters) {
  tipo_evento_t ev;
  Serial.printf("[Sistema] Tarea GetEvent Iniciada. \n");
  
  while (1) {
      ev = verificar_sensor[indice_sensor]();

      if (ev != EVT_CONTINUE) {
        xQueueSend(queueEvents, &ev, NO_WAIT);
      }

      indice_sensor++;
      
      if (indice_sensor >= NUM_SENSORES) {
          indice_sensor = 0; 
          vTaskDelay(pdMS_TO_TICKS(INTERVALO_TAREAS));
      } 
  }
}

// Máquina de Estados (FSM)
void fsm() {
    tipo_evento_t input;
    
    if (xQueueReceive(queueEvents, &input, portMAX_DELAY)) {
        estado_t estado_anterior = estado_actual;

        // Forzar cambio de modo desde MQTT/Serial
        if (input == EV_CHANGE_MODE_AUTO_SENSOR && estado_actual < ESTADO_DETENIDO_AUTO) {
            Serial.printf("[FSM] -> Forzando Modo Automático.\n");
            motor_control(STOP, LOW, LOW);
            estado_actual = ESTADO_DETENIDO_AUTO;
            
            if (client.connected()) client.publish(TOPIC_MODO, "AUTOMATICO");
            return;
        }
        if (input == EV_CHANGE_MODE_MANUAL && estado_actual >= ESTADO_DETENIDO_AUTO) {
            Serial.printf("[FSM] -> Forzando Modo Manual.\n");
            motor_control(STOP, LOW, LOW);
            estado_actual = ESTADO_DETENIDO_MANUAL;
            
            if (client.connected()) client.publish(TOPIC_MODO, "MANUAL");
            return;
        }

        switch (estado_actual) {
            // --- RAMA MANUAL ---
            case ESTADO_DETENIDO_MANUAL:
                switch (input) {
                    case EVT_ABRIR_M:
                        if (digitalRead(GPIO_FC_A) == LOW) {
                            estado_actual = ESTADO_ABRIENDO_MANUAL;
                            client.publish(TOPIC_ESTADO, "ABRIENDO");
                            motor_control(VELOCIDAD, HIGH, LOW);
                        }
                        break;
                    case EVT_CERRAR_M:
                        if (digitalRead(GPIO_FC_C) == LOW) {
                            estado_actual = ESTADO_CERRANDO_MANUAL;
                            client.publish(TOPIC_ESTADO, "CERRANDO");
                            motor_control(VELOCIDAD, LOW, HIGH);
                        }
                        break;
                    default: break;
                }
                break;

            case ESTADO_ABRIENDO_MANUAL:
                switch (input) {
                    case EVT_FC_ABIERTO:
                        client.publish(TOPIC_ESTADO, "ABIERTA");
                    case EVT_EMRGENCE:   
                        estado_actual = ESTADO_DETENIDO_MANUAL;
                        motor_control(STOP, LOW, LOW);
                        break;
                    case EVT_CERRAR_M:   
                        estado_actual = ESTADO_CERRANDO_MANUAL;
                        client.publish(TOPIC_ESTADO, "CERRANDO");
                        motor_control(VELOCIDAD, LOW, HIGH);
                        break;
                    default: break;
                }
                break;

            case ESTADO_CERRANDO_MANUAL:
                switch (input) {
                    case EVT_FC_CERRADO:
                        client.publish(TOPIC_ESTADO, "CERRADA");
                    case EVT_EMRGENCE:  
                        estado_actual = ESTADO_DETENIDO_MANUAL;
                        motor_control(STOP, LOW, LOW);
                        break;
                    case EVT_ABRIR_M:    
                        estado_actual = ESTADO_ABRIENDO_MANUAL;
                        client.publish(TOPIC_ESTADO, "ABRIENDO");
                        motor_control(VELOCIDAD, HIGH, LOW);
                        break;
                    case EVT_PIR_ON:    
                        estado_actual = ESTADO_BLOQUEADO_MANUAL;
                        motor_control(STOP, LOW, LOW);
                        xTimerStart(pirTimer, NO_WAIT);
                        break;
                    default: break;
                }
                break;

            case ESTADO_BLOQUEADO_MANUAL:
                switch (input) {
                    case EVT_TIMEOUT_PIR: 
                        if (digitalRead(GPIO_FC_C) == LOW) {
                            estado_actual = ESTADO_CERRANDO_MANUAL;
                            client.publish(TOPIC_ESTADO, "CERRANDO");
                            motor_control(VELOCIDAD, LOW, HIGH);
                        }
                        break;
                    case EVT_PIR_ON:     
                        xTimerReset(pirTimer, NO_WAIT);
                        break;
                    case EVT_EMRGENCE:
                        estado_actual = ESTADO_DETENIDO_MANUAL;
                        motor_control(STOP, LOW, LOW);
                        break;
                    default: break;
                }
                break;

            // --- RAMA AUTOMÁTICA ---
            case ESTADO_DETENIDO_AUTO:
                switch (input) {
                    case EVT_LLUVIA:
                        if (digitalRead(GPIO_FC_C) == LOW) {
                            estado_actual = ESTADO_CERRANDO_AUTO;
                            client.publish(TOPIC_ESTADO, "CERRANDO");
                            motor_control(VELOCIDAD, LOW, HIGH);
                        }
                        break;
                    case EVT_HUMEDAD:
                        if (digitalRead(GPIO_FC_A) == LOW) {
                            estado_actual = ESTADO_ABRIENDO_AUTO;
                            client.publish(TOPIC_ESTADO, "ABRIENDO");
                            motor_control(VELOCIDAD, HIGH, LOW);
                        }
                        break;
                    default: break;
                }
                break;

            case ESTADO_ABRIENDO_AUTO:
                switch (input) {
                    case EVT_FC_ABIERTO: 
                        client.publish(TOPIC_ESTADO, "ABIERTA");
                    case EVT_EMRGENCE:
                        estado_actual = ESTADO_DETENIDO_AUTO;
                        motor_control(STOP, LOW, LOW);
                        break;
                    case EVT_LLUVIA:
                        estado_actual = ESTADO_CERRANDO_AUTO;
                        client.publish(TOPIC_ESTADO, "CERRANDO");
                        motor_control(VELOCIDAD, LOW, HIGH);
                        break;
                    default: break;
                }
                break;

            case ESTADO_CERRANDO_AUTO:
                switch (input) {
                    case EVT_FC_CERRADO: 
                        client.publish(TOPIC_ESTADO, "CERRADA");
                    case EVT_EMRGENCE:   
                        estado_actual= ESTADO_DETENIDO_AUTO;
                        motor_control(STOP, LOW, LOW);
                        break;
                    case EVT_HUMEDAD:   
                        estado_actual = ESTADO_ABRIENDO_AUTO;
                        client.publish(TOPIC_ESTADO, "ABRIENDO");
                        motor_control(VELOCIDAD, HIGH, LOW);
                        break;
                    case EVT_PIR_ON:    
                        estado_actual= ESTADO_BLOQUEADO_AUTO;
                        motor_control(STOP, LOW, LOW);
                        xTimerStart(pirTimer, NO_WAIT);
                        break;
                    default: break;
                }
                break;

            case ESTADO_BLOQUEADO_AUTO:
                switch (input) {
                    case EVT_TIMEOUT_PIR:
                        if (digitalRead(GPIO_FC_C) == LOW) {
                            estado_actual = ESTADO_CERRANDO_AUTO;
                            client.publish(TOPIC_ESTADO, "CERRANDO");
                            motor_control(VELOCIDAD, LOW, HIGH);
                        }
                        break;
                    case EVT_PIR_ON:      
                        xTimerReset(pirTimer, NO_WAIT);
                        break;
                    case EVT_EMRGENCE:
                        estado_actual = ESTADO_DETENIDO_AUTO;
                        motor_control(STOP, LOW, LOW);
                        break;
                    default: break;
                }
                break;
        }

        if (estado_actual != estado_anterior) {
            Serial.printf("[FSM] -> Cambio de Estado: %d al %d\n", estado_anterior, estado_actual);
        }
    }
}

void vFSMTask(void *pvParameters) {
  Serial.printf("[Sistema] Tarea FSM Iniciada.\n");
  while (1) {
      fsm(); 
  }
}

// === Tarea y Callback de MQTT ===
void callback(char* topic, byte* payload, unsigned int length) {
  
  Serial.print("Se recibió mensaje en el topico: ");
  Serial.println(topic);

  if (topic == TOPIC_MODO_MANUAL) {
    tipo_evento_t evento = EV_CHANGE_MODE_MANUAL;
    xQueueSend(queueEvents, &evento, NO_WAIT);
  }
  else if (topic == TOPIC_MODO_AUTOMATICO) {
    tipo_evento_t evento = EV_CHANGE_MODE_AUTO_SENSOR;
    xQueueSend(queueEvents, &evento, NO_WAIT);
  }
  else if (topic == TOPIC_ABRIR) {
    tipo_evento_t evento = EVT_ABRIR_M;
    xQueueSend(queueEvents, &evento, NO_WAIT);
  }
  else if (topic == TOPIC_CERRAR) {
    tipo_evento_t evento = EVT_CERRAR_M;
    xQueueSend(queueEvents, &evento, NO_WAIT);
  }
}

void vMQTTTask(void *pvParameters) {
  WiFi.begin(WIFISSID, PASSWORD);
  client.setServer(MQTT_SERVER, MQTT_PORT); 
  client.setCallback(callback);

  while (1) {
    // Si no hay red, esperar
    if (WiFi.status() != WL_CONNECTED) {
      vTaskDelay(pdMS_TO_TICKS(1000));
      continue;
    }
    
    // Si hay red pero MQTT está desconectado, intentar conectar
    if (!client.connected()) {
      Serial.println("[MQTT] Conectando...");
      String clientId = "ESP32Ventana-" + String(random(0xffff), HEX);
      
      if (client.connect(clientId.c_str(), MQTT_USER, MQTT_PASSWORD)) {
        Serial.println("[MQTT] Conectandose");
        client.subscribe(TOPIC_MODO);
        client.subscribe(TOPIC_COMANDO);
      } else {
        vTaskDelay(pdMS_TO_TICKS(5000)); 
      }
    } else {
      // Si todo está OK, mantener la conexión y leer mensajes
      client.loop(); 
    }
    
    vTaskDelay(pdMS_TO_TICKS(20)); // Evita colgar la tarea
  }
}

void inicializacion(){
  Serial.begin(115200);
  Serial.printf("\n--- Iniciando Sistema ---\n");

  pinMode(GPIO_Lluvia, INPUT);
  pinMode(GPIO_DHT11, INPUT);
  pinMode(GPIO_FC_A, INPUT_PULLDOWN);
  pinMode(GPIO_FC_C, INPUT_PULLDOWN);
  pinMode(GPIO_EMERG, INPUT_PULLUP);
  pinMode(GPIO_PIR, INPUT);
  pinMode(GPIO_LED_IND, OUTPUT);
  
  ledcAttach(motor1PWM, PWM_FREQ, PWM_RES);
  pinMode(motor1Dir1, OUTPUT);
  pinMode(motor1Dir2, OUTPUT);
  
  dht.setup(GPIO_DHT11, DHTesp::DHT11);

  estado_actual = ESTADO_DETENIDO_MANUAL;

  pirTimer = xTimerCreate("PIR Timer", pdMS_TO_TICKS(PIR_TIME), pdFALSE, NULL, pirTimeoutCallback);
  queueEvents = xQueueCreate(QUEUE_SIZE, sizeof(tipo_evento_t));

  xTaskCreate(vFSMTask, "LoopFSM", STACK_SIZE, NULL, PRIORIDAD_TAREAS, &loopTaskHandler);
  xTaskCreate(vGetEventTask, "EventTask", STACK_SIZE, NULL, PRIORIDAD_TAREAS, &loopNewEventHandler);
  xTaskCreate(vMQTTTask, "MQTT_Task", STACK_SIZE * 2, NULL, PRIORIDAD_TAREAS, NULL); 

}

void setup(){
  inicializacion();
}

void loop(){
  // Vacío, manejado por FreeRTOS
}