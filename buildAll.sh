./gradlew clean
./gradlew assembleQA
cp app/build/outputs/apk/qa/*.apk .

./gradlew clean
./gradlew assembleStaging
cp app/build/outputs/apk/staging/*.apk .

./gradlew clean
./gradlew assembleProd
cp app/build/outputs/apk/prod/*.apk .

./gradlew clean
./gradlew assembleDaikin_prod
cp app/build/outputs/apk/daikin_prod/*.apk .

./gradlew clean
./gradlew assembleCarrier_prod
cp app/build/outputs/apk/carrier_prod/*.apk .

