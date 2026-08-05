# MyBatis TypeHandler 設計 - カスタム型ハンドラー仕様書

**作成日**: 2026-08-05  
**対象**: PostgreSQL 拡張型・アプリケーション独自型の MyBatis 型変換  
**ステータス**: 設計確定

---

## 1. 概要

本ドキュメントは、MyBatis が標準で対応していない PostgreSQL の拡張型およびアプリケーション独自型を、Java オブジェクトとデータベースの間で双方向に変換するための **TypeHandler** の設計を定義します。

### 対応する型

1. **Vector 型** (PostgreSQL pgvector 拡張)
   - テーブルカラム型: `vector(1536)`
   - Java型: `Vector`（アプリケーション独自）
   - 用途: AI 埋め込みベクトル

2. **Money 型** (アプリケーション独自)
   - テーブルカラム型: `NUMERIC(10, 2)`
   - Java型: `Money`（アプリケーション独自）
   - 用途: 案件・要員の単価管理

3. **OriginalDateTime 型** (アプリケーション独自)
   - テーブルカラム型: `timestamp`
   - Java型: `OriginalDateTime`（アプリケーション独自、タイムゾーン対応）
   - 用途: 登録日時、有効期限など

---

## 2. Vector 型 TypeHandler

### 2.1 概要

PostgreSQL の `pgvector` 拡張による `vector(1536)` 型は、OpenAI Embedding や Gemini 等の生成 AI が出力する 1536 次元のベクトルを保持します。

### 2.2 メタデータ

| 項目 | 値 |
| :--- | :--- |
| **テーブルカラム型** | `vector(1536)` |
| **Java型** | `Vector` |
| **パッケージ** | `copel.sesproductpackage.core.unit` |
| **DB 出力形式** | `[0.123, -0.456, ..., 0.789]` (JSON 配列形式) |
| **Java 内部表現** | `double[]` または `List<Double>` |

### 2.3 仕様

#### 2.3.1 getResult() - ResultSet → Java 型（SELECT時）

**処理フロー**:
1. `ResultSet.getString(columnName)` で PostgreSQL から文字列取得
2. JSON 配列 `[0.1, 0.2, ...]` を `double[]` にパース
3. `Vector` オブジェクトに変換

**例**:
```
PostgreSQL: '[0.123, -0.456, 0.789, ...]'
  ↓ (TypeHandler)
Java: Vector(new double[] {0.123, -0.456, 0.789, ...})
```

#### 2.3.2 setParameter() - Java 型 → PreparedStatement（INSERT/UPDATE時）

**処理フロー**:
1. `Vector.toArray()` で `double[]` を取得
2. JSON 配列文字列に変換
3. `PreparedStatement.setString()` に バインド

**例**:
```
Java: Vector(new double[] {0.123, -0.456, 0.789, ...})
  ↓ (TypeHandler)
PostgreSQL: '[0.123, -0.456, 0.789, ...]'
```

### 2.4 実装コード例（概要）

```java
/**
 * PostgreSQL Vector型 → Java Vector型 の双方向変換.
 * 
 * @author Copel Co., Ltd.
 */
public class VectorTypeHandler extends BaseTypeHandler<Vector> {
    
    private static final Logger log = LoggerFactory.getLogger(VectorTypeHandler.class);
    
    /**
     * Java Vector → PreparedStatement へバインド (INSERT/UPDATE)
     */
    @Override
    public void setNonNullParameter(
        PreparedStatement ps, int i, Vector parameter, JdbcType jdbcType)
        throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.VARCHAR);
            return;
        }
        
        try {
            // Vector → JSON配列文字列に変換
            String vectorStr = parameter.toPostgresString();
            ps.setString(i, vectorStr);
            
            if (log.isDebugEnabled()) {
                log.debug("[TypeHandler] Vector.setParameter: vectorStr={}", vectorStr);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to convert Vector to PostgreSQL format", e);
        }
    }
    
    /**
     * ResultSet → Java Vector (SELECT)
     */
    @Override
    public Vector getNullableResult(ResultSet rs, String columnName)
        throws SQLException {
        String vectorStr = rs.getString(columnName);
        if (vectorStr == null || vectorStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Vector.parse(vectorStr);
        } catch (Exception e) {
            throw new SQLException(
                "Failed to parse Vector from PostgreSQL: " + vectorStr, e);
        }
    }
    
    /**
     * ResultSet → Java Vector (by index)
     */
    @Override
    public Vector getNullableResult(ResultSet rs, int columnIndex)
        throws SQLException {
        String vectorStr = rs.getString(columnIndex);
        if (vectorStr == null || vectorStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Vector.parse(vectorStr);
        } catch (Exception e) {
            throw new SQLException(
                "Failed to parse Vector from PostgreSQL at column " + columnIndex, e);
        }
    }
    
    /**
     * CallableStatement → Java Vector (ストアドプロシージャ)
     */
    @Override
    public Vector getNullableResult(CallableStatement cs, int columnIndex)
        throws SQLException {
        String vectorStr = cs.getString(columnIndex);
        if (vectorStr == null || vectorStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Vector.parse(vectorStr);
        } catch (Exception e) {
            throw new SQLException(
                "Failed to parse Vector from CallableStatement at index " + columnIndex, e);
        }
    }
}
```

