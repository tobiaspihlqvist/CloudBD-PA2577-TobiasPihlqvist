package se.bth.serl.clony.hadoop;

import java.util.Arrays;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Writable;

import se.bth.serl.clony.chunks.CloneInstance;

public class InstanceArrayWritable extends ArrayWritable {

	public InstanceArrayWritable(Writable[] values) {
		super(CloneInstance.class, values);
	}
	
	public InstanceArrayWritable() {
		super(CloneInstance.class);
	}
	
	@Override
	public String toString() {
		return Arrays.toString(get());
	}

}
