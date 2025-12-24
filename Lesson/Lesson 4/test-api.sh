#!/bin/bash

# Script de pruebas para la API Ticketero
# Uso: ./test-api.sh

BASE_URL="http://localhost:8080/api/tickets"

echo "🎫 Probando API del Sistema Ticketero"
echo "======================================"

# 1. Health Check
echo "1. Health Check..."
curl -s "$BASE_URL/health" | jq '.'
echo ""

# 2. Crear ticket GENERAL
echo "2. Creando ticket GENERAL..."
TICKET_RESPONSE=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678",
    "telefono": "555-0001",
    "branchOffice": "Sucursal Centro",
    "queueType": "GENERAL"
  }')

echo "$TICKET_RESPONSE" | jq '.'
CODIGO_REF=$(echo "$TICKET_RESPONSE" | jq -r '.codigoReferencia')
echo "Código de referencia: $CODIGO_REF"
echo ""

# 3. Crear ticket PREFERENCIAL
echo "3. Creando ticket PREFERENCIAL..."
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "87654321",
    "telefono": "555-0002",
    "branchOffice": "Sucursal Norte",
    "queueType": "PREFERENCIAL"
  }' | jq '.'
echo ""

# 4. Consultar estado del ticket
echo "4. Consultando estado del ticket $CODIGO_REF..."
curl -s "$BASE_URL/$CODIGO_REF/status" | jq '.'
echo ""

# 5. Actualizar estado del ticket
echo "5. Actualizando estado del ticket a IN_PROGRESS..."
curl -s -X PUT "$BASE_URL/$CODIGO_REF/status" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "assignedAdvisor": "Ana García",
    "assignedModuleNumber": 1
  }' | jq '.'
echo ""

# 6. Obtener tickets en espera
echo "6. Obteniendo tickets en espera..."
curl -s "$BASE_URL/waiting?branchOffice=Sucursal Centro&queueType=GENERAL" | jq '.'
echo ""

# 7. Estadísticas del sistema
echo "7. Estadísticas del sistema..."
curl -s "$BASE_URL/stats" | jq '.'
echo ""

# 8. Cancelar ticket
echo "8. Cancelando ticket $CODIGO_REF..."
curl -s -X DELETE "$BASE_URL/$CODIGO_REF" | jq '.'
echo ""

echo "✅ Pruebas completadas!"