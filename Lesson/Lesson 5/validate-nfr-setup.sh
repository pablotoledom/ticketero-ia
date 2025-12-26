#!/bin/bash
# =============================================================================
# TICKETERO - NFR Test Suite Validation
# =============================================================================
# Validates that the NFR test suite is properly configured
# Usage: ./validate-nfr-setup.sh
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        TICKETERO - NFR TEST SUITE VALIDATION                  ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

ERRORS=0

# Check directory structure
echo -e "${YELLOW}1. Validating directory structure...${NC}"

REQUIRED_DIRS=(
    "scripts/utils"
    "scripts/performance" 
    "scripts/concurrency"
    "scripts/resilience"
    "k6"
    "results"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo -e "   ✓ $dir"
    else
        echo -e "   ${RED}✗ $dir (missing)${NC}"
        ERRORS=$((ERRORS + 1))
    fi
done

# Check required scripts
echo -e "${YELLOW}2. Validating test scripts...${NC}"

REQUIRED_SCRIPTS=(
    "scripts/utils/metrics-collector.sh"
    "scripts/utils/validate-consistency.sh"
    "scripts/performance/load-test.sh"
    "scripts/concurrency/race-condition-test.sh"
    "scripts/resilience/worker-crash-test.sh"
    "k6/load-test.js"
    "run-nfr-tests.sh"
)

for script in "${REQUIRED_SCRIPTS[@]}"; do
    if [ -f "$script" ]; then
        if [ -x "$script" ] || [[ "$script" == *.js ]]; then
            echo -e "   ✓ $script"
        else
            echo -e "   ${YELLOW}⚠ $script (not executable)${NC}"
        fi
    else
        echo -e "   ${RED}✗ $script (missing)${NC}"
        ERRORS=$((ERRORS + 1))
    fi
done

# Check documentation
echo -e "${YELLOW}3. Validating documentation...${NC}"

if [ -f "docs/NFR-TEST-RESULTS.md" ]; then
    echo -e "   ✓ docs/NFR-TEST-RESULTS.md"
else
    echo -e "   ${RED}✗ docs/NFR-TEST-RESULTS.md (missing)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Check dependencies
echo -e "${YELLOW}4. Checking dependencies...${NC}"

# Check if bc is available (for calculations)
if command -v bc &> /dev/null; then
    echo -e "   ✓ bc (calculator)"
else
    echo -e "   ${YELLOW}⚠ bc (calculator) - install with: apt-get install bc${NC}"
fi

# Check if curl is available
if command -v curl &> /dev/null; then
    echo -e "   ✓ curl"
else
    echo -e "   ${RED}✗ curl (required)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Check if docker is available
if command -v docker &> /dev/null; then
    echo -e "   ✓ docker"
else
    echo -e "   ${RED}✗ docker (required)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Check optional K6
if command -v k6 &> /dev/null; then
    echo -e "   ✓ k6 (optional - enhanced performance testing)"
else
    echo -e "   ${YELLOW}⚠ k6 (optional) - install from https://k6.io${NC}"
fi

# Summary
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ $ERRORS -eq 0 ]; then
    echo -e "  ${GREEN}✅ NFR TEST SUITE SETUP VALID${NC}"
    echo ""
    echo -e "  Ready to run tests:"
    echo -e "    ${CYAN}./run-nfr-tests.sh all${NC}          # All tests"
    echo -e "    ${CYAN}./run-nfr-tests.sh performance${NC}  # Performance only"
    echo -e "    ${CYAN}./run-nfr-tests.sh concurrency${NC}  # Concurrency only"
    echo -e "    ${CYAN}./run-nfr-tests.sh resilience${NC}   # Resilience only"
    echo ""
    exit 0
else
    echo -e "  ${RED}❌ $ERRORS VALIDATION ERRORS${NC}"
    echo ""
    echo -e "  Fix the errors above before running tests."
    echo ""
    exit 1
fi