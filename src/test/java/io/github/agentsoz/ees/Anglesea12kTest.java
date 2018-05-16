package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
//@Ignore
public class Anglesea12kTest {
    // have tests in separate classes so that they run, at least und    er maven, in separate JVMs.  kai, nov'17

    Logger log = Logger.getLogger(Anglesea12kTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testAnglesea12k() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/anglesea-12k/scenario_main.xml",
                "--logfile", "scenarios/surf-coast-shire/anglesea-12k/scenario.log",
                "--loglevel", "INFO",
                //	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
                "--seed", "12345",
                "--safeline-output-file-pattern", "scenarios/surf-coast-shire/anglesea-12k/safeline.%d%.out",
                MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:12052}]," +
                "logLevel: WARN," +
                "logFile: \"scenarios/surf-coast-shire/anglesea-12k/jill.log\"," +
                "programOutputFile: \"scenarios/surf-coast-shire/anglesea-12k/jill.out\"," +
                "randomSeed: 12345," + // jill random seed
                "numThreads: 1" + // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "100000:100000", // virtually disallow congestion re-routing (painfully slow otherwise!)
                "--x-messages-file", "scenarios/surf-coast-shire/anglesea-12k/scenario_messages.json",
        };

        Main.main(args);

        final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
        long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
        System.err.println("actual(events)="+actualEventsCRC) ;

        long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
        System.err.println("actual(plans)="+actualPlansCRC) ;

        // ---

        final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";

        // ---

        //TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 300.);
        //TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

    }
}

