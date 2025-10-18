#!/bin/bash
set -e

TOPIC_NAME="auction-bid"
PARTITIONS=4
REPLICATION=1

echo "Kafka topic initialization started..."

EXISTING=$(kafka-topics --bootstrap-server localhost:9092 --list | grep "$TOPIC_NAME" || true)

if [ -z "$EXISTING" ]; then
  echo "Creating topic: $TOPIC_NAME with $PARTITIONS partitions"
  kafka-topics --create \
    --topic "$TOPIC_NAME" \
    --partitions "$PARTITIONS" \
    --replication-factor "$REPLICATION" \
    --bootstrap-server localhost:9092
else
  echo "⚙Topic exists. Updating partitions to $PARTITIONS..."
  kafka-topics --alter \
    --topic "$TOPIC_NAME" \
    --partitions "$PARTITIONS" \
    --bootstrap-server localhost:9092 || echo "Partition already set or update skipped"
fi

echo "Kafka topic initialization finished!"