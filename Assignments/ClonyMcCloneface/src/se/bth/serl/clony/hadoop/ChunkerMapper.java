package se.bth.serl.clony.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import se.bth.serl.clony.HadoopDetector;
import se.bth.serl.clony.chunks.Chunk;
import se.bth.serl.clony.processors.SourceLine;
import se.bth.serl.clony.processors.SourceProcessor;
import se.bth.serl.clony.processors.SourceReader;
import se.bth.serl.clony.transformers.Hash;
import se.bth.serl.clony.transformers.TransformerChain;

public class ChunkerMapper extends Mapper<Text, Text, Text, Chunk> {
	private int chunkSize;
    private TransformerChain tchain;
    private MultipleOutputs output;
    private String tempDir;
    private String namedOutput;
	
    
    public void setup(Context context) throws IOException, InterruptedException {
    	tchain = new TransformerChain();
    	tchain.addTransformer(new Hash());
    	output = new MultipleOutputs<>(context);
    	Configuration conf = context.getConfiguration();
    	tempDir = conf.get(HadoopDetector.CONFIG_TEMPDIR);
    	namedOutput = conf.get(HadoopDetector.CONFIG_CHUNKSNAMEDOUTPUT);
    	chunkSize = conf.getInt(HadoopDetector.CONFIG_CHUNKSIZE, SourceProcessor.DEFAULT_CHUNKSIZE);
    }
    
    @Override
    public void cleanup(Context context) throws IOException, InterruptedException {
    	output.close();
    }
    
    @Override
    public void map(Text filename, Text content, Context context) throws IOException, InterruptedException {
    	SourceReader sr = new SourceReader(content.toString());
    	List<SourceLine> sourceLines = sr.getOnlySourceWithContent();
		int numLines = sourceLines.size();
		
		List<Chunk> chunks = new ArrayList<>();
		int chunkIndex = 0;
		for(int i = 0; i < numLines - chunkSize + 1; i++) {
			int startOffset = i;
			int endOffset = (i + chunkSize) < numLines ? i + chunkSize : numLines;
			
			List<String> chunkData = new ArrayList<>();
			for(int j = startOffset; j < endOffset; j++)
				chunkData.add(sourceLines.get(j).getContent());
				
			Chunk c = new Chunk(filename.toString(), 
					tchain.execute(chunkData), 
					sourceLines.get(startOffset).getLineNumber(), 
					sourceLines.get(endOffset - 1).getLineNumber());
			c.setIndex(chunkIndex);
			chunkIndex++; 
			Text chunkHashKey = new Text(Integer.toString(c.getChunkContent().hashCode())); // or c.hashCode()??
			context.write(chunkHashKey, c);
			//TODO Write the correct key/value pairs to the context
			
			chunks.add(c);
		}
		
		if(chunks.size() > 0) {
			output.write(namedOutput, new Text(chunks.get(0).getOriginId()), 
				new ChunkArrayWritable(chunks.toArray(new Chunk[chunks.size()])), tempDir + "/" + namedOutput);
		}
    }
}
