#!/usr/bin/env bash
mvn -U -DskipTests -Pnative spring-javaformat:apply clean  native:compile
./target/blog-promotion