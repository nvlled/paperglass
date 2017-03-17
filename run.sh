#!/bin/bash
./gradlew build &&\
    java -cp build/classes com.github.nvlled.paperglass.Main $@
