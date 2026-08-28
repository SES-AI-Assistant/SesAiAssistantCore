package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FromId ユーティリティのテストクラス.
 *
 * @author Copel Co., Ltd.
 */
@DisplayName("FromId ユーティリティテスト")
class FromIdTest {

  @Test
  @DisplayName("isEmailFormat: メールアドレス形式は true を返す")
  void testIsEmailFormat_WithValidEmail() {
    assertTrue(FromId.isEmailFormat("user@example.com"));
  }

  @Test
  @DisplayName("isEmailFormat: LINE ID形式は false を返す")
  void testIsEmailFormat_WithLineId() {
    assertFalse(FromId.isEmailFormat("U1234567890abcdef1234567890abcdef"));
  }

  @Test
  @DisplayName("isEmailFormat: null は false を返す")
  void testIsEmailFormat_WithNull() {
    assertFalse(FromId.isEmailFormat(null));
  }

  @Test
  @DisplayName("isEmailFormat: 空文字は false を返す")
  void testIsEmailFormat_WithEmpty() {
    assertFalse(FromId.isEmailFormat(""));
  }

  @Test
  @DisplayName("extractDomain: メールアドレスからドメインを抽出")
  void testExtractDomain_WithValidEmail() {
    assertEquals("example.com", FromId.extractDomain("user@example.com"));
  }

  @Test
  @DisplayName("extractDomain: 大文字ドメインは小文字で返す")
  void testExtractDomain_WithUppercaseDomain() {
    assertEquals("example.com", FromId.extractDomain("user@EXAMPLE.COM"));
  }

  @Test
  @DisplayName("extractDomain: サブドメインを含むメールアドレスから抽出")
  void testExtractDomain_WithSubdomain() {
    assertEquals("mail.example.co.jp", FromId.extractDomain("user@mail.example.co.jp"));
  }

  @Test
  @DisplayName("extractDomain: メールアドレス形式でない場合は空文字を返す")
  void testExtractDomain_WithLineId() {
    assertEquals("", FromId.extractDomain("U1234567890abcdef1234567890abcdef"));
  }

  @Test
  @DisplayName("extractDomain: null は空文字を返す")
  void testExtractDomain_WithNull() {
    assertEquals("", FromId.extractDomain(null));
  }

  @Test
  @DisplayName("extractDomain: 空文字は空文字を返す")
  void testExtractDomain_WithEmpty() {
    assertEquals("", FromId.extractDomain(""));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: 同じFromIdは true を返す")
  void testIsSameSenderOrDomain_SameId() {
    assertTrue(FromId.isSameSenderOrDomain("user@example.com", "user@example.com"));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: 同じメールドメインは true を返す")
  void testIsSameSenderOrDomain_SameDomain() {
    assertTrue(FromId.isSameSenderOrDomain("user1@example.com", "user2@example.com"));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: 異なるドメインは false を返す")
  void testIsSameSenderOrDomain_DifferentDomain() {
    assertFalse(FromId.isSameSenderOrDomain("user1@example.com", "user2@other.com"));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: メールアドレスとLINE IDは false を返す")
  void testIsSameSenderOrDomain_EmailAndLineId() {
    assertFalse(FromId.isSameSenderOrDomain("user@example.com", "U1234567890abcdef1234567890abcdef"));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: 両方LINE IDは false を返す")
  void testIsSameSenderOrDomain_BothLineId() {
    assertFalse(FromId.isSameSenderOrDomain("U1234567890abcdef1234567890abcdef", "U9876543210fedcba9876543210fedcba"));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: null を含む場合は false を返す")
  void testIsSameSenderOrDomain_WithNull() {
    assertFalse(FromId.isSameSenderOrDomain(null, "user@example.com"));
    assertFalse(FromId.isSameSenderOrDomain("user@example.com", null));
    assertFalse(FromId.isSameSenderOrDomain(null, null));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: 大文字小文字を区別しない（ドメイン）")
  void testIsSameSenderOrDomain_CaseInsensitive() {
    assertTrue(FromId.isSameSenderOrDomain("user1@EXAMPLE.COM", "user2@example.com"));
  }

  @Test
  @DisplayName("isSameSenderOrDomain: 空ドメインは false を返す（両方メール形式でも）")
  void testIsSameSenderOrDomain_EmptyDomain() {
    assertFalse(FromId.isSameSenderOrDomain("user@", "user@"));
  }
}
