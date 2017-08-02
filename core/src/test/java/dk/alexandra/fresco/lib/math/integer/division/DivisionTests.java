/*
 * Copyright (c) 2015, 2016 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 */
package dk.alexandra.fresco.lib.math.integer.division;

import dk.alexandra.fresco.framework.BuilderFactory;
import dk.alexandra.fresco.framework.Computation;
import dk.alexandra.fresco.framework.ProtocolFactory;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.TestApplication;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadConfiguration;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.BuilderFactoryNumeric;
import dk.alexandra.fresco.framework.builder.NumericBuilder;
import dk.alexandra.fresco.framework.builder.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.ProtocolBuilderNumeric.SequentialNumericBuilder;
import dk.alexandra.fresco.framework.network.ResourcePoolCreator;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.SequentialProtocolProducer;
import dk.alexandra.fresco.lib.helper.builder.NumericIOBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;


/**
 * Generic test cases for basic finite field operations.
 *
 * Can be reused by a test case for any protocol suite that implements the basic
 * field protocol factory.
 *
 * TODO: Generic tests should not reside in the runtime package. Rather in
 * mpc.lib or something.
 */
public class DivisionTests {

  /**
   * Test Euclidian division
   */
  public static class TestEuclidianDivision extends TestThreadFactory {

    @Override
    public TestThread next(TestThreadConfiguration conf) {

      return new TestThread() {
        private final BigInteger x = new BigInteger("123978634193227335452345761");
        private final BigInteger d = new BigInteger("6543212341214412");

        @Override
        public void test() throws Exception {
          List<Computation<BigInteger>> cOutputs = new ArrayList<>();
          TestApplication app = new TestApplication() {

            @Override
            public ProtocolProducer prepareApplication(
                BuilderFactory factoryProducer) {
              ProtocolFactory producer = factoryProducer.getProtocolFactory();

              BasicNumericFactory basicNumericFactory = (BasicNumericFactory) producer;

              NumericIOBuilder ioBuilder = new NumericIOBuilder(basicNumericFactory);
              SequentialProtocolProducer sequentialProtocolProducer = new SequentialProtocolProducer();

              SInt input1 = ioBuilder.input(x, 1);
              BigInteger input2 = d;
              sequentialProtocolProducer.append(ioBuilder.getProtocol());

              SequentialNumericBuilder applicationRoot = ProtocolBuilderNumeric
                  .createApplicationRoot((BuilderFactoryNumeric) factoryProducer,
                      (seq) -> {
                        Computation<SInt> division = seq.createSequentialSub(
                            new KnownDivisor(
                                (BuilderFactoryNumeric) factoryProducer, () -> input1, input2));

                        Computation<SInt> remainder = seq.createSequentialSub(
                            new KnownDivisorRemainder(() -> input1, input2));
                        NumericBuilder NumericBuilder = seq.numeric();
                        Computation<BigInteger> output1 = NumericBuilder.open(division);
                        Computation<BigInteger> output2 = NumericBuilder.open(remainder);
                        cOutputs.add(output1);
                        cOutputs.add(output2);
                      });
              sequentialProtocolProducer.append(applicationRoot.build());
              return sequentialProtocolProducer;
            }
          };
          secureComputationEngine
              .runApplication(app, ResourcePoolCreator.createResourcePool(conf.sceConf));
          BigInteger quotient = cOutputs.get(0).out();
          BigInteger remainder = cOutputs.get(1).out();
          Assert.assertEquals(x.mod(d), remainder);
          Assert.assertEquals(x.divide(d), quotient);
        }
      };
    }
  }

  /**
   * Test division with secret shared divisor
   */
  public static class TestSecretSharedDivision extends TestThreadFactory {

    @Override
    public TestThread next(TestThreadConfiguration conf) {

      return new TestThread() {
        private final BigInteger[] x = new BigInteger[]{new BigInteger("1234567"),
            BigInteger.valueOf(1230121230), BigInteger.valueOf(313222110),
            BigInteger.valueOf(5111215), BigInteger.valueOf(6537)};
        private final BigInteger d = BigInteger.valueOf(1110);
        private final int n = x.length;

        private List<Computation<BigInteger>> results = new ArrayList<>(n);

        @Override
        public void test() throws Exception {
          TestApplication app = new TestApplication() {

            @Override
            public ProtocolProducer prepareApplication(
                BuilderFactory factoryProducer) {
              return ProtocolBuilderNumeric
                  .createApplicationRoot((BuilderFactoryNumeric) factoryProducer, (builder) -> {
                    NumericBuilder input = builder.numeric();
                    Computation<SInt> divisor = input.known(d);
                    for (BigInteger value : x) {
                      Computation<SInt> dividend = input.known(value);
                      Computation<SInt> division = builder.advancedNumeric()
                          .div(dividend, divisor);
                      results.add(builder.numeric().open(division));
                    }
                  }).build();
            }
          };
          secureComputationEngine
              .runApplication(app, ResourcePoolCreator.createResourcePool(conf.sceConf));
          for (int i = 0; i < n; i++) {
            BigInteger actual = results.get(i).out();

            BigInteger expected = x[i].divide(d);

            boolean isCorrect = expected.equals(actual);

            System.out.println(
                x[i] + "/" + d + " = " + actual + ", expected " + expected + ". ");
            Assert.assertTrue(isCorrect);

          }
        }
      };
    }
  }
}