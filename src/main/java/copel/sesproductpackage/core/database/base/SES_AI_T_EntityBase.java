package copel.sesproductpackage.core.database.base;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;

/**
 * SES AIアシスタントのベクトルDB群の基底クラス.
 *
 * @author 鈴木一矢
 *
 */
public abstract class SES_AI_T_EntityBase extends EntityBase {
    /**
     * 送信元グループ / from_group
     */
    @Column(
        physicalName = "from_group",
        logicalName = "送信元グループ")
    protected String fromGroup;
    /**
     * 送信者ID / from_id
     */
    @Column(
        physicalName = "from_id",
        logicalName = "送信者ID")
    protected String fromId;
    /**
     * 送信者名 / from_name
     */
    @Column(
        physicalName = "from_name",
        logicalName = "送信者名")
    protected String fromName;
    /**
     * OpenAIベクトルデータ / vector_data.
     */
    @Column(
        physicalName = "vector_data",
        logicalName = "OpenAIベクトルデータ")
    protected Vector vectorData;
    /**
     * 有効期限 / ttl
     */
    @Column(
        physicalName = "ttl",
        logicalName = "有効期限")
    protected OriginalDateTime ttl;
    /**
     * ユークリッド距離 / distance.
     */
    @Column(
        physicalName = "distance",
        logicalName = "ユークリッド距離")
    protected double distance;

    /**
     * このエンティティが持つ内容をエンベディングする.
     *
     * @param transformer GPT
     * @throws IOException
     * @throws RuntimeException
     */
    public abstract void embedding(final Transformer transformer) throws IOException, RuntimeException;
    /**
     * テーブル内にこのエンティティの持つ内容と類似したレコードがあるかどうを判定する.
     *
     * @param connection DBコネクション
     * @param similarityThreshold 類似度基準値(0.0～1.0で指定する。文章の一致率を示す。例えば0.8であれば、80%以上一致する文章が存在しなければユニークであると判定)
     * @return 類似するレコードがなければtrue、あればfalse
     * @throws SQLException
     */
    public abstract boolean uniqueCheck(final Connection connection, final double similarityThreshold) throws SQLException;
    /**
     * このレコードのユークリッド距離で比較する.
     *
     * @param o SES_AI_T_EntityBase
     * @return このレコードの方が小さければtrue、遠ければfalse
     */
    public int compareTo(SES_AI_T_EntityBase o) {
        return Double.compare(this.getDistance(), o.getDistance());
    }

    // ================================
    // Getter / Setter
    // ================================
    public String getFromGroup() {
        return fromGroup;
    }
    public void setFromGroup(String fromGroup) {
        this.fromGroup = fromGroup;
    }
    public String getFromId() {
        return fromId;
    }
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }
    public String getFromName() {
        return fromName;
    }
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
    public Vector getVectorData() {
        return vectorData;
    }
    public void setVectorData(Vector vectorData) {
        this.vectorData = vectorData;
    }
    public OriginalDateTime getTtl() {
        return ttl;
    }
    public void setTtl(OriginalDateTime ttl) {
        this.ttl = ttl;
    }
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
}
