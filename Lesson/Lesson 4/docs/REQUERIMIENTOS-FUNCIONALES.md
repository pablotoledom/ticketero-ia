# Requerimientos Funcionales - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Cliente:** Institución Financiera  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Analista:** Analista de Negocio Senior

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requerimientos funcionales del Sistema Ticketero Digital, diseñado para modernizar la experiencia de atención en sucursales mediante:

- Digitalización completa del proceso de tickets
- Notificaciones automáticas en tiempo real vía Telegram
- Movilidad del cliente durante la espera
- Asignación inteligente de clientes a ejecutivos
- Panel de monitoreo para supervisión operacional

### 1.2 Alcance

Este documento cubre:

- ✅ 8 Requerimientos Funcionales (RF-001 a RF-008)
- ✅ 13 Reglas de Negocio (RN-001 a RN-013)
- ✅ Criterios de aceptación en formato Gherkin
- ✅ Modelo de datos funcional
- ✅ Matriz de trazabilidad

Este documento NO cubre:

- ❌ Arquitectura técnica (ver documento ARQUITECTURA.md)
- ❌ Tecnologías de implementación
- ❌ Diseño de interfaces de usuario

### 1.3 Definiciones

| Término | Definición |
|---------|------------|
| Ticket | Turno digital asignado a un cliente para ser atendido |
| Cola | Fila virtual de tickets esperando atención |
| Asesor | Ejecutivo bancario que atiende clientes |
| Módulo | Estación de trabajo de un asesor (numerados 1-5) |
| Chat ID | Identificador único de usuario en Telegram |
| UUID | Identificador único universal para tickets |

## 2. Reglas de Negocio

Las siguientes reglas de negocio aplican transversalmente a todos los requerimientos funcionales:

**RN-001: Unicidad de Ticket Activo**  
Un cliente solo puede tener 1 ticket activo a la vez. Los estados activos son: EN_ESPERA, PROXIMO, ATENDIENDO. Si un cliente intenta crear un nuevo ticket teniendo uno activo, el sistema debe rechazar la solicitud con error HTTP 409 Conflict.

**RN-002: Prioridad de Colas**  
Las colas tienen prioridades numéricas para asignación automática:
- GERENCIA: prioridad 4 (máxima)
- EMPRESAS: prioridad 3
- PERSONAL_BANKER: prioridad 2
- CAJA: prioridad 1 (mínima)

Cuando un asesor se libera, el sistema asigna primero tickets de colas con mayor prioridad.

**RN-003: Orden FIFO Dentro de Cola**  
Dentro de una misma cola, los tickets se procesan en orden FIFO (First In, First Out). El ticket más antiguo (createdAt menor) se asigna primero.

**RN-004: Balanceo de Carga Entre Asesores**  
Al asignar un ticket, el sistema selecciona el asesor AVAILABLE con menor valor de assignedTicketsCount, distribuyendo equitativamente la carga de trabajo.

**RN-005: Formato de Número de Ticket**  
El número de ticket sigue el formato: [Prefijo][Número secuencial 01-99]
- Prefijo: 1 letra según el tipo de cola
- Número: 2 dígitos, del 01 al 99, reseteado diariamente

Ejemplos: C01, P15, E03, G02

**RN-006: Prefijos por Tipo de Cola**  
- CAJA → C
- PERSONAL_BANKER → P
- EMPRESAS → E
- GERENCIA → G

**RN-007: Reintentos Automáticos de Mensajes**  
Si el envío de un mensaje a Telegram falla, el sistema reintenta automáticamente hasta 3 veces antes de marcarlo como FALLIDO.

**RN-008: Backoff Exponencial en Reintentos**  
Los reintentos de mensajes usan backoff exponencial:
- Intento 1: inmediato
- Intento 2: después de 30 segundos
- Intento 3: después de 60 segundos
- Intento 4: después de 120 segundos

**RN-009: Estados de Ticket**  
Un ticket puede estar en uno de estos estados:
- EN_ESPERA: esperando asignación a asesor
- PROXIMO: próximo a ser atendido (posición ≤ 3)
- ATENDIENDO: siendo atendido por un asesor
- COMPLETADO: atención finalizada exitosamente
- CANCELADO: cancelado por cliente o sistema
- NO_ATENDIDO: cliente no se presentó cuando fue llamado

**RN-010: Cálculo de Tiempo Estimado**  
El tiempo estimado de espera se calcula como:
tiempoEstimado = posiciónEnCola × tiempoPromedioCola

Donde tiempoPromedioCola varía por tipo:
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos

**RN-011: Auditoría Obligatoria**  
Todos los eventos críticos del sistema deben registrarse en auditoría con: timestamp, tipo de evento, actor involucrado, entityId afectado, y cambios de estado.

**RN-012: Umbral de Pre-aviso**  
El sistema envía el Mensaje 2 (pre-aviso) cuando la posición del ticket es ≤ 3, indicando que el cliente debe acercarse a la sucursal.

**RN-013: Estados de Asesor**  
Un asesor puede estar en uno de estos estados:
- AVAILABLE: disponible para recibir asignaciones
- BUSY: atendiendo un cliente (no recibe nuevas asignaciones)
- OFFLINE: no disponible (almuerzo, capacitación, etc.)

## 3. Enumeraciones

### 3.1 QueueType

Tipos de cola disponibles en el sistema:

| Valor | Display Name | Tiempo Promedio | Prioridad | Prefijo |
|-------|--------------|-----------------|-----------|---------|
| CAJA | Caja | 5 min | 1 | C |
| PERSONAL_BANKER | Personal Banker | 15 min | 2 | P |
| EMPRESAS | Empresas | 20 min | 3 | E |
| GERENCIA | Gerencia | 30 min | 4 | G |

### 3.2 TicketStatus

Estados posibles de un ticket:

| Valor | Descripción | Es Activo? |
|-------|-------------|------------|
| EN_ESPERA | Esperando asignación | Sí |
| PROXIMO | Próximo a ser atendido | Sí |
| ATENDIENDO | Siendo atendido | Sí |
| COMPLETADO | Atención finalizada | No |
| CANCELADO | Cancelado | No |
| NO_ATENDIDO | Cliente no se presentó | No |

### 3.3 AdvisorStatus

Estados posibles de un asesor:

| Valor | Descripción | Recibe Asignaciones? |
|-------|-------------|----------------------|
| AVAILABLE | Disponible | Sí |
| BUSY | Atendiendo cliente | No |
| OFFLINE | No disponible | No |

### 3.4 MessageTemplate

Plantillas de mensajes para Telegram:

| Valor | Descripción | Momento de Envío |
|-------|-------------|------------------|
| totem_ticket_creado | Confirmación de creación | Inmediato al crear ticket |
| totem_proximo_turno | Pre-aviso | Cuando posición ≤ 3 |
| totem_es_tu_turno | Turno activo | Al asignar a asesor |

## 4. Requerimientos Funcionales

### RF-001: Crear Ticket Digital

**Descripción:** El sistema debe permitir al cliente crear un ticket digital para ser atendido en sucursal, ingresando su identificación nacional (RUT/ID), número de teléfono y seleccionando el tipo de atención requerida. El sistema generará un número único de ticket, calculará la posición actual en cola y el tiempo estimado de espera basado en datos reales de la operación.

**Prioridad:** Alta

**Actor Principal:** Cliente

**Precondiciones:**
- Terminal de autoservicio disponible y funcional
- Sistema de gestión de colas operativo
- Conexión a base de datos activa

