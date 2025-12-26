#!/bin/bash
# =============================================================================
# TICKETERO - Metrics Collector
# =============================================================================
# Recolecta métricas del sistema durante pruebas de performance
# Usage: ./scripts/utils/metrics-collector.sh [duration_seconds] [output_file]
# =============================================================================

DURATION=${1:-60}
OUTPUT_FILE=${2:-"metrics-$(date +%Y%m%d-%H%M%S).csv"}

echo "timestamp,cpu_app,mem_app_mb,cpu_postgres,mem_postgres_mb,cpu_rabbitmq,mem_rabbitmq_mb,db_connections,rabbitmq_messages,tickets_waiting,tickets_completed,outbox_pending,outbox_failed" > "$OUTPUT_FILE"

echo "📊 Collecting metrics for ${DURATION} seconds..."
echo "📁 Output: ${OUTPUT_FILE}"

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))

while [ $(date +%s) -lt $END_TIME ]; do
    TIMESTAMP=$(date +%Y-%m-%d\ %H:%M:%S)
    
    # Container stats
    APP_STATS=$(docker stats ticketero-app --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" 2>/dev/null | head -1)
    APP_CPU=$(echo "$APP_STATS" | cut -d',' -f1 | tr -d '%')
    APP_MEM=$(echo "$APP_STATS" | cut -d',' -f2 | cut -d'/' -f1 | tr -d 'MiB ')
    
    PG_STATS=$(docker stats ticketero-postgres --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" 2>/dev/null | head -1)
    PG_CPU=$(echo "$PG_STATS" | cut -d',' -f1 | tr -d '%')
    PG_MEM=$(echo "$PG_STATS" | cut -d',' -f2 | cut -d'/' -f1 | tr -d 'MiB ')
    
    MQ_STATS=$(docker stats ticketero-rabbitmq --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" 2>/dev/null | head -1)
    MQ_CPU=$(echo "$MQ_STATS" | cut -d',' -f1 | tr -d '%')
    MQ_MEM=$(echo "$MQ_STATS" | cut -d',' -f2 | cut -d'/' -f1 | tr -d 'MiB ')
    
    # Database metrics
    DB_CONNECTIONS=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT count(*) FROM pg_stat_activity WHERE datname='ticketero';" 2>/dev/null | xargs)
    
    # RabbitMQ messages
    MQ_MESSAGES=$(docker exec ticketero-rabbitmq rabbitmqctl list_queues messages 2>/dev/null | \
        grep -v "Listing\|Timeout" | awk '{sum+=$2} END {print sum}')
    
    # Ticket stats
    TICKETS_WAITING=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='WAITING';" 2>/dev/null | xargs)
    TICKETS_COMPLETED=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" 2>/dev/null | xargs)
    
    # Outbox stats
    OUTBOX_PENDING=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM outbox_message WHERE status='PENDING';" 2>/dev/null | xargs)
    OUTBOX_FAILED=$(docker exec ticketero-postgres psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM outbox_message WHERE status='FAILED';" 2>/dev/null | xargs)
    
    # Write to CSV
    echo "${TIMESTAMP},${APP_CPU:-0},${APP_MEM:-0},${PG_CPU:-0},${PG_MEM:-0},${MQ_CPU:-0},${MQ_MEM:-0},${DB_CONNECTIONS:-0},${MQ_MESSAGES:-0},${TICKETS_WAITING:-0},${TICKETS_COMPLETED:-0},${OUTBOX_PENDING:-0},${OUTBOX_FAILED:-0}" >> "$OUTPUT_FILE"
    
    sleep 5
done

echo "✅ Metrics collection complete: ${OUTPUT_FILE}"