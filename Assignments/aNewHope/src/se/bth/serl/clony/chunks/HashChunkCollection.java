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

package se.bth.serl.clony.chunks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * 
 * @author Michael Unterkalmsteiner
 *
 */
public class HashChunkCollection extends BaseChunkCollection {
	private Map<String, List<Chunk>> chunksByFile;
	private Map<String, List<Chunk>> chunksByHash;
	
	public HashChunkCollection() {
		chunksByFile = new HashMap<>();
		chunksByHash = new HashMap<>();
	}

	@Override
	public void addChunk(Chunk c) {	
		String fileKey = c.getOriginId();

		if(chunksByFile.get(fileKey)== null)
		{
			List<Chunk> newChunkList = new ArrayList<Chunk>();
			newChunkList.add(c);
			chunksByFile.put(fileKey, newChunkList);
		}
		else {
			List<Chunk> ExistingChunkList = chunksByFile.get(fileKey);
			ExistingChunkList.add(c);
			chunksByFile.put(fileKey, ExistingChunkList);
		}
		
		String hashKey = c.getChunkContent();
		if(chunksByHash.get(hashKey)== null)
		{
			List<Chunk> newChunkList = new ArrayList<Chunk>();
			newChunkList.add(c);
			chunksByHash.put(hashKey, newChunkList);
		}
		else {
			List<Chunk> ExistingChunkList = chunksByHash.get(hashKey);
			ExistingChunkList.add(c);
			chunksByHash.put(hashKey, ExistingChunkList);
		}
		// VI vill ha en nyckel samt en lista med en chunk i. f;r att kunna l'gga till hashmapen.
		
		//TODO Chunks need to be added to both hashmaps: chunksByHash and chunksByFile
	}
	
	/**
	 * The detection algorithm here is somewhat straightforward and not optimized for 
	 * minimum operations, i.e. expansions. This is a tradeoff so the code remains understandable.
	 * 
	 * Each entry in the hash table with size>1 is a potential clone. We expand the clone (by step 1 chunk)
	 * first backwards and then forwards. Once this expansion is finished, we add the clone to a SortedSet.
	 * Since a set can only contain unique objects, equivalent clones are added only once to the set. 
	 * The Clone class needs to implement the Comparable interface.
	 * This algorithm is computationally more expensive than pre-emptively identifying unique clones, but
	 * it makes the detection rather easy as the work is done by the set.
	 */
	@Override
	public SortedSet<Clone> getClones() {
		if(clones.isEmpty()) {
			List<Map.Entry<String, List<Chunk>>> dup = chunksByHash.entrySet().stream().filter(p -> p.getValue().size() > 1).collect(Collectors.toList());
			
			int counter = 1;
			for(Map.Entry<String, List<Chunk>> entry : dup) {
				List<Chunk> dupes = entry.getValue();
				List<LinkedList<Chunk>> listOfInstances = new ArrayList<>();
				for(Chunk c : dupes) {
					LinkedList<Chunk> instance = new LinkedList<>();
					instance.add(c);
					listOfInstances.add(instance);
				}
				
				listOfInstances = expand(listOfInstances, -1);
				
				//If the new clone is not yet in the set, it's added.  
				clones.add(new Clone(listOfInstances));
				
				System.out.println("Chunks processed: " + counter + "/" + dup.size());
				counter++;
			}
		}
		
		return clones;
	}
	
	/**
	 * With large datasets, this recursion can cause a stack overflow. Java 8 does not support
	 * tail-call recursion (see http://softwareengineering.stackexchange.com/q/272061), 
	 * so this recursion is not optimized. The only options are to increase the stack size 
	 * to 10MB (-Xss10m) or to rewrite this as an iterative method. 
	 */
	private List<LinkedList<Chunk>> expand(List<LinkedList<Chunk>> listOfInstances, int step) {
		if(step < 0) { //expanding backwards
			Chunk[] expandedChunk = new Chunk[listOfInstances.size()];
			String expandedChunkContent = null;
			for(int i = 0; i < listOfInstances.size(); i++) { 
				LinkedList<Chunk> instance = listOfInstances.get(i);
				int previous = instance.getFirst().getIndex() - 1;
				if(previous < 0) // we are at the first chunk, no further backward expansion possible
					return expand(listOfInstances, 1);
				else {
					List<Chunk> c = chunksByFile.get(instance.getFirst().getOriginId());
					Chunk candidate = c.get(previous);
					if(i == 0) { //first instance we simply add
						expandedChunkContent = candidate.getChunkContent();
						expandedChunk[i] = candidate;
					}
					else {
						//if the following instances have the same chunk content, add the chunk...
						if(expandedChunkContent.equals(candidate.getChunkContent()))
							expandedChunk[i] = candidate;
						else //...otherwise continue with forward expansion 
							return expand(listOfInstances, 1);
					}
				}
			}
			
			for(int i = 0; i < listOfInstances.size(); i++)
				listOfInstances.get(i).addFirst(expandedChunk[i]);
			System.out.println("backward " + step);

			//Continue with backward expansion
			return expand(listOfInstances, step);
		}
		else { //expanding forwards
			System.out.println("forward " + step);
			Chunk[] expandedChunk = new Chunk[listOfInstances.size()];
			String expandedChunkContent = null;
			for(int i = 0; i < listOfInstances.size(); i++) { 
				LinkedList<Chunk> instance = listOfInstances.get(i);
				int next = instance.getLast().getIndex() + 1;
				LinkedList<Chunk> lastChunk = listOfInstances.get(listOfInstances.size() - 1);
				
				if( next == lastChunk.getLast().getIndex() + 1) // last chunk in instance
				{		
					System.out.println(next + "listof instances size " + listOfInstances.size());
					System.out.println("we are out");
					return listOfInstances; //
				}
				else {
					List<Chunk> c = chunksByFile.get(instance.getLast().getOriginId());
					System.out.println("kmr vi innan kandidaten");

					Chunk candidate = c.get(next);
					System.out.println("elr efter");

					if(i == 0) { //first instance we simply add
						expandedChunkContent = candidate.getChunkContent();
						expandedChunk[i] = candidate;
					}
					else {
						//if the following instances have the same chunk content, add the chunk...
						if(expandedChunkContent.equals(candidate.getChunkContent()))
							expandedChunk[i] = candidate;
						else //...otherwise continue with forward expansion 
							return listOfInstances;
					}
				}
			}
			
			for(int i = 0; i < listOfInstances.size(); i++)
				listOfInstances.get(i).addLast(expandedChunk[i]);
				return expand(listOfInstances, step);

			//TODO the implementation here should mirror the backwards expansion
		}
	}

	@Override
	public boolean isEmpty() {
		return chunksByHash.isEmpty();
	}

	@Override
	public List<Chunk> getChunks() {
		List<Chunk> c = new ArrayList<>();
		for(Map.Entry<String, List<Chunk>> entry : chunksByHash.entrySet()) 
			c.addAll(entry.getValue());
		
		return c;
	}
}
