package qa.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.control.gui.RecordController;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.proxy.gui.ProxyControlGui;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class JMeterAPISampleTest
{
    static Path jmeterPath;
    static Path workingPath;
    static Path jmeterPropertiesFile;
    static Path outputPath;
    static ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    static final String WWW_USERNAME = "~jausten";

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

                StandardJMeterEngine jmeter = new StandardJMeterEngine();

                JMeterUtils.setJMeterHome(jmeterPath.toString());
                JMeterUtils.loadJMeterProperties(jmeterPropertiesFile.toString());
                JMeterUtils.initLogging(); // comment this line out to see extra log messages
                JMeterUtils.initLocale();

                org.apache.jorphan.collections.HashTree testPlanTree = new HashTree();

                HTTPSamplerProxy fooSampler = new HTTPSamplerProxy();
                fooSampler.setDomain("localhost");
                fooSampler.setPort(80);
                fooSampler.setPath("/" + WWW_USERNAME + "/foo");
                fooSampler.setMethod("GET");
                fooSampler.setName("Open localhost foo");
                fooSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
                fooSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

                HTTPSamplerProxy barSampler = new HTTPSamplerProxy();
                barSampler.setDomain("localhost");
                barSampler.setPort(80);
                barSampler.setPath("/" + WWW_USERNAME + "/bar");
                barSampler.setMethod("GET");
                barSampler.setName("Open localhost bar");
                barSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
                barSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

                LoopController loopController = new LoopController();
                loopController.setLoops(2);
                loopController.setFirst(true);
                loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
                loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
                loopController.initialize();

                RecordingController recordingController = new RecordingController();
                recordingController.setName("Recordings");
                recordingController.setProperty(TestElement.TEST_CLASS, RecordController.class.getName());
                recordingController.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());

                ProxyControl proxyController = new ProxyControl();
                proxyController.setName("Proxy Recorder");
                proxyController.setProperty(TestElement.TEST_CLASS, ProxyControl.class.getName());
                proxyController.setProperty(TestElement.GUI_CLASS, ProxyControlGui.class.getName());

                ThreadGroup threadGroup = new ThreadGroup();
                threadGroup.setName("Sample Thread Group");
                threadGroup.setNumThreads(1);
                threadGroup.setRampUp(1);
                threadGroup.setSamplerController(loopController);
                threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
                threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());

                TestPlan testPlan = new TestPlan("Create JMeter Script From Java Code");
                testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
                testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
                testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());

                // Construct primary 'Test Plan' from previously initialized elements
                testPlanTree.add(testPlan);
                testPlanTree.add(proxyController);

                HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
                threadGroupHashTree.add(fooSampler);
                threadGroupHashTree.add(barSampler);
                threadGroupHashTree.add(recordingController);

                File jmxOutDir = new File(outputPath.toString());
                jmxOutDir.mkdirs();
                File jmxFile = new File(outputPath.toString(), "jmeter_api_sample.jmx");
                SaveService.saveTree(testPlanTree, new FileOutputStream(jmxFile));

                Summariser summer = null;
                String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
                if (summariserName.length() > 0) {
                    summer = new Summariser(summariserName);
                }


                // Store execution results into a .jtl file, we can save file as csv also
                File reportDir = new File(workingPath.toString(),"reports");
                reportDir.mkdirs();
                File reportFile = new File(reportDir.toString() ,"report.jtl");
                File csvFile = new File(reportDir.toString(), "report.csv");
                ResultCollector logger = new ResultCollector(summer);
                logger.setFilename(reportFile.getAbsolutePath());
                ResultCollector csvLogger = new ResultCollector(summer);
                csvLogger.setFilename(csvFile.getAbsolutePath());
                testPlanTree.add(testPlanTree.getArray()[0], logger);
                testPlanTree.add(testPlanTree.getArray()[0], csvLogger);
                // Run Test Plan
                jmeter.configure(testPlanTree);
                jmeter.run();

                System.out.println("Test completed. See " + reportDir.toString() + File.pathSeparator + "report.jtl file for results");
                System.out.println("JMeter .jmx script is available at " + outputPath.toString() + File.pathSeparator + "jmeter_api_sample.jmx");
                System.exit(0);

            }
        }

        System.err.println("workingPath or jmeterPath property is not set or pointing to incorrect location");
        System.exit(1);

    }
}