#!/bin/bash
javac -d build src/*  \
    && ./run.sh $@
