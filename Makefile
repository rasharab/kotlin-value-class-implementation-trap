.PHONY: repro fixed clean

repro:
	./gradlew --quiet :app:installDist
	app/build/install/app/bin/app

fixed:
	./gradlew --quiet :app:installDist -PuseApi=true
	app/build/install/app/bin/app

clean:
	./gradlew --quiet clean
