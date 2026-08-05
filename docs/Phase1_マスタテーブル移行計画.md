# Phase 1 マスタテーブル移行計画 - 実装仕様書

**作成日**: 2026-08-05  
**対象**: 5つのマスタテーブル Entity の MyBatis 化  
**ステータス**: 実装仕様確定

---

## 1. 概要

本ドキュメントは、SesAiAssistantCore のマスタテーブル 5 つを MyBatis ORM に移行する際の実装仕様を定義します。各 Entity の CRUD メソッド定義、対応する MyBatis Mapper XML クエリ仕様、テーブル構造とカラムマッピングを一覧化します。

---

## 2. 対象マスタテーブル一覧

| # | Entity クラス | テーブル名 | 説明 | 件数目安 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `SES_AI_M_TENANT` | `SES_AI_M_TENANT` | テナント情報マスタ | 1～10 |
| 2 | `SES_AI_M_GROUP` | `SES_AI_M_GROUP` | 送信元グループマスタ | 10～50 |
| 3 | `SES_AI_M_SENDER` | `SES_AI_M_SENDER` | 送信者マスタ | 50～200 |
| 4 | `SES_AI_WEBAPP_M_USER` | `SES_AI_WEBAPP_M_USER` | Webアプリユーザーマスタ | 100～1000 |
| 5 | `SES_AI_WEBAPP_M_NOTIFICATION` | `SES_AI_WEBAPP_M_NOTIFICATION` | プッシュ通知デバイス登録マスタ | 200～5000 |

---

## 3. Entity ごとの CRUD メソッド定義

### 3.1 SES_AI_M_TENANT（テナント情報マスタ）

#### テーブル構造
| カラム名 | 型 | NULL許可 | 説明 | Java型 |
| :--- | :--- | :--- | :--- | :--- |
| `tenant_id` | `VARCHAR(36)` | NO | PK：テナントID | `String` |
| `tenant_name` | `VARCHAR(100)` | YES | テナント名 | `String` |
| `tenant_status_cd` | `CHAR(2)` | YES | ステータス区分 | `String` |
| `register_date` | `timestamp` | NO | 登録日時 | `OriginalDateTime` |
| `register_user` | `VARCHAR(50)` | NO | 登録ユーザー | `String` |

#### Entity クラス
```java
public class SES_AI_M_TENANT extends EntityBase {
    private String tenantId;     // PK
    private String tenantName;
    private String tenantStatusCd;
    // registerDate, registerUser は継承
}
```

#### CRUD メソッドシグネチャ
```java
// INSERT
public int insert(Connection connection) throws SQLException;

// SELECT-by-PK
public void selectByPk(Connection connection) throws SQLException;

// UPDATE-by-PK
public boolean updateByPk(Connection connection) throws SQLException;

// DELETE-by-PK
public boolean deleteByPk(Connection connection) throws SQLException;
```

#### 対応する MyBatis Mapper SQL

**INSERT**:
```xml
<insert id="insertTenant" parameterType="SES_AI_M_TENANT">
    INSERT INTO SES_AI_M_TENANT 
        (tenant_id, tenant_name, tenant_status_cd, register_date, register_user)
    VALUES
        (#{tenantId}, #{tenantName}, #{tenantStatusCd}, 
         #{registerDate, typeHandler=OriginalDateTimeTypeHandler}, 
         #{registerUser})
</insert>
```

**SELECT-by-PK**:
```xml
<select id="selectTenantByPk" parameterType="string" resultMap="TenantMap">
    SELECT 
        tenant_id, tenant_name, tenant_status_cd, register_date, register_user
    FROM SES_AI_M_TENANT
    WHERE tenant_id = #{tenantId}
</select>

<resultMap id="TenantMap" type="SES_AI_M_TENANT">
    <id column="tenant_id" property="tenantId" />
    <result column="tenant_name" property="tenantName" />
    <result column="tenant_status_cd" property="tenantStatusCd" />
    <result column="register_date" property="registerDate" 
            typeHandler="OriginalDateTimeTypeHandler" />
    <result column="register_user" property="registerUser" />
</resultMap>
```

