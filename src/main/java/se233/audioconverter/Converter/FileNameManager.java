package se233.audioconverter.Converter;

import java.io.File;

public class FileNameManager {

    public File chname(String path , int type){
        File sourceFile = new File(path);
        String[] newExtension = {"mp3" ,"wav","m4a","flac"};
        String originalFileName = sourceFile.getName();
        String baseName = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            baseName = originalFileName.substring(0, lastDotIndex);
        } else {
            baseName = originalFileName;
        }
        String parentDirectory = sourceFile.getParent();

        String newFilePath = parentDirectory + File.separator + baseName + "." + newExtension[type];

        File targetFile = new File(newFilePath);
        System.out.println(targetFile.getPath());
        return  targetFile;
    }
}
