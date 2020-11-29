package se.bth.serl.clony.hadoop;

import java.util.Arrays;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Writable;

import se.bth.serl.clony.chunks.Chunk;

public class ChunkArrayWritable extends ArrayWritable {
	
	public ChunkArrayWritable(Writable[] values) {
		super(Chunk.class, values);
	}
	
	public ChunkArrayWritable() {
		super(Chunk.class);
	}
	
	@Override
	public String toString() {
		return Arrays.toString(get());
	}
}
