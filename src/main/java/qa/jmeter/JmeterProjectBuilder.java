package qa.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.control.gui.RecordController;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.proxy.gui.ProxyControlGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JmeterProjectBuilder {

    private final Path workingPath;
    private final Path outputPath;

    // tmp variable
    private final String WWW_USERNAME = "~jausten";

    JmeterProjectBuilder() {
        workingPath = getProjectPath();
        outputPath = Paths.get(workingPath.toString(), "output");
        System.out.println(getWorkingPath());
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

    public FileOutputStream getFileOutputStream(String targetFile) throws FileNotFoundException {
        File jmxOutDir = new File(outputPath.toString());
        jmxOutDir.mkdirs();
        File jmxFile = new File(outputPath.toString(), targetFile);
        return new FileOutputStream(jmxFile);
    }

    public Path getWorkingPath() {
        return workingPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public HashTree generateJmeterProject(String jmxFileName)
    {
        JMeterUtils.setJMeterHome(JmeterProgramHelper.getJmeterPath().toString());
        JMeterUtils.loadJMeterProperties(JmeterProgramHelper.getJmeterPropertiesFile().toString());
        JMeterUtils.initLogging(); // comment this line out to see extra log messages
        JMeterUtils.initLocale();

        // org.apache.jorphan.collections.HashTree
        HashTree testPlanTree = new HashTree();

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

        testPlanTree.add(testPlan);
        testPlanTree.add(proxyController);

        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(fooSampler);
        threadGroupHashTree.add(barSampler);
        threadGroupHashTree.add(recordingController);

        try {
            SaveService.saveTree(testPlanTree, getFileOutputStream(jmxFileName));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("JMeter .jmx script is available at " + outputPath.toString() + File.pathSeparator + jmxFileName);

        return testPlanTree;
    }

}
