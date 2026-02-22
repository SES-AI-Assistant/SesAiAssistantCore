package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;

class ContentTests {

    // =========================================================
    // 実際の config.properties 相当の特徴ワードを @BeforeAll で設定する。
    // Content クラスの動的読み込みによりこの設定が確実に反映される。
    // =========================================================

    /** 判定基準文字数 */
    private static final int CRITERIA = 150;

    /** 案件強ワード（HIGH: 3点） */
    private static final String JOB_HIGH = "エンド直,支払サイト,支払いサイト,精算,就業,制限,契約,見合い,優遇,有識者,若手,エンド,外国";

    /** 案件弱ワード（LOW: 1点） */
    private static final String JOB_LOW = "募集,提案,期間,歓迎,支払い,向け,プロジェクト,概要,場所,内容,時期,前後,作業";

    /** 要員強ワード（HIGH: 3点） */
    private static final String PERSON_HIGH = "氏名,名前,稼働,男性,女性,性別,最寄り駅,直近,本人,資格,応相談";

    /** 要員弱ワード（LOW: 1点） */
    private static final String PERSON_LOW = "自己,性格,周囲,勤怠,職歴,アピール,人物,対人,正社員,取得,試験,人柄,キャッチアップ,忍耐,体力,主体性,作業,希望,活躍";

    /** 後方互換用 案件ワード */
    private static final String JOB_FEATURES = "エンド,エンド直,支払サイト,支払いサイト,募集,提案,外国,期間,就業,歓迎,支払い,制限,契約,精算,向け,見合い,優遇,有識者,プロジェクト,若手,概要,場所,内容,時期,前後,作業";

    /** 後方互換用 要員ワード */
    private static final String PERSON_FEATURES = "氏名,名前,本人,自己,性格,周囲,勤怠,職歴,アピール,人物,対人,稼働,資格,正社員,応相談,取得,性別,試験,人柄,男性,女性,キャッチアップ,忍耐,体力,主体性,作業,最寄り駅,希望,性別,直近,活躍";

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupProperties() throws Exception {
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
        propertiesMap.put("JOB_FEATURES_ARRAY", JOB_FEATURES);
        propertiesMap.put("JOB_FEATURES_ARRAY_HIGH", JOB_HIGH);
        propertiesMap.put("JOB_FEATURES_ARRAY_LOW", JOB_LOW);
        propertiesMap.put("PERSONEL_FEATURES_ARRAY", PERSON_FEATURES);
        propertiesMap.put("PERSONEL_FEATURES_ARRAY_HIGH", PERSON_HIGH);
        propertiesMap.put("PERSONEL_FEATURES_ARRAY_LOW", PERSON_LOW);
        propertiesMap.put("TARGET_NUMBER_OF_CRITERIA", String.valueOf(CRITERIA));
        propertiesMap.put("MULTIPLE_PERSONNEL_JUDGMENT_PROMPT", "Multiple Personnel?");
        propertiesMap.put("MULTIPLE_JOB_JUDGMENT_PROMPT", "Multiple Job?");
    }

    // =========================================================
    // 共通ユーティリティ
    // =========================================================