**UPDATE-by-PK**:
```xml
<update id="updateTenantByPk" parameterType="SES_AI_M_TENANT">
    UPDATE SES_AI_M_TENANT
    SET 
        tenant_name = #{tenantName},
        tenant_status_cd = #{tenantStatusCd}
    WHERE tenant_id = #{tenantId}
</update>
```

**DELETE-by-PK**:
```xml
<delete id="deleteTenantByPk" parameterType="string">
    DELETE FROM SES_AI_M_TENANT
    WHERE tenant_id = #{tenantId}
</delete>
```

---

### 3.2 SES_AI_M_GROUP（送信元グループマスタ）

#### テーブル構造
| カラム名 | 型 | NULL許可 | 説明 | Java型 |
| :--- | :--- | :--- | :--- | :--- |
| `from_group` | `VARCHAR(100)` | NO | PK：送信元グループID | `String` |
| `group_name` | `VARCHAR(100)` | YES | グループ名 | `String` |
| `tenant_id` | `VARCHAR(36)` | NO | テナントID（FK） | `String` |
| `register_date` | `timestamp` | NO | 登録日時 | `OriginalDateTime` |
| `register_user` | `VARCHAR(50)` | NO | 登録ユーザー | `String` |

#### Entity クラス
```java
public class SES_AI_M_GROUP extends EntityBase {
    private String fromGroup;     // PK
    private String groupName;
    // tenantId, registerDate, registerUser は継承
}
```

#### CRUD メソッドシグネチャ
```java
public int insert(Connection connection) throws SQLException;
public void selectByPk(Connection connection) throws SQLException;
public boolean updateByPk(Connection connection) throws SQLException;
public boolean deleteByPk(Connection connection) throws SQLException;
```

#### 対応する MyBatis Mapper SQL

**INSERT**:
```xml
<insert id="insertGroup" parameterType="SES_AI_M_GROUP">
    INSERT INTO SES_AI_M_GROUP 
        (from_group, group_name, tenant_id, register_date, register_user)
    VALUES
        (#{fromGroup}, #{groupName}, #{tenantId}, 
         #{registerDate, typeHandler=OriginalDateTimeTypeHandler}, 
         #{registerUser})
</insert>
```

**SELECT-by-PK** (tenantId フィルタ付き):
```xml
<select id="selectGroupByPk" parameterType="map" resultMap="GroupMap">
    SELECT 
        from_group, group_name, tenant_id, register_date, register_user
    FROM SES_AI_M_GROUP
    WHERE from_group = #{fromGroup}
      AND tenant_id = #{tenantId}
</select>

<resultMap id="GroupMap" type="SES_AI_M_GROUP">
    <id column="from_group" property="fromGroup" />
    <result column="group_name" property="groupName" />
    <result column="tenant_id" property="tenantId" />
    <result column="register_date" property="registerDate" 
            typeHandler="OriginalDateTimeTypeHandler" />
    <result column="register_user" property="registerUser" />
</resultMap>
```

**UPDATE-by-PK** (tenantId フィルタ付き):
```xml
<update id="updateGroupByPk" parameterType="SES_AI_M_GROUP">
    UPDATE SES_AI_M_GROUP
    SET 
        group_name = #{groupName}
    WHERE from_group = #{fromGroup}
      AND tenant_id = #{tenantId}
</update>
```

**DELETE-by-PK** (tenantId フィルタ付き):
```xml
<delete id="deleteGroupByPk" parameterType="map">
    DELETE FROM SES_AI_M_GROUP
    WHERE from_group = #{fromGroup}
      AND tenant_id = #{tenantId}
</delete>
```

---

