package copel.sesproductpackage.core.api.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【SES AIアシスタント】 Google Drive 連携クラス.
 * スプレッドシートやドキュメントの公開URLから内容をテキストで取得します。
 *
 * @author Copel Co., Ltd..
 */
public class GoogleDriveFetcher {

    private static final Pattern SPREADSHEET_PATTERN = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("/document/d/([a-zA-Z0-9-_]+)");
    private static final Pattern GID_PATTERN = Pattern.compile("gid=([0-9]+)");

    /**
     * プライベートコンストラクタ.
     */
    private GoogleDriveFetcher() {}

    /**
     * URLからドキュメントのテキスト内容（またはCSV）を取得する.
     * @param documentUrl Google Driveの公開URL
     * @return 抽出されたテキスト
     * @throws IOException 通信エラーや不正なURLの場合
     */
    public static String fetchText(String documentUrl) throws IOException {
        if (documentUrl == null || documentUrl.isBlank()) {
            throw new IllegalArgumentException("URLが空です");
        }

        Matcher sheetMatcher = SPREADSHEET_PATTERN.matcher(documentUrl);
        Matcher docMatcher = DOCUMENT_PATTERN.matcher(documentUrl);

        String exportUrl = null;
        if (sheetMatcher.find()) {
            String fileId = sheetMatcher.group(1);
            Matcher gidMatcher = GID_PATTERN.matcher(documentUrl);
            String gid = gidMatcher.find() ? gidMatcher.group(1) : "0";
            exportUrl = "https://docs.google.com/spreadsheets/d/" + fileId + "/export?format=csv&gid=" + gid;
        } else if (docMatcher.find()) {
            String fileId = docMatcher.group(1);
            exportUrl = "https://docs.google.com/document/d/" + fileId + "/export?format=txt";
        } else {
            throw new IllegalArgumentException("対応していないURL形式です。GoogleスプレッドシートまたはドキュメントのURLを指定してください。");
        }

        return executeGet(exportUrl);
    }

    private static String executeGet(String targetUrl) throws IOException {
        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
            || responseCode == HttpURLConnection.HTTP_MOVED_PERM
            || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
            String redirectUrl = connection.getHeaderField("Location");
            return executeGet(redirectUrl);
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
             throw new IOException("Failed to fetch document. HTTP Response Code: " + responseCode);
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
