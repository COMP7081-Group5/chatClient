#!/bin/sh

THE_CLASSPATH=
PROGRAM_NAME=MainProgram.java
cd src
for i in `ls ../lib/*.jar`
  do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done

javac -classpath ".:${THE_CLASSPATH}" ./*.java

if [ $? -eq 0 ]
then
  echo "compile worked!"
fi