### 3.3 SES_AI_M_SENDER（送信者マスタ）

#### テーブル構造
| カラム名 | 型 | NULL許可 | 説明 | Java型 |
| :--- | :--- | :--- | :--- | :--- |
| `from_id` | `VARCHAR(50)` | NO | PK：送信者ID | `String` |
| `from_name` | `VARCHAR(50)` | YES | 送信者名 | `String` |
| `tenant_id` | `VARCHAR(36)` | NO | テナントID（FK） | `String` |
| `register_date` | `timestamp` | NO | 登録日時 | `OriginalDateTime` |
| `register_user` | `VARCHAR(50)` | NO | 登録ユーザー | `String` |

#### Entity クラス
```java
public class SES_AI_M_SENDER extends EntityBase {
    private String fromId;        // PK
    private String fromName;
    // tenantId, registerDate, registerUser は継承
}
```

#### CRUD メソッドシグネチャ
```java
public int insert(Connection connection) throws SQLException;
public void selectByPk(Connection connection) throws SQLException;
public boolean updateByPk(Connection connection) throws SQLException;
public boolean deleteByPk(Connection connection) throws SQLException;
public boolean isExist(Connection connection) throws SQLException;  // カスタムメソッド
```

#### 対応する MyBatis Mapper SQL

**INSERT**:
```xml
<insert id="insertSender" parameterType="SES_AI_M_SENDER">
    INSERT INTO SES_AI_M_SENDER 
        (from_id, from_name, tenant_id, register_date, register_user)
    VALUES
        (#{fromId}, #{fromName}, #{tenantId}, 
         #{registerDate, typeHandler=OriginalDateTimeTypeHandler}, 
         #{registerUser})
</insert>
```

**SELECT-by-PK** (tenantId フィルタ付き):
```xml
<select id="selectSenderByPk" parameterType="map" resultMap="SenderMap">
    SELECT 
        from_id, from_name, tenant_id, register_date, register_user
    FROM SES_AI_M_SENDER
    WHERE from_id = #{fromId}
      AND tenant_id = #{tenantId}
</select>

<resultMap id="SenderMap" type="SES_AI_M_SENDER">
    <id column="from_id" property="fromId" />
    <result column="from_name" property="fromName" />
    <result column="tenant_id" property="tenantId" />
    <result column="register_date" property="registerDate" 
            typeHandler="OriginalDateTimeTypeHandler" />
    <result column="register_user" property="registerUser" />
</resultMap>
```

**UPDATE-by-PK** (tenantId フィルタ付き):
```xml
<update id="updateSenderByPk" parameterType="SES_AI_M_SENDER">
    UPDATE SES_AI_M_SENDER
    SET 
        from_name = #{fromName}
    WHERE from_id = #{fromId}
      AND tenant_id = #{tenantId}
</update>
```

**DELETE-by-PK** (tenantId フィルタ付き):
```xml
<delete id="deleteSenderByPk" parameterType="map">
    DELETE FROM SES_AI_M_SENDER
    WHERE from_id = #{fromId}
      AND tenant_id = #{tenantId}
</delete>
```

**EXISTS** (カスタムメソッド):
```xml
<select id="checkSenderExists" parameterType="map" resultType="boolean">
    SELECT EXISTS (
        SELECT 1 FROM SES_AI_M_SENDER
        WHERE from_id = #{fromId}
          AND tenant_id = #{tenantId}
    )
</select>
```

---

### 3.4 SES_AI_WEBAPP_M_USER（Webアプリユーザーマスタ）

