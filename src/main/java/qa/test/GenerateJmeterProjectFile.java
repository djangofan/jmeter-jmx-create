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
import java.nio.file.Files;

@Slf4j
public class GenerateJmeterProjectFile
{
    static final String WWW_USERNAME = "~jausten";

	public static void main(String[] argv) throws Exception
    {
        if (Files.exists(JmeterProgramHelper.getJmeterPath())) {

            if (Files.exists(JmeterProgramHelper.getJmeterPropertiesFile())) {

                StandardJMeterEngine jmeter = new StandardJMeterEngine();

                JMeterUtils.setJMeterHome(JmeterProgramHelper.getJmeterPath().toString());
                JMeterUtils.loadJMeterProperties(JmeterProgramHelper.getJmeterPropertiesFile().toString());
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

                File jmxOutDir = new File(JmeterProjectFileHelper.getOutputPath().toString());
                jmxOutDir.mkdirs();
                File jmxFile = new File(JmeterProjectFileHelper.getOutputPath().toString(), "jmeter_api_sample.jmx");
                SaveService.saveTree(testPlanTree, new FileOutputStream(jmxFile));

                Summariser summer = null;
                String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
                if (summariserName.length() > 0) {
                    summer = new Summariser(summariserName);
                }


                // Store execution results into a .jtl file, we can save file as csv also
                File reportDir = new File(JmeterProjectFileHelper.getWorkingPath().toString(),"reports");
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
                System.out.println("JMeter .jmx script is available at " + JmeterProjectFileHelper.getOutputPath().toString() + File.pathSeparator + "jmeter_api_sample.jmx");
                System.exit(0);

            }
        }

        System.err.println("workingPath or jmeterPath property is not set or pointing to incorrect location");
        System.exit(1);

    }
}