    /**
     * テストリソースファイルを文字列として読み込む.
     *
     * @param resourcePath クラスパスルートからのパス
     * @return ファイルの内容
     */
    private static String loadResource(final String resourcePath) throws IOException {
        try (InputStream is = ContentTests.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "テストリソースが見つかりません: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // =========================================================
    // 基本テスト
    // =========================================================

    @Test
    void testConstructorAndIsEmpty() {
        Content emptyContent = new Content();
        assertTrue(emptyContent.isEmpty());
        assertEquals(0, emptyContent.getContentList().size());
    }

    @Test
    void testIs案件紹介文() {
        // 150文字以上かつ案件強ワード・弱ワードが要員ワードよりも高スコアとなるテキスト
        // 案件強ワード: エンド直, 精算, 就業, 制限, 契約, 見合い, 優遇, 若手, 外国
        // 案件弱ワード: 募集, 提案, 期間, 歓迎, 場所, 内容, プロジェクト, 向け
        String text = "案件のご紹介です。エンド直の案件となります。精算幅は140H〜180Hです。"
                + "就業場所は東京都千代田区です。外国籍は不可です。契約は準委任。"
                + "見合い単価は80万円。優遇スキルあり。有識者歓迎。若手も歓迎。"
                + "制限なし。募集は2名。提案はメールにてお願いします。期間は3ヶ月。"
                + "プロジェクト概要：業務改善向けシステムの開発。内容はAPI設計・実装。";
        Content content = new Content(text);
        assertTrue(content.is案件紹介文(), "案件紹介文テキストが正しく判定されませんでした");
        assertFalse(content.is要員紹介文(), "案件紹介文テキストが要員と誤判定されました");
    }

    @Test
    void testIs要員紹介文() {
        // 150文字以上かつ要員強ワード・弱ワードが案件ワードよりも高スコアとなるテキスト
        // 要員強ワード: 氏名, 男性, 稼働, 性別, 最寄り駅, 直近, 本人, 資格, 応相談
        // 要員弱ワード: 職歴, アピール, 人物, 人柄, 希望, 活躍, 主体性
        String text = "氏名：田中太郎。性別：男性。稼働開始日：即日。"
                + "最寄り駅：渋谷駅。稼働率：100%。本人希望：フルリモート。"
                + "職歴：Javaエンジニア10年。直近の現場：大手金融系システム開発。"
                + "アピールポイント：主体性を持って活躍できます。人柄：明るく協調性あり。"
                + "資格：情報処理技術者。応相談で対応可能。人物像：コミュニケーション重視。";
        Content content = new Content(text);
        assertTrue(content.is要員紹介文(), "要員紹介文テキストが正しく判定されませんでした");
        assertFalse(content.is案件紹介文(), "要員紹介文テキストが案件と誤判定されました");
    }

    @Test
    void testIs案件紹介文_空文字はfalse() {
        Content emptyContent = new Content();
        assertFalse(emptyContent.is案件紹介文());
        assertFalse(emptyContent.is要員紹介文());
    }

    @Test
    void testIs要員紹介文_短文は判定不能() {
        // 150文字未満なので要員でも案件でもない
        Content shortContent = new Content("短いテキスト");
        assertFalse(shortContent.is案件紹介文());
        assertFalse(shortContent.is要員紹介文());
    }

    // =========================================================
    // 複数判定処理テスト
    // =========================================================

    @Test
    void testIs複数紹介文And複数判定処理実行_要員ルート() throws Exception {
        String text = "氏名：田中太郎。性別：男性。稼働開始日：即日。"
                + "最寄り駅：渋谷駅。稼働率：100%。本人希望：フルリモート。"
                + "職歴：Javaエンジニア10年。直近の現場：大手金融系システム開発。"
                + "アピールポイント：主体性を持って活躍できます。人柄：明るく協調性あり。"
                + "資格：情報処理技術者。応相談で対応可能。人物像：コミュニケーション重視。";
        Content content = new Content(text);
        assertFalse(content.is複数紹介文());

        Transformer transformer = mock(Transformer.class);
        GptAnswer answer = mock(GptAnswer.class);

        when(answer.length()).thenReturn(20);
        when(answer.isJsonArrayFormat()).thenReturn(true);
        when(answer.getAsList()).thenReturn(List.of("要員1", "要員2"));
        when(transformer.generate(anyString())).thenReturn(answer);

        boolean result = content.複数判定処理実行(transformer);
        assertTrue(result);
        assertTrue(content.is複数紹介文());
        assertEquals(2, content.getContentList().size());
        assertEquals("要員1", content.getContentList().get(0));
        // toString は contentList を返す
        assertTrue(content.toString().contains("要員1"));
    }

    @Test
    void test複数判定処理実行_FalseCase_短文() throws Exception {
        // 150文字未満のため is要員紹介文()・is案件紹介文() が共に false → 即 false を返す
        Content content = new Content("短い");
        Transformer transformer = mock(Transformer.class);
        assertFalse(content.複数判定処理実行(transformer));
        // toString は rawContent を返す（contentList が空）
        assertEquals("短い", content.toString());
    }

    @Test
    void test複数判定処理実行_案件ルート_GPT単一回答() throws Exception {
        // 案件紹介文に対して複数判定処理を実行し、GPTが短い文字列を返す場合（単一判定）
        String text = "案件のご紹介です。エンド直の案件となります。精算幅は140H〜180Hです。"
                + "就業場所は東京都千代田区です。外国籍は不可です。契約は準委任。"
                + "見合い単価は80万円。優遇スキルあり。有識者歓迎。若手も歓迎。"
                + "制限なし。募集は2名。提案はメールにてお願いします。期間は3ヶ月。"
                + "プロジェクト概要：業務改善向けシステムの開発。内容はAPI設計・実装。";
        Content content = new Content(text);
        Transformer transformer = mock(Transformer.class);
        GptAnswer answer = mock(GptAnswer.class);

        when(answer.length()).thenReturn(5);
        when(answer.isJsonArrayFormat()).thenReturn(false);
        when(transformer.generate(anyString())).thenReturn(answer);

        boolean result = content.複数判定処理実行(transformer);
        assertFalse(result);
        assertFalse(content.is複数紹介文());
    }

    @Test
    void test複数判定処理実行_案件ルート_GPT複数回答() throws Exception {
        // 案件紹介文に対して複数判定処理を実行し、GPTが複数件の配列を返す場合
        String text = "案件のご紹介です。エンド直の案件となります。精算幅は140H〜180Hです。"
                + "就業場所は東京都千代田区です。外国籍は不可です。契約は準委任。"
                + "見合い単価は80万円。優遇スキルあり。有識者歓迎。若手も歓迎。"
                + "制限なし。募集は2名。提案はメールにてお願いします。期間は3ヶ月。"
                + "プロジェクト概要：業務改善向けシステムの開発。内容はAPI設計・実装。";
        Content content = new Content(text);
        Transformer transformer = mock(Transformer.class);
        GptAnswer answer = mock(GptAnswer.class);

        when(answer.length()).thenReturn(50);
        when(answer.isJsonArrayFormat()).thenReturn(true);
        when(answer.getAsList()).thenReturn(List.of("案件A", "案件B"));
        when(transformer.generate(anyString())).thenReturn(answer);

        boolean result = content.複数判定処理実行(transformer);
        assertTrue(result);
        assertEquals(2, content.getContentList().size());
        assertTrue(content.toString().contains("案件A"));
    }

    // =========================================================
    // 実データを使った振り分けテスト（案件5種・要員5種）
    // =========================================================

    @Test
    void testIs案件紹介文_実データ5種() throws Exception {
        String[] jobFiles = {
                "content/job/job_sample_01.txt",
                "content/job/job_sample_02.txt",
                "content/job/job_sample_03.txt",
                "content/job/job_sample_04.txt",
                "content/job/job_sample_05.txt"
        };
        for (String file : jobFiles) {
            String text = loadResource(file);
            Content content = new Content(text);
            assertTrue(content.is案件紹介文(),
                    "案件として判定されるべきファイルが正しく判定されませんでした: " + file
                            + " [文字数=" + text.length() + "]");
            assertFalse(content.is要員紹介文(),
                    "案件ファイルが要員紹介文と誤判定されました: " + file);
        }
    }

    @Test
    void testIs要員紹介文_実データ5種() throws Exception {
        String[] personFiles = {
                "content/person/person_sample_01.txt",
                "content/person/person_sample_02.txt",
                "content/person/person_sample_03.txt",
                "content/person/person_sample_04.txt",
                "content/person/person_sample_05.txt"
        };
        for (String file : personFiles) {
            String text = loadResource(file);
            Content content = new Content(text);
            assertTrue(content.is要員紹介文(),
                    "要員として判定されるべきファイルが正しく判定されませんでした: " + file
                            + " [文字数=" + text.length() + "]");
            assertFalse(content.is案件紹介文(),
                    "要員ファイルが案件紹介文と誤判定されました: " + file);
        }
    }

    // =========================================================
    // カバレッジ補完テスト
    // =========================================================

    @Test
    void testIsEmpty_空文字はtrue() {
        // rawContent が空文字の場合 isEmpty() は true を返す
        Content content = new Content("");
        assertTrue(content.isEmpty());
        assertFalse(content.is案件紹介文());
        assertFalse(content.is要員紹介文());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCalcScore_フォールバックパス_HIGH設定なし() throws Exception {
        // JOB_FEATURES_ARRAY_HIGH / LOW を削除して後方互換フォールバックパスを通す
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);

        String savedJobHigh = propertiesMap.remove("JOB_FEATURES_ARRAY_HIGH");
        String savedJobLow = propertiesMap.remove("JOB_FEATURES_ARRAY_LOW");
        String savedPersonHigh = propertiesMap.remove("PERSONEL_FEATURES_ARRAY_HIGH");
        String savedPersonLow = propertiesMap.remove("PERSONEL_FEATURES_ARRAY_LOW");

        try {
            // フォールバック: JOB_FEATURES_ARRAY のみ使用（精算・就業・制限等が含まれる）
            String text = "案件のご紹介です。エンド直の案件となります。精算幅は140H〜180Hです。"
                    + "就業場所は東京都千代田区です。外国籍は不可です。契約は準委任。"
                    + "見合い単価は80万円。優遇スキルあり。有識者歓迎。若手も歓迎。"
                    + "制限なし。募集は2名。提案はメールにてお願いします。期間は3ヶ月。"
                    + "プロジェクト概要：業務改善向けシステムの開発。内容はAPI設計・実装。";
            Content content = new Content(text);
            // フォールバックパスでも Content が正常に生成されることを確認
            assertNotNull(content);
            assertFalse(content.isEmpty());
        } finally {
            // 必ず元に戻す
            if (savedJobHigh != null)
                propertiesMap.put("JOB_FEATURES_ARRAY_HIGH", savedJobHigh);
            if (savedJobLow != null)
                propertiesMap.put("JOB_FEATURES_ARRAY_LOW", savedJobLow);
            if (savedPersonHigh != null)
                propertiesMap.put("PERSONEL_FEATURES_ARRAY_HIGH", savedPersonHigh);
            if (savedPersonLow != null)
                propertiesMap.put("PERSONEL_FEATURES_ARRAY_LOW", savedPersonLow);
        }
    }

    @Test
    void test複数判定処理実行_answer長さ11かつJSON非配列() throws Exception {
        // answer.length() > 10 だが isJsonArrayFormat() が false の場合
        // → is複数紹介文 は false のまま
        String text = "案件のご紹介です。エンド直の案件となります。精算幅は140H〜180Hです。"
                + "就業場所は東京都千代田区です。外国籍は不可です。契約は準委任。"
                + "見合い単価は80万円。優遇スキルあり。有識者歓迎。若手も歓迎。"
                + "制限なし。募集は2名。提案はメールにてお願いします。期間は3ヶ月。"
                + "プロジェクト概要：業務改善向けシステムの開発。内容はAPI設計・実装。";
        Content content = new Content(text);
        assertTrue(content.is案件紹介文());

        Transformer transformer = mock(Transformer.class);
        GptAnswer answer = mock(GptAnswer.class);

        // length > 10 だが JSON 配列でない
        when(answer.length()).thenReturn(11);
        when(answer.isJsonArrayFormat()).thenReturn(false);
        when(transformer.generate(anyString())).thenReturn(answer);

        boolean result = content.複数判定処理実行(transformer);
        assertFalse(result);
        assertFalse(content.is複数紹介文());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCalcScore_HIGH設定ありLOW設定なし_elseブランチ() throws Exception {
        // highRaw != null だが lowRaw == null のブランチを通す（L175の残りブランチ）
        // かつ fallbackRaw == null のブランチも通す（L182の残りブランチ）
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);

        String savedJobHigh = propertiesMap.get("JOB_FEATURES_ARRAY_HIGH");
        String savedJobLow = propertiesMap.remove("JOB_FEATURES_ARRAY_LOW");
        String savedPersonHigh = propertiesMap.get("PERSONEL_FEATURES_ARRAY_HIGH");
        String savedPersonLow = propertiesMap.remove("PERSONEL_FEATURES_ARRAY_LOW");
        String savedJobFallback = propertiesMap.remove("JOB_FEATURES_ARRAY");
        String savedPersonFallback = propertiesMap.remove("PERSONEL_FEATURES_ARRAY");

        try {
            // LOW も fallback も null → else -> fallbackRaw==null ブランチ
            String text = "氏名：田中太郎。性別：男性。稼働開始日：即日。"
                    + "最寄り駅：渋谷駅。稼働率：100%。本人希望：フルリモート。"
                    + "職歴：Javaエンジニア10年。直近の現場：大手金融系システム開発。"
                    + "アピールポイント：主体性を持って活躍できます。人柄：明るく協調性あり。"
                    + "資格：情報処理技術者。応相談で対応可能。人物像：コミュニケーション重視。";
            Content content = new Content(text);
            // スコアが正常に計算されることを確認
            assertNotNull(content);
            assertFalse(content.isEmpty());
        } finally {
            propertiesMap.put("JOB_FEATURES_ARRAY_HIGH", savedJobHigh);
            if (savedJobLow != null)
                propertiesMap.put("JOB_FEATURES_ARRAY_LOW", savedJobLow);
            propertiesMap.put("PERSONEL_FEATURES_ARRAY_HIGH", savedPersonHigh);
            if (savedPersonLow != null)
                propertiesMap.put("PERSONEL_FEATURES_ARRAY_LOW", savedPersonLow);
            if (savedJobFallback != null)
                propertiesMap.put("JOB_FEATURES_ARRAY", savedJobFallback);
            if (savedPersonFallback != null)
                propertiesMap.put("PERSONEL_FEATURES_ARRAY", savedPersonFallback);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCountOccurrences_キーワードに空文字を含む() throws Exception {
        // keyword が trim().isEmpty() になるケースを通す（L199の残りブランチ）
        // カンマだけのワード（,）を JOB_FEATURES_ARRAY_HIGH に設定することで空文字を生成
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);

        String savedJobHigh = propertiesMap.put("JOB_FEATURES_ARRAY_HIGH", "エンド直, ,精算");
        String savedJobLow = propertiesMap.put("JOB_FEATURES_ARRAY_LOW", "");

        try {
            // 空文字キーワードが含まれた状態で Content を生成しても正常動作することを確認
            String text = "案件のご紹介です。エンド直の案件となります。精算幅は140H〜180Hです。"
                    + "就業場所は東京都千代田区です。外国籍は不可です。契約は準委任。"
                    + "見合い単価は80万円。優遇スキルあり。有識者歓迎。若手も歓迎。"
                    + "制限なし。募集は2名。提案はメールにてお願いします。期間は3ヶ月。"
                    + "プロジェクト概要：業務改善向けシステムの開発。内容はAPI設計・実装。";
            Content content = new Content(text);
            assertNotNull(content);
        } finally {
            if (savedJobHigh != null)
                propertiesMap.put("JOB_FEATURES_ARRAY_HIGH", savedJobHigh);
            else
                propertiesMap.remove("JOB_FEATURES_ARRAY_HIGH");
            if (savedJobLow != null)
                propertiesMap.put("JOB_FEATURES_ARRAY_LOW", savedJobLow);
            else
                propertiesMap.remove("JOB_FEATURES_ARRAY_LOW");
        }
    }
}
