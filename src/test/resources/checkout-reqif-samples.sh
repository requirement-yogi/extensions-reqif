#!/bin/bash

echo
echo "      Goal: Checkout files from the internet that are not part of this open-source projet, but can be helpful to test"
echo "     Usage: ./checkout-reqif-sample.sh"
echo

set -u
set -e

rm -rf samples
mkdir samples
cd samples

echo "Checking out https://git.eclipse.org/c/rmf/org.eclipse.rmf.git/"
git clone --depth 1 git://git.eclipse.org/gitroot/rmf/org.eclipse.rmf.git
wget http://formalmind.com/sites/default/files/blog/manual-testing.reqif


echo
echo "Filtering files"
find org.eclipse.rmf -iname "*.reqif" -exec mv {} ./ \;

echo -e "\nDone\n";