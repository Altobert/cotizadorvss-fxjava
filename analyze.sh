#!/bin/bash

cd /Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava

# Compilar
javac -cp "target/classes:$HOME/.m2/repository/org/apache/poi/poi/5.3.0/poi-5.3.0.jar:$HOME/.m2/repository/org/apache/poi/poi-ooxml/5.3.0/poi-ooxml-5.3.0.jar:$HOME/.m2/repository/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar:$HOME/.m2/repository/org/apache/xmlbeans/xmlbeans/5.1.1/xmlbeans-5.1.1.jar" AnalysisOneSphere.java

# Ejecutar
java -cp ".:target/classes:$HOME/.m2/repository/org/apache/poi/poi/5.3.0/poi-5.3.0.jar:$HOME/.m2/repository/org/apache/poi/poi-ooxml/5.3.0/poi-ooxml-5.3.0.jar:$HOME/.m2/repository/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar:$HOME/.m2/repository/org/apache/xmlbeans/xmlbeans/5.1.1/xmlbeans-5.1.1.jar" AnalysisOneSphere cotizaciones/Libro4.xlsx
