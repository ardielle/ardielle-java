#!/usr/bin/env bash

set -e

apt-get update
apt-get clean
apt-get autoremove

echo "-----------------Install libs: -----------------"
apt-get install -y libaio1 libnuma-dev build-essential libncurses5 aptitude

echo "-----------------Install maven: -----------------"
apt-get install -y maven

# check all installed dependencies
echo "-----------------Java Version: -----------------"
java -version
echo "-----------------Maven Version: -----------------"
mvn -version
