package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RequestTypeTests {

  @Test
  void testGetEnum() {
    assertNull(RequestType.getEnum(null));
    assertEquals(RequestType.LineMessage, RequestType.getEnum("11"));
    assertEquals(RequestType.LineFile, RequestType.getEnum("12"));
    assertEquals(RequestType.EmailMessage, RequestType.getEnum("21"));
    assertEquals(RequestType.EmailFile, RequestType.getEnum("22"));
    assertEquals(RequestType.OtherMessage, RequestType.getEnum("01"));
    assertEquals(RequestType.OtherFile, RequestType.getEnum("02"));
    assertEquals(RequestType.OtherMessage, RequestType.getEnum("ZZ")); // default case
  }

  @Test
  void testGetCode() {
    assertEquals("11", RequestType.LineMessage.getCode());
  }
}
