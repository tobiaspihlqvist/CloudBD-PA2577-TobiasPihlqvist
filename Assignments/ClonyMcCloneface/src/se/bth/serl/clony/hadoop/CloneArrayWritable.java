package se.bth.serl.clony.hadoop;

import java.util.Arrays;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Writable;

import se.bth.serl.clony.chunks.Clone;

public class CloneArrayWritable extends ArrayWritable {

	public CloneArrayWritable(Writable[] values) {
		super(Clone.class, values);
	}
	
	public CloneArrayWritable() {
		super(Clone.class);
	}
	
	@Override
	public String toString() {
		return Arrays.toString(get());
	}
}
