package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RequestTypeTest {

  @Test
  void testGetEnum() {
    assertNull(RequestType.getEnum(null));
    assertEquals(RequestType.LineMessage, RequestType.getEnum("LineMessage"));
    assertEquals(RequestType.LineFile, RequestType.getEnum("LineFile"));
    assertEquals(RequestType.EmailMessage, RequestType.getEnum("EmailMessage"));
    assertEquals(RequestType.EmailFile, RequestType.getEnum("EmailFile"));
    assertEquals(RequestType.ScreenMessage, RequestType.getEnum("ScreenMessage"));
    assertEquals(RequestType.ScreenFile, RequestType.getEnum("ScreenFile"));
    assertEquals(RequestType.OtherMessage, RequestType.getEnum("OtherMessage"));
    assertEquals(RequestType.OtherFile, RequestType.getEnum("OtherFile"));
    assertEquals(RequestType.OtherMessage, RequestType.getEnum("UNKNOWN"));
  }
}
