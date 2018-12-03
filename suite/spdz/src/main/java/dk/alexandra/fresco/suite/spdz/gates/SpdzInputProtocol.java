package dk.alexandra.fresco.suite.spdz.gates;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.builder.numeric.BigIntegerI;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzInputMask;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDataSupplier;
import java.math.BigInteger;

public class SpdzInputProtocol extends SpdzNativeProtocol<SInt> {

  private SpdzInputMask inputMask; // is opened by this gate.
  protected BigIntegerI input;
  private BigIntegerI valueMasked;
  protected SpdzSInt out;
  private int inputter;
  private byte[] digest;

  public SpdzInputProtocol(BigIntegerI input, int inputter) {
    this.input = input;
    this.inputter = inputter;
  }

  @Override
  public EvaluationStatus evaluate(int round, SpdzResourcePool spdzResourcePool,
      Network network) {
    int myId = spdzResourcePool.getMyId();
    BigInteger modulus = spdzResourcePool.getModulus();
    SpdzDataSupplier dataSupplier = spdzResourcePool.getDataSupplier();
    ByteSerializer<BigIntegerI> serializer = spdzResourcePool.getSerializer();
    if (round == 0) {
      this.inputMask = dataSupplier.getNextInputMask(this.inputter);
      if (myId == this.inputter) {
        BigIntegerI bcValue = this.input.copy();
        bcValue.subtract(this.inputMask.getRealValue());
        bcValue.mod(modulus);
        network.sendToAll(serializer.serialize(bcValue));
      }
      return EvaluationStatus.HAS_MORE_ROUNDS;
    } else if (round == 1) {
      this.valueMasked = serializer.deserialize(network.receive(inputter));
      this.digest = sendBroadcastValidation(
          spdzResourcePool.getMessageDigest(), network,
          valueMasked);
      return EvaluationStatus.HAS_MORE_ROUNDS;
    } else {
      boolean validated = receiveBroadcastValidation(network, digest);
      if (!validated) {
        throw new MaliciousException("Broadcast digests did not match");
      }
      BigIntegerI maskedValue = dataSupplier.getSecretSharedKey().copy();
      maskedValue.multiply(valueMasked);
      maskedValue.mod(modulus);
      SpdzSInt valueMaskedElement =
          new SpdzSInt(
              valueMasked,
              maskedValue,
              modulus);
      this.out = this.inputMask.getMask().add(valueMaskedElement, myId);
      return EvaluationStatus.IS_DONE;
    }
  }

  @Override
  public SpdzSInt out() {
    return out;
  }
}
