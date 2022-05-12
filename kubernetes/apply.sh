#!/bin/bash

LIST_OF_FILES=""

for file in $(find -name "*\.y(a)?ml" ".")
do
  LIST_OF_FILES="$LIST_OF_FILES,$file"
done

kubectl apply -f "$LIST_OF_FILES"