package dk.alexandra.fresco.suite.dummy.arithmetic;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.BigIntegerI;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.value.SInt;


/**
 * Implements multiplication for the Dummy Arithmetic protocol suite, where all operations are done
 * in the clear.
 *
 */
public class DummyArithmeticMultProtocol extends DummyArithmeticNativeProtocol<SInt> {

  private DRes<SInt> left;
  private DRes<SInt> right;
  private DummyArithmeticSInt out;

  /**
   * Constructs a protocol to multiply the result of two computations.
   * 
   * @param left the left operand
   * @param right the right operand
   */
  public DummyArithmeticMultProtocol(DRes<SInt> left, DRes<SInt> right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public EvaluationStatus evaluate(int round, DummyArithmeticResourcePool rp, Network network) {
    BigIntegerI prod = ((DummyArithmeticSInt) left.out()).getValue().copy();
    BigIntegerI r = ((DummyArithmeticSInt) right.out()).getValue();
    prod.multiply(r);
    prod.mod(rp.getModulus());
    out = new DummyArithmeticSInt(prod);
    return EvaluationStatus.IS_DONE;
  }

  @Override
  public SInt out() {
    return out;
  }
}
