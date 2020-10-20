#!/usr/bin/env sh
set -e
cd $(dirname $0)
. ./builder.conf

X_CMD="hugo"

# FUNCTIONS
print(){ printf "$@"; }
println(){ printf "$@\n"; }

deploy(){
  bash tools/run.sh
}

build(){
    if [ "$ISSUE_ID" = "" ]; then
      println "ERROR: Variables ISSUE_ID check builder.conf "
      exit 201
    fi

    ISSUE_TITLE=$(curl -s https://api.github.com/repos/BhuwanUpadhyay/bhuwanupadhyay.github.io/issues/"$ISSUE_ID" | jq '.title' | sed 's/"//g' | sed 's/ /-/g' | awk '{print tolower($0)}')
    POST_DATE=$(date '+%Y-%m-%d')
    POST_TITLE=$(curl -s https://api.github.com/repos/BhuwanUpadhyay/bhuwanupadhyay.github.io/issues/"$ISSUE_ID" | jq '.title' | sed 's/"//g')
    POST_FILE="$POST_DATE-$ISSUE_TITLE"

    if [ "$POST_FILE" = "" -o "$POST_TITLE" = "" ]; then
      println "ERROR: Github issue not found: https://github.com/BhuwanUpadhyay/bhuwanupadhyay.github.io/issues/$ISSUE_ID"
      exit 201
    fi

    git checkout -b posts/"$ISSUE_ID"
    cat <<EOF> _posts/"$POST_FILE.md"
---
title: $POST_TITLE
author: Bhuwan Prasad Upadhyay
date: $POST_DATE 00:00:00 +0000
categories: [x]
tags: [y, z]
---
EOF

}

# MAIN
case $1 in
  "build") build ;;
  "deploy") deploy ;;
  *)
    cat <<EOF | sed 's/^[ \t]*//'
      Usage: $0 <OPTION>

      Where OPTION is one of the following:
      - build
      - deploy
EOF
    exit 1
  ;;
esac