**Modelo de Datos (Campos del Ticket):**
- codigoReferencia: UUID único (ej: "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
- numero: String formato específico por cola (ej: "C01", "P15", "E03", "G02")
- nationalId: String, identificación nacional del cliente
- telefono: String, número de teléfono para Telegram
- branchOffice: String, nombre de la sucursal
- queueType: Enum (CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA)
- status: Enum (EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, CANCELADO, NO_ATENDIDO)
- positionInQueue: Integer, posición actual en cola (calculada en tiempo real)
- estimatedWaitMinutes: Integer, minutos estimados de espera
- createdAt: Timestamp, fecha/hora de creación
- assignedAdvisor: Relación a entidad Advisor (null inicialmente)
- assignedModuleNumber: Integer 1-5 (null inicialmente)

**Reglas de Negocio Aplicables:**
- RN-001: Un cliente solo puede tener 1 ticket activo a la vez
- RN-005: Número de ticket formato: [Prefijo][Número secuencial 01-99]
- RN-006: Prefijos por cola: C=Caja, P=Personal Banker, E=Empresas, G=Gerencia
- RN-010: Cálculo de tiempo estimado: posiciónEnCola × tiempoPromedioCola

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Creación exitosa de ticket para cola de Caja**
```gherkin
Given el cliente con nationalId "12345678-9" no tiene tickets activos
And el terminal está en pantalla de selección de servicio
When el cliente ingresa:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | +56912345678    |
  | branchOffice | Sucursal Centro |
  | queueType    | CAJA            |
Then el sistema genera un ticket con:
  | Campo                 | Valor Esperado                    |
  | codigoReferencia      | UUID válido                       |
  | numero                | "C[01-99]"                        |
  | status                | EN_ESPERA                         |
  | positionInQueue       | Número > 0                        |
  | estimatedWaitMinutes  | positionInQueue × 5               |
  | assignedAdvisor       | null                              |
  | assignedModuleNumber  | null                              |
And el sistema almacena el ticket en base de datos
And el sistema programa 3 mensajes de Telegram
And el sistema retorna HTTP 201 con JSON:
  {
    "identificador": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "C01",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25,
    "queueType": "CAJA"
  }
```

**Escenario 2: Error - Cliente ya tiene ticket activo**
```gherkin
Given el cliente con nationalId "12345678-9" tiene un ticket activo:
  | numero | status     | queueType       |
  | P05    | EN_ESPERA  | PERSONAL_BANKER |
When el cliente intenta crear un nuevo ticket con queueType CAJA
Then el sistema rechaza la creación
And el sistema retorna HTTP 409 Conflict con JSON:
  {
    "error": "TICKET_ACTIVO_EXISTENTE",
    "mensaje": "Ya tienes un ticket activo: P05",
    "ticketActivo": {
      "numero": "P05",
      "positionInQueue": 3,
      "estimatedWaitMinutes": 45
    }
  }
And el sistema NO crea un nuevo ticket
```

**Escenario 3: Validación - RUT/ID inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa nationalId vacío
Then el sistema retorna HTTP 400 Bad Request con JSON:
  {
    "error": "VALIDACION_FALLIDA",
    "campos": {
      "nationalId": "El RUT/ID es obligatorio"
    }
  }
And el sistema NO crea el ticket
```

**Escenario 4: Validación - Teléfono en formato inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa telefono "123"
Then el sistema retorna HTTP 400 Bad Request
And el mensaje de error especifica formato requerido "+56XXXXXXXXX"
```

**Escenario 5: Cálculo de posición - Primera persona en cola**
```gherkin
Given la cola de tipo PERSONAL_BANKER está vacía
When el cliente crea un ticket para PERSONAL_BANKER
Then el sistema calcula positionInQueue = 1
And estimatedWaitMinutes = 15
And el número de ticket es "P01"
```

**Escenario 6: Cálculo de posición - Cola con tickets existentes**
```gherkin
Given la cola de tipo EMPRESAS tiene 4 tickets EN_ESPERA
When el cliente crea un nuevo ticket para EMPRESAS
Then el sistema calcula positionInQueue = 5
And estimatedWaitMinutes = 100
And el cálculo es: 5 × 20min = 100min
```

**Escenario 7: Creación sin teléfono (cliente no quiere notificaciones)**
```gherkin
Given el cliente no proporciona número de teléfono
When el cliente crea un ticket
Then el sistema crea el ticket exitosamente
And el sistema NO programa mensajes de Telegram
```

**Postcondiciones:**
- Ticket almacenado en base de datos con estado EN_ESPERA
- 3 mensajes programados (si hay teléfono)
- Evento de auditoría registrado: "TICKET_CREADO"

**Endpoints HTTP:**
- `POST /api/tickets` - Crear nuevo ticket

---

### RF-002: Enviar Notificaciones Automáticas vía Telegram

**Descripción:** El sistema debe enviar automáticamente tres tipos de mensajes vía Telegram Bot API para mantener informado al cliente sobre el progreso de su ticket. Los mensajes se programan al crear el ticket y se procesan de forma asíncrona con reintentos automáticos en caso de fallo.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Ticket creado con teléfono válido
- Telegram Bot configurado y activo
- Cliente tiene cuenta de Telegram

**Modelo de Datos (Entidad Mensaje):**
- id: BIGSERIAL (primary key)
- ticket_id: BIGINT (foreign key a ticket)
- plantilla: String (totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno)
- estadoEnvio: Enum (PENDIENTE, ENVIADO, FALLIDO)
- fechaProgramada: Timestamp
- fechaEnvio: Timestamp (nullable)
- telegramMessageId: String (nullable, retornado por Telegram API)
- intentos: Integer (contador de reintentos, default 0)

**Plantillas de Mensajes:**

**1. totem_ticket_creado:**
```
✅ <b>Ticket Creado</b>

Tu número de turno: <b>{numero}</b>
Posición en cola: <b>#{posicion}</b>
Tiempo estimado: <b>{tiempo} minutos</b>

Te notificaremos cuando estés próximo.
```

**2. totem_proximo_turno:**
```
⏰ <b>¡Pronto será tu turno!</b>

Turno: <b>{numero}</b>
Faltan aproximadamente 3 turnos.

Por favor, acércate a la sucursal.
```

**3. totem_es_tu_turno:**
```
🔔 <b>¡ES TU TURNO {numero}!</b>

Dirígete al módulo: <b>{modulo}</b>
Asesor: <b>{nombreAsesor}</b>
```

**Reglas de Negocio Aplicables:**
- RN-007: 3 reintentos automáticos
- RN-008: Backoff exponencial (30s, 60s, 120s)
- RN-011: Auditoría de envíos
- RN-012: Mensaje 2 cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Envío exitoso del Mensaje 1 (confirmación)**
```gherkin
Given un ticket "C01" fue creado con teléfono "+56912345678"
And el sistema programó 3 mensajes automáticamente
When el scheduler procesa el mensaje "totem_ticket_creado"
Then el sistema envía POST a Telegram Bot API con:
  | Campo     | Valor                           |
  | chat_id   | +56912345678                    |
  | text      | ✅ <b>Ticket Creado</b>...      |
  | parse_mode| HTML                            |
And Telegram responde HTTP 200 con message_id "12345"
And el sistema actualiza el mensaje:
  | Campo              | Valor    |
  | estadoEnvio        | ENVIADO  |
  | fechaEnvio         | now()    |
  | telegramMessageId  | "12345"  |
  | intentos           | 1        |
And se registra evento de auditoría "MENSAJE_ENVIADO"
```

**Escenario 2: Envío exitoso del Mensaje 2 (pre-aviso)**
```gherkin
Given un ticket "P05" tiene positionInQueue = 3
When el sistema detecta que posición ≤ 3
Then el sistema programa mensaje "totem_proximo_turno"
And el mensaje contiene "Faltan aproximadamente 3 turnos"
And el sistema envía la notificación inmediatamente
```

**Escenario 3: Envío exitoso del Mensaje 3 (turno activo)**
```gherkin
Given un ticket "E02" fue asignado al asesor "María González" en módulo 3
When el sistema procesa la asignación
Then el sistema programa mensaje "totem_es_tu_turno"
And el mensaje contiene:
  | Variable      | Valor           |
  | {numero}      | E02             |
  | {modulo}      | 3               |
  | {nombreAsesor}| María González  |
And el sistema envía la notificación inmediatamente
```

**Escenario 4: Fallo de red en primer intento, éxito en segundo**
```gherkin
Given un mensaje "totem_ticket_creado" está PENDIENTE
When el scheduler intenta enviar el mensaje
And Telegram API responde HTTP 500 (error de servidor)
Then el sistema marca estadoEnvio = PENDIENTE
And incrementa intentos = 1
And programa reintento en 30 segundos (RN-008)
When el sistema reintenta después de 30 segundos
And Telegram API responde HTTP 200
Then el sistema marca estadoEnvio = ENVIADO
And actualiza fechaEnvio = now()
```

**Escenario 5: 3 reintentos fallidos → estado FALLIDO**
```gherkin
Given un mensaje ha fallado 3 veces
And los reintentos fueron en: 0s, 30s, 60s, 120s
When el cuarto intento también falla
Then el sistema marca estadoEnvio = FALLIDO
And NO programa más reintentos
And registra evento de auditoría "MENSAJE_FALLIDO"
And genera alerta para supervisión
```

**Escenario 6: Backoff exponencial entre reintentos**
```gherkin
Given un mensaje falló en el primer intento a las 10:00:00
When el sistema programa el primer reintento
Then el reintento se programa para 10:00:30 (30 segundos después)
When el primer reintento falla a las 10:00:30
Then el segundo reintento se programa para 10:01:30 (60 segundos después)
When el segundo reintento falla a las 10:01:30
Then el tercer reintento se programa para 10:03:30 (120 segundos después)
```

**Escenario 7: Cliente sin teléfono, no se programan mensajes**
```gherkin
Given un cliente crea un ticket sin proporcionar teléfono
When el sistema crea el ticket exitosamente
Then el sistema NO programa ningún mensaje de Telegram
And la tabla mensajes permanece vacía para este ticket
```

**Postcondiciones:**
- Mensaje insertado en BD con estado según resultado
- telegram_message_id almacenado si éxito
- Intentos incrementado en cada reintento
- Auditoría registrada

**Endpoints HTTP:**
- Ninguno (proceso interno automatizado por scheduler)

---

### RF-003: Calcular Posición y Tiempo Estimado

**Descripción:** El sistema debe calcular en tiempo real la posición exacta del cliente en cola y estimar el tiempo de espera basado en la posición actual, tiempo promedio de atención por tipo de cola, y cantidad de ejecutivos disponibles. El cálculo se actualiza automáticamente cuando cambia el estado de otros tickets.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Ticket existe en el sistema
- Cola del tipo correspondiente está operativa
- Base de datos accesible para consultas

**Algoritmos de Cálculo:**

**Posición en Cola:**
```
posición = COUNT(tickets WHERE queueType = mismo_tipo 
                        AND status IN ('EN_ESPERA', 'PROXIMO') 
                        AND createdAt < ticket_actual.createdAt) + 1
```

**Tiempo Estimado:**
```
tiempoEstimado = posición × tiempoPromedioCola

Donde tiempoPromedioCola:
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos  
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos
```

**Reglas de Negocio Aplicables:**
- RN-003: Orden FIFO dentro de cola (createdAt determina posición)
- RN-010: Fórmula de cálculo de tiempo estimado
- RN-012: Cambio a estado PROXIMO cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Cálculo de posición - Primera persona en cola**
```gherkin
Given la cola de tipo PERSONAL_BANKER está vacía
And no hay tickets con status EN_ESPERA o PROXIMO
When un cliente crea un ticket para PERSONAL_BANKER
Then el sistema calcula positionInQueue = 1
And estimatedWaitMinutes = 15 (1 × 15min)
And el ticket mantiene status = EN_ESPERA
```

**Escenario 2: Cálculo con tickets existentes en cola**
```gherkin
Given la cola EMPRESAS tiene los siguientes tickets:
  | numero | status    | createdAt           |
  | E01    | EN_ESPERA | 2025-01-15 10:00:00 |
  | E02    | EN_ESPERA | 2025-01-15 10:05:00 |
  | E03    | PROXIMO   | 2025-01-15 10:10:00 |
  | E04    | EN_ESPERA | 2025-01-15 10:15:00 |
When un cliente crea ticket E05 a las 10:20:00
Then el sistema calcula positionInQueue = 5
And estimatedWaitMinutes = 100 (5 × 20min)
And el orden es: E01, E02, E03, E04, E05
```

**Escenario 3: Recálculo automático cuando ticket es atendido**
```gherkin
Given la cola CAJA tiene tickets: C01, C02, C03, C04
And el ticket C03 tiene positionInQueue = 3
When el ticket C01 cambia a status ATENDIENDO
Then el sistema recalcula automáticamente:
  | numero | nueva_posicion | nuevo_tiempo |
  | C02    | 1              | 5 min        |
  | C03    | 2              | 10 min       |
  | C04    | 3              | 15 min       |
And se actualizan los registros en base de datos
```

**Escenario 4: Cambio automático a estado PROXIMO**
```gherkin
Given un ticket tiene positionInQueue = 4
And status = EN_ESPERA
When otros tickets son atendidos
And la nueva posición calculada es 3
Then el sistema cambia automáticamente status = PROXIMO
And programa el envío del Mensaje 2 (pre-aviso)
And registra evento de auditoría "TICKET_PROXIMO"
```

**Escenario 5: Consulta de posición por API**
```gherkin
Given un ticket "P07" tiene positionInQueue = 6
And estimatedWaitMinutes = 90
When el cliente consulta GET /api/tickets/P07/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "P07",
    "positionInQueue": 6,
    "estimatedWaitMinutes": 90,
    "queueType": "PERSONAL_BANKER",
    "status": "EN_ESPERA",
    "lastUpdated": "2025-01-15T10:30:00Z"
  }
```

**Escenario 6: Cálculo con diferentes tipos de cola simultáneamente**
```gherkin
Given existen tickets en múltiples colas:
  | numero | queueType       | posicion | tiempo_estimado |
  | C05    | CAJA           | 2        | 10 min          |
  | P03    | PERSONAL_BANKER| 1        | 15 min          |
  | E02    | EMPRESAS       | 3        | 60 min          |
  | G01    | GERENCIA       | 1        | 30 min          |
When el sistema calcula posiciones
Then cada cola mantiene su orden independiente
And los cálculos no se afectan entre colas diferentes
```

**Escenario 7: Manejo de tickets completados o cancelados**
```gherkin
Given la cola tiene tickets: C01(EN_ESPERA), C02(COMPLETADO), C03(EN_ESPERA)
When el sistema calcula posiciones
Then solo considera tickets activos (EN_ESPERA, PROXIMO, ATENDIENDO)
And C02(COMPLETADO) no afecta el cálculo
And C03 tiene positionInQueue = 2 (no 3)
```

**Postcondiciones:**
- Posición calculada y almacenada en base de datos
- Tiempo estimado actualizado
- Estado cambiado a PROXIMO si posición ≤ 3
- Evento de auditoría registrado si hay cambio de estado

**Endpoints HTTP:**
- `GET /api/tickets/{numero}/position` - Consultar posición actual
- `GET /api/tickets/{codigoReferencia}` - Consultar ticket completo con posición

---

### RF-004: Asignar Ticket a Ejecutivo Automáticamente

**Descripción:** El sistema debe asignar automáticamente el siguiente ticket en cola cuando un ejecutivo se libere, considerando la prioridad de colas, balanceo de carga entre ejecutivos disponibles, y orden FIFO dentro de cada cola. La asignación debe ser inmediata y notificar tanto al cliente como al ejecutivo.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Al menos un ejecutivo con status AVAILABLE
- Tickets en estado EN_ESPERA o PROXIMO en alguna cola
- Sistema de notificaciones operativo

**Modelo de Datos (Entidad Advisor):**
- id: BIGSERIAL (primary key)
- name: String, nombre completo del ejecutivo
- email: String, correo electrónico corporativo
- status: Enum (AVAILABLE, BUSY, OFFLINE)
- moduleNumber: Integer (1-5), número del módulo asignado
- assignedTicketsCount: Integer, contador de tickets asignados actualmente
- lastAssignedAt: Timestamp, última vez que recibió asignación
- createdAt: Timestamp, fecha de registro
- updatedAt: Timestamp, última actualización

**Algoritmo de Asignación:**

**1. Selección de Cola (por prioridad):**
```
PRIORIDAD_COLAS = {
  GERENCIA: 4 (máxima),
  EMPRESAS: 3,
  PERSONAL_BANKER: 2,
  CAJA: 1 (mínima)
}

SELECT queueType FROM tickets 
WHERE status IN ('EN_ESPERA', 'PROXIMO')
ORDER BY PRIORIDAD_COLAS[queueType] DESC, createdAt ASC
LIMIT 1
```

**2. Selección de Ticket (FIFO dentro de cola):**
```
SELECT * FROM tickets 
WHERE queueType = cola_seleccionada 
  AND status IN ('EN_ESPERA', 'PROXIMO')
ORDER BY createdAt ASC
LIMIT 1
```

**3. Selección de Ejecutivo (balanceo de carga):**
```
SELECT * FROM advisors 
WHERE status = 'AVAILABLE'
ORDER BY assignedTicketsCount ASC, lastAssignedAt ASC
LIMIT 1
```

**Reglas de Negocio Aplicables:**
- RN-002: Prioridad de colas (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
- RN-003: Orden FIFO dentro de cada cola
- RN-004: Balanceo de carga entre asesores disponibles
- RN-013: Solo asesores AVAILABLE reciben asignaciones

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Asignación básica con un ejecutivo disponible**
```gherkin
Given existe un ejecutivo disponible:
  | name           | status    | moduleNumber | assignedTicketsCount |
  | María González | AVAILABLE | 3            | 0                    |
And existe un ticket en cola:
  | numero | queueType | status    | createdAt           |
  | C01    | CAJA      | EN_ESPERA | 2025-01-15 10:00:00 |
When el sistema ejecuta el proceso de asignación
Then el sistema asigna el ticket C01 a María González
And actualiza el ticket:
  | campo                | valor          |
  | status               | ATENDIENDO     |
  | assignedAdvisor      | María González |
  | assignedModuleNumber | 3              |
And actualiza el ejecutivo:
  | campo                | valor |
  | status               | BUSY  |
  | assignedTicketsCount | 1     |
And programa el envío del Mensaje 3 (es tu turno)
```

**Escenario 2: Prioridad de colas - GERENCIA antes que CAJA**
```gherkin
Given existen tickets en múltiples colas:
  | numero | queueType | status    | createdAt           |
  | C01    | CAJA      | EN_ESPERA | 2025-01-15 09:00:00 |
  | G01    | GERENCIA  | EN_ESPERA | 2025-01-15 10:00:00 |
And existe un ejecutivo AVAILABLE
When el sistema ejecuta la asignación
Then el sistema asigna G01 (GERENCIA) antes que C01 (CAJA)
And respeta la prioridad: GERENCIA (4) > CAJA (1)
```

**Escenario 3: Balanceo de carga entre múltiples ejecutivos**
```gherkin
Given existen múltiples ejecutivos disponibles:
  | name        | status    | assignedTicketsCount | lastAssignedAt      |
  | Ana López   | AVAILABLE | 2                    | 2025-01-15 09:30:00 |
  | Juan Pérez  | AVAILABLE | 1                    | 2025-01-15 09:45:00 |
  | Luis Torres | AVAILABLE | 1                    | 2025-01-15 09:20:00 |
And existe un ticket P05 en cola PERSONAL_BANKER
When el sistema ejecuta la asignación
Then el sistema selecciona Juan Pérez (menor assignedTicketsCount=1)
And en caso de empate, selecciona por lastAssignedAt más antiguo
And Luis Torres tiene lastAssignedAt anterior, pero Juan tiene mismo count
```

**Escenario 4: FIFO dentro de la misma cola**
```gherkin
Given la cola EMPRESAS tiene múltiples tickets:
  | numero | status    | createdAt           |
  | E01    | EN_ESPERA | 2025-01-15 10:00:00 |
  | E02    | PROXIMO   | 2025-01-15 10:05:00 |
  | E03    | EN_ESPERA | 2025-01-15 10:10:00 |
When un ejecutivo se libera
Then el sistema asigna E01 (createdAt más antiguo)
And respeta el orden cronológico de creación
```

**Escenario 5: No hay ejecutivos disponibles**
```gherkin
Given todos los ejecutivos están ocupados:
  | name        | status  | assignedTicketsCount |
  | Ana López   | BUSY    | 1                    |
  | Juan Pérez  | OFFLINE | 0                    |
  | Luis Torres | BUSY    | 1                    |
And existen tickets EN_ESPERA
When el sistema ejecuta la asignación
Then el sistema NO asigna ningún ticket
And los tickets permanecen EN_ESPERA
And el sistema programa reintento en 30 segundos
```

**Escenario 6: Ejecutivo se libera al completar atención**
```gherkin
Given un ejecutivo está atendiendo:
  | name        | status | assignedTicketsCount | moduleNumber |
  | Ana López   | BUSY   | 1                    | 2            |
When el ejecutivo marca el ticket como COMPLETADO
Then el sistema actualiza automáticamente:
  | campo                | valor     |
  | status               | AVAILABLE |
  | assignedTicketsCount | 0         |
  | lastAssignedAt       | now()     |
And el sistema ejecuta inmediatamente el proceso de asignación
And busca el siguiente ticket en cola de mayor prioridad
```

**Escenario 7: Notificaciones tras asignación exitosa**
```gherkin
Given un ticket P03 fue asignado a ejecutivo "Carlos Ruiz" en módulo 4
When la asignación se completa exitosamente
Then el sistema programa el Mensaje 3 con variables:
  | variable      | valor       |
  | {numero}      | P03         |
  | {modulo}      | 4           |
  | {nombreAsesor}| Carlos Ruiz |
And el sistema notifica al ejecutivo en su terminal
And registra evento de auditoría "TICKET_ASIGNADO"
```

**Postcondiciones:**
- Ticket actualizado con status ATENDIENDO y ejecutivo asignado
- Ejecutivo actualizado con status BUSY y contador incrementado
- Mensaje 3 programado para envío inmediato
- Evento de auditoría registrado
- Recálculo automático de posiciones en cola

**Endpoints HTTP:**
- `PUT /api/admin/advisors/{id}/status` - Cambiar estado de ejecutivo
- `POST /api/admin/tickets/{id}/complete` - Marcar ticket como completado
- `GET /api/admin/advisors` - Listar ejecutivos y sus estados

---

### RF-005: Gestionar Múltiples Colas

**Descripción:** El sistema debe gestionar cuatro tipos de cola independientes con diferentes características operacionales: tiempo promedio de atención, prioridad para asignación, y prefijos de numeración. Cada cola opera de forma autónoma pero coordinada para optimizar la experiencia del cliente y la eficiencia operacional.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Sistema de colas inicializado
- Configuración de tipos de cola cargada
- Base de datos operativa

**Configuración de Colas:**

| Tipo de Cola | Display Name | Tiempo Promedio | Prioridad | Prefijo | Descripción |
|--------------|--------------|-----------------|-----------|---------|-------------|
| CAJA | Caja | 5 minutos | 1 (baja) | C | Transacciones básicas, depósitos, retiros |
| PERSONAL_BANKER | Personal Banker | 15 minutos | 2 (media) | P | Productos financieros, créditos, inversiones |
| EMPRESAS | Empresas | 20 minutos | 3 (media-alta) | E | Clientes corporativos, servicios empresariales |
| GERENCIA | Gerencia | 30 minutos | 4 (máxima) | G | Casos especiales, reclamos, autorizaciones |

**Operaciones por Cola:**

**1. Estadísticas en Tiempo Real:**
```sql
SELECT 
  queueType,
  COUNT(*) FILTER (WHERE status = 'EN_ESPERA') as waiting_count,
  COUNT(*) FILTER (WHERE status = 'ATENDIENDO') as being_served,
  AVG(estimatedWaitMinutes) FILTER (WHERE status IN ('EN_ESPERA', 'PROXIMO')) as avg_wait_time,
  MAX(positionInQueue) as max_position
FROM tickets 
WHERE DATE(createdAt) = CURRENT_DATE
GROUP BY queueType
```

**2. Próximos Tickets por Cola:**
```sql
SELECT numero, positionInQueue, estimatedWaitMinutes
FROM tickets 
WHERE queueType = ? 
  AND status IN ('EN_ESPERA', 'PROXIMO')
ORDER BY createdAt ASC
LIMIT 5
```

**Reglas de Negocio Aplicables:**
- RN-002: Prioridad de colas para asignación automática
- RN-005: Formato de número con prefijo específico por cola
- RN-006: Prefijos únicos por tipo de cola
- RN-010: Tiempo estimado basado en tiempo promedio de cada cola

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Operación independiente de múltiples colas**
```gherkin
Given el sistema tiene 4 colas configuradas:
  | tipo            | prefijo | tiempo_promedio | prioridad |
  | CAJA           | C       | 5               | 1         |
  | PERSONAL_BANKER| P       | 15              | 2         |
  | EMPRESAS       | E       | 20              | 3         |
  | GERENCIA       | G       | 30              | 4         |
When se crean tickets simultáneamente en todas las colas
Then cada cola mantiene su numeración independiente:
  | cola            | tickets_generados |
  | CAJA           | C01, C02, C03     |
  | PERSONAL_BANKER| P01, P02          |
  | EMPRESAS       | E01               |
  | GERENCIA       | G01, G02          |
And cada cola calcula posiciones independientemente
```

**Escenario 2: Consulta de estadísticas por cola específica**
```gherkin
Given la cola EMPRESAS tiene los siguientes tickets:
  | numero | status     | estimatedWaitMinutes |
  | E01    | ATENDIENDO | 0                    |
  | E02    | EN_ESPERA  | 20                   |
  | E03    | EN_ESPERA  | 40                   |
  | E04    | PROXIMO    | 60                   |
When se consulta GET /api/admin/queues/EMPRESAS/stats
Then el sistema retorna HTTP 200 con JSON:
  {
    "queueType": "EMPRESAS",
    "displayName": "Empresas",
    "waitingCount": 2,
    "beingServed": 1,
    "avgWaitTime": 40.0,
    "maxPosition": 3,
    "averageServiceTime": 20
  }
```

**Escenario 3: Listado de próximos tickets por cola**
```gherkin
Given la cola PERSONAL_BANKER tiene múltiples tickets esperando:
  | numero | positionInQueue | estimatedWaitMinutes | createdAt           |
  | P01    | 1               | 15                   | 2025-01-15 10:00:00 |
  | P02    | 2               | 30                   | 2025-01-15 10:05:00 |
  | P03    | 3               | 45                   | 2025-01-15 10:10:00 |
  | P04    | 4               | 60                   | 2025-01-15 10:15:00 |
When se consulta GET /api/admin/queues/PERSONAL_BANKER
Then el sistema retorna los próximos 5 tickets ordenados por createdAt
And incluye posición y tiempo estimado actualizado
```

**Escenario 4: Priorización automática entre colas**
```gherkin
Given existen tickets en múltiples colas:
  | numero | queueType       | status    | createdAt           |
  | C05    | CAJA           | EN_ESPERA | 2025-01-15 09:00:00 |
  | P03    | PERSONAL_BANKER| EN_ESPERA | 2025-01-15 09:30:00 |
  | E02    | EMPRESAS       | EN_ESPERA | 2025-01-15 10:00:00 |
  | G01    | GERENCIA       | EN_ESPERA | 2025-01-15 10:30:00 |
And un ejecutivo se libera
When el sistema ejecuta la asignación automática
Then selecciona G01 (GERENCIA, prioridad 4) primero
And respeta el orden: GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA
```

**Escenario 5: Cálculo independiente de tiempos estimados**
```gherkin
Given cada cola tiene su tiempo promedio configurado:
  | queueType       | averageTime | ticketsWaiting |
  | CAJA           | 5 min       | 3              |
  | PERSONAL_BANKER| 15 min      | 2              |
  | EMPRESAS       | 20 min      | 4              |
  | GERENCIA       | 30 min      | 1              |
When un cliente crea un ticket en cada cola (posición 4, 3, 5, 2 respectivamente)
Then los tiempos estimados son:
  | cola            | posicion | tiempo_estimado |
  | CAJA           | 4        | 20 min (4×5)    |
  | PERSONAL_BANKER| 3        | 45 min (3×15)   |
  | EMPRESAS       | 5        | 100 min (5×20)  |
  | GERENCIA       | 2        | 60 min (2×30)   |
```

**Postcondiciones:**
- Cada cola mantiene su estado independiente
- Estadísticas calculadas en tiempo real
- Priorización automática funcionando
- Numeración secuencial por cola preservada

**Endpoints HTTP:**
- `GET /api/admin/queues/{type}` - Listar tickets de una cola específica
- `GET /api/admin/queues/{type}/stats` - Estadísticas de una cola
- `GET /api/admin/queues` - Resumen de todas las colas

---

### RF-006: Consultar Estado del Ticket

**Descripción:** El sistema debe permitir al cliente consultar en cualquier momento el estado actual de su ticket, mostrando información actualizada sobre posición en cola, tiempo estimado, ejecutivo asignado si aplica, y historial de cambios de estado. La consulta puede realizarse por UUID o número de ticket.

**Prioridad:** Alta

**Actor Principal:** Cliente

**Precondiciones:**
- Ticket existe en el sistema
- Cliente conoce el UUID o número del ticket
- API de consulta disponible

**Información Retornada:**
- Datos básicos del ticket (número, tipo de cola, estado)
- Posición actual en cola (recalculada en tiempo real)
- Tiempo estimado actualizado
- Ejecutivo asignado y módulo (si aplica)
- Historial de cambios de estado
- Timestamps relevantes

**Tipos de Consulta:**

**1. Por UUID (Código de Referencia):**
```
GET /api/tickets/{codigoReferencia}
Ejemplo: GET /api/tickets/a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6
```

**2. Por Número de Ticket:**
```
GET /api/tickets/{numero}/position
Ejemplo: GET /api/tickets/P05/position
```

**Reglas de Negocio Aplicables:**
- RN-009: Estados válidos del ticket
- RN-010: Recálculo de tiempo estimado en tiempo real
- RN-012: Estado PROXIMO cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Consulta exitosa por UUID - ticket EN_ESPERA**
```gherkin
Given existe un ticket con los siguientes datos:
  | campo                | valor                                    |
  | codigoReferencia     | a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6   |
  | numero               | P05                                      |
  | queueType            | PERSONAL_BANKER                          |
  | status               | EN_ESPERA                                |
  | positionInQueue      | 4                                        |
  | estimatedWaitMinutes | 60                                       |
  | createdAt            | 2025-01-15T10:00:00Z                     |
When el cliente consulta GET /api/tickets/a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6
Then el sistema retorna HTTP 200 con JSON:
  {
    "codigoReferencia": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "P05",
    "queueType": "PERSONAL_BANKER",
    "queueDisplayName": "Personal Banker",
    "status": "EN_ESPERA",
    "positionInQueue": 4,
    "estimatedWaitMinutes": 60,
    "createdAt": "2025-01-15T10:00:00Z",
    "assignedAdvisor": null,
    "assignedModuleNumber": null,
    "lastUpdated": "2025-01-15T10:30:00Z"
  }
```

**Escenario 2: Consulta por número - ticket ATENDIENDO**
```gherkin
Given un ticket "C03" está siendo atendido:
  | campo                | valor          |
  | status               | ATENDIENDO     |
  | assignedAdvisor      | Ana López      |
  | assignedModuleNumber | 2              |
  | positionInQueue      | 0              |
When el cliente consulta GET /api/tickets/C03/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "C03",
    "status": "ATENDIENDO",
    "positionInQueue": 0,
    "estimatedWaitMinutes": 0,
    "assignedAdvisor": "Ana López",
    "assignedModuleNumber": 2,
    "message": "Tu turno está siendo atendido en el módulo 2"
  }
```

**Escenario 3: Consulta - ticket PROXIMO (posición ≤ 3)**
```gherkin
Given un ticket "E02" tiene posición 2 en cola EMPRESAS
And el sistema cambió automáticamente status = PROXIMO
When el cliente consulta el estado
Then el sistema retorna:
  {
    "numero": "E02",
    "status": "PROXIMO",
    "positionInQueue": 2,
    "estimatedWaitMinutes": 40,
    "message": "¡Pronto será tu turno! Por favor acércate a la sucursal."
  }
And indica que debe acercarse a la sucursal
```

**Escenario 4: Consulta - ticket COMPLETADO**
```gherkin
Given un ticket "G01" fue completado:
  | campo        | valor                    |
  | status       | COMPLETADO               |
  | completedAt  | 2025-01-15T11:30:00Z     |
  | servedBy     | María González           |
  | moduleNumber | 4                        |
When el cliente consulta el estado
Then el sistema retorna HTTP 200 con:
  {
    "numero": "G01",
    "status": "COMPLETADO",
    "completedAt": "2025-01-15T11:30:00Z",
    "servedBy": "María González",
    "moduleNumber": 4,
    "message": "Tu atención ha sido completada. Gracias por tu visita."
  }
```

**Escenario 5: Error - ticket no existe**
```gherkin
Given no existe un ticket con número "X99"
When el cliente consulta GET /api/tickets/X99/position
Then el sistema retorna HTTP 404 Not Found con JSON:
  {
    "error": "TICKET_NO_ENCONTRADO",
    "mensaje": "No se encontró un ticket con el número X99",
    "codigo": "TICKET_404"
  }
```

**Escenario 6: Recálculo automático de posición en consulta**
```gherkin
Given un ticket "P07" fue creado con positionInQueue = 5
And desde entonces 2 tickets anteriores fueron atendidos
When el cliente consulta el estado actual
Then el sistema recalcula automáticamente la posición
And retorna positionInQueue = 3 (actualizada)
And estimatedWaitMinutes = 45 (3 × 15min)
And el cálculo refleja el estado actual de la cola
```

**Escenario 7: Consulta con historial de cambios**
```gherkin
Given un ticket ha pasado por múltiples estados:
  | timestamp            | status     | evento           |
  | 2025-01-15T10:00:00Z | EN_ESPERA  | Ticket creado    |
  | 2025-01-15T10:25:00Z | PROXIMO    | Posición ≤ 3     |
  | 2025-01-15T10:30:00Z | ATENDIENDO | Asignado a asesor|
When el cliente consulta con parámetro ?includeHistory=true
Then el sistema incluye el historial completo:
  {
    "numero": "P05",
    "currentStatus": "ATENDIENDO",
    "history": [
      {
        "timestamp": "2025-01-15T10:00:00Z",
        "status": "EN_ESPERA",
        "event": "Ticket creado"
      },
      {
        "timestamp": "2025-01-15T10:25:00Z",
        "status": "PROXIMO",
        "event": "Posición ≤ 3"
      },
      {
        "timestamp": "2025-01-15T10:30:00Z",
        "status": "ATENDIENDO",
        "event": "Asignado a asesor"
      }
    ]
  }
```

**Postcondiciones:**
- Información actualizada retornada al cliente
- Posición recalculada en tiempo real
- Tiempo estimado actualizado
- Log de consulta registrado para auditoría

**Endpoints HTTP:**
- `GET /api/tickets/{codigoReferencia}` - Consultar por UUID
- `GET /api/tickets/{numero}/position` - Consultar por número
- `GET /api/tickets/{numero}?includeHistory=true` - Incluir historial

---

### RF-007: Panel de Monitoreo para Supervisor

**Descripción:** El sistema debe proveer un dashboard en tiempo real que permita al supervisor monitorear el estado operacional completo: resumen de tickets por estado, clientes en espera por cola, estado de ejecutivos, tiempos promedio de atención, y alertas de situaciones críticas. La información se actualiza automáticamente cada 5 segundos.

**Prioridad:** Alta

**Actor Principal:** Supervisor

**Precondiciones:**
- Usuario con rol de supervisor autenticado
- Dashboard web accesible
- Conexión a base de datos operativa
- WebSocket o polling configurado para actualizaciones

**Componentes del Dashboard:**

**1. Resumen General:**
- Total de tickets del día por estado
- Clientes actualmente en espera
- Ejecutivos disponibles vs ocupados
- Tiempo promedio de atención por cola
- Alertas críticas activas

**2. Vista por Colas:**
- Tickets en espera por cada cola
- Tiempo de espera máximo actual
- Próximos 5 tickets a ser atendidos
- Tendencia de creación de tickets (últimas 2 horas)

**3. Estado de Ejecutivos:**
- Lista de todos los ejecutivos con estado actual
- Tiempo en estado actual
- Tickets atendidos en el día
- Módulo asignado

**4. Alertas y Métricas:**
- Cola crítica (>15 personas esperando)
- Ejecutivo inactivo (>30 min en OFFLINE)
- Tiempo de espera excesivo (>60 min)
- Fallos de mensajería Telegram

**Consultas de Dashboard:**

**1. Resumen General:**
```sql
SELECT 
  COUNT(*) FILTER (WHERE status = 'EN_ESPERA') as tickets_waiting,
  COUNT(*) FILTER (WHERE status = 'ATENDIENDO') as tickets_being_served,
  COUNT(*) FILTER (WHERE status = 'COMPLETADO' AND DATE(createdAt) = CURRENT_DATE) as tickets_completed_today,
  COUNT(DISTINCT assignedAdvisor) FILTER (WHERE status = 'ATENDIENDO') as advisors_busy,
  (SELECT COUNT(*) FROM advisors WHERE status = 'AVAILABLE') as advisors_available
FROM tickets 
WHERE DATE(createdAt) = CURRENT_DATE
```

**2. Estado por Colas:**
```sql
SELECT 
  queueType,
  COUNT(*) FILTER (WHERE status IN ('EN_ESPERA', 'PROXIMO')) as waiting_count,
  MAX(estimatedWaitMinutes) as max_wait_time,
  AVG(estimatedWaitMinutes) FILTER (WHERE status IN ('EN_ESPERA', 'PROXIMO')) as avg_wait_time
FROM tickets 
WHERE DATE(createdAt) = CURRENT_DATE
GROUP BY queueType
```

**Reglas de Negocio Aplicables:**
- RN-013: Estados de ejecutivos para monitoreo
- RN-002: Priorización de colas en vista de supervisor
- RN-011: Auditoría para trazabilidad de eventos

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Dashboard muestra resumen general correctamente**
```gherkin
Given el sistema tiene los siguientes tickets del día:
  | status      | cantidad |
  | EN_ESPERA   | 12       |
  | ATENDIENDO  | 5        |
  | COMPLETADO  | 45       |
  | CANCELADO   | 3        |
And hay 3 ejecutivos AVAILABLE y 2 BUSY
When el supervisor accede al dashboard
Then el resumen general muestra:
  | métrica                | valor |
  | tickets_waiting        | 12    |
  | tickets_being_served   | 5     |
  | tickets_completed_today| 45    |
  | advisors_available     | 3     |
  | advisors_busy          | 2     |
And la información se actualiza cada 5 segundos
```

**Escenario 2: Vista detallada por colas**
```gherkin
Given las colas tienen el siguiente estado:
  | queueType       | waiting | max_wait | avg_wait |
  | CAJA           | 8       | 25       | 15       |
  | PERSONAL_BANKER| 4       | 60       | 45       |
  | EMPRESAS       | 2       | 40       | 30       |
  | GERENCIA       | 1       | 30       | 30       |
When el supervisor consulta GET /api/admin/dashboard
Then el sistema retorna HTTP 200 con JSON:
  {
    "summary": {
      "totalWaiting": 15,
      "totalBeingServed": 5,
      "completedToday": 45
    },
    "queueStats": [
      {
        "queueType": "CAJA",
        "waitingCount": 8,
        "maxWaitTime": 25,
        "avgWaitTime": 15
      },
      {
        "queueType": "PERSONAL_BANKER",
        "waitingCount": 4,
        "maxWaitTime": 60,
        "avgWaitTime": 45
      }
    ]
  }
```

**Escenario 3: Estado de ejecutivos en tiempo real**
```gherkin
Given los ejecutivos tienen el siguiente estado:
  | name           | status    | moduleNumber | currentTicket | timeInStatus |
  | Ana López      | BUSY      | 2            | C05           | 15 min       |
  | Juan Pérez     | AVAILABLE | 3            | null          | 5 min        |
  | María González | OFFLINE   | 4            | null          | 45 min       |
  | Luis Torres    | BUSY      | 1            | P03           | 8 min        |
When el supervisor consulta GET /api/admin/advisors
Then el sistema retorna la lista completa con:
  {
    "advisors": [
      {
        "name": "Ana López",
        "status": "BUSY",
        "moduleNumber": 2,
        "currentTicket": "C05",
        "timeInCurrentStatus": "15 min",
        "ticketsServedToday": 8
      }
    ]
  }
```

**Escenario 4: Alertas críticas automáticas**
```gherkin
Given la cola PERSONAL_BANKER tiene 16 personas esperando
And el ejecutivo "María González" está OFFLINE por 35 minutos
And hay un ticket con tiempo de espera de 75 minutos
When el sistema evalúa las condiciones de alerta
Then genera las siguientes alertas:
  {
    "alerts": [
      {
        "type": "COLA_CRITICA",
        "message": "Cola PERSONAL_BANKER tiene 16 personas esperando (límite: 15)",
        "severity": "HIGH",
        "timestamp": "2025-01-15T10:30:00Z"
      },
      {
        "type": "EJECUTIVO_INACTIVO",
        "message": "María González lleva 35 min OFFLINE (límite: 30 min)",
        "severity": "MEDIUM",
        "timestamp": "2025-01-15T10:30:00Z"
      },
      {
        "type": "ESPERA_EXCESIVA",
        "message": "Ticket P07 lleva 75 min esperando (límite: 60 min)",
        "severity": "HIGH",
        "timestamp": "2025-01-15T10:30:00Z"
      }
    ]
  }
```

**Escenario 5: Actualización automática cada 5 segundos**
```gherkin
Given el supervisor tiene el dashboard abierto
And la página se cargó a las 10:00:00
When pasan 5 segundos (10:00:05)
Then el sistema ejecuta automáticamente una nueva consulta
And actualiza los datos sin recargar la página
And muestra un indicador de "Última actualización: 10:00:05"
And continúa actualizando cada 5 segundos
```

**Escenario 6: Próximos tickets por cola**
```gherkin
Given la cola EMPRESAS tiene los siguientes tickets esperando:
  | numero | positionInQueue | estimatedWaitMinutes | createdAt           |
  | E01    | 1               | 20                   | 2025-01-15 10:00:00 |
  | E02    | 2               | 40                   | 2025-01-15 10:05:00 |
  | E03    | 3               | 60                   | 2025-01-15 10:10:00 |
When el supervisor consulta los próximos tickets de EMPRESAS
Then el sistema muestra los próximos 5 tickets ordenados por posición
And incluye tiempo estimado actualizado
And resalta el próximo ticket a ser asignado (E01)
```

**Postcondiciones:**
- Dashboard actualizado con información en tiempo real
- Alertas generadas y mostradas al supervisor
- Métricas calculadas y almacenadas para histórico
- Log de acceso registrado para auditoría

**Endpoints HTTP:**
- `GET /api/admin/dashboard` - Resumen completo del dashboard
- `GET /api/admin/summary` - Métricas generales
- `GET /api/admin/advisors` - Estado de todos los ejecutivos
- `GET /api/admin/advisors/stats` - Estadísticas de ejecutivos
- `PUT /api/admin/advisors/{id}/status` - Cambiar estado de ejecutivo
- `GET /api/admin/alerts` - Alertas activas

---

### RF-008: Registrar Auditoría de Eventos

**Descripción:** El sistema debe registrar automáticamente todos los eventos relevantes del ciclo de vida de tickets, cambios de estado de ejecutivos, envío de mensajes, y acciones administrativas. La auditoría debe incluir timestamp, tipo de evento, actor involucrado, entidad afectada, y detalles de cambios para garantizar trazabilidad completa y cumplimiento regulatorio.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Sistema de auditoría inicializado
- Base de datos con tabla de auditoría configurada
- Eventos del sistema funcionando correctamente

**Modelo de Datos (Entidad AuditLog):**
- id: BIGSERIAL (primary key)
- timestamp: TIMESTAMP WITH TIME ZONE (momento exacto del evento)
- eventType: String (TICKET_CREADO, TICKET_ASIGNADO, MENSAJE_ENVIADO, etc.)
- actor: String (identificación del actor: cliente, ejecutivo, sistema)
- entityType: String (TICKET, ADVISOR, MESSAGE)
- entityId: String (ID de la entidad afectada)
- oldValues: JSONB (valores anteriores, nullable)
- newValues: JSONB (valores nuevos)
- additionalData: JSONB (metadata adicional, nullable)
- ipAddress: String (dirección IP del origen, nullable)
- userAgent: String (información del navegador/cliente, nullable)

**Tipos de Eventos a Auditar:**

**1. Eventos de Tickets:**
- TICKET_CREADO: Cliente crea nuevo ticket
- TICKET_ASIGNADO: Sistema asigna ticket a ejecutivo
- TICKET_COMPLETADO: Ejecutivo completa atención
- TICKET_CANCELADO: Cliente o sistema cancela ticket
- TICKET_NO_ATENDIDO: Cliente no se presenta
- TICKET_ESTADO_CAMBIADO: Cambio automático de estado (EN_ESPERA → PROXIMO)

**2. Eventos de Mensajería:**
- MENSAJE_PROGRAMADO: Sistema programa mensaje para envío
- MENSAJE_ENVIADO: Mensaje enviado exitosamente a Telegram
- MENSAJE_FALLIDO: Fallo en envío de mensaje
- MENSAJE_REINTENTADO: Reintento de envío de mensaje

**3. Eventos de Ejecutivos:**
- ADVISOR_ESTADO_CAMBIADO: Cambio de estado (AVAILABLE ↔ BUSY ↔ OFFLINE)
- ADVISOR_ASIGNACION: Ejecutivo recibe asignación de ticket
- ADVISOR_LIBERADO: Ejecutivo se libera tras completar atención

**4. Eventos Administrativos:**
- ADMIN_LOGIN: Supervisor accede al dashboard
- ADMIN_CONFIG_CHANGED: Cambio en configuración del sistema
- ADMIN_ALERT_GENERATED: Sistema genera alerta crítica

**Reglas de Negocio Aplicables:**
- RN-011: Auditoría obligatoria para todos los eventos críticos
- Retención: 7 años para cumplimiento regulatorio
- Inmutabilidad: Registros de auditoría no pueden ser modificados
- Integridad: Checksums para detectar manipulación

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Auditoría de creación de ticket**
```gherkin
Given un cliente con nationalId "12345678-9" crea un ticket
When el sistema procesa la creación exitosamente
Then se registra automáticamente un evento de auditoría:
  | campo         | valor                           |
  | eventType     | TICKET_CREADO                   |
  | actor         | cliente:12345678-9              |
  | entityType    | TICKET                          |
  | entityId      | {UUID del ticket}               |
  | oldValues     | null                            |
  | newValues     | {datos completos del ticket}    |
  | timestamp     | 2025-01-15T10:00:00.123Z        |
And el registro es inmutable
And incluye metadata como IP y user agent
```

**Escenario 2: Auditoría de asignación de ticket**
```gherkin
Given un ticket "P05" en estado EN_ESPERA
And un ejecutivo "Ana López" disponible
When el sistema asigna automáticamente el ticket
Then se registran 2 eventos de auditoría:
  # Evento 1: Cambio de estado del ticket
  | eventType     | TICKET_ASIGNADO                 |
  | actor         | sistema:auto-assignment         |
  | entityType    | TICKET                          |
  | entityId      | P05                             |
  | oldValues     | {"status": "EN_ESPERA"}         |
  | newValues     | {"status": "ATENDIENDO", "assignedAdvisor": "Ana López"} |
  
  # Evento 2: Cambio de estado del ejecutivo
  | eventType     | ADVISOR_ASIGNACION              |
  | actor         | sistema:auto-assignment         |
  | entityType    | ADVISOR                         |
  | entityId      | ana.lopez@banco.com             |
  | oldValues     | {"status": "AVAILABLE"}         |
  | newValues     | {"status": "BUSY", "assignedTicketsCount": 1} |
```

**Escenario 3: Auditoría de envío de mensaje**
```gherkin
Given un mensaje "totem_ticket_creado" está programado
When el scheduler intenta enviarlo a Telegram
And el envío es exitoso con message_id "12345"
Then se registra evento de auditoría:
  | eventType     | MENSAJE_ENVIADO                 |
  | actor         | sistema:telegram-scheduler      |
  | entityType    | MESSAGE                         |
  | entityId      | {ID del mensaje}                |
  | oldValues     | {"estadoEnvio": "PENDIENTE"}    |
  | newValues     | {"estadoEnvio": "ENVIADO", "telegramMessageId": "12345"} |
  | additionalData| {"plantilla": "totem_ticket_creado", "intentos": 1} |
```

**Escenario 4: Auditoría de fallo en mensaje con reintentos**
```gherkin
Given un mensaje falló 3 veces consecutivas
When el sistema lo marca como FALLIDO
Then se registran múltiples eventos:
  # Eventos de reintento (3 registros)
  | eventType     | MENSAJE_REINTENTADO             |
  | additionalData| {"intento": 1, "error": "HTTP 500"} |
  
  | eventType     | MENSAJE_REINTENTADO             |
  | additionalData| {"intento": 2, "error": "Timeout"} |
  
  | eventType     | MENSAJE_REINTENTADO             |
  | additionalData| {"intento": 3, "error": "HTTP 503"} |
  
  # Evento final de fallo
  | eventType     | MENSAJE_FALLIDO                 |
  | newValues     | {"estadoEnvio": "FALLIDO"}      |
  | additionalData| {"totalIntentos": 3, "ultimoError": "HTTP 503"} |
```

**Escenario 5: Auditoría de acceso administrativo**
```gherkin
Given un supervisor accede al dashboard
When se autentica exitosamente
Then se registra evento de auditoría:
  | eventType     | ADMIN_LOGIN                     |
  | actor         | supervisor:maria.gonzalez       |
  | entityType    | ADMIN_SESSION                   |
  | entityId      | {session ID}                    |
  | ipAddress     | 192.168.1.100                   |
  | userAgent     | Mozilla/5.0 (Windows NT 10.0)  |
  | additionalData| {"loginMethod": "credentials", "dashboardAccessed": true} |
```

**Escenario 6: Consulta de auditoría por entidad**
```gherkin
Given un ticket "P05" ha pasado por múltiples estados
And tiene varios eventos de auditoría registrados
When el administrador consulta GET /api/admin/audit/ticket/P05
Then el sistema retorna el historial completo ordenado por timestamp:
  {
    "entityId": "P05",
    "entityType": "TICKET",
    "events": [
      {
        "timestamp": "2025-01-15T10:00:00Z",
        "eventType": "TICKET_CREADO",
        "actor": "cliente:12345678-9",
        "changes": {
          "status": {"old": null, "new": "EN_ESPERA"}
        }
      },
      {
        "timestamp": "2025-01-15T10:25:00Z",
        "eventType": "TICKET_ESTADO_CAMBIADO",
        "actor": "sistema:position-calculator",
        "changes": {
          "status": {"old": "EN_ESPERA", "new": "PROXIMO"}
        }
      }
    ]
  }
```

**Postcondiciones:**
- Evento registrado de forma inmutable en base de datos
- Timestamp con precisión de milisegundos
- Integridad de datos garantizada
- Disponible para consultas de auditoría
- Cumplimiento regulatorio asegurado

**Endpoints HTTP:**
- `GET /api/admin/audit/ticket/{id}` - Auditoría de un ticket específico
- `GET /api/admin/audit/advisor/{id}` - Auditoría de un ejecutivo
- `GET /api/admin/audit/events?type={eventType}` - Eventos por tipo
- `GET /api/admin/audit/search?actor={actor}&from={date}&to={date}` - Búsqueda avanzada

---

## 5. Matrices de Trazabilidad

### 5.1 Matriz RF → Beneficio → Endpoints

| RF | Requerimiento | Beneficio de Negocio | Endpoints HTTP | Prioridad |
|----|---------------|---------------------|----------------|----------|
| RF-001 | Crear Ticket Digital | Digitalización del proceso, eliminación de papel | `POST /api/tickets` | Alta |
| RF-002 | Notificaciones Telegram | Movilidad del cliente, reducción de abandonos | Ninguno (automatizado) | Alta |
| RF-003 | Calcular Posición y Tiempo | Transparencia, gestión de expectativas | `GET /api/tickets/{numero}/position` | Alta |
| RF-004 | Asignar Ticket Automáticamente | Eficiencia operacional, balanceo de carga | `PUT /api/admin/advisors/{id}/status` | Alta |
| RF-005 | Gestionar Múltiples Colas | Segmentación de servicios, priorización | `GET /api/admin/queues/{type}` | Alta |
| RF-006 | Consultar Estado | Autoservicio, reducción de consultas | `GET /api/tickets/{uuid}` | Alta |
| RF-007 | Panel de Monitoreo | Supervisión operacional, alertas proactivas | `GET /api/admin/dashboard` | Alta |
| RF-008 | Auditoría de Eventos | Cumplimiento regulatorio, trazabilidad | `GET /api/admin/audit/ticket/{id}` | Alta |

### 5.2 Matriz de Dependencias entre RFs

| RF Origen | RF Dependiente | Tipo de Dependencia | Descripción |
|-----------|----------------|--------------------|--------------|
| RF-001 | RF-002 | Secuencial | Crear ticket antes de enviar notificaciones |
| RF-001 | RF-003 | Concurrente | Calcular posición al crear ticket |
| RF-003 | RF-004 | Trigger | Cambio de posición activa asignación |
| RF-004 | RF-002 | Secuencial | Asignación dispara Mensaje 3 |
| RF-001 | RF-008 | Concurrente | Crear ticket genera evento de auditoría |
| RF-004 | RF-008 | Concurrente | Asignación genera evento de auditoría |
| RF-002 | RF-008 | Concurrente | Envío de mensaje genera evento de auditoría |
| RF-007 | RF-005 | Consulta | Dashboard consulta estado de colas |

### 5.3 Matriz de Endpoints HTTP

| Método | Endpoint | RF | Descripción | Autenticación |
|--------|----------|----|--------------|--------------|
| POST | `/api/tickets` | RF-001 | Crear nuevo ticket | No |
| GET | `/api/tickets/{uuid}` | RF-006 | Consultar ticket por UUID | No |
| GET | `/api/tickets/{numero}/position` | RF-003 | Consultar posición actual | No |
| GET | `/api/admin/dashboard` | RF-007 | Dashboard completo | Sí |
| GET | `/api/admin/summary` | RF-007 | Métricas generales | Sí |
| GET | `/api/admin/queues/{type}` | RF-005 | Tickets por cola | Sí |
| GET | `/api/admin/queues/{type}/stats` | RF-005 | Estadísticas de cola | Sí |
| GET | `/api/admin/advisors` | RF-007 | Estado de ejecutivos | Sí |
| PUT | `/api/admin/advisors/{id}/status` | RF-004 | Cambiar estado ejecutivo | Sí |
| GET | `/api/admin/audit/ticket/{id}` | RF-008 | Auditoría de ticket | Sí |
| GET | `/api/admin/alerts` | RF-007 | Alertas activas | Sí |

## 6. Modelo de Datos Consolidado

### 6.1 Entidades Principales

**Ticket** (RF-001, RF-003, RF-006)
- codigoReferencia: UUID (PK)
- numero: String (UK)
- nationalId: String
- telefono: String (nullable)
- branchOffice: String
- queueType: Enum
- status: Enum
- positionInQueue: Integer
- estimatedWaitMinutes: Integer
- createdAt: Timestamp
- assignedAdvisor: FK → Advisor
- assignedModuleNumber: Integer

**Advisor** (RF-004, RF-007)
- id: BIGSERIAL (PK)
- name: String
- email: String (UK)
- status: Enum
- moduleNumber: Integer
- assignedTicketsCount: Integer
- lastAssignedAt: Timestamp
- createdAt: Timestamp
- updatedAt: Timestamp

**Message** (RF-002)
- id: BIGSERIAL (PK)
- ticket_id: FK → Ticket
- plantilla: String
- estadoEnvio: Enum
- fechaProgramada: Timestamp
- fechaEnvio: Timestamp (nullable)
- telegramMessageId: String (nullable)
- intentos: Integer

**AuditLog** (RF-008)
- id: BIGSERIAL (PK)
- timestamp: Timestamp
- eventType: String
- actor: String
- entityType: String
- entityId: String
- oldValues: JSONB (nullable)
- newValues: JSONB
- additionalData: JSONB (nullable)
- ipAddress: String (nullable)
- userAgent: String (nullable)

### 6.2 Enumeraciones

**QueueType:** CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA  
**TicketStatus:** EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, CANCELADO, NO_ATENDIDO  
**AdvisorStatus:** AVAILABLE, BUSY, OFFLINE  
**MessageTemplate:** totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno  
**MessageStatus:** PENDIENTE, ENVIADO, FALLIDO

## 7. Casos de Uso Principales

### CU-001: Flujo Completo de Atención
**Actor:** Cliente  
**Flujo:**
1. Cliente crea ticket (RF-001)
2. Sistema envía Mensaje 1 (RF-002)
3. Sistema calcula posición (RF-003)
4. Cliente sale de sucursal
5. Sistema envía Mensaje 2 cuando posición ≤ 3 (RF-002)
6. Sistema asigna a ejecutivo disponible (RF-004)
7. Sistema envía Mensaje 3 (RF-002)
8. Cliente consulta estado (RF-006)
9. Ejecutivo atiende y completa
10. Sistema registra auditoría (RF-008)

### CU-002: Supervisión Operacional
**Actor:** Supervisor  
**Flujo:**
1. Supervisor accede al dashboard (RF-007)
2. Revisa estado de colas (RF-005)
3. Monitorea ejecutivos (RF-007)
4. Recibe alertas críticas (RF-007)
5. Consulta auditoría si necesario (RF-008)
6. Ajusta estados de ejecutivos (RF-004)

### CU-003: Gestión de Fallos
**Actor:** Sistema  
**Flujo:**
1. Falla envío de mensaje (RF-002)
2. Sistema reintenta con backoff (RF-002)
3. Registra intentos en auditoría (RF-008)
4. Genera alerta si falla definitivamente (RF-007)
5. Supervisor revisa y toma acción (RF-007)

## 8. Validaciones y Reglas de Formato

### 8.1 Validaciones de Entrada

**RUT/ID Nacional:**
- Formato: 12345678-9 (Chile) o equivalente por país
- Validación: Algoritmo de dígito verificador
- Obligatorio para crear ticket

**Teléfono:**
- Formato: +56912345678 (internacional)
- Validación: Regex `^\+[1-9]\d{1,14}$`
- Opcional (si no se proporciona, no se envían mensajes)

**Número de Ticket:**
- Formato: [C|P|E|G][01-99]
- Ejemplos: C01, P15, E03, G02
- Único por día y por cola

### 8.2 Reglas de Negocio Críticas

**Unicidad:** Un cliente = 1 ticket activo máximo  
**FIFO:** Orden cronológico dentro de cada cola  
**Prioridad:** GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA  
**Balanceo:** Ejecutivo con menor assignedTicketsCount  
**Reintentos:** 3 intentos con backoff exponencial (30s, 60s, 120s)  
**Auditoría:** Todos los eventos críticos registrados

## 9. Checklist de Validación

### 9.1 Completitud
- ✅ 8 Requerimientos Funcionales documentados
- ✅ 13 Reglas de Negocio numeradas
- ✅ 44+ Escenarios Gherkin totales
- ✅ 11 Endpoints HTTP mapeados
- ✅ 4 Entidades principales definidas
- ✅ 5 Enumeraciones especificadas

### 9.2 Claridad
- ✅ Formato Gherkin correcto (Given/When/Then/And)
- ✅ Ejemplos JSON en respuestas HTTP
- ✅ Sin ambigüedades en descripciones
- ✅ Algoritmos matemáticos explícitos

### 9.3 Trazabilidad
- ✅ Cada RF mapea a beneficio de negocio
- ✅ Dependencias entre RFs documentadas
- ✅ Reglas de negocio aplicadas por RF
- ✅ Endpoints mapeados a funcionalidades

### 9.4 Verificabilidad
- ✅ Criterios de aceptación medibles
- ✅ Ejemplos concretos con datos
- ✅ Casos de error especificados
- ✅ Postcondiciones claras

## 10. Glosario

| Término | Definición |
|---------|------------|
| **Ticket** | Turno digital asignado a un cliente para ser atendido |
| **Cola** | Fila virtual de tickets esperando atención por tipo de servicio |
| **Asesor/Ejecutivo** | Empleado bancario que atiende clientes en módulos |
| **Módulo** | Estación de trabajo numerada (1-5) donde atiende un ejecutivo |
| **UUID** | Identificador único universal para tickets (36 caracteres) |
| **FIFO** | First In, First Out - orden cronológico de atención |
| **Backoff Exponencial** | Incremento progresivo de tiempo entre reintentos |
| **Chat ID** | Identificador único de usuario en Telegram |
| **Plantilla** | Formato predefinido de mensaje con variables dinámicas |
| **Auditoría** | Registro inmutable de eventos para trazabilidad |

---

**Documento completado:** 8 RF + 13 RN + 44 Escenarios + 11 Endpoints + Matrices de Trazabilidad  
**Preparado para:** Diseño de Arquitectura (PROMPT 2)  
**Validación:** ✅ Completitud, Claridad, Trazabilidad, Verificabilidad