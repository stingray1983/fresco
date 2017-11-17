package dk.alexandra.fresco.demo;

import java.math.BigInteger;
import java.util.Scanner;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Comparison;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;

public class ReactiveApp implements Application<BigInteger, ProtocolBuilderNumeric> {

  @Override
  public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
    // who am I?
    Integer myPartyId = builder.getBasicNumericContext().getMyId();
    // get input from user
    final BigInteger myInput = getUserInput(myPartyId);
    System.out.println("Will run as party " + myPartyId + " with input " + myInput);

    // basic numeric functionality
    Numeric numericCompDir = builder.numeric();
    // comparison functionality
    Comparison comparisonCompDir = builder.comparison();

    // each party provides its input to the MPC
    final DRes<SInt> inputPartyOne =
        myPartyId == 1 ? numericCompDir.input(myInput, 1) : numericCompDir.input(null, 1);
    final DRes<SInt> inputPartyTwo =
        myPartyId == 2 ? numericCompDir.input(myInput, 2) : numericCompDir.input(null, 2);
    // equals
    DRes<SInt> equalsFlag = comparisonCompDir.equals(inputPartyOne, inputPartyTwo);
    DRes<BigInteger> openedEqualsFlag = numericCompDir.open(equalsFlag);
    
    // this would break since the result has not been computed in this computation scope!
    // BigInteger res = openedEqualsFlag.out();

    return builder.seq((subBuilder) -> {
      /*
       * This is safe, since we are in a subsequent computation scope and we won't reach this point
       * until the previous computation is complete (i.e., all its native protocols have been
       * evaluated).
       */
      BigInteger unwrapped = openedEqualsFlag.out();
      if (unwrapped.equals(BigInteger.ONE)) {
        System.out.println("Inputs are equal! Will compute sum.");
        // can't use builder here, must use subBuilder
        DRes<SInt> sum = subBuilder.numeric().add(inputPartyOne, inputPartyTwo);
        return subBuilder.numeric().open(sum);
      } else {
        System.out.println("Inputs are not equal! Will compute product.");
        DRes<SInt> prod = subBuilder.numeric().mult(inputPartyOne, inputPartyTwo);
        return subBuilder.numeric().open(prod);
      }
    });
  }

  // Helper methods

  private BigInteger getUserInput(Integer partyId) {
    // read some user input from console
    Scanner reader = new Scanner(System.in);
    System.out.println("Enter your input: ");
    final BigInteger myInput = BigInteger.valueOf(reader.nextInt());
    reader.close();
    return myInput;
  }

}