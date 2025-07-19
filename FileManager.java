package Locker_app;
import java.io.*;
import java.nio.file.*;

public class FileManager {
private static final String LOCKER_DIR = "locker";

static {
    File locker = new File(LOCKER_DIR);
    if(!locker.exists()){
        locker.mkdir();
    }
}
public static boolean saveFile(File file){
    try{
        Path destination = Paths.get(LOCKER_DIR,file.getName());
        Files.copy(file.toPath(),destination,StandardCopyOption.REPLACE_EXISTING);
        return true;
    }catch(IOException e){
        System.out.println("File saving failed:" + e.getMessage());
        return false;
    }
}
}
