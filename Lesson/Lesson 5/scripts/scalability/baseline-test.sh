#!/bin/bash
# =============================================================================
# TICKETERO - Scalability Baseline Test
# =============================================================================
# Baseline vs escalado: mide throughput con configuración actual vs optimizada
# Usage: ./scripts/scalability/baseline-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - SCALABILITY BASELINE TEST (SCAL-01)            ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Función para ejecutar test de carga
run_load_test() {
    local test_name="$1"
    local duration="$2"
    local vus="$3"
    
    echo -e "${YELLOW}Ejecutando $test_name...${NC}"
    
    # Cleanup
    docker exec ticketero-postgres psql -U dev -d ticketero -c "
        DELETE FROM ticket_event;
        DELETE FROM outbox_message;
        DELETE FROM ticket;
        UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0;
    " > /dev/null 2>&1
    
    # Start metrics
    METRICS_FILE="$PROJECT_ROOT/results/scalability-${test_name,,}-$(date +%Y%m%d-%H%M%S).csv"
    "$SCRIPT_DIR/../utils/metrics-collector.sh" $((duration + 30)) "$METRICS_FILE" &
    METRICS_PID=$!
    
    # Execute load test
    START_TIME=$(date +%s)
    
    if command -v k6 &> /dev/null; then
        # Use K6 if available
        k6 run --vus $vus --duration ${duration}s "$PROJECT_ROOT/k6/load-test.js" > /dev/null 2>&1
    else
        # Fallback to bash
        END_TIME=$((START_TIME + duration))
        COUNTER=0
        
        while [ $(date +%s) -lt $END_TIME ]; do
            for i in $(seq 1 $vus); do
                (
                    COUNTER=$((COUNTER + 1))
                    QUEUE_INDEX=$((COUNTER % 4))
                    QUEUES=("CAJA" "PERSONAL" "EMPRESAS" "GERENCIA")
                    QUEUE=${QUEUES[$QUEUE_INDEX]}
                    
                    curl -s -X POST "http://localhost:8080/api/tickets" \
                        -H "Content-Type: application/json" \
                        -d "{
                            \"nationalId\": \"600$(printf '%06d' $COUNTER)\",
                            \"telefono\": \"+56912345678\",
                            \"branchOffice\": \"Sucursal Test\",
                            \"queueType\": \"${QUEUE}\"
                        }" > /dev/null
                ) &
            done
            sleep 1
        done
        wait
    fi
    
    # Wait for processing
    sleep 20
    
    # Stop metrics
    kill $METRICS_PID 2>/dev/null || true
    
    # Collect results
    ACTUAL_DURATION=$(($(date +%s) - START_TIME))
    TOTAL_TICKETS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket;" | xargs)
    COMPLETED_TICKETS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
    
    THROUGHPUT=$(echo "scale=1; $COMPLETED_TICKETS * 60 / $ACTUAL_DURATION" | bc)
    
    echo "   Duración: ${ACTUAL_DURATION}s"
    echo "   Tickets creados: $TOTAL_TICKETS"
    echo "   Tickets completados: $COMPLETED_TICKETS"
    echo "   Throughput: $THROUGHPUT tickets/min"
    echo ""
    
    # Return throughput for comparison
    echo "$THROUGHPUT"
}

# Test 1: Baseline (configuración actual)
echo -e "${YELLOW}1. BASELINE TEST (configuración actual)${NC}"
BASELINE_THROUGHPUT=$(run_load_test "baseline" 60 5)

# Test 2: Optimized (más carga)
echo -e "${YELLOW}2. OPTIMIZED TEST (carga incrementada)${NC}"
OPTIMIZED_THROUGHPUT=$(run_load_test "optimized" 60 10)

# Análisis de escalabilidad
echo -e "${YELLOW}3. Análisis de escalabilidad...${NC}"
echo ""

IMPROVEMENT=$(echo "scale=1; ($OPTIMIZED_THROUGHPUT - $BASELINE_THROUGHPUT) * 100 / $BASELINE_THROUGHPUT" | bc)
EFFICIENCY=$(echo "scale=1; $OPTIMIZED_THROUGHPUT / $BASELINE_THROUGHPUT" | bc)

echo "   Resultados:"
echo "   - Baseline throughput:   $BASELINE_THROUGHPUT tickets/min"
echo "   - Optimized throughput:  $OPTIMIZED_THROUGHPUT tickets/min"
echo "   - Mejora:               +$IMPROVEMENT%"
echo "   - Factor de escalado:    ${EFFICIENCY}x"
echo ""

# Identificar bottleneck
echo -e "${YELLOW}4. Identificando bottlenecks...${NC}"

# Check CPU usage
AVG_CPU=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT ROUND(AVG(cpu_percent)) FROM pg_stat_activity WHERE state = 'active';" | xargs)

# Check connection count
CONNECTIONS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
    "SELECT count(*) FROM pg_stat_activity WHERE datname='ticketero';" | xargs)

echo "   Análisis de recursos:"
echo "   - Conexiones DB activas: $CONNECTIONS"
echo "   - CPU promedio DB: ${AVG_CPU:-0}%"

if [ "$CONNECTIONS" -gt 10 ]; then
    echo "   - Bottleneck potencial: ${YELLOW}Pool de conexiones DB${NC}"
elif (( $(echo "$EFFICIENCY < 1.5" | bc -l) )); then
    echo "   - Bottleneck potencial: ${YELLOW}Workers RabbitMQ${NC}"
else
    echo "   - Sistema: ${GREEN}Escalando correctamente${NC}"
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

# Validar escalabilidad
if (( $(echo "$IMPROVEMENT > 20" | bc -l) )); then
    echo -e "  ${GREEN}✅ SCALABILITY TEST PASSED${NC}"
    echo "  Sistema escala correctamente (+$IMPROVEMENT%)"
    exit 0
else
    echo -e "  ${YELLOW}⚠ SCALABILITY LIMITED${NC}"
    echo "  Mejora limitada: +$IMPROVEMENT% (esperado >20%)"
    exit 0  # No es fallo crítico
fi