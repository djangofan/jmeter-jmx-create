package qa.test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JmeterProgramHelper {

    public static Path getJmeterPath() {
        return jmeterPath;
    }

    public static Path getJmeterPropertiesFile() {
        return jmeterPropertiesFile;
    }

    private static Path jmeterPath;
    private static Path jmeterPropertiesFile;

    private static ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    static {
        jmeterPath = Paths.get("/opt/apache-jmeter-4.0");
        URL propsLocation = classLoader.getResource("jmeter.properties");
        try {
            jmeterPropertiesFile = Paths.get(propsLocation.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        System.out.println(jmeterPath);
        System.out.println(Files.exists(jmeterPropertiesFile));
    }

}