#### テーブル構造
| カラム名 | 型 | NULL許可 | 説明 | Java型 |
| :--- | :--- | :--- | :--- | :--- |
| `user_id` | `VARCHAR(50)` | NO | PK：ユーザーID | `String` |
| `user_name` | `VARCHAR(100)` | YES | ユーザー名 | `String` |
| `role_cd` | `VARCHAR(10)` | YES | ロール区分コード | `Role` (Enum) |
| `plan_cd` | `VARCHAR(10)` | YES | プラン区分コード | `Plan` (Enum) |
| `tenant_id` | `VARCHAR(36)` | NO | テナントID（FK） | `String` |
| `register_date` | `timestamp` | NO | 登録日時 | `OriginalDateTime` |
| `register_user` | `VARCHAR(50)` | NO | 登録ユーザー | `String` |

#### Entity クラス
```java
public class SES_AI_WEBAPP_M_USER extends EntityBase {
    private String userId;        // PK
    private String userName;
    private Role role;            // Enum
    private Plan plan;            // Enum
    // tenantId, registerDate, registerUser は継承
}
```

#### CRUD メソッドシグネチャ
```java
public int insert(Connection connection) throws SQLException;
public void selectByPk(Connection connection) throws SQLException;
public boolean updateByPk(Connection connection) throws SQLException;
public boolean deleteByPk(Connection connection) throws SQLException;
public void selectByPkWithoutTenantId(Connection connection) throws SQLException;
public boolean updateByPkWithoutTenantId(Connection connection) throws SQLException;
public boolean deleteByPkWithoutTenantIdFilter(Connection connection) throws SQLException;
```

#### 対応する MyBatis Mapper SQL

**INSERT**:
```xml
<insert id="insertUser" parameterType="SES_AI_WEBAPP_M_USER">
    INSERT INTO SES_AI_WEBAPP_M_USER 
        (user_id, user_name, role_cd, plan_cd, tenant_id, register_date, register_user)
    VALUES
        (#{userId}, #{userName}, #{role}, #{plan}, #{tenantId}, 
         #{registerDate, typeHandler=OriginalDateTimeTypeHandler}, 
         #{registerUser})
</insert>
```

**SELECT-by-PK** (tenantId フィルタ付き):
```xml
<select id="selectUserByPk" parameterType="map" resultMap="UserMap">
    SELECT 
        user_id, user_name, role_cd, plan_cd, tenant_id, register_date, register_user
    FROM SES_AI_WEBAPP_M_USER
    WHERE user_id = #{userId}
      AND tenant_id = #{tenantId}
</select>

<resultMap id="UserMap" type="SES_AI_WEBAPP_M_USER">
    <id column="user_id" property="userId" />
    <result column="user_name" property="userName" />
    <result column="role_cd" property="role" 
            typeHandler="org.apache.ibatis.type.EnumTypeHandler" />
    <result column="plan_cd" property="plan" 
            typeHandler="org.apache.ibatis.type.EnumTypeHandler" />
    <result column="tenant_id" property="tenantId" />
    <result column="register_date" property="registerDate" 
            typeHandler="OriginalDateTimeTypeHandler" />
    <result column="register_user" property="registerUser" />
</resultMap>
```

**SELECT-by-PK** (tenantId フィルタ**なし** - システム管理者用):
```xml
<select id="selectUserByPkWithoutTenantId" parameterType="string" resultMap="UserMap">
    SELECT 
        user_id, user_name, role_cd, plan_cd, tenant_id, register_date, register_user
    FROM SES_AI_WEBAPP_M_USER
    WHERE user_id = #{userId}
</select>
```

**UPDATE-by-PK** (tenantId フィルタ付き):
```xml
<update id="updateUserByPk" parameterType="SES_AI_WEBAPP_M_USER">
    UPDATE SES_AI_WEBAPP_M_USER
    SET 
        user_id = #{userId},
        user_name = #{userName},
        role_cd = #{role},
        plan_cd = #{plan},
        register_date = #{registerDate, typeHandler=OriginalDateTimeTypeHandler},
        register_user = #{registerUser}
    WHERE user_id = #{userId}
      AND tenant_id = #{tenantId}
</update>
```

