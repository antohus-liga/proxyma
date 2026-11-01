set -a
source ./.env
set +a

# ./gradlew bootRun --stacktrace
./gradlew bootRun --args='grant-admin proxyma.test.1793467064@example.com'
