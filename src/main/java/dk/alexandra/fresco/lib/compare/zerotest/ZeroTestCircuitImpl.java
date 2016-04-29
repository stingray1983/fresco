/*******************************************************************************
 * Copyright (c) 2015 FRESCO (http://github.com/aicis/fresco).
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
 *******************************************************************************/
package dk.alexandra.fresco.lib.compare.zerotest;

import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.AbstractSimpleProtocol;
import dk.alexandra.fresco.lib.helper.sequential.SequentialProtocolProducer;

/** 
 * Circuit testing for equality with zero for a bitLength-bit number (positive or negative)
 * @author ttoft
 *
 */
public class ZeroTestCircuitImpl  extends AbstractSimpleProtocol implements ZeroTestProtocol{

	// input size
	private final int bitLength;
	private final int securityParameter;

	// I/O-storage
	private final SInt input, output;

	// providers
	private final BasicNumericFactory provider;
	private	final ZeroTestReducerFactory ztrProvider;
	private final ZeroTestBruteforceFactory ztbProvider;
	
	// local stuff
	private ProtocolProducer gp = null;
	
	private SInt reduced;	
	
	public ZeroTestCircuitImpl(
			int bitLength, int securityParameter,
			SInt input, SInt output,
			ZeroTestReducerFactory ztrProvider, 
			ZeroTestBruteforceFactory ztbProvider, 
			BasicNumericFactory provider) {
		this.bitLength = bitLength;
		this.securityParameter = securityParameter;
		this.input = input;
		this.output = output;
		this.ztrProvider = ztrProvider;
		this.ztbProvider = ztbProvider;
		this.provider = provider;
	}

	@Override
	protected ProtocolProducer initializeProtocolProducer() {
		reduced = provider.getSInt();
		ProtocolProducer reducer = ztrProvider.getZeroTestReducerCircuit(
				bitLength, 
				securityParameter, 
				input, 
				reduced);
		ProtocolProducer bruteForce = ztbProvider.getZeroTestBruteforceCircuit(bitLength, reduced, output);
		gp = new SequentialProtocolProducer(reducer, bruteForce);
		return gp;
	}
}
