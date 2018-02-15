package dk.alexandra.fresco.suite.marlin.resource;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.builder.numeric.BuilderFactoryNumeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedStrategy;
import dk.alexandra.fresco.framework.sce.evaluator.NetworkBatchDecorator;
import dk.alexandra.fresco.framework.sce.evaluator.ProtocolCollectionList;
import dk.alexandra.fresco.framework.sce.resources.Broadcast;
import dk.alexandra.fresco.framework.sce.resources.ResourcePoolImpl;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.ByteArrayHelper;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.lib.field.integer.BasicNumericContext;
import dk.alexandra.fresco.suite.marlin.MarlinBuilder;
import dk.alexandra.fresco.suite.marlin.datatypes.BigUInt;
import dk.alexandra.fresco.suite.marlin.datatypes.BigUIntFactory;
import dk.alexandra.fresco.suite.marlin.protocols.computations.MarlinCommitmentComputation;
import dk.alexandra.fresco.suite.marlin.resource.storage.MarlinDataSupplier;
import dk.alexandra.fresco.suite.marlin.resource.storage.MarlinOpenedValueStore;
import java.io.Closeable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.Supplier;

public class MarlinResourcePoolImpl<T extends BigUInt<T>> extends ResourcePoolImpl implements
    MarlinResourcePool<T> {

  private final int operationalBitLength;
  private final int effectiveBitLength;
  private final BigInteger modulus;
  private final MarlinOpenedValueStore<T> storage;
  private final MarlinDataSupplier<T> supplier;
  private final BigUIntFactory<T> factory;
  private final ByteSerializer<T> rawSerializer;
  private Drbg drbg;

  /**
   * Creates new {@link MarlinResourcePool}.
   */
  private MarlinResourcePoolImpl(int myId, int noOfPlayers, Drbg drbg, int operationalBitLength,
      int effectiveBitLength, MarlinOpenedValueStore<T> storage, MarlinDataSupplier<T> supplier,
      BigUIntFactory<T> factory) {
    super(myId, noOfPlayers);
    if (operationalBitLength != 128) {
      throw new IllegalArgumentException(
          "Current implementation only supports 128 operational bit length");
    }
    if (effectiveBitLength != 64) {
      throw new IllegalArgumentException(
          "Current implementation only supports 64 effective bit length");
    }
    this.operationalBitLength = operationalBitLength;
    this.effectiveBitLength = effectiveBitLength;
    this.modulus = BigInteger.ONE.shiftLeft(operationalBitLength);
    this.storage = storage;
    this.supplier = supplier;
    this.factory = factory;
    this.rawSerializer = factory.createSerializer();
    this.drbg = drbg;
  }

  /**
   * Creates new {@link MarlinResourcePool}.
   */
  public MarlinResourcePoolImpl(int myId, int noOfPlayers, Drbg drbg,
      MarlinOpenedValueStore<T> storage, MarlinDataSupplier<T> supplier,
      BigUIntFactory<T> factory) {
    this(myId, noOfPlayers, drbg, 128, 64, storage, supplier, factory);
  }

  @Override
  public int getOperationalBitLength() {
    return operationalBitLength;
  }

  @Override
  public int getEffectiveBitLength() {
    return effectiveBitLength;
  }

  @Override
  public MarlinOpenedValueStore<T> getOpenedValueStore() {
    return storage;
  }

  @Override
  public MarlinDataSupplier<T> getDataSupplier() {
    return supplier;
  }

  @Override
  public BigUIntFactory<T> getFactory() {
    return factory;
  }

  @Override
  public ByteSerializer<T> getRawSerializer() {
    return rawSerializer;
  }

  @Override
  public void initializeJointRandomness(Supplier<Network> networkSupplier) {
    BasicNumericContext numericContext = new BasicNumericContext(effectiveBitLength, modulus,
        getMyId(), getNoOfParties());
    Network network = networkSupplier.get();
    NetworkBatchDecorator networkBatchDecorator =
        new NetworkBatchDecorator(
            this.getNoOfParties(),
            network);
    BuilderFactoryNumeric builderFactory = new MarlinBuilder<>(factory, numericContext);
    ProtocolBuilderNumeric root = builderFactory.createSequential();
    byte[] ownSeed = new byte[32];
    new SecureRandom().nextBytes(ownSeed);
    DRes<List<byte[]>> seeds = new MarlinCommitmentComputation<>(this, ownSeed)
        .buildComputation(root);
    ProtocolProducer commitmentProducer = root.build();
    do {
      ProtocolCollectionList<MarlinResourcePool> protocolCollectionList =
          new ProtocolCollectionList<>(
              128); // batch size is irrelevant since this is a very light-weight protocol
      commitmentProducer.getNextProtocols(protocolCollectionList);
      new BatchedStrategy<MarlinResourcePool>()
          .processBatch(protocolCollectionList, this, networkBatchDecorator);
    } while (commitmentProducer.hasNextProtocols());
    byte[] jointSeed = new byte[32];
    for (byte[] seed : seeds.out()) {
      ByteArrayHelper.xor(jointSeed, seed);
    }
    // TODO should be supplied
    drbg = new AesCtrDrbg(jointSeed);
    ExceptionConverter.safe(() -> {
      ((Closeable) network).close();
      return null;
    }, "Failed to close network");
  }

  @Override
  public Broadcast createBroadcast(Network network) {
    // TODO come up with way to cache this
    return new Broadcast(network);
  }

  @Override
  public BigInteger getModulus() {
    return modulus;
  }

  @Override
  public ByteSerializer<BigInteger> getSerializer() {
    throw new UnsupportedOperationException("This suite does not support serializing big integers");
  }

  @Override
  public Drbg getRandomGenerator() {
    return drbg;
  }

}
