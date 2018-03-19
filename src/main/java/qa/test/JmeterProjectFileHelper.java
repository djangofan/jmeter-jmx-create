package qa.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JmeterProjectFileHelper {

    private static Path workingPath;
    private static Path outputPath;

    static {
        workingPath = getProjectPath();
        System.out.println(JmeterProjectFileHelper.getWorkingPath());
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectory(Paths.get(outputPath.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(outputPath);
    }

    private static Path getProjectPath() {
        return Paths.get(new File(".").getAbsolutePath());
    }

    public static OutputStream getOutputStream(File targetFile) {
        return null;
    }

    public static Path getWorkingPath() {
        return workingPath;
    }
    public static Path getOutputPath() {
        return outputPath;
    }

}
