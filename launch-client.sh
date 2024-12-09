#!/bin/bash

IP="127.0.0.1"
PORT="808${1:-0}"
DIR=${2:-$(pwd)}

java -cp bin App $IP $PORT $DIR