**UPDATE-by-PK** (tenantId フィルタ**なし** - システム管理者用):
```xml
<update id="updateUserByPkWithoutTenantId" parameterType="SES_AI_WEBAPP_M_USER">
    UPDATE SES_AI_WEBAPP_M_USER
    SET 
        user_id = #{userId},
        user_name = #{userName},
        role_cd = #{role},
        plan_cd = #{plan},
        register_date = #{registerDate, typeHandler=OriginalDateTimeTypeHandler},
        register_user = #{registerUser},
        tenant_id = #{tenantId}
    WHERE user_id = #{userId}
</update>
```

**DELETE-by-PK** (tenantId フィルタ付き):
```xml
<delete id="deleteUserByPk" parameterType="map">
    DELETE FROM SES_AI_WEBAPP_M_USER
    WHERE user_id = #{userId}
      AND tenant_id = #{tenantId}
</delete>
```

**DELETE-by-PK** (tenantId フィルタ**なし** - システム管理者用):
```xml
<delete id="deleteUserByPkWithoutTenantId" parameterType="string">
    DELETE FROM SES_AI_WEBAPP_M_USER
    WHERE user_id = #{userId}
</delete>
```

---

### 3.5 SES_AI_WEBAPP_M_NOTIFICATION（プッシュ通知デバイス登録マスタ）

#### テーブル構造
| カラム名 | 型 | NULL許可 | 説明 | Java型 |
| :--- | :--- | :--- | :--- | :--- |
| `notification_id` | `VARCHAR(100)` | NO | PK：通知デバイスID | `String` |
| `user_id` | `VARCHAR(50)` | NO | ユーザーID（FK） | `String` |
| `device_type` | `VARCHAR(50)` | NO | デバイスタイプ（browser等） | `String` |
| `device_name` | `VARCHAR(100)` | YES | デバイス名 | `String` |
| `push_notification_endpoint` | `VARCHAR(500)` | NO | Push API エンドポイント | `String` |
| `p256dh` | `VARCHAR(500)` | NO | ECDH公開鍵（base64） | `String` |
| `auth` | `VARCHAR(500)` | NO | HMAC トークン（base64） | `String` |
| `enabled` | `BOOLEAN` | YES | 有効フラグ | `Boolean` |
| `notify_all_match` | `BOOLEAN` | YES | 全件通知フラグ | `Boolean` |
| `tenant_id` | `VARCHAR(36)` | NO | テナントID（FK） | `String` |
| `register_date` | `timestamp` | NO | 登録日時 | `OriginalDateTime` |
| `register_user` | `VARCHAR(50)` | NO | 登録ユーザー | `String` |

#### Entity クラス
```java
public class SES_AI_WEBAPP_M_NOTIFICATION extends EntityBase {
    private String notificationId;           // PK
    private String userId;
    private String deviceType;
    private String deviceName;
    private String pushNotificationEndpoint;
    private String p256dh;
    private String auth;
    private Boolean enabled;
    private Boolean notifyAllMatch;
    // tenantId, registerDate, registerUser は継承
}
```

#### CRUD メソッドシグネチャ
```java
public int insert(Connection connection) throws SQLException;
public void selectByPk(Connection connection) throws SQLException;
public boolean updateByPk(Connection connection) throws SQLException;
public boolean deleteByPk(Connection connection) throws SQLException;
```

#### 対応する MyBatis Mapper SQL

**INSERT**:
```xml
<insert id="insertNotification" parameterType="SES_AI_WEBAPP_M_NOTIFICATION">
    INSERT INTO SES_AI_WEBAPP_M_NOTIFICATION 
        (notification_id, user_id, device_type, device_name, 
         push_notification_endpoint, p256dh, auth, enabled, notify_all_match,
         tenant_id, register_date, register_user)
    VALUES
        (#{notificationId}, #{userId}, #{deviceType}, #{deviceName}, 
         #{pushNotificationEndpoint}, #{p256dh}, #{auth}, #{enabled}, #{notifyAllMatch},
         #{tenantId}, 
         #{registerDate, typeHandler=OriginalDateTimeTypeHandler}, 
         #{registerUser})
</insert>
```