### 2.5 Vector クラス実装（参考）

```java
/**
 * PostgreSQL vector型の Java ラッパー.
 * 
 * @author Copel Co., Ltd.
 */
public class Vector implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Pattern VECTOR_PATTERN = 
        Pattern.compile("^\\[(.*?)\\]$");
    
    private double[] values;
    
    public Vector(double[] values) {
        this.values = values;
    }
    
    /**
     * JSON配列形式の文字列からVectorをパース.
     * 例: "[0.123, -0.456, 0.789]"
     */
    public static Vector parse(String vectorStr) throws NumberFormatException {
        vectorStr = vectorStr.trim();
        Matcher matcher = VECTOR_PATTERN.matcher(vectorStr);
        if (!matcher.matches()) {
            throw new NumberFormatException(
                "Invalid vector format: " + vectorStr);
        }
        
        String values = matcher.group(1);
        if (values.trim().isEmpty()) {
            return new Vector(new double[0]);
        }
        
        String[] parts = values.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].trim());
        }
        
        return new Vector(result);
    }
    
    /**
     * PostgreSQL形式の文字列に変換.
     */
    public String toPostgresString() {
        if (values == null || values.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    public double[] toArray() {
        return values;
    }
    
    public int dimension() {
        return values != null ? values.length : 0;
    }
}
```

---

## 3. Money 型 TypeHandler

### 3.1 概要

アプリケーション独自の `Money` クラスは、金銭額を `BigDecimal` で正確に管理し、通貨単位（JPY 等）を保持します。DB では `NUMERIC(10, 2)` に格納。

### 3.2 メタデータ

| 項目 | 値 |
| :--- | :--- |
| **テーブルカラム型** | `NUMERIC(10, 2)` |
| **Java型** | `Money` |
| **パッケージ** | `copel.sesproductpackage.core.unit` |
| **DB 出力形式** | `100000.00` (Decimal) |
| **Java 内部表現** | `BigDecimal + Currency` |

### 3.3 仕様

#### 3.3.1 getResult() - ResultSet → Java 型（SELECT時）

**処理フロー**:
1. `ResultSet.getBigDecimal(columnName)` で Decimal 値を取得
2. 通貨情報（JPY）を付加
3. `Money` オブジェクト化

**例**:
```
PostgreSQL: 100000.00 (NUMERIC)
  ↓ (TypeHandler)
Java: Money(new BigDecimal("100000.00"), Currency.JPY)
```

#### 3.3.2 setParameter() - Java 型 → PreparedStatement（INSERT/UPDATE時）

**処理フロー**:
1. `Money.getAmount()` で `BigDecimal` を取得
2. `PreparedStatement.setBigDecimal()` にバインド

**例**:
```
Java: Money(new BigDecimal("100000.00"), Currency.JPY)
  ↓ (TypeHandler)
PostgreSQL: 100000.00 (NUMERIC)
```

### 3.4 実装コード例（概要）

