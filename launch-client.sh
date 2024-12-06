#!/bin/bash

IP="127.0.0.1"
PORT="808${1:-0}"
DIR=${2:-$(pwd)}

echo "Iniciando com IP: $IP, PORTA: $PORT, PASTA: $DIR"

javac -d bin src/*.java
java --enable-preview -cp bin App $IP $PORT $DIR
