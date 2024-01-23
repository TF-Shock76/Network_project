@echo off
set /p serv="IP du serveur : "
set /p port="Port : "

java client.Client %serv% %port%