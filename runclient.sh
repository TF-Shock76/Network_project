#!/bin/bash

serv=""
port=0

if [ $# -ne 2 ]; then
	serv="localhost"
	port=6000
else
	serv=$1
	port=$2
fi

java client.Client $serv $port
