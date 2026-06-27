#ifndef CONSTANTES_H
#define CONSTANTES_H

// Pines del microcontrolador
#define GPIO_Lluvia   34    // Sensor Lluvia
#define GPIO_DHT11    19    // Sensor humedad 
#define GPIO_FC_A     25    // Final carrera Apertura
#define GPIO_FC_C     33    // Final carrera Cierre
#define GPIO_EMERG    27    // Pulsador de emergencia
#define GPIO_LED_IND  23    // Led indicador
#define GPIO_PIR      21    // Sensor pir
#define motor1PWM     5     // Pin PWM para el motor 1
#define motor1Dir2    4     // Pin de dirección 2 para el motor 1
#define motor1Dir1    2     // Pin de dirección 1 para el motor 1 (Led Azul)

// Parámetros PWM y Motor
#define PWM_FREQ           5000  
#define PWM_RES            8     
#define VELOCIDAD          150
#define STOP               0

// Definicion de umbral humedad y lluvia
#define HUMEDAD_UMBRAL_MAX  70
#define LLUVIA_MIN_VALIDA   500
#define HUMEDAD_MIN_VALIDA  0
#define INTERVALO           2000
#define RESET_LECTURA       0
#define PORCENTAJE_BASE     100

// Tiempos y FreeRTOS
#define INTERVALO_TAREAS    20
#define STACK_SIZE          4096
#define QUEUE_SIZE          10
#define PRIORIDAD_TAREAS    1
#define PIR_TIME            5000 // Tiempo en ms
#define MAX_EVENTOS         8
#define NO_WAIT             0
#define NO_NOTIF            0
#define TIEMPO_FC           50
#define TIEMPO_REPOSO       10000
#define T_ANTIREBOTE        150

#define INI                 0
#define FALSO               -1

// Config Wifi y MQTT
#define WIFISSID              "red_pincha"
#define PASSWORD              "suzuki15"


#define MQTT_SERVER           "broker.emqx.io"
#define MQTT_NAME             "SmartWindow"
#define MQTT_USER             ""
#define MQTT_PASSWORD         ""
#define MQTT_PORT             1883

#define TOPIC_HUMEDAD             "/ventana/humedad"
#define TOPIC_TEMP                "/ventana/temp"
#define TOPIC_MODO                "/ventana/modo"
#define TOPIC_COMANDO             "/ventana/comando"  //en el modo manual, abre o cierra
#define TOPIC_ESTADO              "/ventana/estado"
#define TOPIC_EMERGENCIA      "/ventana/emergencia"
#define TOPIC_EMERGENCIA_ESTADO   "/ventana/estado/emergencia"





#endif