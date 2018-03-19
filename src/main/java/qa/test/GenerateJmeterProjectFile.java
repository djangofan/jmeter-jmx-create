package qa.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.jorphan.collections.HashTree;

import java.nio.file.Files;

@Slf4j
public class GenerateJmeterProjectFile
{
    private static final String jmxFileName = "jmeter_api_sample.jmx";

	public static void main(String[] argv)
    {
        JmeterProjectBuilder jmeterProjectBuilder = new JmeterProjectBuilder();

        if (Files.exists(JmeterProgramHelper.getJmeterPath()))
        {
            if (Files.exists(JmeterProgramHelper.getJmeterPropertiesFile()))
            {
                HashTree testPlanTree = jmeterProjectBuilder.generateJmeterProject(jmxFileName);
                JmeterProgramHelper.executeJmeterProject(testPlanTree, jmeterProjectBuilder.getWorkingPath()); // will exit 0
            }
        }

        System.err.println("Property for workingPath or jmeterPath is not set or it may be pointing to incorrect location.");
        System.exit(1);
    }
}