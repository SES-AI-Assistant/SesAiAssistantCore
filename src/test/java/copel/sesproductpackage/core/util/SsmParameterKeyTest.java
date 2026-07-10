package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SsmParameterKey のテストクラス.
 *
 * @author Copel Co., Ltd.
 */
@DisplayName("SsmParameterKey Test")
class SsmParameterKeyTest {

  @Test
  @DisplayName("すべての Enum 値が定義されていること")
  void testAllEnumValuesAreDefined() {
    SsmParameterKey[] values = SsmParameterKey.values();
    assertNotNull(values);
    assertTrue(values.length > 0, "SsmParameterKey に少なくとも1つの値が定義されている必要があります");
  }

  @Test
  @DisplayName("getKey() がパスを返すこと")
  void testGetKeyReturnsPath() {
    String key = SsmParameterKey.EMAIL_RECEIVER_BUCKET_NAME.getKey();
    assertNotNull(key);
    assertFalse(key.isEmpty());
    assertTrue(key.startsWith("infrastructure/"), "キーは infrastructure/ で始まる必要があります");
  }

  @Test
  @DisplayName("S3 パラメータキーが正しいこと")
  void testS3ParameterKeys() {
    assertTrue(
        SsmParameterKey.EMAIL_RECEIVER_BUCKET_NAME
            .getKey()
            .equals("infrastructure/s3/email-receiver-bucket/name"));
    assertTrue(
        SsmParameterKey.EMAIL_RECEIVER_BUCKET_ARN
            .getKey()
            .equals("infrastructure/s3/email-receiver-bucket/arn"));
    assertTrue(
        SsmParameterKey.SKILLSHEET_ARCHIVE_BUCKET_NAME
            .getKey()
            .equals("infrastructure/s3/skillsheet-archive-bucket/name"));
    assertTrue(
        SsmParameterKey.SKILLSHEET_ARCHIVE_BUCKET_ARN
            .getKey()
            .equals("infrastructure/s3/skillsheet-archive-bucket/arn"));
    assertTrue(
        SsmParameterKey.ENVIRONMENT_CONFIG_BUCKET_NAME
            .getKey()
            .equals("infrastructure/s3/environment-config-bucket/name"));
    assertTrue(
        SsmParameterKey.ENVIRONMENT_CONFIG_BUCKET_ARN
            .getKey()
            .equals("infrastructure/s3/environment-config-bucket/arn"));
    assertTrue(
        SsmParameterKey.LAMBDA_DEPLOY_BUCKET_NAME
            .getKey()
            .equals("infrastructure/s3/lambda-deploy-bucket/name"));
    assertTrue(
        SsmParameterKey.LAMBDA_DEPLOY_BUCKET_ARN
            .getKey()
            .equals("infrastructure/s3/lambda-deploy-bucket/arn"));
  }

  @Test
  @DisplayName("Messaging パラメータキーが正しいこと")
  void testMessagingParameterKeys() {
    assertTrue(
        SsmParameterKey.REGISTER_QUEUE_URL
            .getKey()
            .equals("infrastructure/messaging/register-queue/url"));
    assertTrue(
        SsmParameterKey.REGISTER_QUEUE_ARN
            .getKey()
            .equals("infrastructure/messaging/register-queue/arn"));
    assertTrue(
        SsmParameterKey.MATCHER_QUEUE_URL
            .getKey()
            .equals("infrastructure/messaging/matcher-queue/url"));
    assertTrue(
        SsmParameterKey.NOTIFIER_QUEUE_URL
            .getKey()
            .equals("infrastructure/messaging/notifier-queue/url"));
  }

  @Test
  @DisplayName("Network パラメータキーが正しいこと")
  void testNetworkParameterKeys() {
    assertTrue(
        SsmParameterKey.VPC_ID.getKey().equals("infrastructure/network/vpc/id"));
    assertTrue(
        SsmParameterKey.DB_SECURITY_GROUP_ID
            .getKey()
            .equals("infrastructure/network/db-security-group/id"));
    assertTrue(
        SsmParameterKey.LAMBDA_SECURITY_GROUP_ID
            .getKey()
            .equals("infrastructure/network/lambda-security-group/id"));
  }

  @Test
  @DisplayName("RDS パラメータキーが正しいこと")
  void testRdsParameterKeys() {
    assertTrue(
        SsmParameterKey.RDS_DATABASE_ENDPOINT
            .getKey()
            .equals("infrastructure/rds/database/endpoint"));
    assertTrue(
        SsmParameterKey.RDS_DATABASE_PORT
            .getKey()
            .equals("infrastructure/rds/database/port"));
    assertTrue(
        SsmParameterKey.RDS_DATABASE_SECRET_ARN
            .getKey()
            .equals("infrastructure/rds/database/secret-arn"));
  }