```java
/**
 * Money型 ↔ NUMERIC(10, 2) の双方向変換.
 * 
 * @author Copel Co., Ltd.
 */
public class MoneyTypeHandler extends BaseTypeHandler<Money> {
    
    private static final Logger log = LoggerFactory.getLogger(MoneyTypeHandler.class);
    
    /**
     * Java Money → PreparedStatement へバインド (INSERT/UPDATE)
     */
    @Override
    public void setNonNullParameter(
        PreparedStatement ps, int i, Money parameter, JdbcType jdbcType)
        throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.NUMERIC);
            return;
        }
        
        try {
            BigDecimal amount = parameter.getAmount();
            ps.setBigDecimal(i, amount);
            
            if (log.isDebugEnabled()) {
                log.debug("[TypeHandler] Money.setParameter: amount={}, currency={}", 
                    amount, parameter.getCurrency());
            }
        } catch (Exception e) {
            throw new SQLException("Failed to convert Money to NUMERIC", e);
        }
    }
    
    /**
     * ResultSet → Java Money (SELECT)
     */
    @Override
    public Money getNullableResult(ResultSet rs, String columnName)
        throws SQLException {
        BigDecimal amount = rs.getBigDecimal(columnName);
        if (amount == null) {
            return null;
        }
        
        // デフォルト通貨: JPY
        return new Money(amount, Currency.JPY);
    }
    
    /**
     * ResultSet → Java Money (by index)
     */
    @Override
    public Money getNullableResult(ResultSet rs, int columnIndex)
        throws SQLException {
        BigDecimal amount = rs.getBigDecimal(columnIndex);
        if (amount == null) {
            return null;
        }
        
        return new Money(amount, Currency.JPY);
    }
    
    /**
     * CallableStatement → Java Money
     */
    @Override
    public Money getNullableResult(CallableStatement cs, int columnIndex)
        throws SQLException {
        BigDecimal amount = cs.getBigDecimal(columnIndex);
        if (amount == null) {
            return null;
        }
        
        return new Money(amount, Currency.JPY);
    }
}
```

### 3.5 Money クラス実装（参考）

```java
/**
 * 金銭額を安全に扱う値オブジェクト.
 * 
 * @author Copel Co., Ltd.
 */
public class Money implements Comparable<Money>, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency must not be null");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    /**
     * 通常の単価フォーマット(JSON出力等)
     */
    public static Money of(String amountStr, Currency currency) {
        return new Money(new BigDecimal(amountStr), currency);
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    /**
     * 2つのMoney を加算（通貨が異なる場合はException）
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot add money of different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    @Override
    public int compareTo(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot compare money of different currencies");
        }
        return this.amount.compareTo(other.amount);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", currency.code, amount.toPlainString());
    }
}

/**
 * 通貨enum
 */
public enum Currency {
    JPY("JPY"),
    USD("USD"),
    EUR("EUR");
    
    public final String code;
    
    Currency(String code) {
        this.code = code;
    }
}
```

---

## 4. OriginalDateTime 型 TypeHandler

### 4.1 概要

アプリケーション独自の `OriginalDateTime` クラスは、タイムゾーン情報を保持する日時を管理します。DB では `timestamp` (UTC) に格納し、Java では JST （日本標準時）で解釈。

### 4.2 メタデータ

| 項目 | 値 |
| :--- | :--- |
| **テーブルカラム型** | `timestamp` (UTC) |
| **Java型** | `OriginalDateTime` |
| **パッケージ** | `copel.sesproductpackage.core.unit` |
| **DB 出力形式** | `2026-08-05 12:34:56` (UTC timestamp) |
| **Java 内部表現** | `LocalDateTime + ZoneId` |

### 4.3 仕様

#### 4.3.1 getResult() - ResultSet → Java 型（SELECT時）

**処理フロー**:
1. `ResultSet.getTimestamp(columnName)` で UTC timestamp を取得
2. UTC から JST（Asia/Tokyo）に変換
3. `OriginalDateTime` オブジェクト化

**例**:
```
PostgreSQL: 2026-08-05 03:34:56 (UTC)
  ↓ (時間帯変換: UTC → JST +9時間)
Java: OriginalDateTime(LocalDateTime: 2026-08-05 12:34:56, ZoneId: Asia/Tokyo)
```

