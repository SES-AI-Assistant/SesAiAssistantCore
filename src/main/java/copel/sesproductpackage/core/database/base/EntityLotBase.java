package copel.sesproductpackage.core.database.base;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * EntityLotの基底クラス.
 *
 * @author 鈴木一矢
 *
 */
public abstract class EntityLotBase<E extends EntityBase> implements Iterable<E> {
    // ================================
    // メンバ
    // ================================
    /**
     * エンティティLot.
     */
    protected Collection<E> entityLot;

    // ================================
    // コンストラクタ
    // ================================
    public EntityLotBase() {
        this.entityLot = new ArrayList<E>();
    }

    // ================================
    // メソッド定義
    // ================================
    /**
     * テーブルからレコードを全件SELECTし、このLotに保持します.
     *
     * @param connection DBコネクション
     * @throws SQLException
     */
    public abstract void selectAll(final Connection connection) throws SQLException;

    /**
     * このLotにEntityを追加します.
     *
     * @param entity エンティティ
     */
    public void add(E entity) {
        this.entityLot.add(entity);
    }

    /**
     * 引数のindex番目のエンティティを追加します.
     *
     * @param index インデックス
     * @return エンティティ
     */
    public E get(final int index) {
        if (index >= this.size()) {
            return null;
        }
        int currentIndex = 0;
        for (E entity : this.entityLot) {
            if (currentIndex == index) {
                return entity;
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * このLotの要素数を返却します.
     *
     * @return 要素数
     */
    public int size() {
        return this.entityLot.size();
    }

    /**
     * このLotが空であるかどうを返却します.
     *
     * @return 空であればtrue、そうでなければfalse
     */
    public boolean isEmpty() {
        return this.entityLot.isEmpty();
    }

    /**
     * このLotをIteratorとして返却します.
     *
     * @return Iterator
     */
    public Iterator<E> iterator() {
        return this.entityLot.iterator();
    }

    /**
     * このLotを昇順でソートします.
     */
    public void sort() {
        this.entityLot = this.entityLot.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String result = "";
        int i = 0;
        for (E entity : entityLot) {
            result += "(" + Integer.toString(i) + ")" + entity.toString();
            i++;
        }
        return result;
    }
}
