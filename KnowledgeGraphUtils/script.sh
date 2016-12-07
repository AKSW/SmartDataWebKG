#!/bin/bash

filename=$1

outputFile=$2

rm -f $outputFile

while read -r line
do
    name="$line"
    echo "Company name - $name"

    commandEn="java -cp ./target/KnowledgeGraphUtils-0.0.1-SNAPSHOT.jar aksw.org.sdw.kg.handler.solr.EntityExtractor http://localhost:10083/solr/companies \"nameEn:\\\"${name}\\\"\" http://localhost:8892/sparql ${outputFile} ${name}  >> outputEn.log"
    echo "command $commandEn"
    eval $commandEn

    commandDe="java -cp ./target/KnowledgeGraphUtils-0.0.1-SNAPSHOT.jar aksw.org.sdw.kg.handler.solr.EntityExtractor http://localhost:10083/solr/companies \"nameEn:\\\"${name}\\\"\" http://localhost:8892/sparql ${outputFile} ${name} >> outputDe.log"
    echo "command $commandDe"
    eval $commandDe



done < "$filename"