#### 4.3.2 setParameter() - Java 型 → PreparedStatement（INSERT/UPDATE時）

**処理フロー**:
1. `OriginalDateTime.toTimestamp()` で JST 日時を取得
2. UTC に変換
3. `PreparedStatement.setTimestamp()` にバインド

**例**:
```
Java: OriginalDateTime(LocalDateTime: 2026-08-05 12:34:56, ZoneId: Asia/Tokyo)
  ↓ (時間帯変換: JST → UTC -9時間)
PostgreSQL: 2026-08-05 03:34:56 (UTC)
```

### 4.4 実装コード例（概要）

```java
/**
 * OriginalDateTime型 ↔ PostgreSQL timestamp の双方向変換.
 * タイムゾーン対応（UTC ↔ JST）
 * 
 * @author Copel Co., Ltd.
 */
public class OriginalDateTimeTypeHandler extends BaseTypeHandler<OriginalDateTime> {
    
    private static final Logger log = 
        LoggerFactory.getLogger(OriginalDateTimeTypeHandler.class);
    
    private static final ZoneId SYSTEM_ZONE_ID = ZoneId.of("Asia/Tokyo");
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    
    /**
     * Java OriginalDateTime → PreparedStatement へバインド (INSERT/UPDATE)
     */
    @Override
    public void setNonNullParameter(
        PreparedStatement ps, int i, OriginalDateTime parameter, JdbcType jdbcType)
        throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.TIMESTAMP);
            return;
        }
        
        try {
            // OriginalDateTime（JST）→ java.sql.Timestamp（UTC）
            java.sql.Timestamp dbTimestamp = parameter.toTimestamp();
            ps.setTimestamp(i, dbTimestamp);
            
            if (log.isDebugEnabled()) {
                log.debug("[TypeHandler] OriginalDateTime.setParameter: dbTimestamp={}", 
                    dbTimestamp);
            }
        } catch (Exception e) {
            throw new SQLException(
                "Failed to convert OriginalDateTime to Timestamp", e);
        }
    }
    
    /**
     * ResultSet → Java OriginalDateTime (SELECT)
     */
    @Override
    public OriginalDateTime getNullableResult(ResultSet rs, String columnName)
        throws SQLException {
        java.sql.Timestamp dbTimestamp = rs.getTimestamp(columnName);
        if (dbTimestamp == null) {
            return null;
        }
        
        // DB timestamp（UTC）→ OriginalDateTime（JST）
        LocalDateTime utcDateTime = dbTimestamp.toLocalDateTime();
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC_ZONE_ID);
        ZonedDateTime jstZoned = utcZoned.withZoneSameInstant(SYSTEM_ZONE_ID);
        
        return new OriginalDateTime(jstZoned.toLocalDateTime(), SYSTEM_ZONE_ID);
    }
    
    /**
     * ResultSet → Java OriginalDateTime (by index)
     */
    @Override
    public OriginalDateTime getNullableResult(ResultSet rs, int columnIndex)
        throws SQLException {
        java.sql.Timestamp dbTimestamp = rs.getTimestamp(columnIndex);
        if (dbTimestamp == null) {
            return null;
        }
        
        LocalDateTime utcDateTime = dbTimestamp.toLocalDateTime();
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC_ZONE_ID);
        ZonedDateTime jstZoned = utcZoned.withZoneSameInstant(SYSTEM_ZONE_ID);
        
        return new OriginalDateTime(jstZoned.toLocalDateTime(), SYSTEM_ZONE_ID);
    }
    
    /**
     * CallableStatement → Java OriginalDateTime
     */
    @Override
    public OriginalDateTime getNullableResult(CallableStatement cs, int columnIndex)
        throws SQLException {
        java.sql.Timestamp dbTimestamp = cs.getTimestamp(columnIndex);
        if (dbTimestamp == null) {
            return null;
        }
        
        LocalDateTime utcDateTime = dbTimestamp.toLocalDateTime();
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC_ZONE_ID);
        ZonedDateTime jstZoned = utcZoned.withZoneSameInstant(SYSTEM_ZONE_ID);
        
        return new OriginalDateTime(jstZoned.toLocalDateTime(), SYSTEM_ZONE_ID);
    }
}
```