**SELECT-by-PK** (tenantId フィルタ付き):
```xml
<select id="selectNotificationByPk" parameterType="map" resultMap="NotificationMap">
    SELECT 
        notification_id, user_id, device_type, device_name, 
        push_notification_endpoint, p256dh, auth, enabled, notify_all_match,
        tenant_id, register_date, register_user
    FROM SES_AI_WEBAPP_M_NOTIFICATION
    WHERE notification_id = #{notificationId}
      AND tenant_id = #{tenantId}
</select>

<resultMap id="NotificationMap" type="SES_AI_WEBAPP_M_NOTIFICATION">
    <id column="notification_id" property="notificationId" />
    <result column="user_id" property="userId" />
    <result column="device_type" property="deviceType" />
    <result column="device_name" property="deviceName" />
    <result column="push_notification_endpoint" property="pushNotificationEndpoint" />
    <result column="p256dh" property="p256dh" />
    <result column="auth" property="auth" />
    <result column="enabled" property="enabled" />
    <result column="notify_all_match" property="notifyAllMatch" />
    <result column="tenant_id" property="tenantId" />
    <result column="register_date" property="registerDate" 
            typeHandler="OriginalDateTimeTypeHandler" />
    <result column="register_user" property="registerUser" />
</resultMap>
```

**UPDATE-by-PK** (tenantId フィルタ付き):
```xml
<update id="updateNotificationByPk" parameterType="SES_AI_WEBAPP_M_NOTIFICATION">
    UPDATE SES_AI_WEBAPP_M_NOTIFICATION
    SET 
        notification_id = #{notificationId},
        user_id = #{userId},
        device_type = #{deviceType},
        device_name = #{deviceName},
        push_notification_endpoint = #{pushNotificationEndpoint},
        p256dh = #{p256dh},
        auth = #{auth},
        enabled = #{enabled},
        notify_all_match = #{notifyAllMatch},
        register_date = #{registerDate, typeHandler=OriginalDateTimeTypeHandler},
        register_user = #{registerUser}
    WHERE notification_id = #{notificationId}
      AND tenant_id = #{tenantId}
</update>
```

**DELETE-by-PK** (tenantId フィルタ付き):
```xml
<delete id="deleteNotificationByPk" parameterType="map">
    DELETE FROM SES_AI_WEBAPP_M_NOTIFICATION
    WHERE notification_id = #{notificationId}
      AND tenant_id = #{tenantId}
</delete>
```

---

## 4. テーブル構造の統一

### 4.1 共通カラム（すべてのテーブルに存在）

| カラム名 | 型 |説明 | MyBatis 対応 |
| :--- | :--- | :--- | :--- |
| `register_date` | `timestamp` | 登録日時 | `OriginalDateTimeTypeHandler` |
| `register_user` | `VARCHAR(50)` | 登録ユーザー | 標準型（String） |
| `tenant_id` | `VARCHAR(36)` | テナントID | 標準型（String） |

### 4.2 NULL値処理ポリシー

| データ型 | NULL許可 | MyBatis 処理 | 例 |
| :--- | :--- | :--- | :--- |
| `String` | YES | `null` のまま | `from_name` |
| `Boolean` | YES | `false` に正規化 | `enabled = false` |
| `OriginalDateTime` | NO | 必須（登録時自動生成） | `NOW()` |
| `Enum` | YES | `null` 許容（未選択状態） | `role_cd = null` |

---

## 5. NULL値処理とデータ型変換

### 5.1 OriginalDateTime 型

**入力側（INSERT/UPDATE）**:
```xml
#{registerDate, typeHandler=OriginalDateTimeTypeHandler}
```

