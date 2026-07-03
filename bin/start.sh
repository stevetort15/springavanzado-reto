#!/bin/bash
# Script de inicio del servicio de facturación (Spring Boot) - Steve Tort
# Ejecuta el JAR en segundo plano, guarda la bitácora en logs/facturacion.log
# y el id del proceso en bin/pid.file
cd "$(dirname "$0")/.."
JAR=$(ls target/facturacion-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo "[ERROR] No existe el JAR. Ejecuta primero: ./mvnw package"
  exit 1
fi
mkdir -p logs
nohup java -jar "$JAR" > logs/facturacion.log 2>&1 &
echo $! > bin/pid.file
echo "[OK] Servicio iniciado (PID $(cat bin/pid.file)). Log: logs/facturacion.log"
