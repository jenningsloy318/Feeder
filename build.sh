#!/bin/bash
# Signed release build — signing config comes from ~/.gradle/gradle.properties
# (STORE_FILE / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD)
./gradlew assembleFdroidRelease