**出力側（SELECT）**:
```xml
<result column="register_date" property="registerDate" 
        typeHandler="OriginalDateTimeTypeHandler" />
```

### 5.2 Money 型（Phase 2以降）

**入力側**:
```xml
#{unitPrice, typeHandler=MoneyTypeHandler}
```

**出力側**:
```xml
<result column="unit_price" property="unitPrice" 
        typeHandler="MoneyTypeHandler" />
```

### 5.3 Enum 型（Role、Plan）

**入力側**:
```xml
#{role, typeHandler=org.apache.ibatis.type.EnumTypeHandler}
```

**出力側**:
```xml
<result column="role_cd" property="role" 
        typeHandler="org.apache.ibatis.type.EnumTypeHandler" />
```

---

## 6. カラムマッピング一覧

### SES_AI_M_TENANT

| 物理名（DB） | 論理名 | Java型 | Entity プロパティ | TypeHandler |
| :--- | :--- | :--- | :--- | :--- |
| `tenant_id` | テナントID | `String` | `tenantId` | なし |
| `tenant_name` | テナント名 | `String` | `tenantName` | なし |
| `tenant_status_cd` | ステータス区分 | `String` | `tenantStatusCd` | なし |
| `register_date` | 登録日時 | `OriginalDateTime` | `registerDate` | `OriginalDateTimeTypeHandler` |
| `register_user` | 登録ユーザー | `String` | `registerUser` | なし |

### SES_AI_M_GROUP

| 物理名（DB） | 論理名 | Java型 | Entity プロパティ | TypeHandler |
| :--- | :--- | :--- | :--- | :--- |
| `from_group` | 送信元グループID | `String` | `fromGroup` | なし |
| `group_name` | グループ名 | `String` | `groupName` | なし |
| `tenant_id` | テナントID | `String` | `tenantId` | なし |
| `register_date` | 登録日時 | `OriginalDateTime` | `registerDate` | `OriginalDateTimeTypeHandler` |
| `register_user` | 登録ユーザー | `String` | `registerUser` | なし |

### SES_AI_M_SENDER

| 物理名（DB） | 論理名 | Java型 | Entity プロパティ | TypeHandler |
| :--- | :--- | :--- | :--- | :--- |
| `from_id` | 送信者ID | `String` | `fromId` | なし |
| `from_name` | 送信者名 | `String` | `fromName` | なし |
| `tenant_id` | テナントID | `String` | `tenantId` | なし |
| `register_date` | 登録日時 | `OriginalDateTime` | `registerDate` | `OriginalDateTimeTypeHandler` |
| `register_user` | 登録ユーザー | `String` | `registerUser` | なし |

### SES_AI_WEBAPP_M_USER

| 物理名（DB） | 論理名 | Java型 | Entity プロパティ | TypeHandler |
| :--- | :--- | :--- | :--- | :--- |
| `user_id` | ユーザーID | `String` | `userId` | なし |
| `user_name` | ユーザー名 | `String` | `userName` | なし |
| `role_cd` | ロール区分 | `Role` | `role` | `EnumTypeHandler` |
| `plan_cd` | プラン区分 | `Plan` | `plan` | `EnumTypeHandler` |
| `tenant_id` | テナントID | `String` | `tenantId` | なし |
| `register_date` | 登録日時 | `OriginalDateTime` | `registerDate` | `OriginalDateTimeTypeHandler` |
| `register_user` | 登録ユーザー | `String` | `registerUser` | なし |

### SES_AI_WEBAPP_M_NOTIFICATION

