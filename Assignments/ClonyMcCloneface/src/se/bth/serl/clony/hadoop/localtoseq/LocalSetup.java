package se.bth.serl.clony.hadoop.localtoseq;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

public class LocalSetup {
	private FileSystem fileSystem;
    private Configuration config;
   
    public LocalSetup() throws Exception {
        config = new Configuration();
 
         
        config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
 
        fileSystem = FileSystem.get(config);
        if (fileSystem.getConf() == null) {
                throw new Exception("LocalFileSystem configuration is null");
        }
    }
     
    public Configuration getConf() {
        return config;
    }
     
    public FileSystem getLocalFileSystem() {
        return fileSystem;
    }
}
