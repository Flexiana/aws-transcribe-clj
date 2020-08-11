#!/bin/bash

path=./resources/public
port=9000

cp $path/index.html $path/404.html
clj -A:prod && npx http-server $path -p $port
rm $path/404.html