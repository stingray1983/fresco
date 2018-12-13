package dk.alexandra.fresco.suite.spdz;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerModulus;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.ModulusFinder;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import org.junit.Test;

public class TestSpdzSInt {

  private BigIntegerFieldDefinition definition = new BigIntegerFieldDefinition(
      new BigIntegerModulus(ModulusFinder.findSuitableModulus(8)));

  @Test
  public void testEquals() {
    SpdzSInt element = new SpdzSInt(getI(25), getI(15));

    assertTrue(element.equals(element));
    assertFalse(element.equals("This is a String"));
    assertFalse(element.equals(null));

    SpdzSInt element2 = new SpdzSInt(getI(25), null);
    assertFalse(element.equals(element2));
    element2 = new SpdzSInt(getI(25), getI(11)
    );
    assertFalse(element.equals(element2));
    element = new SpdzSInt(getI(25), null);
    assertFalse(element.equals(element2));
    element2 = new SpdzSInt(getI(25), null);
    assertTrue(element.equals(element2));

    element = new SpdzSInt(getI(25), null);
    element2 = new SpdzSInt(getI(25), null);
    assertTrue(element.equals(element2));

    element = new SpdzSInt(null, getI(11));
    element2 = new SpdzSInt(getI(25), getI(11));
    assertFalse(element.equals(element2));
    element2 = new SpdzSInt(null, getI(11));
    assertTrue(element.equals(element2));
    element = new SpdzSInt(getI(25), getI(11));
    assertFalse(element.equals(element2));
  }

  private FieldElement getI(int i) {
    return definition.createElement(i);
  }

  @Test
  public void testHashCode() {
    SpdzSInt e1 = new SpdzSInt(getI(25), getI(15));
    SpdzSInt e2 = new SpdzSInt(null, getI(15));
    SpdzSInt e3 = new SpdzSInt(getI(25), null);
    assertAllDifferent(new int[]{
        e1.hashCode(),
        e2.hashCode(),
        e3.hashCode()
    });
  }

  private void assertAllDifferent(int[] elements) {
    for (int i = 0; i < elements.length; i++) {
      for (int j = 0; j < elements.length; j++) {
        if (i != j) {
          assertNotEquals(elements[i], elements[j]);
        }
      }
    }
  }
}
