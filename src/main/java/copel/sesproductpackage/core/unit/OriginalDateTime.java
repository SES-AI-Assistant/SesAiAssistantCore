package copel.sesproductpackage.core.unit;

import copel.sesproductpackage.core.util.ApplicationClock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 【フレームワーク部品】 日時の情報を持つクラス
 *
 * @author Copel Co., Ltd.
 */
public class OriginalDateTime implements Comparable<OriginalDateTime> {
  // ================================
  // フィールド定義
  // ================================
  /** 日時 */
  protected LocalDateTime dateTime;

  // ================================
  // コンストラクタ
  // ================================
  /**
   * 現在日時でこのオブジェクトを初期化する.
   *
   * <p>現在時刻は {@link ApplicationClock} の時計から取得する。テストで時刻を固定できるようにするため。
   */
  public OriginalDateTime() {
    this.dateTime = LocalDateTime.now(ApplicationClock.INSTANCE.clock());
  }

  /**
   * 文字列の日付情報を解析しこのオブジェクトを初期化する
   *
   * @param date 文字列の日付情報
   */
  public OriginalDateTime(String date) {
    if (date == null) {
      this.dateTime = null;
    } else {
      // パースを試みるパターンのリスト
      String[] patterns = {
        "yyyy-MM-dd HH:mm:ss.SSSSSS",
        "yyyy-MM-dd HH:mm:ss.SSSSS",
        "yyyy-MM-dd HH:mm:ss.SSSS",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss.SS",
        "yyyy-MM-dd HH:mm:ss.S",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd",
        "yyyy-MM",
        "yyyy/MM/dd HH:mm:ss.SSSSSS",
        "yyyy/MM/dd HH:mm:ss.SSSSS",
        "yyyy/MM/dd HH:mm:ss.SSSS",
        "yyyy/MM/dd HH:mm:ss.SSS",
        "yyyy/MM/dd HH:mm:ss.SS",
        "yyyy/MM/dd HH:mm:ss.S",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy/MM/dd",
        "yyyy/MM"
      };
      // パースを試みる
      for (String pattern : patterns) {
        try {
          if ("yyyy-MM-dd".equals(pattern) || "yyyy/MM/dd".equals(pattern)) {
            this.dateTime =
                LocalDateTime.parse(
                    date + " 00:00:00", DateTimeFormatter.ofPattern(pattern + " HH:mm:ss"));
          } else if ("yyyy-MM".equals(pattern) || "yyyy/MM".equals(pattern)) {
            // 月単位の入力の場合、01日として解析
            String dateWithDay =
                date + ("-".equals(String.valueOf(pattern.charAt(4))) ? "-01" : "/01");
            String fullPattern =
                pattern + ("-".equals(String.valueOf(pattern.charAt(4))) ? "-dd" : "/dd");
            this.dateTime =
                LocalDateTime.parse(
                    dateWithDay + " 00:00:00",
                    DateTimeFormatter.ofPattern(fullPattern + " HH:mm:ss"));
          } else {
            this.dateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(pattern));
          }
          break;
        } catch (DateTimeParseException ignored) {
          // パースに失敗した場合、次のパターンを試す
        }
      }
    }
  }

  /** 年月日時分秒でこのオブジェクトを初期化する */
  public OriginalDateTime(int year, int month, int date, int hour, int minutes, int second) {
    this.dateTime = LocalDateTime.of(year, month, date, hour, minutes, second);
  }

  /**
   * java.sql.Dateをjava.time.LocalDateTimeに変換
   *
   * @param date 変換元の日付（null の場合は空のオブジェクト）
   */
  public OriginalDateTime(java.sql.Date date) {
    if (date == null) {
      this.dateTime = null;
    } else {
      this.dateTime = date.toLocalDate().atStartOfDay();
    }
  }

  /**
   * java.sql.Timestampをjava.time.LocalDateTimeに変換
   *
   * @param timestamp 変換元の日時（null の場合は空のオブジェクト）
   */
  public OriginalDateTime(java.sql.Timestamp timestamp) {
    if (timestamp == null) {
      this.dateTime = null;
    } else {
      this.dateTime = timestamp.toLocalDateTime();
    }
  }

  // ================================
  // メソッド定義
  // ================================
  @Override
  public String toString() {
    if (this.dateTime == null) {
      return null;
    }
    return this.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  /**
   * 日時の前後関係を比較します。
   *
   * <p>空（日時が null）の日時と null は最も古い日時として扱う。例外は投げない。両方が空または null の場合は 0、自身だけが空の場合は負の値、比較対象だけが空または null
   * の場合は正の値を返す。両方に値がある場合は日時の前後関係を返し、等しければ 0 を返す。
   *
   * @param targetDateTime 比較対象の日時。null を指定できる
   * @return 自身が後なら 1、前なら -1、等しい場合と両方が空の場合は 0
   */
  @Override
  public int compareTo(OriginalDateTime targetDateTime) {
    boolean targetEmpty = targetDateTime == null || targetDateTime.isEmpty();
    if (this.isEmpty()) {
      return targetEmpty ? 0 : -1;
    }
    if (targetEmpty) {
      return 1;
    }
    LocalDateTime target = targetDateTime.toLocalDateTime();
    if (this.dateTime.isEqual(target)) {
      return 0;
    }
    return this.dateTime.isAfter(target) ? 1 : -1;
  }

  /**
   * このオブジェクトが空であるかどうかを判定します.
   *
   * @return this.dateTimeがNULLであればtrue、そうでなければfalseを返却する
   */
  public boolean isEmpty() {
    return this.dateTime == null;
  }

  /**
   * 引数のDateTimeオブジェクトとこのオブジェクトを比較し、日付の値が等しい場合はtrueを返却する。
   *
   * <p>両方が空の場合は等しいとする。compareTo で空を最も古い日時として 0 を返すことと揃えるため。片方だけが空の場合は等しくない。
   *
   * @param obj 比較対象のオブジェクト
   * @return 比較結果
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    OriginalDateTime other = (OriginalDateTime) obj;
    if (this.isEmpty() || other.isEmpty()) {
      return this.isEmpty() && other.isEmpty();
    }
    return this.dateTime.equals(other.toLocalDateTime());
  }

  /**
   * 日時の値から計算したハッシュコードを返却します。
   *
   * <p>{@link #equals(Object)} で等しいと判定される日時は、同じハッシュコードになる必要がある。そのため日時の値だけから計算する。日時が空の場合は 0 を返す。
   *
   * <p>この型は可変である。{@link #plusDays(int)} や {@link #minusMinutes(int)} で値が変わるため、{@code HashMap} や
   * {@code HashSet} のキーにした後は値を変更しないこと。変更するとハッシュの位置が変わり、コレクションから探せなくなる。
   *
   * @return 日時のハッシュコード。日時が空の場合は 0
   */
  @Override
  public int hashCode() {
    return this.dateTime == null ? 0 : this.dateTime.hashCode();
  }

  /**
   * このオブジェクトが持つ日時をYYYYMM形式で返却します。
   *
   * @return YYYYMM形式の日付を表す文字列
   */
  public String getYYYYMM() {
    if (this.dateTime == null) {
      return null;
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
    return this.dateTime.format(formatter);
  }

  /**
   * このオブジェクトが持つ日時をYYYYMMDD形式で返却します。
   *
   * @return YYYYMMDD形式の日付を表す文字列
   */
  public String getYyyyMmDd() {
    if (this.dateTime == null) {
      return null;
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    return this.dateTime.format(formatter);
  }

  /**
   * このオブジェクトが持つ日付の情報をLocalDateTime型で返却します
   *
   * @return LocalDateTime型の日付情報
   */
  public LocalDateTime toLocalDateTime() {
    return this.dateTime;
  }

  /**
   * このオブジェクトが持つ日付の情報をLocalDate型で返却します
   *
   * @return LocalDate型の日付情報
   */
  public LocalDate toLocalDate() {
    return this.dateTime != null ? this.dateTime.toLocalDate() : null;
  }

  /**
   * このオブジェクトが持つ日付の情報をjava.sql.Timestamp型で返却します
   *
   * @return java.sql.Timestamp型の日付情報
   */
  public java.sql.Timestamp toTimestamp() {
    return this.dateTime != null ? java.sql.Timestamp.valueOf(this.dateTime) : null;
  }

  /**
   * 引数のOriginalDateTime - このオブジェクトで日付の差を出力します. 例：2024/1/8 - 2024/1/1 = 7、2024/1/1 - 2024/1/8 = -7
   *
   * @param date 比べたい日付
   * @return 日付の差の日数
   */
  public int betweenDays(OriginalDateTime date) {
    if (date != null && !date.isEmpty() && this.dateTime != null) {
      // 両日付のLocalDateを取得
      LocalDate startDate = this.dateTime.toLocalDate();
      LocalDate endDate = date.toLocalDate();

      // 日付の差を計算
      long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);

      return (int) daysBetween;
    } else {
      return 0;
    }
  }

  /**
   * このDateTimeに引数日プラスします.
   *
   * @param days 足す日数
   */
  public void plusDays(final int days) {
    if (this.dateTime != null) {
      this.dateTime = this.dateTime.plusDays(days);
    }
  }

  /**
   * このDateTimeから引数分引きます.
   *
   * @param minutes 引く分数
   */
  public void minusMinutes(final int minutes) {
    if (this.dateTime != null) {
      this.dateTime = this.dateTime.minusMinutes(minutes);
    }
  }

  /**
   * 年と月から OriginalDateTime を生成します.
   *
   * @param year 年
   * @param month 月（1～12、-1の場合は即日）
   * @return OriginalDateTime
   */
  public static OriginalDateTime fromMonth(int year, int month) {
    if (month == -1) {
      return new OriginalDateTime();
    }
    return new OriginalDateTime(year, month, 1, 0, 0, 0);
  }

  /**
   * 「年/月」の形式の文字列から OriginalDateTime を生成します.
   *
   * <p>年と月の区切り（/）が無い場合と、数字に変換できない場合、null の場合は現在の年月を返す。 年または月が範囲外の場合（例: 2026/13）は、現在の年月に補わず {@link
   * DateTimeException} を投げる。
   *
   * @param yearMonthText 年月の文字列（例: 2026/4）。null も可
   * @return 指定の年月の日時、または現在の年月
   * @throws DateTimeException 年または月が範囲外の場合
   */
  public static OriginalDateTime fromYearMonthText(final String yearMonthText) {
    if (yearMonthText != null) {
      String[] parts = yearMonthText.split("/");
      if (parts.length == 2) {
        try {
          return fromMonth(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
          return currentYearMonth();
        }
      }
    }
    return currentYearMonth();
  }

  /**
   * 現在の年月を、日を1日とした OriginalDateTime で返します.
   *
   * @return 現在の年月
   */
  private static OriginalDateTime currentYearMonth() {
    OriginalDateTime now = new OriginalDateTime();
    return fromMonth(now.toLocalDate().getYear(), now.toLocalDate().getMonthValue());
  }

  /**
   * 月を「M月」形式で返す.
   *
   * <p>プッシュ通知のメッセージテキストで開始月を簡潔に表示する際に、 数値の月を日本語表記に統一してペイロード内で効率的に表現する.
   *
   * @return 月（例：「10月」）、nullの場合は「未定」
   */
  public String month() {
    if (this.dateTime == null) {
      return "未定";
    }
    return String.format("%d月", this.dateTime.getMonthValue());
  }
}
