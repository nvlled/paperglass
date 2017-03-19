# paperglass
cursory documentation with java reflection

[![demo](https://asciinema.org/a/107851.png)](https://asciinema.org/a/107851?autoplay=1)

## What for
For viewing class API or package list from the command line without using an IDE.
This might be useful for 
* recalling the methods/properties of a class
* searching for a class that matches a given pattern
* having a partial API reference when documentation is not available

## Building
Run ```./gradlew dist``` from the project directory.
The executable file will be in build/dist. Add or copy
this directory to your $PATH. Then run ```paperglass.sh``` to see the help contents.
