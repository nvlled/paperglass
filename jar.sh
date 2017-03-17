#!/bin/bash
./build.sh \
    && jar cfm paperglass.jar Manifest.txt -C build .
