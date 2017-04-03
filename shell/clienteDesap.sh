#!/bin/bash
RUTA_ACTUAL=`dirname $0`
if [ $RUTA_ACTUAL != "." ] ; then
  cd "$RUTA_ACTUAL"
fi
JAVA="java"
"$JAVA" -cp lib/clienteDesap_1.0.jar:lib/log4j-1.2.13.jar:lib/utilCnv.jar prg.glz.cli.Principal
