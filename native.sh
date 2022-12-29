#!/usr/bin/env bash
mvn -DskipTests -Pnative spring-javaformat:apply clean  native:compile
./target/blog-promotion