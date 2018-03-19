package qa.jmeter;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.File;
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

    public static void executeJmeterProject(HashTree testPlanTree, Path workingDir)
    {
        StandardJMeterEngine jmeter = new StandardJMeterEngine();

        Summariser summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (summariserName.length() > 0) {
            summer = new Summariser(summariserName);
        }

        File reportDir = new File(workingDir.toString(),"reports");
        reportDir.mkdirs();
        File reportFile = new File(reportDir.toString() ,"report.jtl");
        File csvFile = new File(reportDir.toString(), "report.csv");
        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(reportFile.getAbsolutePath());
        ResultCollector csvLogger = new ResultCollector(summer);
        csvLogger.setFilename(csvFile.getAbsolutePath());
        testPlanTree.add(testPlanTree.getArray()[0], logger);
        testPlanTree.add(testPlanTree.getArray()[0], csvLogger);

        jmeter.configure(testPlanTree);
        jmeter.run();

        System.out.println("Test completed. See " + reportDir.toString() + File.pathSeparator + "report.jtl file for results");
        System.exit(0);
    }


}
