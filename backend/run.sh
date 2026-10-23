set -a
source ./.env
set +a

./gradlew bootRun --stacktrace
