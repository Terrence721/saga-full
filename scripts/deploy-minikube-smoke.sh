#!/usr/bin/env bash
set -euo pipefail

NS="${SAGA_NAMESPACE:-saga-ecosystem}"
kubectl create namespace "$NS" 2>/dev/null || true
kubectl create configmap cluster-config -n "$NS" --from-literal=dummy=true 2>/dev/null || true

cat <<YAML | kubectl apply -n "$NS" -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-db
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres-db
  template:
    metadata:
      labels:
        app: postgres-db
    spec:
      containers:
        - name: postgres
          image: postgres:18-alpine
          env:
            - name: POSTGRES_DB
              value: saga_db
            - name: POSTGRES_USER
              value: postgres
            - name: POSTGRES_PASSWORD
              value: postgres
          ports:
            - containerPort: 5432
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-db
spec:
  selector:
    app: postgres-db
  ports:
    - port: 5432
      targetPort: 5432
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-broker
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka-broker
  template:
    metadata:
      labels:
        app: kafka-broker
    spec:
      containers:
        - name: kafka
          image: bitnami/kafka:3.7
          env:
            - name: ALLOW_PLAINTEXT_LISTENER
              value: "yes"
            - name: KAFKA_CFG_NODE_ID
              value: "0"
            - name: KAFKA_CFG_PROCESS_ROLES
              value: controller,broker
            - name: KAFKA_CFG_CONTROLLER_QUORUM_VOTERS
              value: 0@kafka-broker:9093
            - name: KAFKA_CFG_LISTENERS
              value: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
            - name: KAFKA_CFG_ADVERTISED_LISTENERS
              value: PLAINTEXT://kafka-broker:9092
            - name: KAFKA_CFG_CONTROLLER_LISTENER_NAMES
              value: CONTROLLER
            - name: KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP
              value: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
            - name: KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE
              value: "true"
            - name: KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_CFG_TRANSACTION_STATE_LOG_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_CFG_TRANSACTION_STATE_LOG_MIN_ISR
              value: "1"
          ports:
            - containerPort: 9092
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-broker
spec:
  selector:
    app: kafka-broker
  ports:
    - port: 9092
      targetPort: 9092
YAML

echo "Waiting for dependencies..."
kubectl -n "$NS" rollout status deployment/postgres-db --timeout=300s
kubectl -n "$NS" rollout status deployment/kafka-broker --timeout=300s

echo "Applying application manifests..."
kubectl apply -n "$NS" -f manifests/apps/

services=(api-gateway-service user-service order-service payment-service reservation-service restaurant-service)
for svc in "${services[@]}"; do
  kubectl -n "$NS" set env "deployment/$svc"     SPRING_PROFILES_ACTIVE=default     DATABASE_HOST=postgres-db     DATABASE_PORT=5432     KAFKA_BOOTSTRAP_SERVERS=kafka-broker:9092     JWT_SECRET=local-dev-smoke-secret     JWT_PREVIOUS_SECRETS=     TLS_ENABLED=false     SPRING_DATASOURCE_USERNAME=postgres     SPRING_DATASOURCE_PASSWORD=postgres 2>/dev/null || true
done

echo "Waiting for applications..."
for svc in "${services[@]}"; do
  kubectl -n "$NS" rollout status "deployment/$svc" --timeout=590s || true
done

echo "Smoke deployment complete. Check: kubectl -n $NS get pods"
