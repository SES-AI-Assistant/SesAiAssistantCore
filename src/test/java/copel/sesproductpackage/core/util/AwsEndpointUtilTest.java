package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AwsEndpointUtilTest {

  @Test
  void resolveEndpointUrl_trimsSystemProperty() {
    String existing = System.getProperty("aws.endpointUrl");
    try {
      System.setProperty("aws.endpointUrl", "  http://localhost:4566  ");
      assertEquals("http://localhost:4566", AwsEndpointUtil.resolveEndpointUrl());
    } finally {
      if (existing != null) {
        System.setProperty("aws.endpointUrl", existing);
      } else {
        System.clearProperty("aws.endpointUrl");
      }
    }
  }
}
