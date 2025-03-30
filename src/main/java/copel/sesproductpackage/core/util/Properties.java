package copel.sesproductpackage.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * プロパティファイルを扱うクラス.
 *
 * @author 鈴木一矢
 *
 */
@Slf4j
public class Properties {
    /**
     * プロパティファイルが格納されているS3バケット名.
     */
    private static final String bucketName = "environment-variables-configuration";
    /**
     * プロパティファイル名.
     */
    private static final String objectKey = "config.properties";
    /**
     * プロパティファイルが存在するリージョン.
     */
    private static final Region region = Region.AP_NORTHEAST_1;
    /**
     * プロパティ.
     */
    private final static Map<String, String> properties = new HashMap<String, String>();

    /**
     * staticイニシャライザ.
     */
    static {
        // S3クライアントの作成
        S3Client s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(region)
                .build();

        try (InputStream inputStream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build())) {

            // プロパティファイルの内容を読み込む
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // 行が空でなく、'='で区切られていればプロパティとして登録
                if (!line.trim().isEmpty() && line.contains("=")) {
                    String[] keyValue = line.split("=", 2);
                    if (keyValue.length == 2) {
                        properties.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            log.info("【SesAiAssitantCore】バケット名：{} ファイル名：{}", bucketName, objectKey);
            log.error("【SesAiAssitantCore】環境変数の読み込みに失敗しました。{}", e.getMessage());
        }
    }

    /**
     * 引数をキーにプロパティの値を取得します.
     *
     * @param key キー
     * @return 値
     */
    public static String get(final String key) {
        return properties.get(key);
    }

    /**
     * 引数をキーにプロパティの値を文字配列で取得します.
     *
     * @param key キー
     * @return 配列
     */
    public static String[] getAsArray(final String key) {
        String value = properties.get(key);
        return value.split(",");
    }
}
