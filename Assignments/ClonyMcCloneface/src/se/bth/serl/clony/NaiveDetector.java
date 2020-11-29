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

/**
 * The simplest form of clone detection:
 * - minimal pre-processing: comments removed and lines trimmed
 * - a file with n lines is split up in n-m+1 chunks, each consisting of m lines
 * - to detect clones, we check for each chunk if there is another chunk with the same content.
 *   When a clone is detected, we check the following chunks whether they are also clones, i.e.
 *   we expand the clone to its largest size.
 *   
 * Pros:
 * - Overall intuitive, linear logic
 * - Clone expansion is rather trivial
 * Cons:
 * - SLOW: for each chunk we need to traverse the whole list of chunks: O(n²)
 * - Memory problems with large corpora since each file is kept in memory effectively several times
 * - Does not detect all clones (run test cases to see explanation)  
 * 
 * Some stats (Heap size: 4096m, Chunksize: 20)
 * 1) QualitasCorpus-20130901r/Systems/ant/ant-1.8.4/src/apache-ant-1.8.4/src
 * 	- Runtime: ~5 minutes
 *  - Files: 1196
 *  - Source lines: 128219
 *  - Clones found: 47
 *  - Largest clone: 213 lines
 * 2) QualitasCorpus-20130901r/Systems/
 * java.lang.OutOfMemoryError: Java heap space
 * 
 * @author Michael Unterkalmsteiner
 *
 */
public class NaiveDetector extends DetectorBase {
	
	public NaiveDetector(Path rootFolder, int chunkSize) {
		super(rootFolder, chunkSize);
		sp = new SourceProcessor(rootFolder, chunkSize, new ListChunkCollection());
	}
	
	public static void main(String[] args) {
		Path rootFolder = new File(args[0]).toPath();
		int chunksize = Integer.parseInt(args[1]);
		
		long start = System.currentTimeMillis();
		NaiveDetector d = new NaiveDetector(rootFolder, chunksize);
		d.run();
		d.saveResults();
		System.out.println("Runtime (s): " + (System.currentTimeMillis() - start) / 1000);
		
	}
}
