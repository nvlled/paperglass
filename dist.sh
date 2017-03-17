#!/bin/bash
./build.sh          &&\
    ./jar.sh        &&\
    mkdir -p dist   &&\
    cp paperglass.jar dist
