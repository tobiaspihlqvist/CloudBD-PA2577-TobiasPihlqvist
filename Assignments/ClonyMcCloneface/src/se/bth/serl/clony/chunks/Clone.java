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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import se.bth.serl.clony.hadoop.InstanceArrayWritable;

/**
 * A clone instance consists of a list of consecutive chunks. A clone consists of 2 or more clone instances.
 * Since the expansion algorithm can create multiple equals clones, we implement here the comparable interface 
 * which allows us to add clones to a set, effectively storing only unique clones.  
 * @author Michael Unterkalmsteiner
 *
 */
public class Clone implements WritableComparable<Clone> {
	private Set<CloneInstance> instances = new TreeSet<>();
	
	public Clone(List<LinkedList<Chunk>> listOfInstances) {
		listOfInstances.forEach( entry -> instances.add(new CloneInstance(entry)));
	}
	
	public Clone() {}
	
	public boolean isConsistent() {
		return instances.stream().allMatch(p -> p.getLength() == size());
	}
	
	public int instances() {
		return instances.size();
	}
	
	public int size() {
		int size = 0;
		
		for(CloneInstance i : instances) {
			size = i.getLength();
			break;
		}
		
		return size;
	}
	
	/**
	 * Its crucial for the detection that this class implements compareTo() and equals()
	 * consistently (see http://stackoverflow.com/questions/7229977/java-why-it-is-implied-that-objects-are-equal-if-compareto-returns-0),
	 * that is, (x.compareTo(y)==0) == (x.equals(y)) 
	 */
	@Override
	public int compareTo(Clone o) {
		if(this.equals(o))
			return 0;
		
		int cmp = o.size() - this.size();
		if(cmp != 0)
			return cmp;
		
		
		Iterator<CloneInstance> thisI = instances.iterator();
		Iterator<CloneInstance> otherI = o.instances.iterator();
		
		while (thisI.hasNext() && otherI.hasNext()) {
			cmp = 0;
			CloneInstance thisCloneInstance = thisI.next();
			CloneInstance otherCloneInstance = otherI.next();
			cmp = thisCloneInstance.compareTo(otherCloneInstance);
			if(cmp != 0)
				return cmp;
		}
		
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!(obj instanceof Clone))
			return false;
		
		Clone other = (Clone) obj;
		return other.instances.equals(instances);
	}
	
	@Override
	public int hashCode() {
		return 31 * instances.hashCode();
	}
	
	public String getCloneInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("###Clone size: ").append(size()).append("###\n");
		
		for(CloneInstance i : instances) {
			sb.append(i.toString());			
		}
		sb.append("\n");
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return getCloneInfo();
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		instances.clear();
		InstanceArrayWritable c = new InstanceArrayWritable();
		c.readFields(in);
		Writable[] instancesArray = c.get();
		for(int i = 0; i < instancesArray.length; i++) {
			instances.add((CloneInstance) instancesArray[i]);
		} 
	}

	@Override
	public void write(DataOutput out) throws IOException {
		InstanceArrayWritable c = new InstanceArrayWritable(instances.toArray(new CloneInstance[instances.size()]));
		c.write(out); 
	}
}
