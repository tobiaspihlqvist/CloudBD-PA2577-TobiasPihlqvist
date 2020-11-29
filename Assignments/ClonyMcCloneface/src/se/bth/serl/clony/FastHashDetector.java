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

import se.bth.serl.clony.chunks.HashChunkCollection;
import se.bth.serl.clony.processors.SourceProcessor;
import se.bth.serl.clony.transformers.Hash;

/**
 * We change now how to store the chunks. Instead of using a list, they are stored in 
 * a hashtable. Chunks that have equal content are stored in the same bucket, in a list.
 * Retrieving clones of the size of one chunk are then simply retrieved by finding those
 * buckets that contain more than 1 chunks.
 * 
 * This works now with the large corpus, identifying a surprisingly large number of clones.
 * Even though the unit-tests pass, there seems to be somewhere a overflow (or other bug) since 
 * we find negative sized clones. This bug does not appear when running the detection on those 
 * specific files, so the question is, by what is it triggered. This needs some investigation.
 * 
 * Note also that stack size needs to be increased (-Xss100m) since clone expansion is a 
 * recursive function and it overflows the default stack size. 
 *  
 * Some stats (Heap size: 6144m, Stack size: 100m, Chunksize: 20)
 * 1) QualitasCorpus-20130901r/Systems/ant/ant-1.8.4/src/apache-ant-1.8.4/src
 * 	- Runtime: 3 seconds
 *  - Files: 1196
 *  - Source lines: 128219
 *  - Clones found: 50
 *  - Largest clone: 180 lines
 * 2) QualitasCorpus-20130901r/Systems/
 	- Runtime: ~5 minutes
 *  - Files: 164109
 *  - Source lines: 19195434
 *  - Clones found: 236483
 *  - Largest clone: 10838 lines
 *  
 * @author Michael Unterkalmsteiner
 *
 */
public class FastHashDetector extends DetectorBase {
		
	public FastHashDetector(Path rootFolder, int chunkSize) {
		super(rootFolder, chunkSize);
		sp = new SourceProcessor(rootFolder, chunkSize, new HashChunkCollection());
		sp.addTransformer(new Hash());
	}
	
	public static void main(String[] args) {
		Path rootFolder = new File(args[0]).toPath();
		int chunksize = Integer.parseInt(args[1]);
		
		long start = System.currentTimeMillis();
		FastHashDetector d = new FastHashDetector(rootFolder, chunksize);
		d.run();
		d.saveResults();
		System.out.println("Runtime (s): " + (System.currentTimeMillis() - start) / 1000);
	}
}
