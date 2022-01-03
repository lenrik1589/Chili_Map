#!/usr/bin/env zsh

echo starting building git submodules
for i in .gradle/vcs-1/*; do
  if [ -d $i ]; then
    cd $i
    for j in *; do
      cd $j
      printf $i/$j"\n"
      ./gradlew build publishToMavenLocal
      cd ../
    done
    cd ../../../
  fi
done
echo built git submodules
