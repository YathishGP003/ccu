./gradlew clean
sleep 5
./gradlew assembleQA
cp app/build/outputs/apk/qa/*.apk .

sleep 5
./gradlew clean
sleep 5
./gradlew assembleStaging
cp app/build/outputs/apk/staging/*.apk .

sleep 5
./gradlew clean
sleep 5
./gradlew assembleProd
cp app/build/outputs/apk/prod/*.apk .

sleep 5
./gradlew clean
sleep 5
./gradlew assembleCarrier_prod
cp app/build/outputs/apk/carrier_prod/*.apk .

sleep 5
./gradlew clean
sleep 5
./gradlew assembleAiroverse_prod
cp app/build/outputs/apk/airoverse_prod/*.apk .
