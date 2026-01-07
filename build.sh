#!/bin/bash
./gradlew assembleFdroid
apksigner sign \
        --ks feeder.keystore \
        --out feeder-release-fdroid-signed.apk \
        app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk
