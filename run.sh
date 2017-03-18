#!/bin/bash

dir=`dirname $0`
classpath=${CLASSPATH/;/:}

java -cp "$classpath:$dir/paperglass.jar" com.github.nvlled.paperglass.Main $@
