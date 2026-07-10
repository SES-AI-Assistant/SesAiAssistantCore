package copel.sesproductpackage.core.util;

/**
 * SSM Parameter Store のキーを定義する Enum.
 *
 * @author Copel Co., Ltd.
 */
public enum SsmParameterKey {
  // S3
  EMAIL_RECEIVER_BUCKET_NAME("infrastructure/s3/email-receiver-bucket/name"),
  EMAIL_RECEIVER_BUCKET_ARN("infrastructure/s3/email-receiver-bucket/arn"),
  SKILLSHEET_ARCHIVE_BUCKET_NAME("infrastructure/s3/skillsheet-archive-bucket/name"),
  SKILLSHEET_ARCHIVE_BUCKET_ARN("infrastructure/s3/skillsheet-archive-bucket/arn"),
  ENVIRONMENT_CONFIG_BUCKET_NAME("infrastructure/s3/environment-config-bucket/name"),
  ENVIRONMENT_CONFIG_BUCKET_ARN("infrastructure/s3/environment-config-bucket/arn"),
  // アプリケーションからアクセスさせたくないインフラリソースのためコメントアウト
//  LAMBDA_DEPLOY_BUCKET_NAME("infrastructure/s3/lambda-deploy-bucket/name"),
//  LAMBDA_DEPLOY_BUCKET_ARN("infrastructure/s3/lambda-deploy-bucket/arn"),

  // Messaging
  REGISTER_QUEUE_URL("infrastructure/messaging/register-queue/url"),
  REGISTER_QUEUE_ARN("infrastructure/messaging/register-queue/arn"),
  REGISTER_DLQ_URL("infrastructure/messaging/register-dlq/url"),
  REGISTER_DLQ_ARN("infrastructure/messaging/register-dlq/arn"),
  MATCHER_QUEUE_URL("infrastructure/messaging/matcher-queue/url"),
  MATCHER_QUEUE_ARN("infrastructure/messaging/matcher-queue/arn"),
  NOTIFIER_QUEUE_URL("infrastructure/messaging/notifier-queue/url"),
  NOTIFIER_QUEUE_ARN("infrastructure/messaging/notifier-queue/arn"),

  // Network
  VPC_ID("infrastructure/network/vpc/id"),
  DB_SECURITY_GROUP_ID("infrastructure/network/db-security-group/id"),
  LAMBDA_SECURITY_GROUP_ID("infrastructure/network/lambda-security-group/id"),

  // RDS
  RDS_DATABASE_ENDPOINT("infrastructure/rds/database/endpoint"),
  RDS_DATABASE_PORT("infrastructure/rds/database/port"),
  RDS_DATABASE_SECRET_ARN("infrastructure/rds/database/secret-arn"),

  // Lambda
  LINE_MESSAGE_RECEIVER_NAME("infrastructure/lambda/line-message-receiver/name"),
  LINE_MESSAGE_RECEIVER_ARN("infrastructure/lambda/line-message-receiver/arn"),
  EMAIL_MESSAGE_RECEIVER_NAME("infrastructure/lambda/email-message-receiver/name"),
  EMAIL_MESSAGE_RECEIVER_ARN("infrastructure/lambda/email-message-receiver/arn"),
  SES_INFO_REGISTER_NAME("infrastructure/lambda/ses-info-register/name"),
  SES_INFO_REGISTER_ARN("infrastructure/lambda/ses-info-register/arn"),
  SES_INFO_MATCHER_NAME("infrastructure/lambda/ses-info-matcher/name"),
  SES_INFO_MATCHER_ARN("infrastructure/lambda/ses-info-matcher/arn"),
  WEBAPP_BACKEND_NAME("infrastructure/lambda/webapp-backend/name"),
  WEBAPP_BACKEND_ARN("infrastructure/lambda/webapp-backend/arn"),
  WEBAPP_BATCH_NAME("infrastructure/lambda/webapp-batch/name"),
  WEBAPP_BATCH_ARN("infrastructure/lambda/webapp-batch/arn"),
  COGNITO_RECEIVER_NAME("infrastructure/lambda/cognito-receiver/name"),
  COGNITO_RECEIVER_ARN("infrastructure/lambda/cognito-receiver/arn"),
  WEBAPP_NOTIFIER_NAME("infrastructure/lambda/webapp-notifier/name"),
  WEBAPP_NOTIFIER_ARN("infrastructure/lambda/webapp-notifier/arn"),
  MARKITDOWN_NAME("infrastructure/lambda/markitdown/name"),
  MARKITDOWN_ARN("infrastructure/lambda/markitdown/arn"),

  // API Gateway
  LINE_WEBHOOK_API_ID("infrastructure/api-gateway/line-webhook-api/id"),
  LINE_WEBHOOK_API_ENDPOINT("infrastructure/api-gateway/line-webhook-api/endpoint"),
  BACKEND_API_ID("infrastructure/api-gateway/backend-api/id"),
  BACKEND_API_ENDPOINT("infrastructure/api-gateway/backend-api/endpoint"),

  // Cognito
  COGNITO_USER_POOL_ID("infrastructure/cognito/user-pool/id"),
  COGNITO_USER_POOL_ARN("infrastructure/cognito/user-pool/arn"),
  COGNITO_USER_POOL_NAME("infrastructure/cognito/user-pool/name"),
  COGNITO_USER_POOL_DOMAIN_URL("infrastructure/cognito/user-pool/domain-url"),

  // DynamoDB
  API_USAGE_HISTORY_TABLE_NAME("infrastructure/dynamodb/api-usage-history-table/name"),
  API_USAGE_HISTORY_TABLE_ARN("infrastructure/dynamodb/api-usage-history-table/arn");

  // アプリケーションからアクセスさせたくないインフラリソースのためコメントアウト
  // ECR
//  MARKITDOWN_REPOSITORY_URI("infrastructure/ecr/markitdown-repository/uri"),
//  MARKITDOWN_REPOSITORY_NAME("infrastructure/ecr/markitdown-repository/name");

  private final String path;

  SsmParameterKey(String path) {
    this.path = path;
  }

  /**
   * SSM Parameter Store のキーを取得します. Properties.get() で使用するキーを返します.
   * Parameter Store から読み込まれたキーは /nectar/{ENVIRONMENT}/ 部分が除去されているため、
   * このメソッドは path をそのまま返します.
   *
   * @return SSM Parameter Store のキー（infrastructure/... 形式）
   */
  public String getKey() {
    return path;
  }
}
