#!/bin/bash
amixer set PCM -- 96%
cd servers/new
nohup node app.js &
