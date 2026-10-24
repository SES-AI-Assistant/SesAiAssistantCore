package copel.sesproductpackage.core.api.aws;

import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.core.util.AwsEndpointUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Amazon S3操作クラス.
 *
 * @author Copel Co., Ltd.
 */
@Data
@Slf4j
public class S3 {
  /** バケット名. */
  private String bucketName;

  /** オブジェクトキー(ファイル名). */
  private String objectKey;

  /** データ. */
  private byte[] data;

  /** 更新日時. */
  private Date updateDate;

  /** S3クライアント. */
  private S3Client s3Client;

  /**
   * コンストラクタ.
   *
   * @param bucketName バケット名.
   * @param objectKey オブジェクトキー.
   */
  public S3(final String bucketName, final String objectKey, final Region region) {
    this.bucketName = bucketName;
    this.objectKey = objectKey;
    // S3クライアントの作成
    S3ClientBuilder builder =
        S3Client.builder().credentialsProvider(DefaultCredentialsProvider.create()).region(region);
    // IT1 や LocalStack では、他の AWS クライアントと同じ接続先を使う。moto はパス形式でバケットを指定するため forcePathStyle を有効にする
    String endpointUrl = AwsEndpointUtil.resolveEndpointUrl();
    if (endpointUrl != null) {
      builder.endpointOverride(URI.create(endpointUrl)).forcePathStyle(true);
    }
    this.s3Client = builder.build();
  }

  /** このオブジェクトに持つdataをS3に保存します. */
  public void save() {
    // バケット名かファイルパスが空であれば処理を終了する
    if (this.bucketName == null || this.objectKey == null || this.data == null) {
      log.warn("【SesAiAssitantCore】S3にファイルを保存しようとしましたが、バケット名、ファイルパス、データのいずれかがNULLであるため保存を中止しました。");
      return;
    }
    try {
      // S3にファイルをアップロード
      this.s3Client.putObject(
          PutObjectRequest.builder().bucket(this.bucketName).key(this.objectKey).build(),
          RequestBody.fromBytes(this.data));

      log.info("S3にファイルを保存しました: " + this.objectKey);

      // 更新日時を現在時刻でセット
      this.updateDate = Date.from(ApplicationClock.INSTANCE.clock().instant());

    } catch (Exception e) {
      log.error("【SesAiAssitantCore】S3へのファイル保存中にエラーが発生しました:{}", e.getMessage());
    }
  }

  /**
   * このオブジェクトが持つバケット名の中にあるファイルパス先のファイルを取得しdataに持つ.
   *
   * @throws IOException 入出力（ファイル・ストリーム）の操作に失敗した場合
   */
  public void read() throws IOException {
    try {
      // S3からファイルを取得
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(this.bucketName).key(this.objectKey).build();

      // ファイルを取得してdataにセット
      try (InputStream inputStream = this.s3Client.getObject(getObjectRequest)) {
        this.data = inputStream.readAllBytes();
      }

      // ファイルの保存日時を取得
      HeadObjectRequest headObjectRequest =
          HeadObjectRequest.builder().bucket(this.bucketName).key(this.objectKey).build();

      // ファイルのメタデータを取得
      HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

      // メタデータから保存日時を取得
      this.updateDate = Date.from(headObjectResponse.lastModified());

    } catch (Exception e) {
      log.error("【SesAiAssitantCore】S3からのファイル取得中にエラーが発生しました: {}", e.getMessage());
      throw new IOException("【SesAiAssitantCore】S3からファイルを取得できませんでした: " + e.getMessage(), e);
    }
  }

  /** このオブジェクトが持つバケット内のファイルを削除する. */
  public void delete() {
    try {
      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(this.bucketName).key(this.objectKey).build();

      this.s3Client.deleteObject(deleteObjectRequest);

      // 更新日時をクリア
      this.updateDate = null;

    } catch (Exception e) {
      log.error("【SesAiAssitantCore】S3ファイルの削除中にエラーが発生しました: {}", e.getMessage());
    }
  }
}
