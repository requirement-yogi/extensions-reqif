#!/bin/bash

###
# #%L
# Play SQL - ReqIF Import
# %%
# Copyright (C) 2019 Play SQL S.A.S.U.
# %%
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #L%
###

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
