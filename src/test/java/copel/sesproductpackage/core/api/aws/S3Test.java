package copel.sesproductpackage.core.api.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** {@link S3} の単体テスト。 */
class S3Test {

  private static final String BUCKET = "bucket";
  private static final String KEY = "key/file.txt";

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * バケット名とオブジェクトキーを指定した場合に、それらを保持することを確かめる。 保存と取得の対象を生成時に固定するため、保持する値を揃えることを守る。
       *
       * @given バケット名 bucket とオブジェクトキー key/file.txt と リージョン ap-northeast-1 がある
       * @when S3 を生成する
       * @then バケット名とオブジェクトキーを保持する
       */
      @Test
      void バケット名とキーを指定した場合_バケット名とキーを返す() {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          prepareS3(s3);

          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);

          assertThat(file.getBucketName()).isEqualTo(BUCKET);
          assertThat(file.getObjectKey()).isEqualTo(KEY);
        }
      }

      /**
       * 接続先のエンドポイントURLが指定されている場合に、S3 クライアントの接続先を上書きすることを確かめる。 IT1 は他の AWS クライアントと同じ moto に接続して検証する。
       *
       * @given システムプロパティ aws.endpointUrl が "http://localhost:4566"
       * @when S3 を生成する
       * @then S3 クライアントの接続先を "http://localhost:4566" に上書きして、パス形式でバケットを指定し、バケット名を返す
       */
      @Test
      void エンドポイントURLが指定されている場合_バケット名を返す() {
        System.setProperty("aws.endpointUrl", "http://localhost:4566");
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
          when(builder.build()).thenReturn(mock(S3Client.class));
          s3.when(S3Client::builder).thenReturn(builder);

          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);

          verify(builder).endpointOverride(java.net.URI.create("http://localhost:4566"));
          verify(builder).forcePathStyle(true);
          assertThat(file.getBucketName()).isEqualTo(BUCKET);
        } finally {
          System.clearProperty("aws.endpointUrl");
        }
      }
    }
  }

  @Nested
  class save {

    @Nested
    class 正常系 {

      /**
       * バケット名とキーとデータが揃っている場合に、S3 へ保存して更新日時を設定することを確かめる。 保存の成否を更新日時で判断できるようにするため、保存後に現在時刻を設定することを守る。
       *
       * @given データが3バイトで、バケット名とキーが設定され、現在時刻が2026年10月20日10時である
       * @when save を呼び出す
       * @then PutObject を1回呼び出し、更新日時を現在時刻に設定する
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 必要な値が揃っている場合_更新日時を更新する() throws Exception {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);
          file.setData(new byte[] {1, 2, 3});

          file.save();

          verify(client)
              .putObject(
                  any(PutObjectRequest.class),
                  any(software.amazon.awssdk.core.sync.RequestBody.class));
          assertThat(file.getUpdateDate())
              .isEqualTo(Date.from(Instant.parse("2026-10-20T01:00:00Z")));
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 保存の呼び出しが失敗した場合に、原因例外を持つ IOException を投げることを確かめる。 保存の失敗を握りつぶさず呼び出し元に伝え、原因の例外も辿れるようにすることを守る。
       *
       * @given PutObject が SDK の例外を投げる
       * @when save を呼び出す
       * @then IOException を投げ、その原因が SDK の例外であり、更新日時は null のままである
       */
      @Test
      void 保存の呼び出しが失敗する場合_IOExceptionを投げる() {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          SdkClientException cause = SdkClientException.create("接続失敗");
          doThrow(cause)
              .when(client)
              .putObject(
                  any(PutObjectRequest.class),
                  any(software.amazon.awssdk.core.sync.RequestBody.class));
          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);
          file.setData(new byte[] {1});

          assertThatThrownBy(file::save).isInstanceOf(IOException.class).hasCause(cause);
          assertThat(file.getUpdateDate()).isNull();
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * バケット名が null の場合に、保存せずに終えることを確かめる。 保存先が無いまま呼び出しをしないことを守る。
       *
       * @given バケット名が null で、キーとデータが設定されている
       * @when save を呼び出す
       * @then PutObject を呼び出さない
       */
      @Test
      void バケット名がnullの場合_何もしない() throws Exception {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          S3 file = new S3(null, KEY, Region.AP_NORTHEAST_1);
          file.setData(new byte[] {1});

          file.save();

          verify(client, never())
              .putObject(
                  any(PutObjectRequest.class),
                  any(software.amazon.awssdk.core.sync.RequestBody.class));
        }
      }

      /**
       * オブジェクトキーが null の場合に、保存せずに終えることを確かめる。 保存先のキーが無いまま呼び出しをしないことを守る。
       *
       * @given キーが null で、バケット名とデータが設定されている
       * @when save を呼び出す
       * @then PutObject を呼び出さない
       */
      @Test
      void キーがnullの場合_何もしない() throws Exception {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          S3 file = new S3(BUCKET, null, Region.AP_NORTHEAST_1);
          file.setData(new byte[] {1});

          file.save();

          verify(client, never())
              .putObject(
                  any(PutObjectRequest.class),
                  any(software.amazon.awssdk.core.sync.RequestBody.class));
        }
      }

      /**
       * データが null の場合に、保存せずに終えることを確かめる。 中身の無いオブジェクトを保存しないことを守る。
       *
       * @given データが null で、バケット名とキーが設定されている
       * @when save を呼び出す
       * @then PutObject を呼び出さない
       */
      @Test
      void データがnullの場合_何もしない() throws Exception {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);

          file.save();

          verify(client, never())
              .putObject(
                  any(PutObjectRequest.class),
                  any(software.amazon.awssdk.core.sync.RequestBody.class));
          assertThat(file.getUpdateDate()).isNull();
        }
      }
    }
  }

  @Nested
  class read {

    @Nested
    class 正常系 {

      /**
       * 取得が成功した場合に、本文と最終更新日時を保持することを確かめる。 取得した本文を後続の処理で使い、更新日時を比較に使うことを守る。
       *
       * @given GetObject が本文3バイトを返し、HeadObject が最終更新日時を返す
       * @when read を呼び出す
       * @then 本文の3バイトと最終更新日時を保持する
       */
      @Test
      void 取得が成功する場合_本文と更新日時を返す() throws Exception {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          when(client.getObject(any(GetObjectRequest.class)))
              .thenReturn(stream(new byte[] {1, 2, 3}));
          when(client.headObject(
                  any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
              .thenReturn(
                  HeadObjectResponse.builder()
                      .lastModified(Instant.parse("2026-10-20T01:00:00Z"))
                      .build());
          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);

          file.read();

          assertThat(file.getData()).containsExactly(1, 2, 3);
          assertThat(file.getUpdateDate())
              .isEqualTo(Date.from(Instant.parse("2026-10-20T01:00:00Z")));
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 取得が失敗した場合に、入出力の例外を投げることを確かめる。 取得できない本文を空として扱わず、呼び出し元に失敗を伝えることを守る。
       *
       * @given GetObject が SDK の例外を投げる
       * @when read を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 取得が失敗する場合_IOExceptionを投げる() throws Exception {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          when(client.getObject(any(GetObjectRequest.class)))
              .thenThrow(SdkClientException.create("接続失敗"));
          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);

          assertThatThrownBy(file::read).isInstanceOf(IOException.class);
        }
      }
    }
  }

  @Nested
  class delete {

    @Nested
    class 正常系 {

      /**
       * 削除が成功した場合に、更新日時を消去することを確かめる。 削除済みのファイルを更新日時で存在すると判断しないことを守る。
       *
       * @given 更新日時が設定されている
       * @when delete を呼び出す
       * @then DeleteObject を呼び出し、更新日時を null にする
       */
      @Test
      void 削除が成功する場合_更新日時を削除する() {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);
          file.setUpdateDate(new Date());

          file.delete();

          verify(client).deleteObject(any(DeleteObjectRequest.class));
          assertThat(file.getUpdateDate()).isNull();
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 削除が失敗した場合に、例外を投げず更新日時を残すことを確かめる。 削除できていないのに更新日時を消すと、存在の判断を誤ることを守る。
       *
       * @given DeleteObject が SDK の例外を投げ、更新日時が設定されている
       * @when delete を呼び出す
       * @then 例外を投げず、更新日時を残す
       */
      @Test
      void 削除が失敗する場合_何もしない() {
        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          S3Client client = prepareS3(s3);
          doThrow(SdkClientException.create("接続失敗"))
              .when(client)
              .deleteObject(any(DeleteObjectRequest.class));
          S3 file = new S3(BUCKET, KEY, Region.AP_NORTHEAST_1);
          Date before = new Date();
          file.setUpdateDate(before);

          file.delete();

          assertThat(file.getUpdateDate()).isEqualTo(before);
        }
      }
    }
  }

  /**
   * S3Client の生成を差し替え、テスト用のクライアントを返す.
   *
   * @param s3 S3Client の静的モック
   * @return S3Client のモック
   */
  private static S3Client prepareS3(final MockedStatic<S3Client> s3) {
    S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
    S3Client client = mock(S3Client.class);
    when(builder.build()).thenReturn(client);
    s3.when(S3Client::builder).thenReturn(builder);
    return client;
  }

  /**
   * 本文を持つ取得結果を作る.
   *
   * @param body 本文
   * @return 取得結果
   */
  private static ResponseInputStream<GetObjectResponse> stream(final byte[] body) {
    return new ResponseInputStream<>(
        GetObjectResponse.builder().build(),
        AbortableInputStream.create(new ByteArrayInputStream(body)));
  }
}
