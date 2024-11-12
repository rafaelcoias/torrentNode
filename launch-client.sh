#!/bin/bash

IP=" 192.168.1.111"
PORT="808${1:-0}"
DIR=${2:-$(pwd)}

java -cp bin App $IP $PORT $DIR
