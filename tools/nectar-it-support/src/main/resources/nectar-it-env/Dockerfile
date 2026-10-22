# 結合テスト（IT）用の環境イメージ。DB と AWS を1つのコンテナで提供する
#   - PostgreSQL 16 + pgvector + pg_trgm（5432）: 本番の DDL は vector 型を使うため pgvector 入りのイメージを土台にする
#   - moto server（5000）: S3・SSM・DynamoDB・SQS を1つのポートで提供する
# テスト（ItEnvironment）がこの Dockerfile からイメージをビルドして起動する。手で起動する必要はない
FROM pgvector/pgvector:0.8.1-pg16-bookworm

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && pip3 install --no-cache-dir --break-system-packages "moto[server,s3,ssm,sqs,dynamodb]==5.2.4" \
    && rm -rf /var/lib/apt/lists/*

COPY ddl.sql /docker-entrypoint-initdb.d/001_ddl.sql
COPY entrypoint.sh /usr/local/bin/it-entrypoint.sh
RUN chmod +x /usr/local/bin/it-entrypoint.sh

ENTRYPOINT ["it-entrypoint.sh"]
