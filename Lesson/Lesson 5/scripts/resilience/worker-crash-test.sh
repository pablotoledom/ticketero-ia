#!/bin/bash
# =============================================================================
# TICKETERO - Worker Crash Test
# =============================================================================
# Simula crash de worker y valida auto-recovery
# Usage: ./scripts/resilience/worker-crash-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - WORKER CRASH TEST (RES-01)                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Configurando escenario...${NC}"
docker exec ticketero-postgres psql -U dev -d ticketero -c "
    DELETE FROM ticket_event;
    DELETE FROM recovery_event;
    DELETE FROM outbox_message;
    DELETE FROM ticket;
    UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0, recovery_count = 0;
" > /dev/null 2>&1

# Contar recovery events iniciales
INITIAL_RECOVERIES=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM recovery_event;" | xargs)

# Crear ticket
echo -e "${YELLOW}2. Creando ticket...${NC}"
curl -s -X POST "http://localhost:8080/api/tickets" \
    -H "Content-Type: application/json" \
    -d '{
        "nationalId": "90000001",
        "telefono": "+56912345678",
        "branchOffice": "Sucursal Test",
        "queueType": "CAJA"
    }' > /dev/null

# Esperar que empiece procesamiento
echo -e "${YELLOW}3. Esperando inicio de procesamiento...${NC}"
sleep 5

# Simular crash: detener heartbeat de un asesor BUSY
echo -e "${YELLOW}4. Simulando crash de worker...${NC}"
docker exec ticketero-postgres psql -U dev -d ticketero -c "
    UPDATE advisor 
    SET last_heartbeat = NOW() - INTERVAL '120 seconds'
    WHERE status = 'BUSY'
    LIMIT 1;
" > /dev/null 2>&1

echo "   ✓ Heartbeat detenido (simulando worker muerto)"

# Esperar detección (recovery check cada 30s, timeout 60s)
echo -e "${YELLOW}5. Esperando detección de recovery (max 120s)...${NC}"
START_TIME=$(date +%s)
MAX_WAIT=120
DETECTED=false

while [ $(($(date +%s) - START_TIME)) -lt $MAX_WAIT ]; do
    CURRENT_RECOVERIES=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM recovery_event WHERE recovery_type='DEAD_WORKER';" | xargs)
    
    if [ "$CURRENT_RECOVERIES" -gt "$INITIAL_RECOVERIES" ]; then
        DETECTION_TIME=$(($(date +%s) - START_TIME))
        DETECTED=true
        echo ""
        echo "   ✓ Recovery detectado en ${DETECTION_TIME}s"
        break
    fi
    
    echo -ne "\r   Esperando... $(( $(date +%s) - START_TIME ))s    "
    sleep 5
done

echo ""

# Validar resultados
echo -e "${YELLOW}6. Validando resultados...${NC}"
echo ""

# Check 1: Recovery event registrado
RECOVERIES=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM recovery_event WHERE recovery_type='DEAD_WORKER';" | xargs)

if [ "$DETECTED" = true ]; then
    echo -e "   - Recovery detectado: ${GREEN}PASS${NC} (${DETECTION_TIME}s)"
else
    echo -e "   - Recovery detectado: ${RED}FAIL${NC} (timeout)"
fi

# Check 2: Asesor liberado
BUSY_ADVISORS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM advisor WHERE status='BUSY';" | xargs)

if [ "$BUSY_ADVISORS" -eq 0 ]; then
    echo -e "   - Asesor liberado: ${GREEN}PASS${NC}"
else
    echo -e "   - Asesor liberado: ${YELLOW}WARN${NC} ($BUSY_ADVISORS aún BUSY)"
fi

# Check 3: Recovery count incrementado
RECOVERY_COUNT=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT SUM(recovery_count) FROM advisor;" | xargs)

if [ "$RECOVERY_COUNT" -gt 0 ]; then
    echo -e "   - Recovery count: ${GREEN}$RECOVERY_COUNT${NC}"
else
    echo -e "   - Recovery count: ${YELLOW}0${NC}"
fi

# Check 4: Tiempo de detección < 90s
if [ "$DETECTED" = true ] && [ "$DETECTION_TIME" -lt 90 ]; then
    echo -e "   - Tiempo < 90s: ${GREEN}PASS${NC}"
else
    echo -e "   - Tiempo < 90s: ${RED}FAIL${NC}"
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$DETECTED" = true ] && [ "$DETECTION_TIME" -lt 90 ]; then
    echo -e "  ${GREEN}✅ WORKER CRASH TEST PASSED${NC}"
    exit 0
else
    echo -e "  ${RED}❌ WORKER CRASH TEST FAILED${NC}"
    exit 1
fi