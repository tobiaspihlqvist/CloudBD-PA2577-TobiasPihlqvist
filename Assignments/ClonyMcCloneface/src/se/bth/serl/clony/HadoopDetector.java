package se.bth.serl.clony;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import se.bth.serl.clony.chunks.Chunk;
import se.bth.serl.clony.chunks.Clone;
import se.bth.serl.clony.hadoop.ChunkArrayWritable;
import se.bth.serl.clony.hadoop.ChunkerMapper;
import se.bth.serl.clony.hadoop.ChunkerReducer;
import se.bth.serl.clony.hadoop.ExpanderMapper;
import se.bth.serl.clony.hadoop.ExpanderReducer;

public class HadoopDetector extends Configured implements Tool {
	public static final String CONFIG_CHUNKSIZE = "chunkSize";
	public static final String CONFIG_OUTPUTDIR = "outputDir";
	public static final String CONFIG_TEMPDIR = "tempDir";
	public static final String CONFIG_CHUNKSNAMEDOUTPUT = "unindexedChunks";
	public static final String CONFIG_LOOKUPBYFILEDIR = "lookupbyfiledir";
	public static final String CONFIG_SUCCESSMARKER = "mapreduce.fileoutputcommitter.marksuccessfuljobs";
	public static final String CONFIG_SPLITSIZE = "mapred.max.split.size";
	

	@Override
	public int run(String[] args) throws Exception {
		Path input = new Path(args[0]);
		Path output = new Path(args[1]);
		Path outputChunker = new Path(output, "chunker");
		Path outputIndexer = new Path(output, "indexer");
		Path outputExpander = new Path(output, "expander");
		
		Job	chunkingJob = Job.getInstance();
		chunkingJob.setJobName("JobChaining-Chunking");
        chunkingJob.setJarByClass(HadoopDetector.class);
		
		Configuration chunkingConf = chunkingJob.getConfiguration();
        chunkingConf.setInt(CONFIG_CHUNKSIZE, Integer.parseInt(args[2]));
        chunkingConf.set(CONFIG_TEMPDIR, "temp");
        chunkingConf.set(CONFIG_CHUNKSNAMEDOUTPUT, "tmp");
        chunkingConf.set(CONFIG_OUTPUTDIR, output.getName());
        //We read in the files in the third job, so the file "_SUCCESS" is just in the way. So we don't put it.
		chunkingConf.set(CONFIG_SUCCESSMARKER, "false");
		
		//Controls the split size for the chunker and hence the number of mappers.
    	chunkingConf.set(CONFIG_SPLITSIZE, new Integer(1024*1024*10).toString());
		
		                
        chunkingJob.setInputFormatClass(SequenceFileInputFormat.class);
        chunkingJob.setOutputFormatClass(SequenceFileOutputFormat.class);
        FileInputFormat.addInputPath(chunkingJob, input);
		FileOutputFormat.setOutputPath(chunkingJob, outputChunker);
        
        chunkingJob.setMapperClass(ChunkerMapper.class);
        chunkingJob.setReducerClass(ChunkerReducer.class);
        
        //Mapper output
        chunkingJob.setMapOutputKeyClass(Text.class);
        chunkingJob.setMapOutputValueClass(Chunk.class);
        
        //Reducer output
        chunkingJob.setOutputKeyClass(Text.class);
        chunkingJob.setOutputValueClass(ChunkArrayWritable.class);
        
        //Secondary output produced in the mapper
        MultipleOutputs.addNamedOutput(chunkingJob, chunkingConf.get(CONFIG_CHUNKSNAMEDOUTPUT), 
        		SequenceFileOutputFormat.class, Text.class, ChunkArrayWritable.class);
        
        
        Path tempDir = new Path(outputChunker, chunkingConf.get(CONFIG_TEMPDIR));
		int exitcode = chunkingJob.waitForCompletion(true) ? 0 : 1;
		if(exitcode == 0) {			
			Job indexingJob = Job.getInstance();
			indexingJob.setJobName("JobChaining-Indexing");
			indexingJob.setJarByClass(HadoopDetector.class);
			
			Configuration indexingConf = indexingJob.getConfiguration();
			//We read in the files in the third job, so the file "_SUCCESS" is just in the way. So we don't put it.
			indexingConf.set(CONFIG_SUCCESSMARKER, "false");
			
			SequenceFileInputFormat.addInputPath(indexingJob, tempDir);
			indexingJob.setInputFormatClass(SequenceFileInputFormat.class);
			indexingJob.setOutputKeyClass(Text.class);
			indexingJob.setOutputValueClass(ChunkArrayWritable.class);
			indexingJob.setOutputFormatClass(MapFileOutputFormat.class);
			
			MapFileOutputFormat.setOutputPath(indexingJob, outputIndexer);
	        SequenceFileOutputFormat.setCompressOutput(indexingJob, true);
	        SequenceFileOutputFormat.setOutputCompressorClass(indexingJob, GzipCodec.class);
	        SequenceFileOutputFormat.setOutputCompressionType(indexingJob, CompressionType.BLOCK);
	        
	        exitcode = indexingJob.waitForCompletion(true) ? 0 : 1;
	        
	        if(exitcode == 0) {
	        	Job expandingJob = Job.getInstance();
	        	expandingJob.setJobName("JobChaining-Expanding");
	        	expandingJob.setJarByClass(HadoopDetector.class);
	        	
	        	Configuration expandingConf = expandingJob.getConfiguration();
	        	expandingConf.set(CONFIG_LOOKUPBYFILEDIR, outputIndexer.toString());
	        	//Controls the split size for the expander and hence the number of mappers.
	        	expandingConf.set(CONFIG_SPLITSIZE, new Integer(1024*1024*20).toString());
	        	
	        	expandingJob.setInputFormatClass(SequenceFileInputFormat.class);
	        	SequenceFileInputFormat.addInputPath(expandingJob, new Path(output, "chunker/part-r-00000"));
	        	
	        	expandingJob.setOutputFormatClass(TextOutputFormat.class);
	        	TextOutputFormat.setOutputPath(expandingJob, outputExpander);
	        	
	        	expandingJob.setPartitionerClass(HashPartitioner.class);
	        	expandingJob.setMapperClass(ExpanderMapper.class);
	        	expandingJob.setMapOutputKeyClass(Clone.class);
	        	expandingJob.setMapOutputValueClass(NullWritable.class);
	        	expandingJob.setReducerClass(ExpanderReducer.class);
	        	expandingJob.setOutputKeyClass(Clone.class);
	        	expandingJob.setOutputValueClass(NullWritable.class);
	        	
	        	exitcode = expandingJob.waitForCompletion(true) ? 0 : 1;
	       }
		}
	
		FileSystem.get(chunkingConf).delete(tempDir, true);
		
		return exitcode;
	}

	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new HadoopDetector(), args);
		System.exit(exitCode);
	}

}
