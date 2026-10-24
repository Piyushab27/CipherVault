#!/bin/bash
# Starts CipherVault's private MySQL (port 3307, data in ~/.ciphervault-mysql) if needed, then the app.
# Your other MySQL on port 3306 is not touched.
set -e
cd "$(dirname "$0")/.."
M=/opt/homebrew/opt/mysql/bin
D="$HOME/.ciphervault-mysql"
if ! $M/mysqladmin -u root -h 127.0.0.1 -P 3307 ping >/dev/null 2>&1; then
  if [ ! -d "$D/data" ]; then
    mkdir -p "$D"
    $M/mysqld --no-defaults --initialize-insecure --datadir="$D/data" --basedir=/opt/homebrew/opt/mysql
  fi
  nohup $M/mysqld --no-defaults --datadir="$D/data" --basedir=/opt/homebrew/opt/mysql --port=3307 \
    --bind-address=127.0.0.1 --socket="$D/mysql.sock" --mysqlx=OFF --pid-file="$D/mysql.pid" \
    --log-error="$D/mysql.err" >/dev/null 2>&1 &
  until $M/mysqladmin -u root -h 127.0.0.1 -P 3307 ping >/dev/null 2>&1; do sleep 1; done
fi
[ -f target/ciphervault.jar ] || mvn -q package -DskipTests
export CV_DB_URL="jdbc:mysql://127.0.0.1:3307/ciphervault?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
exec java -jar target/ciphervault.jar
