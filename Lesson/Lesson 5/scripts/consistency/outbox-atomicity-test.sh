#!/bin/bash
# =============================================================================
# TICKETERO - Outbox Atomicity Test
# =============================================================================
# Valida atomicidad Ticket + Outbox en misma transacción
# Usage: ./scripts/consistency/outbox-atomicity-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - OUTBOX ATOMICITY TEST (CONS-01)                ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Limpiando estado...${NC}"
docker exec ticketero-postgres psql -U dev -d ticketero -c "
    DELETE FROM outbox_message;
    DELETE FROM ticket;
" > /dev/null 2>&1

# Crear 100 tickets simultáneos para probar atomicidad
echo -e "${YELLOW}2. Creando 100 tickets simultáneamente...${NC}"
START_TIME=$(date +%s)

for i in $(seq 1 100); do
    (
        curl -s -X POST "http://localhost:8080/api/tickets" \
            -H "Content-Type: application/json" \
            -d "{
                \"nationalId\": \"800$(printf '%05d' $i)\",
                \"telefono\": \"+56912345678\",
                \"branchOffice\": \"Sucursal Test\",
                \"queueType\": \"CAJA\"
            }" > /dev/null
    ) &
done

wait
CREATE_END=$(date +%s)
CREATE_TIME=$((CREATE_END - START_TIME))
echo "   ✓ 100 tickets creados en ${CREATE_TIME}s"

# Verificar atomicidad: cada ticket debe tener exactamente 1 mensaje outbox
TICKETS_COUNT=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket;" | xargs)
OUTBOX_COUNT=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM outbox_message;" | xargs)

echo "   ✓ Tickets en DB: $TICKETS_COUNT"
echo "   ✓ Mensajes en Outbox: $OUTBOX_COUNT"

# Esperar que todos se publiquen
echo -e "${YELLOW}3. Esperando publicación a RabbitMQ (max 30s)...${NC}"
MAX_WAIT=30
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    PENDING=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM outbox_message WHERE status='PENDING';" | xargs)
    
    if [ "$PENDING" -eq 0 ]; then
        PUBLISH_END=$(date +%s)
        PUBLISH_TIME=$((PUBLISH_END - CREATE_END))
        echo "   ✓ Todos publicados en ${PUBLISH_TIME}s"
        break
    fi
    
    echo -ne "\r   Pendientes: $PENDING    "
    sleep 2
    WAITED=$((WAITED + 2))
done

echo ""

# Validar atomicidad
echo -e "${YELLOW}4. Validando atomicidad...${NC}"

SENT=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM outbox_message WHERE status='SENT';" | xargs)
FAILED=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM outbox_message WHERE status='FAILED';" | xargs)

# Verificar que no hay tickets huérfanos (sin mensaje outbox)
ORPHAN_TICKETS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c "
    SELECT COUNT(*) FROM ticket t
    WHERE NOT EXISTS (
        SELECT 1 FROM outbox_message o 
        WHERE o.aggregate_id = t.id::text
    );
" | xargs)

# Verificar que no hay mensajes outbox huérfanos (sin ticket)
ORPHAN_MESSAGES=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c "
    SELECT COUNT(*) FROM outbox_message o
    WHERE NOT EXISTS (
        SELECT 1 FROM ticket t 
        WHERE t.id::text = o.aggregate_id
    );
" | xargs)

echo ""
echo "   Resultados atomicidad:"
echo "   - Tickets: $TICKETS_COUNT"
echo "   - Outbox messages: $OUTBOX_COUNT"
echo "   - Messages SENT: $SENT"
echo "   - Messages FAILED: $FAILED"
echo "   - Tickets huérfanos: $ORPHAN_TICKETS"
echo "   - Messages huérfanos: $ORPHAN_MESSAGES"
echo ""

PASS=true

# Validaciones
if [ "$TICKETS_COUNT" -eq "$OUTBOX_COUNT" ]; then
    echo -e "   - 1:1 Ticket:Outbox: ${GREEN}PASS${NC}"
else
    echo -e "   - 1:1 Ticket:Outbox: ${RED}FAIL${NC} ($TICKETS_COUNT:$OUTBOX_COUNT)"
    PASS=false
fi

if [ "$ORPHAN_TICKETS" -eq 0 ]; then
    echo -e "   - 0 tickets huérfanos: ${GREEN}PASS${NC}"
else
    echo -e "   - 0 tickets huérfanos: ${RED}FAIL${NC} ($ORPHAN_TICKETS)"
    PASS=false
fi

if [ "$ORPHAN_MESSAGES" -eq 0 ]; then
    echo -e "   - 0 messages huérfanos: ${GREEN}PASS${NC}"
else
    echo -e "   - 0 messages huérfanos: ${RED}FAIL${NC} ($ORPHAN_MESSAGES)"
    PASS=false
fi

if [ "$SENT" -eq 100 ]; then
    echo -e "   - 100% enviados: ${GREEN}PASS${NC}"
else
    echo -e "   - 100% enviados: ${RED}FAIL${NC} ($SENT/100)"
    PASS=false
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$PASS" = true ]; then
    echo -e "  ${GREEN}✅ OUTBOX ATOMICITY TEST PASSED${NC}"
    echo "  Transacciones atómicas funcionando correctamente"
    exit 0
else
    echo -e "  ${RED}❌ OUTBOX ATOMICITY TEST FAILED${NC}"
    exit 1
fi