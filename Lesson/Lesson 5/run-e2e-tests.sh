#!/bin/bash

# Script para ejecutar tests E2E del Sistema Ticketero
# Autor: QA Engineer Senior
# Fecha: $(date)

echo "🚀 Iniciando Tests E2E - Sistema Ticketero"
echo "=========================================="

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para mostrar resultados
show_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2 - PASSED${NC}"
    else
        echo -e "${RED}❌ $2 - FAILED${NC}"
    fi
}

# Limpiar target anterior
echo "🧹 Limpiando compilaciones anteriores..."
mvn clean -q

# Ejecutar tests individuales
echo ""
echo "📋 Ejecutando Tests E2E Individuales:"
echo "------------------------------------"

# 1. Configuración Base
echo "1️⃣ ConfigurationIT..."
mvn test -Dtest=ConfigurationIT -q
show_result $? "Configuración Base"

# 2. Creación de Tickets
echo "2️⃣ TicketCreationIT..."
mvn test -Dtest=TicketCreationIT -q
show_result $? "Creación de Tickets"

# 3. Procesamiento de Tickets
echo "3️⃣ TicketProcessingIT..."
mvn test -Dtest=TicketProcessingIT -q
show_result $? "Procesamiento de Tickets"

# 4. Notificaciones Telegram
echo "4️⃣ NotificationIT..."
mvn test -Dtest=NotificationIT -q
show_result $? "Notificaciones Telegram"

# 5. Validaciones Avanzadas
echo "5️⃣ ValidationIT..."
mvn test -Dtest=ValidationIT -q
show_result $? "Validaciones Avanzadas"

# 6. Dashboard Admin
echo "6️⃣ AdminDashboardIT..."
mvn test -Dtest=AdminDashboardIT -q
show_result $? "Dashboard Admin"

# Ejecutar suite completa
echo ""
echo "🎯 Ejecutando Suite Completa E2E:"
echo "--------------------------------"
mvn test -Dtest=TicketeroE2ETestSuite
SUITE_RESULT=$?

# Generar reporte
echo ""
echo "📊 Generando Reporte de Tests:"
echo "-----------------------------"
mvn surefire-report:report -q

# Mostrar resumen final
echo ""
echo "📈 RESUMEN FINAL:"
echo "================"

if [ $SUITE_RESULT -eq 0 ]; then
    echo -e "${GREEN}🎉 TODOS LOS TESTS E2E PASARON EXITOSAMENTE${NC}"
    echo ""
    echo "📋 Cobertura de Escenarios:"
    echo "- ✅ Configuración Base: 3 tests"
    echo "- ✅ Creación Tickets: 7 tests"
    echo "- ✅ Procesamiento: 5 tests"
    echo "- ✅ Notificaciones: 4 tests"
    echo "- ✅ Validaciones: 11 tests"
    echo "- ✅ Dashboard Admin: 4 tests"
    echo "- 📊 TOTAL: 34 escenarios E2E"
    echo ""
    echo "🔗 Ver reporte detallado:"
    echo "   file://$(pwd)/target/site/surefire-report.html"
else
    echo -e "${RED}💥 ALGUNOS TESTS FALLARON${NC}"
    echo ""
    echo -e "${YELLOW}🔍 Revisar logs en:${NC}"
    echo "   target/surefire-reports/"
fi

echo ""
echo "🏁 Tests E2E Completados"
echo "======================="

exit $SUITE_RESULT