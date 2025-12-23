# Diagrama de Arquitectura AWS - Sistema Ticketero

## Arquitectura del Sistema de Gestión de Tickets

```
Usuario (Cliente en Sucursal)
    ↓
CloudFront (Entrega de Contenido)
    ↓
Application Load Balancer (Distribución de Carga)
    ↓
┌─────────────────────────────────────────────────────────────┐
│                    Amazon ECS Fargate                       │
│                 (Lógica de Negocio)                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Spring Boot Application                │   │
│  │  • Ticket Management                               │   │
│  │  • Queue Management                                │   │
│  │  • Executive Management                            │   │
│  │  • Notification Service                            │   │
│  │  • Monitoring Dashboard                            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
    ↓                           ↓                        ↓
Amazon RDS PostgreSQL      Amazon SQS              Amazon SNS
(Almacenamiento de Datos)  (Cola de Mensajes)      (Notificaciones)
    ↓                           ↓                        ↓
Amazon S3                  AWS Lambda               Telegram Bot API
(Logs y Backups)          (Procesamiento           (Mensajes a Clientes)
                          de Notificaciones)
    ↓
CloudWatch (Monitoreo y Alertas)
    ↓
Amazon ElastiCache Redis (Cache de Sesiones)
```

## Componentes de la Arquitectura

### **Capa de Presentación**
- **CloudFront**: Distribución global de contenido estático del dashboard
- **Application Load Balancer**: Balanceador de carga para alta disponibilidad

### **Capa de Aplicación**
- **Amazon ECS Fargate**: Contenedores serverless para la aplicación Spring Boot
- **Auto Scaling**: Escalado automático basado en métricas de CPU y memoria

### **Capa de Datos**
- **Amazon RDS PostgreSQL**: Base de datos principal con Multi-AZ para alta disponibilidad
- **Amazon ElastiCache Redis**: Cache para sesiones y datos frecuentemente accedidos
- **Amazon S3**: Almacenamiento de logs, backups y archivos estáticos

### **Servicios de Mensajería**
- **Amazon SQS**: Cola de mensajes para procesamiento asíncrono
- **Amazon SNS**: Servicio de notificaciones para integración con Telegram
- **AWS Lambda**: Funciones serverless para procesamiento de notificaciones

### **Monitoreo y Observabilidad**
- **CloudWatch**: Métricas, logs y alertas del sistema
- **AWS X-Ray**: Trazabilidad distribuida para debugging

## Flujo de Datos

### **1. Creación de Ticket**
```
Cliente → CloudFront → ALB → ECS Fargate → RDS PostgreSQL
                                    ↓
                              SQS → Lambda → SNS → Telegram
```

### **2. Dashboard en Tiempo Real**
```
Supervisor → CloudFront → ALB → ECS Fargate → ElastiCache Redis
                                        ↓
                                 WebSocket Connection
```

### **3. Procesamiento de Notificaciones**
```
ECS Fargate → SQS → Lambda → SNS → Telegram Bot API
                ↓
            CloudWatch Logs
```

## Configuración de Servicios

### **Amazon ECS Fargate**
- **CPU**: 1 vCPU (escalable hasta 4 vCPU)
- **Memoria**: 2 GB (escalable hasta 8 GB)
- **Auto Scaling**: Basado en CPU > 70% y memoria > 80%
- **Health Checks**: Spring Actuator endpoints

### **Amazon RDS PostgreSQL**
- **Instancia**: db.t3.medium (escalable)
- **Multi-AZ**: Habilitado para alta disponibilidad
- **Backup**: Automático con retención de 7 días
- **Encryption**: En reposo y en tránsito

### **Amazon ElastiCache Redis**
- **Nodo**: cache.t3.micro (escalable)
- **Cluster Mode**: Habilitado para alta disponibilidad
- **TTL**: Configurado por tipo de dato

### **Amazon SQS**
- **Tipo**: Standard Queue
- **Visibility Timeout**: 30 segundos
- **Dead Letter Queue**: Configurada para mensajes fallidos

## Seguridad

### **Controles de Acceso**
- **IAM Roles**: Principio de menor privilegio
- **Security Groups**: Tráfico restringido por puerto y protocolo
- **VPC**: Red privada virtual con subnets públicas y privadas

### **Encriptación**
- **En Tránsito**: TLS 1.3 para todas las comunicaciones
- **En Reposo**: KMS para RDS, S3 y EBS
- **Secrets**: AWS Secrets Manager para credenciales

### **Monitoreo de Seguridad**
- **CloudTrail**: Auditoría de API calls
- **GuardDuty**: Detección de amenazas
- **Config**: Compliance y configuración

## Estimación de Costos (Mensual)

### **Fase Piloto (1 Sucursal - 800 tickets/día)**
- ECS Fargate: $30-50
- RDS PostgreSQL: $25-40
- ElastiCache Redis: $15-25
- CloudFront + ALB: $10-20
- SQS + SNS + Lambda: $5-10
- **Total Estimado**: $85-145/mes

### **Fase Expansión (5 Sucursales - 3,000 tickets/día)**
- ECS Fargate: $120-200
- RDS PostgreSQL: $80-120
- ElastiCache Redis: $40-60
- CloudFront + ALB: $25-40
- SQS + SNS + Lambda: $15-25
- **Total Estimado**: $280-445/mes

### **Fase Nacional (50+ Sucursales - 25,000+ tickets/día)**
- ECS Fargate: $800-1,200
- RDS PostgreSQL: $400-600
- ElastiCache Redis: $200-300
- CloudFront + ALB: $100-150
- SQS + SNS + Lambda: $80-120
- **Total Estimado**: $1,580-2,370/mes

## Ventajas de esta Arquitectura

### **Escalabilidad**
- Auto Scaling automático basado en demanda
- Servicios serverless para picos de carga
- Cache distribuido para mejor performance

### **Disponibilidad**
- Multi-AZ deployment para RDS
- Load Balancer con health checks
- Failover automático en caso de fallas

### **Mantenibilidad**
- Infraestructura como código (CloudFormation/CDK)
- Monitoreo centralizado con CloudWatch
- Logs estructurados para debugging

### **Seguridad**
- Encriptación end-to-end
- Network isolation con VPC
- Compliance con estándares financieros

---

**Creado**: Diciembre 2025  
**Versión**: 1.0  
**Estándar**: AWS Well-Architected Framework