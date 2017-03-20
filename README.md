# paperglass
cursory documentation with java reflection

[![demo](https://asciinema.org/a/107851.png)](https://asciinema.org/a/107851?autoplay=1)

## What for
For viewing class API or package list from the command line without using an IDE.
This might be useful for 
* recalling the methods/properties of a class
* searching for a class that matches a given pattern
* having a partial API reference when documentation is not available

## Installing
Download a ![release](https://github.com/nvlled/paperglass/releases), then 
add the extracted directory in your path. 

## Building
Run ```./gradlew dist``` from the project directory.
The executable file will be in build/dist. 

## Running
Running ```paperglass.sh``` without parameters will show the help contents.
Note that paperglass will use the contents of the $CLASSPATH when searching 
for classes. For example:

```CLASSPATH=/path/to/gdx.jar paperglass.sh com.badlogic.gdx -c sprite```
