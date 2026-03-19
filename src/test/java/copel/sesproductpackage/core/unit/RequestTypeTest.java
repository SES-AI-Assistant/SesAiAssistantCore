package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RequestTypeTest {

  @Test
  void testGetEnum() {
    assertNull(RequestType.getEnum(null));
    assertEquals(RequestType.LineMessage, RequestType.getEnum("11"));
    assertEquals(RequestType.LineFile, RequestType.getEnum("12"));
    assertEquals(RequestType.EmailMessage, RequestType.getEnum("21"));
    assertEquals(RequestType.EmailFile, RequestType.getEnum("22"));
    assertEquals(RequestType.ScreenMessage, RequestType.getEnum("31"));
    assertEquals(RequestType.ScreenFile, RequestType.getEnum("32"));
    assertEquals(RequestType.OtherMessage, RequestType.getEnum("01"));
    assertEquals(RequestType.OtherFile, RequestType.getEnum("02"));
    assertEquals(RequestType.OtherMessage, RequestType.getEnum("ZZ"));
  }

  @Test
  void testGetCode() {
    assertEquals("11", RequestType.LineMessage.getCode());
  }
}
