package se.bth.serl.clony.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;
import se.bth.serl.clony.chunks.Clone;

public class ExpanderReducer extends Reducer<Clone, NullWritable, Clone, NullWritable> {

	//TODO implement the reduce() method
	public void reduce(Clone clone, NullWritable value, Context context) throws IOException, InterruptedException {
		context.write(clone, value.get());
	}
}
