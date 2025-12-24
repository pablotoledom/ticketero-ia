#!/bin/bash
# =============================================================================
# TICKETERO - Non-Functional Test Suite Runner
# =============================================================================
# Ejecuta todos los tests no funcionales del sistema Ticketero
# Usage: ./run-nfr-tests.sh [test_category]
# Categories: all, performance, concurrency, resilience
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Create results directory
mkdir -p "$RESULTS_DIR"

# Test category
CATEGORY=${1:-all}

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                TICKETERO - NFR TEST SUITE                     ║${NC}"
echo -e "${BLUE}║              Non-Functional Requirements                      ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Categoría: ${CATEGORY}${NC}"
echo -e "${CYAN}Timestamp: ${TIMESTAMP}${NC}"
echo ""

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run a test and track results
run_test() {
    local test_name="$1"
    local test_script="$2"
    local category="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}  EJECUTANDO: $test_name${NC}"
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
    
    local start_time=$(date +%s)
    
    if bash "$test_script" 2>&1 | tee "$RESULTS_DIR/${category}-${test_name,,}-${TIMESTAMP}.log"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        echo -e "${GREEN}✅ $test_name PASSED${NC} (${duration}s)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        echo -e "${RED}❌ $test_name FAILED${NC} (${duration}s)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Verificando prerequisitos...${NC}"
    
    # Check Docker containers
    if ! docker ps | grep -q "ticketero-app"; then
        echo -e "${RED}❌ Container ticketero-app no está ejecutándose${NC}"
        exit 1
    fi
    
    if ! docker ps | grep -q "ticketero-postgres"; then
        echo -e "${RED}❌ Container ticketero-postgres no está ejecutándose${NC}"
        exit 1
    fi
    
    if ! docker ps | grep -q "ticketero-rabbitmq"; then
        echo -e "${RED}❌ Container ticketero-rabbitmq no está ejecutándose${NC}"
        exit 1
    fi
    
    # Check API health
    if ! curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        echo -e "${RED}❌ API no está disponible en http://localhost:8080${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Todos los prerequisitos cumplidos${NC}"
    echo ""
}

# Performance Tests
run_performance_tests() {
    echo -e "${CYAN}🚀 PERFORMANCE TESTS${NC}"
    echo ""
    
    run_test "PERF-01-Load-Test-Sostenido" "$SCRIPT_DIR/scripts/performance/load-test.sh" "performance"
    run_test "PERF-02-Spike-Test" "$SCRIPT_DIR/scripts/performance/spike-test.sh" "performance"
    run_test "PERF-03-Soak-Test" "$SCRIPT_DIR/scripts/performance/soak-test.sh" "performance"
}

# Concurrency Tests
run_concurrency_tests() {
    echo -e "${CYAN}⚡ CONCURRENCY TESTS${NC}"
    echo ""
    
    run_test "CONC-01-Race-Condition" "$SCRIPT_DIR/scripts/concurrency/race-condition-test.sh" "concurrency"
    run_test "CONC-02-Idempotency" "$SCRIPT_DIR/scripts/concurrency/idempotency-test.sh" "concurrency"
}

# Resilience Tests
run_resilience_tests() {
    echo -e "${CYAN}🛡️ RESILIENCE TESTS${NC}"
    echo ""
    
    run_test "RES-01-Worker-Crash" "$SCRIPT_DIR/scripts/resilience/worker-crash-test.sh" "resilience"
    run_test "RES-02-RabbitMQ-Failure" "$SCRIPT_DIR/scripts/resilience/rabbitmq-failure-test.sh" "resilience"
}

# Consistency Tests
run_consistency_tests() {
    echo -e "${CYAN}🔒 CONSISTENCY TESTS${NC}"
    echo ""
    
    run_test "CONS-01-Outbox-Atomicity" "$SCRIPT_DIR/scripts/consistency/outbox-atomicity-test.sh" "consistency"
}

# Shutdown Tests
run_shutdown_tests() {
    echo -e "${CYAN}🔄 SHUTDOWN TESTS${NC}"
    echo ""
    
    run_test "SHUT-01-Graceful-Shutdown" "$SCRIPT_DIR/scripts/shutdown/graceful-shutdown-test.sh" "shutdown"
}

# Scalability Tests
run_scalability_tests() {
    echo -e "${CYAN}📈 SCALABILITY TESTS${NC}"
    echo ""
    
    run_test "SCAL-01-Baseline" "$SCRIPT_DIR/scripts/scalability/baseline-test.sh" "scalability"
}

# Generate final report
generate_report() {
    local report_file="$RESULTS_DIR/nfr-test-report-${TIMESTAMP}.md"
    
    cat > "$report_file" << EOF
# TICKETERO - Non-Functional Test Report

**Timestamp:** $(date)
**Category:** $CATEGORY
**Duration:** $(($(date +%s) - SUITE_START_TIME)) seconds

## Summary

- **Total Tests:** $TOTAL_TESTS
- **Passed:** $PASSED_TESTS
- **Failed:** $FAILED_TESTS
- **Success Rate:** $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%

## Test Results

EOF

    if [ $FAILED_TESTS -eq 0 ]; then
        echo "✅ **ALL TESTS PASSED**" >> "$report_file"
    else
        echo "❌ **$FAILED_TESTS TESTS FAILED**" >> "$report_file"
    fi
    
    echo ""
    echo -e "${CYAN}📊 Reporte generado: $report_file${NC}"
}

# Main execution
SUITE_START_TIME=$(date +%s)

check_prerequisites

case $CATEGORY in
    "performance")
        run_performance_tests
        ;;
    "concurrency")
        run_concurrency_tests
        ;;
    "resilience")
        run_resilience_tests
        ;;
    "consistency")
        run_consistency_tests
        ;;
    "shutdown")
        run_shutdown_tests
        ;;
    "scalability")
        run_scalability_tests
        ;;
    "all")
        run_performance_tests
        run_concurrency_tests
        run_resilience_tests
        run_consistency_tests
        run_shutdown_tests
        run_scalability_tests
        ;;
    *)
        echo -e "${RED}❌ Categoría inválida: $CATEGORY${NC}"
        echo "Categorías válidas: all, performance, concurrency, resilience, consistency, shutdown, scalability"
        exit 1
        ;;
esac

# Final summary
echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    RESUMEN FINAL                              ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  Total Tests:    $TOTAL_TESTS"
echo -e "  Passed:         ${GREEN}$PASSED_TESTS${NC}"
echo -e "  Failed:         ${RED}$FAILED_TESTS${NC}"
echo -e "  Success Rate:   $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%"
echo ""

generate_report

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL NFR TESTS PASSED!${NC}"
    exit 0
else
    echo -e "${RED}💥 $FAILED_TESTS TESTS FAILED${NC}"
    exit 1
fi