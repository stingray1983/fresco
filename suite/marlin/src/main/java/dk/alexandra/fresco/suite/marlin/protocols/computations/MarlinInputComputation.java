package dk.alexandra.fresco.suite.marlin.protocols.computations;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.marlin.datatypes.BigUInt;
import dk.alexandra.fresco.suite.marlin.protocols.natives.MarlinBroadcastValidationProtocol;
import dk.alexandra.fresco.suite.marlin.protocols.natives.MarlinInputOnlyProtocol;

public class MarlinInputComputation<T extends BigUInt<T>> implements
    Computation<SInt, ProtocolBuilderNumeric> {

  private final T input;
  private final int inputPartyId;

  public MarlinInputComputation(T input, int inputPartyId) {
    this.inputPartyId = inputPartyId;
    this.input = input;
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    DRes<Pair<DRes<SInt>, byte[]>> pair = builder
        .append(new MarlinInputOnlyProtocol<>(input, inputPartyId));
    return builder.seq(seq -> {
      Pair<DRes<SInt>, byte[]> unwrapped = pair.out();
      DRes<Void> nothing = seq
          .append(new MarlinBroadcastValidationProtocol<>(unwrapped.getSecond()));
      return () -> {
        nothing.out();
        return unwrapped.getFirst().out();
      };
    });
  }

}