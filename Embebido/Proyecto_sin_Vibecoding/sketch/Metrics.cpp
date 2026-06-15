#include "Metrics.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

// Variables para almacenar las capturas de tiempos de los hilos IDLE
static uint32_t startTime = 0;
static uint32_t startIdle0 = 0;
static uint32_t startIdle1 = 0;
static uint32_t startTotalRunTime = 0;

// Función interna para obtener los contadores de las tareas IDLE de FreeRTOS
void getIdleTasksRuntime(uint32_t *idle0, uint32_t *idle1, uint32_t *totalRunTime) {
    UBaseType_t arraySize = uxTaskGetNumberOfTasks();
    TaskStatus_t *statusArray = (TaskStatus_t *)pvPortMalloc(arraySize * sizeof(TaskStatus_t));
    
    if (statusArray != NULL) {
        arraySize = uxTaskGetSystemState(statusArray, arraySize, totalRunTime);
        
        for (UBaseType_t i = 0; i < arraySize; i++) {
            if (strcmp(statusArray[i].pcTaskName, "IDLE0") == 0) {
                *idle0 = statusArray[i].ulRunTimeCounter;
            } else if (strcmp(statusArray[i].pcTaskName, "IDLE1") == 0) {
                *idle1 = statusArray[i].ulRunTimeCounter;
            }
        }
        vPortFree(statusArray);
    }
}

void initStats() {
    startTime = millis();
    getIdleTasksRuntime(&startIdle0, &startIdle1, &startTotalRunTime);
}

void finishStats() {
    uint32_t endTime = millis();
    uint32_t duration = endTime - startTime;
    
    uint32_t endIdle0 = 0, endIdle1 = 0, endTotalRunTime = 0;
    getIdleTasksRuntime(&endIdle0, &endIdle1, &endTotalRunTime);
    
    uint32_t deltaTotal = endTotalRunTime - startTotalRunTime;
    if (deltaTotal == 0) deltaTotal = 1; // Evitar división por cero

    // Calcular la cantidad de tiempo que cada Core estuvo en IDLE durante la ventana
    uint32_t deltaIdle0 = endIdle0 - startIdle0;
    uint32_t deltaIdle1 = endIdle1 - startIdle1;

    // Calcular porcentajes
    float idle0Pct = ((float)deltaIdle0 / (float)deltaTotal) * 100.0;
    float idle1Pct = ((float)deltaIdle1 / (float)deltaTotal) * 100.0;
    
    // El tiempo ocupado es el complemento del tiempo libre (IDLE)
    float ocupado0 = 100.0 - idle0Pct;
    float ocupado1 = 100.0 - idle1Pct;

    // Validar límites por redondeos de punto flotante
    if (ocupado0 < 0.0) ocupado0 = 0.0;
    if (ocupado1 < 0.0) ocupado1 = 0.0;

    // Métricas de Memoria Dinámica (Heap Interno de la ESP32)
    size_t totalHeap = ESP.getHeapSize();
    size_t freeHeap = ESP.getFreeHeap();
    size_t usedHeap = totalHeap - freeHeap;

    Serial.println("\nPromedio del intervalo (initStats() -> finishStats())");
    Serial.printf("Muestras del intervalo en milisegundos: %u ms\n", duration);
    
    Serial.println("\nContribución al total del sistema (promedio)");
    Serial.printf("Core 0 -> Total: 100.00%% | Ocupado: %.3f%% | Libre (IDLE): %.3f%%\n", ocupado0, idle0Pct);
    Serial.printf("Core 1 -> Total: 100.00%% | Ocupado: %.3f%% | Libre (IDLE): %.3f%%\n", ocupado1, idle1Pct);
    
    Serial.println("\nEstado de la memoria en el intervalo (promedio)");
    Serial.printf("Heap total: %u bytes\n", totalHeap);
    Serial.printf("Heap Libre: %u bytes\n", freeHeap);
    Serial.printf("Heap usado: %u bytes\n", usedHeap);
}