/*
 * Copyright <2017> <Blekinge Tekniska Högskola>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package se.bth.serl.clony;

import java.io.File;
import java.nio.file.Path;

import se.bth.serl.clony.chunks.ListChunkCollection;
import se.bth.serl.clony.processors.SourceProcessor;
import se.bth.serl.clony.transformers.Hash;

/**
 * Before storing the chunk data in the list, we hash it. This solves the memory
 * problem, but runtime has not improved.
 * 
 * Pros:
 * - Memory usage reduced, compared to NaiveDectector
 * Cons:
 * - Runtime is as bad as NaiveDetector
 * 
 * Some stats (Heap size: 4096m, Chunksize: 20)
 * 1) QualitasCorpus-20130901r/Systems/ant/ant-1.8.4/src/apache-ant-1.8.4/src
 * 	- Runtime: ~5 minutes
 *  - Files: 1196
 *  - Source lines: 128219
 *  - Clones found: 47
 *  - Largest clone: 213 lines
 * 2) QualitasCorpus-20130901r/Systems/
 *  - No memory problems anymore
 *  - creates ~16M chunks, takes days to compute (stopped after a couple of hours and 50k chunks done)
 * 
 * @author Michael Unterkalmsteiner
 *
 */
public class HashDetector extends DetectorBase {

	public HashDetector(Path rootFolder, int chunkSize) {
		super(rootFolder, chunkSize);
		sp = new SourceProcessor(rootFolder, chunkSize, new ListChunkCollection());
		sp.addTransformer(new Hash());
	}
	
	public static void main(String[] args) {
		Path rootFolder = new File(args[0]).toPath();
		int chunksize = Integer.parseInt(args[1]);
		
		long start = System.currentTimeMillis();
		HashDetector d = new HashDetector(rootFolder, chunksize);
		d.run();
		d.saveResults();
		System.out.println("Runtime (s): " + (System.currentTimeMillis() - start) / 1000);
	}

}
