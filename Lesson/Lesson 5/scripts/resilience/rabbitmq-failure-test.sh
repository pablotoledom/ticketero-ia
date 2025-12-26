#!/bin/bash
# =============================================================================
# TICKETERO - RabbitMQ Failure Test
# =============================================================================
# Simula caída de RabbitMQ y valida que Outbox acumula sin perder mensajes
# Usage: ./scripts/resilience/rabbitmq-failure-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - RABBITMQ FAILURE TEST (RES-02)                 ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Limpiando estado...${NC}"
docker exec ticketero-postgres psql -U dev -d ticketero -c "
    DELETE FROM outbox_message;
    DELETE FROM ticket;
" > /dev/null 2>&1

# Detener RabbitMQ
echo -e "${YELLOW}2. Deteniendo RabbitMQ (30 segundos)...${NC}"
docker stop ticketero-rabbitmq > /dev/null 2>&1

# Crear tickets mientras RabbitMQ está caído
echo -e "${YELLOW}3. Creando 10 tickets (RabbitMQ caído)...${NC}"

for i in $(seq 1 10); do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/api/tickets" \
        -H "Content-Type: application/json" \
        -d "{
            \"nationalId\": \"91000$(printf '%03d' $i)\",
            \"telefono\": \"+56912345678\",
            \"branchOffice\": \"Sucursal Test\",
            \"queueType\": \"CAJA\"
        }")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -ne "\r   Creados: $i/10    "
    else
        echo -e "\r   ${RED}Error en ticket $i: HTTP $HTTP_CODE${NC}"
    fi
    
    sleep 1
done

echo ""

# Verificar mensajes acumulados en Outbox
PENDING=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM outbox_message WHERE status='PENDING';" | xargs)
echo "   ✓ Mensajes en Outbox (PENDING): $PENDING"

# Reiniciar RabbitMQ
echo -e "${YELLOW}4. Reiniciando RabbitMQ...${NC}"
docker start ticketero-rabbitmq > /dev/null 2>&1

# Esperar que RabbitMQ esté listo
sleep 15
echo "   ✓ RabbitMQ reiniciado"

# Esperar que Outbox procese los mensajes pendientes
echo -e "${YELLOW}5. Esperando procesamiento de Outbox (max 30s)...${NC}"
MAX_WAIT=30
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    STILL_PENDING=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM outbox_message WHERE status='PENDING';" | xargs)
    
    if [ "$STILL_PENDING" -eq 0 ]; then
        echo "   ✓ Todos los mensajes procesados"
        break
    fi
    
    echo -ne "\r   Pendientes: $STILL_PENDING    "
    sleep 3
    WAITED=$((WAITED + 3))
done

echo ""

# Validar resultados
echo -e "${YELLOW}6. Validando resultados...${NC}"
echo ""

SENT=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM outbox_message WHERE status='SENT';" | xargs)
FAILED=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM outbox_message WHERE status='FAILED';" | xargs)
PENDING=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM outbox_message WHERE status='PENDING';" | xargs)

echo "   Outbox status:"
echo "   - SENT:    $SENT"
echo "   - FAILED:  $FAILED"
echo "   - PENDING: $PENDING"
echo ""

PASS=true

# Check: Todos enviados sin pérdida
if [ "$SENT" -eq 10 ]; then
    echo -e "   - 0 mensajes perdidos: ${GREEN}PASS${NC}"
else
    echo -e "   - 0 mensajes perdidos: ${RED}FAIL${NC} ($SENT/10 enviados)"
    PASS=false
fi

if [ "$FAILED" -eq 0 ]; then
    echo -e "   - 0 mensajes fallidos: ${GREEN}PASS${NC}"
else
    echo -e "   - 0 mensajes fallidos: ${YELLOW}WARN${NC} ($FAILED)"
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$PASS" = true ]; then
    echo -e "  ${GREEN}✅ RABBITMQ FAILURE TEST PASSED${NC}"
    echo "  Outbox Pattern funcionando correctamente"
    exit 0
else
    echo -e "  ${RED}❌ RABBITMQ FAILURE TEST FAILED${NC}"
    exit 1
fi