  @Test
  @DisplayName("Lambda パラメータキーが正しいこと")
  void testLambdaParameterKeys() {
    assertTrue(
        SsmParameterKey.LINE_MESSAGE_RECEIVER_NAME
            .getKey()
            .equals("infrastructure/lambda/line-message-receiver/name"));
    assertTrue(
        SsmParameterKey.LINE_MESSAGE_RECEIVER_ARN
            .getKey()
            .equals("infrastructure/lambda/line-message-receiver/arn"));
    assertTrue(
        SsmParameterKey.EMAIL_MESSAGE_RECEIVER_NAME
            .getKey()
            .equals("infrastructure/lambda/email-message-receiver/name"));
    assertTrue(
        SsmParameterKey.SES_INFO_REGISTER_NAME
            .getKey()
            .equals("infrastructure/lambda/ses-info-register/name"));
    assertTrue(
        SsmParameterKey.SES_INFO_MATCHER_NAME
            .getKey()
            .equals("infrastructure/lambda/ses-info-matcher/name"));
    assertTrue(
        SsmParameterKey.WEBAPP_BACKEND_NAME
            .getKey()
            .equals("infrastructure/lambda/webapp-backend/name"));
    assertTrue(
        SsmParameterKey.WEBAPP_BATCH_NAME
            .getKey()
            .equals("infrastructure/lambda/webapp-batch/name"));
    assertTrue(
        SsmParameterKey.COGNITO_RECEIVER_NAME
            .getKey()
            .equals("infrastructure/lambda/cognito-receiver/name"));
    assertTrue(
        SsmParameterKey.WEBAPP_NOTIFIER_NAME
            .getKey()
            .equals("infrastructure/lambda/webapp-notifier/name"));
    assertTrue(
        SsmParameterKey.MARKITDOWN_NAME
            .getKey()
            .equals("infrastructure/lambda/markitdown/name"));
  }

  @Test
  @DisplayName("API Gateway パラメータキーが正しいこと")
  void testApiGatewayParameterKeys() {
    assertTrue(
        SsmParameterKey.LINE_WEBHOOK_API_ID
            .getKey()
            .equals("infrastructure/api-gateway/line-webhook-api/id"));
    assertTrue(
        SsmParameterKey.LINE_WEBHOOK_API_ENDPOINT
            .getKey()
            .equals("infrastructure/api-gateway/line-webhook-api/endpoint"));
    assertTrue(
        SsmParameterKey.BACKEND_API_ID
            .getKey()
            .equals("infrastructure/api-gateway/backend-api/id"));
    assertTrue(
        SsmParameterKey.BACKEND_API_ENDPOINT
            .getKey()
            .equals("infrastructure/api-gateway/backend-api/endpoint"));
  }

  @Test
  @DisplayName("Cognito パラメータキーが正しいこと")
  void testCognitoParameterKeys() {
    assertTrue(
        SsmParameterKey.COGNITO_USER_POOL_ID
            .getKey()
            .equals("infrastructure/cognito/user-pool/id"));
    assertTrue(
        SsmParameterKey.COGNITO_USER_POOL_ARN
            .getKey()
            .equals("infrastructure/cognito/user-pool/arn"));
    assertTrue(
        SsmParameterKey.COGNITO_USER_POOL_NAME
            .getKey()
            .equals("infrastructure/cognito/user-pool/name"));
    assertTrue(
        SsmParameterKey.COGNITO_USER_POOL_DOMAIN_URL
            .getKey()
            .equals("infrastructure/cognito/user-pool/domain-url"));
  }

  @Test
  @DisplayName("DynamoDB パラメータキーが正しいこと")
  void testDynamoDbParameterKeys() {
    assertTrue(
        SsmParameterKey.API_USAGE_HISTORY_TABLE_NAME
            .getKey()
            .equals("infrastructure/dynamodb/api-usage-history-table/name"));
    assertTrue(
        SsmParameterKey.API_USAGE_HISTORY_TABLE_ARN
            .getKey()
            .equals("infrastructure/dynamodb/api-usage-history-table/arn"));
  }

  @Test
  @DisplayName("ECR パラメータキーが正しいこと")
  void testEcrParameterKeys() {
    assertTrue(
        SsmParameterKey.MARKITDOWN_REPOSITORY_URI
            .getKey()
            .equals("infrastructure/ecr/markitdown-repository/uri"));
    assertTrue(
        SsmParameterKey.MARKITDOWN_REPOSITORY_NAME
            .getKey()
            .equals("infrastructure/ecr/markitdown-repository/name"));
  }

  @Test
  @DisplayName("Enum 値の一意性")
  void testEnumValuesUniqueness() {
    SsmParameterKey[] values = SsmParameterKey.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertTrue(
            !values[i].getKey().equals(values[j].getKey()),
            "パラメータキーは一意である必要があります: " + values[i] + " と " + values[j]);
      }
    }
  }
}
