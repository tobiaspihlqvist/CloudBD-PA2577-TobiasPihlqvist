package se.bth.serl.clony.hadoop.localtoseq;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
 
/**
 * Inspiration from http://rajkrrsingh.blogspot.se/2013/10/hadoop-merging-small-tar-files-to.html
 * @author Michael Unterkalmsteiner
 *
 */
public class LocalToSeqFile {
    private File inputDir;
    private File outputFile;
    private LocalSetup setup;
     
    public LocalToSeqFile() throws Exception {
        setup = new LocalSetup();
    }
 
     
    public void setInput(File inputDir) {
        this.inputDir = inputDir;
    }
 
    public void setOutput(File outputFile) {
        this.outputFile = outputFile;
    }
 
    public void execute() throws Exception {
        List<java.nio.file.Path> inputFiles = null;
        SequenceFile.Writer output = null;
        try {
            inputFiles = Files.walk(inputDir.toPath(), Integer.MAX_VALUE).
            		filter(Files::isRegularFile).filter(p -> p.toString().
            				endsWith(".java")).collect(Collectors.toList());
            output = openOutputFile();
            
            for(java.nio.file.Path f : inputFiles) {
            	byte[] data = Files.readAllBytes(f);
                
                Text key = new Text(f.toFile().getAbsolutePath());
                Text value = new Text(new String(data));
                output.append(key, value);
            }
        } finally {
            if (output != null) { output.close(); }
        }
    }
 
    private SequenceFile.Writer openOutputFile() throws Exception {
        Path outputPath = new Path(outputFile.getAbsolutePath());
        return SequenceFile.createWriter(setup.getLocalFileSystem(), setup.getConf(),
                                         outputPath,
                                         Text.class, Text.class,
                                         SequenceFile.CompressionType.BLOCK);
    }
 
    public static void main(String[] args) {
        if (args.length != 2) {
            exitWithHelp();
        }
 
        try {
            LocalToSeqFile me = new LocalToSeqFile();
            me.setInput(new File(args[0]));
            me.setOutput(new File(args[1]));
            me.execute();
        } catch (Exception e) {
            e.printStackTrace();
            exitWithHelp();
        }
    }
 
    public static void exitWithHelp() {
        System.err.println("Usage:  LocalToSeqFile <inputDir> <outputFile>\n");
        System.exit(1);
    }
}