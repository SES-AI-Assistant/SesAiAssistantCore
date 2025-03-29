package copel.sesproductpackage.core.unit;

import java.io.IOException;

import copel.sesproductpackage.core.api.gpt.Transformer;

/**
 * 【SES AIアシスタント】
 * ベクトルクラス.
 *
 * @author 鈴木一矢.
 *
 */
public class Vector {
    /**
     * エンベディング処理の対象となる文字列.
     */
    private String rawString;
    /**
     * このベクトルの値.
     */
    private float[] value;
    /**
     * GPT処理リスナー.
     */
    private Transformer transformer;

    /**
     * コンストラクタ.
     *
     * @param transformer GPT処理リスナー
     */
    public Vector(final Transformer transformer) {
        this.transformer = transformer;
    }

    /**
     * このクラスが持つ文字列をエンベディング処理しベクトル値として格納します.
     *
     * @throws IOException
     * @throws RuntimeException
     */
    public void embedding() throws IOException, RuntimeException {
        if (this.transformer != null
                && this.rawString != null
                && !"".equals(this.rawString)) {
            this.value = this.transformer.embedding(this.rawString);
        }
    }

    /**
     * このクラスが持つ文字列を返却します.
     *
     * @return rawString
     */
    public String getRawString() {
        return rawString;
    }

    /**
     * このクラスにエンベディング処理の対象となる文字列をセットします.
     *
     * @param rawString 対象の文字列
     */
    public void setRawString(String rawString) {
        this.rawString = rawString;
    }

    /**
     * このクラスが持つベクトル値を返却します.
     *
     * @return ベクトル値
     */
    public float[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (float value : this.value) {
            i++;
            builder.append(value);
            if (i < this.value.length) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
