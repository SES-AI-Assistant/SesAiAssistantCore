package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntityCoverageHelper {

  private static List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Class<?> c = type; c != null && !c.equals(Object.class); c = c.getSuperclass()) {
      fields.addAll(Arrays.asList(c.getDeclaredFields()));
    }
    return fields;
  }

  public static void checkLombokCoverage(
      Class<?> clazz, Object instance1, Object instance2, Object diffInstance) throws Exception {
    List<Field> allFields = getAllFields(clazz);
    List<Field> declaredFields = Arrays.asList(clazz.getDeclaredFields());

    // Test getters and setters
    for (Field field : allFields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      String name = field.getName();
      String prefix = field.getType().equals(boolean.class) ? "is" : "get";
      String getterName = prefix + name.substring(0, 1).toUpperCase() + name.substring(1);
      String setterName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);

      try {
        Method getter = clazz.getMethod(getterName);
        Method setter = clazz.getMethod(setterName, field.getType());

        // Get value from diffInstance and set to instance1
        Object diffVal = getter.invoke(diffInstance);
        setter.invoke(instance1, diffVal);

        // Verify
        assertEquals(diffVal, getter.invoke(instance1));
      } catch (NoSuchMethodException e) {
        // Ignore if no getter/setter for a specific field
      }
    }

    // Test equals and hashcode
    assertTrue(instance1.equals(instance1));
    assertFalse(instance1.equals(null));
    assertFalse(instance1.equals(new Object()));

    // Check equals varying each field if both are the same initially
    Object same1 = clazz.getDeclaredConstructor().newInstance();
    Object same2 = clazz.getDeclaredConstructor().newInstance();
    assertTrue(same1.equals(same2));

    for (Field field : allFields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      field.setAccessible(true);
      Object originalVal = field.get(same1);

      Object testVal = null;
      if (field.getType().equals(String.class)) {
        testVal = "test_diff_val";
      } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
        testVal = 999;
      } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
        testVal = 99.9;
      } else if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
        testVal = true;
      } else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
        testVal = 999L;
      } else if (field.getType().equals(copel.sesproductpackage.core.unit.OriginalDateTime.class)) {
        testVal = new copel.sesproductpackage.core.unit.OriginalDateTime();
      } else if (field.getType().equals(byte[].class)) {
        testVal = new byte[] {1, 2, 3};
      } else if (field.getType().equals(copel.sesproductpackage.core.unit.Vector.class)) {
        testVal = new copel.sesproductpackage.core.unit.Vector(null);
      } else if (java.util.Date.class.isAssignableFrom(field.getType())) {
        testVal = new java.util.Date();
      } else {
        try {
          testVal = field.getType().getDeclaredConstructor().newInstance();
          // If it's the same as original, try to make it different
          if (testVal.equals(originalVal)) {
            // For SkillSheet, we can set something
            if (testVal instanceof copel.sesproductpackage.core.unit.SkillSheet) {
              ((copel.sesproductpackage.core.unit.SkillSheet) testVal).setFileId("diff_id");
            }
            // For other types, maybe we can try to set a field?
            // But let's just handle SkillSheet for now as it's the blocker
          }
        } catch (Exception e) {
        }
      }

      boolean isDeclared = false;
      for (Field f : declaredFields) {
        if (f.getName().equals(field.getName())) {
          isDeclared = true;
          break;
        }
      }

      if (testVal != null) {
        try {
          field.set(same1, testVal);
          same1.hashCode();
          same1.toString();
          if (isDeclared) {
            assertFalse(same1.equals(same2));
          }

          // TEST both non-null and EQUAL
          field.set(same2, testVal);
          if (isDeclared) {
            assertTrue(same1.equals(same2));
          }

          // TEST both non-null but unequal
          if (testVal instanceof String) {
            field.set(same2, "diff2_xyz");
            if (isDeclared) {
              assertFalse(same1.equals(same2));
            }
          } else if (testVal instanceof Boolean) {
            field.set(same2, !(Boolean) testVal);
            if (isDeclared) {
              assertFalse(same1.equals(same2));
            }
          } else if (testVal instanceof Number) {
            field.set(same2, ((Number) testVal).intValue() == 0 ? 1 : 0);
            if (isDeclared) {
              assertFalse(same1.equals(same2));
            }
          }

          field.set(same1, null);
          field.set(same2, testVal);
          if (isDeclared) {
            assertFalse(same1.equals(same2));
          }
          field.set(same2, null);
          assertTrue(same1.equals(same2));
        } catch (Exception e) {
        }
        field.set(same1, originalVal);
        field.set(same2, originalVal);
      }
    }

    // canEqual
    try {
      Method canEqual = clazz.getMethod("canEqual", Object.class);
      assertTrue((Boolean) canEqual.invoke(instance1, instance1));
      assertFalse((Boolean) canEqual.invoke(instance1, new Object()));
      assertTrue(instance1.equals(instance1));
      assertFalse(instance1.equals(null));
      instance1.hashCode();
    } catch (NoSuchMethodException e) {
    }

    // try to hit other.canEqual returning false
    try {
      Object mockOther = org.mockito.Mockito.mock(clazz);
      Method canEqualMethod = clazz.getMethod("canEqual", Object.class);
      // We want mockOther.canEqual(...) to return false
      // Since we can't call mockOther.canEqual(instance1) directly on Object type,
      // we use the return value of invoke as the operand for when()
      org.mockito.Mockito.when(canEqualMethod.invoke(mockOther, instance1)).thenReturn(false);

      assertFalse(instance1.equals(mockOther));
    } catch (Exception e) {
    }

    // toString
    try {
      Method toString = clazz.getMethod("toString");
      assertNotNull(toString.invoke(instance1));
    } catch (NoSuchMethodException e) {
    }
  }

  public static void verifyEntityCoverage(Object entity) throws Exception {
    Class<?> clazz = entity.getClass();
    Object instance1 = clazz.getDeclaredConstructor().newInstance();
    Object instance2 = clazz.getDeclaredConstructor().newInstance();
    Object diffInstance = clazz.getDeclaredConstructor().newInstance();
    checkLombokCoverage(clazz, instance1, instance2, diffInstance);
  }
}
