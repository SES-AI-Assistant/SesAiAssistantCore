package copel.sesproductpackage.core.api.line;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link LineMessagingApiWebhookEntity} の単体テスト。 */
class LineMessagingApiWebhookEntityTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 個人からのテキストメッセージで、送信元ユーザーIDと本文を保持することを確かめる。 送信元ユーザーIDや本文を取り違えると、別人の情報や空の本文を登録してしまうためである。
       *
       * @given events の先頭が type "message"、source.type "user"、userId "U001"、message.type "text"、text
       *     "Java案件です"
       * @when コンストラクタを呼び出す
       * @then getUserId が "U001"、getText が "Java案件です" を返し、getGroupId、getFileId、getFileName が null
       *     を返す
       */
      @Test
      void 個人からのテキストメッセージの場合_送信元ユーザーIDと本文を返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"user","userId":"U001"},
                "message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.getUserId()).isEqualTo("U001");
        assertThat(entity.getText()).isEqualTo("Java案件です");
        assertThat(entity.getGroupId()).isNull();
        assertThat(entity.getFileId()).isNull();
        assertThat(entity.getFileName()).isNull();
      }

      /**
       * グループからのファイル送信で、グループIDとファイルIDとファイル名を保持することを確かめる。 ファイルIDを保持しないと、登録処理が LINE
       * からファイルを取得できず登録が漏れるためである。
       *
       * @given source.type が "group"、groupId が "G001"、message.type が "file"、id が "F001"、fileName が
       *     "スキルシート.xlsx"
       * @when コンストラクタを呼び出す
       * @then getGroupId が "G001"、getFileId が "F001"、getFileName が "スキルシート.xlsx" を返す
       */
      @Test
      void グループからのファイル送信の場合_グループIDとファイルIDとファイル名を返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"group","groupId":"G001","userId":"U001"},
                "message":{"type":"file","id":"F001","fileName":"スキルシート.xlsx"}}]}
                """);

        assertThat(entity.getGroupId()).isEqualTo("G001");
        assertThat(entity.getFileId()).isEqualTo("F001");
        assertThat(entity.getFileName()).isEqualTo("スキルシート.xlsx");
      }

      /**
       * メッセージ以外のイベントで、本文を保持しないことを確かめる。 本文の無いイベントで本文を持つと、空のメッセージを SQS へ送信してしまうためである。
       *
       * @given events の先頭が type "follow"、source.type "user"、userId "U001"
       * @when コンストラクタを呼び出す
       * @then getText が null を返す
       */
      @Test
      void イベントの種類がfollowの場合_本文にnullを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"follow","source":{"type":"user","userId":"U001"}}]}
                """);

        assertThat(entity.getText()).isNull();
      }
    }

    @Nested
    class 異常系 {

      /**
       * JSON として解析できない文字列で、IllegalArgumentException を投げることを確かめる。 解析できないリクエストを例外にしないと、受信処理がステータス 400
       * を返せないためである。
       *
       * @given 引数が "not json"
       * @when コンストラクタを呼び出す
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 引数がJSONでない場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new LineMessagingApiWebhookEntity("not json"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * events が配列でない JSON で、IllegalArgumentException を投げることを確かめる。 配列でない events を解析すると
       * NullPointerException になり、受信処理がステータス 400 を返せないためである。
       *
       * @given 引数が events にオブジェクトを持つ JSON
       * @when コンストラクタを呼び出す
       * @then IllegalArgumentException を投げる
       */
      @Test
      void eventsが配列でない場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new LineMessagingApiWebhookEntity("{\"events\":{\"type\":\"message\"}}"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * events が空の配列の JSON で、IllegalArgumentException を投げることを確かめる。
       * 先頭のイベントが無いまま解析を続けると、NullPointerException で Lambda が異常終了するためである。
       *
       * @given 引数が events に空の配列を持つ JSON
       * @when コンストラクタを呼び出す
       * @then IllegalArgumentException を投げる
       */
      @Test
      void eventsが空の配列の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new LineMessagingApiWebhookEntity("{\"events\":[]}"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class isValid {

    @Nested
    class 正常系 {

      /**
       * イベントの種類と送信元の種類がそろったリクエストを有効と判定することを確かめる。 有効なリクエストを無効と判定すると、LINE で受信した SES 情報を取り込めなくなるためである。
       *
       * @given events の先頭が type "message"、source.type "user"
       * @when isValid を呼び出す
       * @then true を返す
       */
      @Test
      void イベントの種類と送信元の種類がある場合_trueを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"user","userId":"U001"},
                "message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.isValid()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * イベントの種類が空文字のリクエストを無効と判定することを確かめる。 種類の無いイベントを有効と判定すると、メッセージかどうかを決められないまま処理が進むためである。
       *
       * @given events の先頭が type が空文字、source.type "user"
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void イベントの種類が空文字の場合_falseを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"","source":{"type":"user","userId":"U001"}}]}
                """);

        assertThat(entity.isValid()).isFalse();
      }

      /**
       * 送信元の種類が無いリクエストを無効と判定することを確かめる。 送信元の種類が無いと、グループと個人のどちらから届いたかを決められないためである。
       *
       * @given events の先頭が type "message"、source を持たない
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 送信元の種類が無い場合_falseを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.isValid()).isFalse();
      }
    }
  }

  @Nested
  class isFile {

    @Nested
    class 正常系 {

      /**
       * メッセージの種類が file のリクエストをファイル送信と判定することを確かめる。 ファイル送信を見落とすと、空の本文のメッセージとして送信してしまうためである。
       *
       * @given events の先頭が type "message"、message.type "file"
       * @when isFile を呼び出す
       * @then true を返す
       */
      @Test
      void メッセージの種類がfileの場合_trueを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"user","userId":"U001"},
                "message":{"type":"file","id":"F001","fileName":"スキルシート.xlsx"}}]}
                """);

        assertThat(entity.isFile()).isTrue();
      }

      /**
       * メッセージの種類が text のリクエストをファイル送信と判定しないことを確かめる。
       * テキストメッセージをファイル送信と判定すると、ファイルIDの無いメッセージを送信してしまうためである。
       *
       * @given events の先頭が type "message"、message.type "text"
       * @when isFile を呼び出す
       * @then false を返す
       */
      @Test
      void メッセージの種類がtextの場合_falseを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"user","userId":"U001"},
                "message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.isFile()).isFalse();
      }

      /**
       * メッセージ以外のイベントをファイル送信と判定しないことを確かめる。 メッセージの種類はメッセージイベントにだけ存在し、それ以外では判定に使えないためである。
       *
       * @given events の先頭が type "follow"
       * @when isFile を呼び出す
       * @then false を返す
       */
      @Test
      void イベントの種類がfollowの場合_falseを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"follow","source":{"type":"user","userId":"U001"}}]}
                """);

        assertThat(entity.isFile()).isFalse();
      }
    }
  }

  @Nested
  class isGroup {

    @Nested
    class 正常系 {

      /**
       * 送信元の種類が group でグループIDを持つリクエストをグループと判定することを確かめる。
       * グループの判定を誤ると、どのグループに投稿された情報かを送信元グループで追えなくなるためである。
       *
       * @given events の先頭の source.type が "group"、groupId が "G001"
       * @when isGroup を呼び出す
       * @then true を返す
       */
      @Test
      void 送信元の種類がgroupでグループIDがある場合_trueを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"group","groupId":"G001","userId":"U001"},
                "message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.isGroup()).isTrue();
      }

      /**
       * 送信元の種類が user のリクエストをグループと判定しないことを確かめる。 個人からのメッセージをグループと判定すると、送信元グループが null
       * のメッセージを送信するためである。
       *
       * @given events の先頭の source.type が "user"
       * @when isGroup を呼び出す
       * @then false を返す
       */
      @Test
      void 送信元の種類がuserの場合_falseを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"user","userId":"U001"},
                "message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.isGroup()).isFalse();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 送信元の種類が group でもグループIDが無いリクエストをグループと判定しないことを確かめる。 グループIDの無いリクエストをグループと判定すると、送信元グループが null
       * のメッセージを送信するためである。
       *
       * @given events の先頭の source.type が "group"、groupId を持たない
       * @when isGroup を呼び出す
       * @then false を返す
       */
      @Test
      void 送信元の種類がgroupでグループIDが無い場合_falseを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"group","userId":"U001"},
                "message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.isGroup()).isFalse();
      }

      /**
       * 送信元の種類が group でグループIDが空文字のリクエストをグループと判定しないことを確かめる。
       * 空文字のグループIDをグループと判定すると、送信元グループが空のメッセージを送信するためである。
       *
       * @given events の先頭の source.type が "group"、groupId が空文字
       * @when isGroup を呼び出す
       * @then false を返す
       */
      @Test
      void 送信元の種類がgroupでグループIDが空文字の場合_falseを返す() {
        LineMessagingApiWebhookEntity entity =
            new LineMessagingApiWebhookEntity(
                """
                {"events":[{"type":"message","source":{"type":"group","groupId":"","userId":"U001"},
                "message":{"type":"text","text":"Java案件です"}}]}
                """);

        assertThat(entity.isGroup()).isFalse();
      }
    }
  }
}
