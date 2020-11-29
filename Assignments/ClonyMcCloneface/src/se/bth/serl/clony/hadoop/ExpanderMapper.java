package se.bth.serl.clony.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapFile.Reader;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;

import se.bth.serl.clony.HadoopDetector;
import se.bth.serl.clony.chunks.Chunk;
import se.bth.serl.clony.chunks.Clone;
import se.bth.serl.clony.chunks.HashChunkCollection;

public class ExpanderMapper extends Mapper<Text, ChunkArrayWritable, Clone, NullWritable> {
	private Reader readers[];
	
	@Override
	public void setup(Context context) throws IOException, InterruptedException {
		Path path = new Path(context.getConfiguration().get(HadoopDetector.CONFIG_LOOKUPBYFILEDIR));
		readers = MapFileOutputFormat.getReaders(path, context.getConfiguration());
	}
	
	@Override
	public void map(Text key, ChunkArrayWritable value, Context context) throws IOException, InterruptedException {
		Set<String> alreadyReadFiles = new HashSet<>();
		HashChunkCollection chunks = new HashChunkCollection();
		
		//Deserialize the Chunk data and read in the chunks from the associated files
		Writable[] instancesArray = value.get();
		List<LinkedList<Chunk>> listOfInstances = new ArrayList<>();
		for(int i = 0; i < instancesArray.length; i++) {
			Chunk instanceChunk = (Chunk) instancesArray[i];
			LinkedList<Chunk> l = new LinkedList<>();
			l.add(instanceChunk);
			listOfInstances.add(l);
			
			String file = instanceChunk.getOriginId();
			
			if(alreadyReadFiles.add(file)) {
				ChunkArrayWritable chunkListAW = new ChunkArrayWritable();
				Partitioner<Text, ChunkArrayWritable> partitioner = new HashPartitioner<>();
				Reader reader = readers[partitioner.getPartition(new Text(file), chunkListAW, readers.length)];
				Writable entry = reader.get(new Text(file), chunkListAW);
				
				if (entry != null) {
					chunkListAW = (ChunkArrayWritable) entry; 
					Writable[] chunkListA = chunkListAW.get();
					for(int j = 0; j < chunkListA.length; j++) {
						chunks.addChunk((Chunk) chunkListA[j]);
					}
				}
			}
		}
		
		listOfInstances = chunks.expand(listOfInstances, -1);
		Clone expandedClone = new Clone(listOfInstances);
		context.write(expandedClone, NullWritable.get());
		//TODO Write the correct key/value pairs to the context
	}
}
