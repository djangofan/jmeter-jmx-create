package qa.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.control.gui.RecordController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import sun.misc.ClassLoaderUtil;

@Slf4j
public class JMeterAPISampleTest
{
    static Path jmeterPath;
    static Path workingPath;
    static Path jmeterPropertiesFile;
    static Path outputPath;
    static ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    static {
        jmeterPath = Paths.get("/opt/apache-jmeter-4.0");
        workingPath = Paths.get("").toAbsolutePath();
        outputPath = Paths.get(workingPath.resolve("output").toString());

        URL propsLocation = classLoader.getResource("jmeter.properties");
        try {
            jmeterPropertiesFile = Paths.get(propsLocation.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectory(Paths.get(outputPath.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(outputPath);
        System.out.println(jmeterPath);
        System.out.println(workingPath);
        System.out.println(Files.exists(jmeterPropertiesFile));
    }

	public static void main(String[] argv) throws Exception
    {
        if (Files.exists(jmeterPath)) {

            if (Files.exists(jmeterPropertiesFile)) {
                //JMeter Engine
                StandardJMeterEngine jmeter = new StandardJMeterEngine();

                //JMeter initialization (properties, log levels, locale, etc)
                JMeterUtils.setJMeterHome(jmeterPath.toString());
                JMeterUtils.loadJMeterProperties(jmeterPropertiesFile.toString());
                JMeterUtils.initLogging();// you can comment this line out to see extra log messages of i.e. DEBUG level
                JMeterUtils.initLocale();

                // JMeter Test Plan, basically JOrphan HashTree
                HashTree testPlanTree = new HashTree();

                // First HTTP Sampler - open uttesh.com
                HTTPSamplerProxy localhostSampler = new HTTPSamplerProxy();
                localhostSampler.setDomain("localhost");
                localhostSampler.setPort(80);
                localhostSampler.setPath("/~jausten");
                localhostSampler.setMethod("GET");
                localhostSampler.setName("Open localhost");
                localhostSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
                localhostSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());


                // Loop Controller
                LoopController loopController = new LoopController();
                loopController.setLoops(1);
                loopController.setFirst(true);
                loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
                loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
                loopController.initialize();

                RecordingController recordingController = new RecordingController();
                recordingController.setName("My Recording");


                // Thread Group
                ThreadGroup threadGroup = new ThreadGroup();
                threadGroup.setName("Sample Thread Group");
                threadGroup.setNumThreads(1);
                threadGroup.setRampUp(1);
                threadGroup.setSamplerController(loopController);
                threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
                threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
                threadGroup.addTestElement(recordingController);
                threadGroup.addTestElement(recordingController);
                threadGroup.addTestElement(recordingController);

                // Test Plan
                TestPlan testPlan = new TestPlan("Create JMeter Script From Java Code");
                testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
                testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
                testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());

                // Construct Test Plan from previously initialized elements
                testPlanTree.add(testPlan);
                //testPlanTree.add(recordingController);

                HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
                threadGroupHashTree.add(localhostSampler);
                //threadGroupHashTree.add(recordingController);

                // save generated test plan to JMeter's .jmx file format
                SaveService.saveTree(testPlanTree, new FileOutputStream(new File(outputPath.toString(), "jmeter_api_sample.jmx")));

                //add Summarizer output to get test progress in stdout like:
                // summary =      2 in   1.3s =    1.5/s Avg:   631 Min:   290 Max:   973 Err:     0 (0.00%)
                Summariser summer = null;
                String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
                if (summariserName.length() > 0) {
                    summer = new Summariser(summariserName);
                }


                // Store execution results into a .jtl file, we can save file as csv also
                String reportFile = outputPath.toString() + File.pathSeparator + "reports" + File.pathSeparator + "report.jtl";
                String csvFile = outputPath.toString() + File.pathSeparator + "reports" + File.pathSeparator + "report.csv";
                ResultCollector logger = new ResultCollector(summer);
                logger.setFilename(reportFile);
                ResultCollector csvLogger = new ResultCollector(summer);
                csvLogger.setFilename(csvFile);
                testPlanTree.add(testPlanTree.getArray()[0], logger);
                testPlanTree.add(testPlanTree.getArray()[0], csvLogger);
                // Run Test Plan
                jmeter.configure(testPlanTree);
                jmeter.run();

                System.out.println("Test completed. See " + outputPath.toString() + File.pathSeparator + "report.jtl file for results");
                System.out.println("JMeter .jmx script is available at " + outputPath.toString() + File.pathSeparator + "jmeter_api_sample.jmx");
                System.exit(0);

            }
        }

        System.err.println("workingPath or jmeterPath property is not set or pointing to incorrect location");
        System.exit(1);

    }
}