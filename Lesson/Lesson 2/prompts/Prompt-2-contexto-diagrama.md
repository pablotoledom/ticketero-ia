# Prompt para Generación de Diagramas de Arquitectura AWS

	⁠Utilizando la tool de AWS Diagram, crea un diagrama de arquitectura con las siguientes especificaciones (grábalo dentro de la carpeta /docs):

## Requisitos de Diseño

### Direccionalidad y Flujo
•⁠  ⁠*Dirección*: Flujo de datos de izquierda a derecha
•⁠  ⁠*Estilo de Líneas*: Solo líneas ortogonales/rectas
•⁠  ⁠*Espaciado*: Espaciado adecuado entre componentes para evitar desbordamiento de etiquetas
•⁠  ⁠*Inicio*: Comenzar con un ícono de usuario iniciando la solicitud

### Requisitos de Íconos
•⁠  ⁠*Consistencia*: Aplicar tamaño y estilo uniforme de íconos
•⁠  ⁠*Reconocimiento*: Usar íconos estándar de servicios AWS para identificación inmediata
•⁠  ⁠*Colores*: Usar colores y estilos consistentes de servicios AWS

## Pautas de Etiquetado

### Incluir
•⁠  ⁠*Nombres de Servicios*: Solo nombres claros de servicios AWS
•⁠  ⁠*Posicionamiento*: Agregar contexto funcional dentro de la arquitectura (ejemplo: "Lógica de Negocio", "Almacenamiento de Datos")

### Excluir
•⁠  ⁠Nombres de endpoints de API
•⁠  ⁠Nombres de tablas de base de datos
•⁠  ⁠Detalles técnicos de implementación

## Estándares de Formato
•⁠  ⁠Mantener jerarquía visual adecuada con agrupación
•⁠  ⁠Asegurar espacio en blanco adecuado para legibilidad
•⁠  ⁠Aplicar convenciones estándar de diagramas de arquitectura AWS Well-Architected

## Estructura de Ejemplo


Usuario (iniciando solicitud)
    ↓
CloudFront (Entrega de Contenido)
    ↓
API Gateway (Enrutamiento de Solicitudes)
    ↓
┌─────────────────────────────────────┐
│  Funciones Lambda (Lógica de Negocio) │
│  - ProcessOrder                       │
│  - ValidatePayment                    │
│  - SendNotification                   │
└─────────────────────────────────────┘
    ↓
DynamoDB (Almacenamiento de Datos)


## Instrucciones de Uso

1.⁠ ⁠*Describe tu arquitectura de sistema* o proporciona detalles de componentes AWS a incluir
2.⁠ ⁠*Especifica cualquier servicio AWS particular* a destacar o enfatizar
3.⁠ ⁠*Menciona si ciertos componentes deben ser agrupados* (VPC, subnets, security groups, etc.)
4.⁠ ⁠*Indica flujos especiales* (asíncronos, paralelos, condicionales)
5.⁠ ⁠El diagrama seguirá automáticamente los *estándares visuales de AWS Well-Architected Framework*

