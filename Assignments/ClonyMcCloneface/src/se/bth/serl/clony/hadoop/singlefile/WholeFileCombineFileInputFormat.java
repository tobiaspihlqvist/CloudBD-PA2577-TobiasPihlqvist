package se.bth.serl.clony.hadoop.singlefile;

/**
 * This FileInputFormat class sets the customized input split size, sets splittable as false
 * and returns record reader. This class combines multiple small files and form a input split size
 * mentioned here.
 */
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.CombineFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.CombineFileRecordReader;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;

public class WholeFileCombineFileInputFormat extends
		CombineFileInputFormat<Text, Text> {

	private static final long SPLITSIZE = 1024*1024*4; // 4 MB in bytes

	/**
	 * This method sets the splitsize to SPLITSIZE.
	 */
	public WholeFileCombineFileInputFormat() {
		super();
		setMaxSplitSize(SPLITSIZE); 

	}

	/**
	 * This method informs framework that file should not be splitted in between
	 * its boundaries.
	 */
	@Override
	protected boolean isSplitable(JobContext context, Path file) {
		return false;
	}

	/**
	 * This method calls a MultiFileRecordReader to read multiple files and
	 * returns file content to map method.
	 */
	@Override
	public RecordReader<Text, Text> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException {
		return new CombineFileRecordReader<Text, Text>(
				(CombineFileSplit) split, context, MultiFileRecordReader.class);
	}
}
