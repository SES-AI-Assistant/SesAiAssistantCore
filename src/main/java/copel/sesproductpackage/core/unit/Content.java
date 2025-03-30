package copel.sesproductpackage.core.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;

public class Content {
    /**
     * 案件情報特徴ワードマスタ.
     */
    private static final String[] JOB_FEATURES_ARRAY = Properties.getAsArray("JOB_FEATURES_ARRAY");
    /**
     * 要員情報特徴ワードマスタ.
     */
    private static final String[] PERSONEL_FEATURES_ARRAY = Properties.getAsArray("PERSONEL_FEATURES_ARRAY");
    /**
     * 判定基準文字数(この文字数以上であれば要員 or 案件情報と判定する).
     */
    private static final int TARGET_NUMBER_OF_CRITERIA = Integer.parseInt(Properties.get("TARGET_NUMBER_OF_CRITERIA"));
    /**
     * 文章が複数要員であるかどうかを判定し分割するプロンプト.
     */
    private static final String 複数要員判定プロンプト = Properties.get("MULTIPLE_PERSONNEL_JUDGMENT_PROMPT");
    /**
     * 文章が複数案件であるかどうかを判定し分割するプロンプト.
     */
    private static final String 複数案件判定プロンプト = Properties.get("MULTIPLE_JOB_JUDGMENT_PROMPT");

    /**
     * メッセージ原文.
     */
    private String rawContent;
    /**
     * 案件情報特徴ワードの登場回数.
     */
    private int jobFeatureCount = 0;
    /**
     * 要員情報特徴ワードの登場回数.
     */
    private int personelFeatureCount = 0;
    /**
     * 複数の情報が書かれている場合、各情報ごとに分割したリスト.
     */
    private List<String> contentList;
    /**
     * このコンテンツが複数であるかどうかを持つフラグ.
     */
    private boolean is複数紹介文 = false;

    /**
     * コンストラクタ.
     */
    public Content() {
        this.rawContent = null;
        this.contentList = new ArrayList<String>();
    }
    public Content(final String rawContent) {
        this.rawContent = rawContent;
        this.contentList = new ArrayList<String>();

        for (final String fetureKeyWord : JOB_FEATURES_ARRAY) {
            Pattern pattern = Pattern.compile(Pattern.quote(fetureKeyWord)); // 特殊文字をエスケープ
            Matcher matcher = pattern.matcher(this.rawContent);
            while (matcher.find()) {
                this.jobFeatureCount++;
            }
        }

        for (final String fetureKeyWord : PERSONEL_FEATURES_ARRAY) {
            Pattern pattern = Pattern.compile(Pattern.quote(fetureKeyWord)); // 特殊文字をエスケープ
            Matcher matcher = pattern.matcher(this.rawContent);
            while (matcher.find()) {
                this.personelFeatureCount++;
            }
        }
    }

    /**
     * 原文が空であるかどうかを判定します.
     *
     * @return 空であればtrue、そうでなければfalse
     */
    public boolean isEmpty() {
        return this.rawContent == null || "".equals(this.rawContent);
    }

    /**
     * このメッセージが案件の紹介文であるかどうかを判定します.
     *
     * @return {NUMBER_OF_CRITERIA}文字以上かつ案件特徴ワードが要員特徴ワードよりも頻出であればtrue、それ以外はfalse
     */
    public boolean is案件紹介文() {
        return !this.isEmpty()
                ? (this.rawContent.length() >= TARGET_NUMBER_OF_CRITERIA) && (this.jobFeatureCount > this.personelFeatureCount)
                : false;
    }

    /**
     * このメッセージが要員の紹介文であるかどうかを判定します.
     *
     * @return {NUMBER_OF_CRITERIA}文字以上かつ要員特徴ワードが案件特徴ワードよりも頻出であればtrue、それ以外はfalse
     */
    public boolean is要員紹介文() {
        return !this.isEmpty()
                ? (this.rawContent.length() >= TARGET_NUMBER_OF_CRITERIA) && (this.personelFeatureCount > this.jobFeatureCount)
                : false;
    }

    /**
     * このメッセージが複数の要員の紹介文であるかどうかを判定します.
     *
     * @return 複数であればtrue、複数でなければfalse
     */
    public boolean is複数紹介文() {
        return this.is複数紹介文;
    }

    /**
     * このメッセージが複数要員の情報を持つかどうか判定し、複数であればこのクラスのリストに結果を持ちます.
     *
     * @param transformer GPTクライアント
     * @retrun 複数であればtrue、単一であればfalse
     * @throws IOException
     * @throws RuntimeException
     */
    @SuppressWarnings("unchecked")
    public boolean 複数判定処理実行(final Transformer transformer) throws IOException, RuntimeException {
        String answer = null;

        // 複数の紹介文であれば配列の形で返却され、単一であれば「false」とだけ返すようなプロンプトを実行
        if (this.is要員紹介文()) {
            answer = transformer.generate(複数要員判定プロンプト + this.rawContent);
        } else if (this.is案件紹介文()) {
            answer = transformer.generate(複数案件判定プロンプト + this.rawContent);
        } else {
            return false;
        }

        // 10文字以上の文字列（「false」でない文字列）が返されていれば複数人の情報であると判断
        if (answer.length() > 10) {
            this.is複数紹介文 = true;
            // 回答が配列形式で取れていれば、[]の中を取得する
            Pattern pattern = Pattern.compile("\\[([^]]*)\\]");
            Matcher matcher = pattern.matcher(answer);
            answer = matcher.find() ? matcher.group(0).trim() : null;
            if (answer != null) {
                answer = answer.replace("\t", "\\t");
                answer = answer.replace("\n", "\\n");
                ObjectMapper objectMapper = new ObjectMapper();
                this.contentList = objectMapper.readValue(answer, List.class);
            } else {
                this.is複数紹介文 = false;
            }
        }
        return this.is複数紹介文;
    }

    /**
     * このクラスのcontentListを返却します.
     *
     * @return contentList
     */
    public List<String> getContentList() {
        return this.contentList;
    }

    @Override
    public String toString() {
        if (this.contentList.size() > 0) {
            return this.contentList.toString();
        } else {
            return this.rawContent;
        }
    }
}
