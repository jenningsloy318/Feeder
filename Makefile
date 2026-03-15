.PHONY: build sign clean test compile

KEYSTORE := feeder.keystore
APK_UNSIGNED := app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk
APK_SIGNED := feeder-release-fdroid-signed.apk

build: $(APK_UNSIGNED)

$(APK_UNSIGNED):
	./gradlew assembleFdroid

sign: $(APK_UNSIGNED)
	apksigner sign \
		--ks $(KEYSTORE) \
		--out $(APK_SIGNED) \
		$(APK_UNSIGNED)

compile:
	./gradlew :app:compileFdroidDebugKotlin

test:
	./gradlew :app:testFdroidDebugUnitTest

clean:
	./gradlew clean
	rm -f $(APK_SIGNED) $(APK_SIGNED).idsig

all: build sign