### 4.5 OriginalDateTime クラス実装（参考）

```java
/**
 * タイムゾーン対応の日時を表す値オブジェクト.
 * 内部的には Asia/Tokyo で管理。
 * 
 * @author Copel Co., Ltd.
 */
public class OriginalDateTime implements Comparable<OriginalDateTime>, Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final ZoneId SYSTEM_ZONE_ID = ZoneId.of("Asia/Tokyo");
    
    private LocalDateTime dateTime;
    private ZoneId zoneId;
    
    public OriginalDateTime(LocalDateTime dateTime) {
        this(dateTime, SYSTEM_ZONE_ID);
    }
    
    public OriginalDateTime(LocalDateTime dateTime, ZoneId zoneId) {
        this.dateTime = dateTime;
        this.zoneId = zoneId;
    }
    
    /**
     * 文字列からパース (ISO8601形式: 2026-08-05T12:34:56)
     */
    public OriginalDateTime(String isoDateTimeStr) {
        this(LocalDateTime.parse(isoDateTimeStr));
    }
    
    /**
     * 現在時刻で生成
     */
    public static OriginalDateTime now() {
        return new OriginalDateTime(LocalDateTime.now(SYSTEM_ZONE_ID));
    }
    
    /**
     * java.sql.Timestamp への変換 (UTC)
     */
    public java.sql.Timestamp toTimestamp() {
        ZonedDateTime jst = this.dateTime.atZone(SYSTEM_ZONE_ID);
        ZonedDateTime utc = jst.withZoneSameInstant(ZoneId.of("UTC"));
        return java.sql.Timestamp.valueOf(utc.toLocalDateTime());
    }
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public ZoneId getZoneId() {
        return zoneId;
    }
    
    @Override
    public int compareTo(OriginalDateTime other) {
        if (other == null) return 1;
        return this.dateTime.compareTo(other.dateTime);
    }
    
    @Override
    public String toString() {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
```

---

## 5. MyBatis 設定の登録

### 5.1 mybatis-config.xml での登録

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
  
  <!-- タイプハンドラーの登録 -->
  <typeHandlers>
    <!-- OriginalDateTime 型 -->
    <typeHandler 
      handler="copel.sesproductpackage.core.database.typehandler.OriginalDateTimeTypeHandler"
      javaType="copel.sesproductpackage.core.unit.OriginalDateTime" />
    
    <!-- Money 型 (Phase 2以降) -->
    <typeHandler 
      handler="copel.sesproductpackage.core.database.typehandler.MoneyTypeHandler"
      javaType="copel.sesproductpackage.core.unit.Money" />
    
    <!-- Vector 型 (Phase 2以降) -->
    <typeHandler 
      handler="copel.sesproductpackage.core.database.typehandler.VectorTypeHandler"
      javaType="copel.sesproductpackage.core.unit.Vector" />
  </typeHandlers>
  
  <!-- Mapper登録 -->
  <mappers>
    <mapper resource="mybatis/mapper/TenantMapper.xml" />
    <mapper resource="mybatis/mapper/GroupMapper.xml" />
    <mapper resource="mybatis/mapper/SenderMapper.xml" />
    <mapper resource="mybatis/mapper/UserMapper.xml" />
    <mapper resource="mybatis/mapper/NotificationMapper.xml" />
  </mappers>
  
</configuration>
```

### 5.2 Spring Boot での自動登録（推奨）

```java
@Configuration
public class MyBatisConfig {
    
    @Bean
    public OriginalDateTimeTypeHandler originalDateTimeTypeHandler() {
        return new OriginalDateTimeTypeHandler();
    }
    
    @Bean
    public MoneyTypeHandler moneyTypeHandler() {
        return new MoneyTypeHandler();
    }
    
