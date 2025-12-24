#!/bin/bash
# =============================================================================
# TICKETERO - Idempotency Test
# =============================================================================
# Valida que tickets ya procesados no se reprocesan
# Usage: ./scripts/concurrency/idempotency-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - IDEMPOTENCY TEST (CONC-02)                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Configurando escenario...${NC}"
docker exec ticketero-postgres psql -U dev -d ticketero -c "
    DELETE FROM ticket_event;
    DELETE FROM recovery_event;
    DELETE FROM outbox_message;
    DELETE FROM ticket;
    UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0;
" > /dev/null 2>&1

# Crear y esperar que se complete un ticket
echo -e "${YELLOW}2. Creando ticket y esperando procesamiento...${NC}"

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/tickets" \
    -H "Content-Type: application/json" \
    -d '{
        "nationalId": "70000001",
        "telefono": "+56912345678",
        "branchOffice": "Sucursal Test",
        "queueType": "CAJA"
    }')

TICKET_ID=$(echo "$RESPONSE" | grep -o '"numero":"[^"]*"' | cut -d'"' -f4)
echo "   ✓ Ticket creado: $TICKET_ID"

# Esperar procesamiento
sleep 30

# Capturar estado
INITIAL_COMPLETED=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
INITIAL_EVENTS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket_event;" | xargs)

echo "   Estado inicial:"
echo "   - Tickets completados: $INITIAL_COMPLETED"
echo "   - Eventos registrados: $INITIAL_EVENTS"

# Simular redelivery creando ticket duplicado
echo -e "${YELLOW}3. Simulando ticket duplicado...${NC}"

curl -s -X POST "http://localhost:8080/api/tickets" \
    -H "Content-Type: application/json" \
    -d '{
        "nationalId": "70000001",
        "telefono": "+56912345678",
        "branchOffice": "Sucursal Test",
        "queueType": "CAJA"
    }' > /dev/null

# Esperar procesamiento del duplicado
echo -e "${YELLOW}4. Esperando procesamiento (10s)...${NC}"
sleep 10

# Validar que nada cambió
FINAL_COMPLETED=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
FINAL_EVENTS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket_event;" | xargs)
TOTAL_TICKETS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket;" | xargs)

echo -e "${YELLOW}5. Validando idempotencia...${NC}"
echo ""
echo "   Estado final:"
echo "   - Tickets completados: $FINAL_COMPLETED"
echo "   - Eventos registrados: $FINAL_EVENTS"
echo "   - Total tickets: $TOTAL_TICKETS"
echo ""

PASS=true

# Validar que se creó segundo ticket pero no se duplicó procesamiento
if [ "$TOTAL_TICKETS" -eq 2 ]; then
    echo -e "   - Segundo ticket creado: ${GREEN}PASS${NC}"
else
    echo -e "   - Segundo ticket creado: ${RED}FAIL${NC} ($TOTAL_TICKETS tickets)"
    PASS=false
fi

if [ "$FINAL_COMPLETED" -eq 1 ]; then
    echo -e "   - Solo 1 procesado: ${GREEN}PASS${NC}"
else
    echo -e "   - Solo 1 procesado: ${RED}FAIL${NC} ($FINAL_COMPLETED procesados)"
    PASS=false
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$PASS" = true ]; then
    echo -e "  ${GREEN}✅ IDEMPOTENCY TEST PASSED${NC}"
    exit 0
else
    echo -e "  ${RED}❌ IDEMPOTENCY TEST FAILED${NC}"
    exit 1
fi