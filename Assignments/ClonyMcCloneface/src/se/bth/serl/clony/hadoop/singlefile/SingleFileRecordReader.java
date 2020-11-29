package se.bth.serl.clony.hadoop.singlefile;

/**
 * This class reads single file and returns the single file content as value to 
 * map method.
 */
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

public class SingleFileRecordReader extends RecordReader<Text, Text> {

	private LineReader in;
	private FileSplit fileSplit;
	private Configuration conf;
	private BytesWritable value;
	private boolean processed = false;
	private Path file;
	private FSDataInputStream fsDataInputStream;
	private FileSystem fileSystem;
	private byte[] contents;

	public void initialize(InputSplit genericSplit, TaskAttemptContext context)
			throws IOException {
		fileSplit = (FileSplit) genericSplit;
		conf = context.getConfiguration();

	}

	public boolean nextKeyValue() throws IOException {
		if (!processed) {
			contents = new byte[(int) fileSplit.getLength()];
			file = fileSplit.getPath();
			value = new BytesWritable();
			fileSystem = file.getFileSystem(conf);
			fsDataInputStream = null;
			try {
				this.fsDataInputStream = this.fileSystem.open(file);
				IOUtils.readFully(this.fsDataInputStream, contents, 0,
						contents.length);
				value.set(contents, 0, contents.length);
			} finally {
				IOUtils.closeStream(in);
			}
			processed = true;
			return true;
		}
		return false;
	}

	/**
	 * This returns the key of the current record which is a current file path.
	 */
	@Override
	public Text getCurrentKey() {
		return new Text(file.toString());
	}

	/**
	 * This passes the current values of record which is a file content.
	 */
	@Override
	public Text getCurrentValue() {
		Text fileContent = new Text(new String(value.getBytes()).replaceAll(
				"\u0000", ""));
		return fileContent;
	}

	/**
	 * Get the progress within the split
	 */
	public float getProgress() throws IOException {
		return processed ? 1.0f : 0.0f;
	}

	public synchronized void close() throws IOException {

		if (in != null) {
			in.close();
		}
	}
}
