#!/bin/bash
# Script de fin del servicio de facturación - Steve Tort
# Termina el proceso usando el PID almacenado en bin/pid.file
cd "$(dirname "$0")/.."
if [ ! -f bin/pid.file ]; then
  echo "[ERROR] No existe bin/pid.file. ¿El servicio está iniciado?"
  exit 1
fi
PID=$(cat bin/pid.file)
kill "$PID" 2>/dev/null && echo "[OK] Servicio detenido (PID $PID)." || echo "[AVISO] El proceso $PID ya no estaba corriendo."
rm -f bin/pid.file
