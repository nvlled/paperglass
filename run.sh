#!/bin/bash
./gradlew --daemon build &&\
    java -cp build/classes/main com.github.nvlled.paperglass.Main $@