    @Bean
    public VectorTypeHandler vectorTypeHandler() {
        return new VectorTypeHandler();
    }
}
```

---

## 6. Mapper XML での TypeHandler 指定

### 6.1 INSERT での指定

```xml
<insert id="insertEntity" parameterType="SES_AI_T_JOB">
    INSERT INTO SES_AI_T_JOB 
        (job_id, vector_data, unit_price, register_date, register_user)
    VALUES
        (#{jobId}, 
         #{vectorData, typeHandler=VectorTypeHandler}, 
         #{unitPrice, typeHandler=MoneyTypeHandler}, 
         #{registerDate, typeHandler=OriginalDateTimeTypeHandler}, 
         #{registerUser})
</insert>
```

### 6.2 SELECT での ResultMap

```xml
<resultMap id="JobMap" type="SES_AI_T_JOB">
    <id column="job_id" property="jobId" />
    <result column="vector_data" property="vectorData" 
            typeHandler="VectorTypeHandler" />
    <result column="unit_price" property="unitPrice" 
            typeHandler="MoneyTypeHandler" />
    <result column="register_date" property="registerDate" 
            typeHandler="OriginalDateTimeTypeHandler" />
    <result column="register_user" property="registerUser" />
</resultMap>
```

---

## 7. テスト戦略

### 7.1 UT（単体テスト）

各 TypeHandler のテストコード：

**VectorTypeHandlerTest**:
```java
@Test
void testGetResult_ValidVector() {
    // "[0.1, 0.2, 0.3]" → Vector(0.1, 0.2, 0.3)
}

@Test
void testSetParameter_VectorToString() {
    // Vector(0.1, 0.2, 0.3) → "[0.1, 0.2, 0.3]"
}

@Test
void testGetResult_NullVector() {
    // null → null
}
```

**MoneyTypeHandlerTest**:
```java
@Test
void testGetResult_ValidMoney() {
    // BigDecimal(100000.00) → Money(100000.00, JPY)
}

@Test
void testSetParameter_MoneyToDecimal() {
    // Money(100000.00, JPY) → BigDecimal(100000.00)
}
```

**OriginalDateTimeTypeHandlerTest**:
```java
@Test
void testGetResult_UTCtoJST() {
    // UTC: 2026-08-05 03:34:56 → JST: 2026-08-05 12:34:56
}

@Test
void testSetParameter_JSTtoUTC() {
    // JST: 2026-08-05 12:34:56 → UTC: 2026-08-05 03:34:56
}
```

### 7.2 IT1（統合テスト）

各 Entity の SELECT/INSERT での型変換検証：

```java
@Test
void testInsertAndSelect_VectorType() {
    // 1. Vector をINSERTしてDELECT
    // 2. SELECT結果の Vector が元のデータと等しいか確認
}

@Test
void testInsertAndSelect_MoneyType() {
    // 1. Money をINSERTしてSELECT
    // 2. SELECT結果の Money が元のデータと等しいか確認（金額＋通貨）
}

@Test
void testInsertAndSelect_OriginalDateTimeType() {
    // 1. OriginalDateTime をINSERTしてSELECT
    // 2. UTC ↔ JST の変換が正しいか確認
}
```

---

## 8. トラブルシューティング

### 8.1 PostgreSQL vector 型が認識されない

**症状**: `ERROR: type "vector" does not exist`

**原因**: pgvector 拡張がインストールされていない

**解決方法**:
```sql
-- PostgreSQL 管理者が実行
CREATE EXTENSION IF NOT EXISTS vector;
```

### 8.2 タイムゾーン変換がおかしい

**症状**: SELECT で取得した日時が ±9時間ずれている

**原因**: PostgreSQL サーバーまたは JDBC ドライバーのタイムゾーン設定が UTC でない

**解決方法**: JDBC URL に `serverTimezone=UTC` を追加
```
jdbc:postgresql://localhost:5432/sesdb?serverTimezone=UTC&useSSL=false
```

### 8.3 Money の精度が失われる

**症状**: 100000.00 が 100000 に見える

**原因**: `BigDecimal.setScale()` で小数点以下を丸めている

**解決方法**: Mapper XML で `typeHandler` を指定し、TypeHandler に処理させる

---

## 9. 参考資料

- [MyBatis TypeHandler 公式ドキュメント](https://mybatis.org/mybatis-3/configuration.html#typeHandlers)
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- PostgreSQL 公式: [NUMERIC 型](https://www.postgresql.org/docs/current/datatype-numeric.html)
- Java 公式: [java.time パッケージ](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/package-summary.html)

---

**承認待機中**  
**最終更新**: 2026-08-05