| 物理名（DB） | 論理名 | Java型 | Entity プロパティ | TypeHandler |
| :--- | :--- | :--- | :--- | :--- |
| `notification_id` | 通知デバイスID | `String` | `notificationId` | なし |
| `user_id` | ユーザーID | `String` | `userId` | なし |
| `device_type` | デバイスタイプ | `String` | `deviceType` | なし |
| `device_name` | デバイス名 | `String` | `deviceName` | なし |
| `push_notification_endpoint` | Push APIエンドポイント | `String` | `pushNotificationEndpoint` | なし |
| `p256dh` | ECDH公開鍵 | `String` | `p256dh` | なし |
| `auth` | HMACトークン | `String` | `auth` | なし |
| `enabled` | 有効フラグ | `Boolean` | `enabled` | なし |
| `notify_all_match` | 全件通知フラグ | `Boolean` | `notifyAllMatch` | なし |
| `tenant_id` | テナントID | `String` | `tenantId` | なし |
| `register_date` | 登録日時 | `OriginalDateTime` | `registerDate` | `OriginalDateTimeTypeHandler` |
| `register_user` | 登録ユーザー | `String` | `registerUser` | なし |

---

## 7. 実装チェックリスト

### 7.1 DAO 層の実装

- [ ] `EntityBaseDAO.java` の作成
  - [ ] `insert(Entity, SqlSession)` メソッド
  - [ ] `selectByPk(Entity, SqlSession)` メソッド
  - [ ] `selectByPkWithoutTenantFilter(Entity, SqlSession)` メソッド
  - [ ] `update(Entity, SqlSession)` メソッド
  - [ ] `delete(Entity, SqlSession)` メソッド

### 7.2 Mapper XML の作成

- [ ] `TenantMapper.xml`（SES_AI_M_TENANT）
- [ ] `GroupMapper.xml`（SES_AI_M_GROUP）
- [ ] `SenderMapper.xml`（SES_AI_M_SENDER）
- [ ] `UserMapper.xml`（SES_AI_WEBAPP_M_USER）
- [ ] `NotificationMapper.xml`（SES_AI_WEBAPP_M_NOTIFICATION）

### 7.3 TypeHandler の実装

- [ ] `OriginalDateTimeTypeHandler.java`
- [ ] `MoneyTypeHandler.java`（Phase 2以降）
- [ ] `VectorTypeHandler.java`（Phase 2以降）

### 7.4 Entity の修正

- [ ] `SES_AI_M_TENANT` - insert/selectByPk/updateByPk/deleteByPk を DAO 委譲に変更
- [ ] `SES_AI_M_GROUP` - 同上
- [ ] `SES_AI_M_SENDER` - 同上
- [ ] `SES_AI_WEBAPP_M_USER` - 同上（selectByPkWithoutTenantId/updateByPkWithoutTenantId/deleteByPkWithoutTenantIdFilter も）
- [ ] `SES_AI_WEBAPP_M_NOTIFICATION` - 同上

### 7.5 UT 修正

- [ ] 各 Entity のテストコード修正
- [ ] DAO テストコード新規作成
- [ ] TypeHandler テストコード新規作成

---

## 8. 補足説明

### 8.1 テナント ID フィルタリングの自動化

Entity が tenantId を持つ場合、SELECT/UPDATE/DELETE に自動的に `AND tenant_id = #{tenantId}` 条件が付加されます。

**例（SES_AI_M_SENDER）**:
```java
sender.selectByPk(connection);  // tenantId フィルタ付きで実行
```

**実行する SQL**:
```sql
SELECT * FROM SES_AI_M_SENDER
WHERE from_id = ?
  AND tenant_id = ?  -- 自動追加
```

### 8.2 システム管理者用メソッド（tenantId フィルタなし）

`SES_AI_WEBAPP_M_USER` には以下のメソッドがあります：
- `selectByPkWithoutTenantId()` - テナント関係なくユーザーを取得
- `updateByPkWithoutTenantId()` - テナント関係なくユーザーを更新（tenantId も更新対象）
- `deleteByPkWithoutTenantIdFilter()` - テナント関係なくユーザーを削除

これらは **システム管理者のみが使用** し、通常のテナント利用者からのアクセスは許可しません。

---

**承認待機中**  
**最終更新**: 2026-08-05
