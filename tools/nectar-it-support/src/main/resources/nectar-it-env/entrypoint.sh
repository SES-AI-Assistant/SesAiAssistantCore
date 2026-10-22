#!/bin/sh
# moto server をバックグラウンドで起動してから、PostgreSQL を本来のエントリポイントで起動する。
# PostgreSQL を exec で PID 1 にし、コンテナの停止時に PostgreSQL を正しく終了させる。
#
# PostgreSQL はアプリが実行した SQL を、バインド値つきでコンテナのログに出す（ItEnvironment が読み取り、ケースごとに記録する）。
#   log_statement=all           : 実行した SQL をすべて出す
#   log_parameter_max_length=-1 : バインド値を切り詰めない
#   log_line_prefix             : 行の先頭にアプリ名（接続の application_name）を付け、アプリの SQL とテストの準備用の SQL を区別する
set -e
moto_server -H 0.0.0.0 -p 5000 &
exec docker-entrypoint.sh postgres \
  -c log_statement=all \
  -c log_parameter_max_length=-1 \
  -c "log_line_prefix=PGSQL|%a| " \
  "$@"
