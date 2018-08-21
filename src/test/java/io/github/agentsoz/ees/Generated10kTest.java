package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.MATSimModel;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
@Ignore
public class Generated10kTest {
    // have tests in separate classes so that they run, at least und    er maven, in separate JVMs.  kai, nov'17

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();
    @Ignore
    @Test
    public void testGenerated10k() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/generated-10k/scenario_main.xml",
                "--logfile", utils.getOutputDirectory()+"../scenario.log",
                "--loglevel", "INFO",
                //	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
                "--seed", "12345",
                "--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
                MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:10000}]," +
                "logLevel: WARN," +
                "logFile: \""+utils.getOutputDirectory()+"../jill.log\"," +
                "programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\"," +
                "randomSeed: 12345," + // jill random seed
                "numThreads: 1" + // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "100000:100000", // virtually disallow congestion re-routing (painfully slow otherwise!)
                "--sendFireAlertOnFireStart", "false", // disable fire alert from fire model, instead will use messaging
//                "--x-messages-file", "scenarios/surf-coast-shire/generated-10k/scenario_messages.json", // specifies when to send evac now msg
//                "--x-zones-file", "scenarios/surf-coast-shire/generated-10k/Anglesea_SA1s_WSG84.json", // map from zone (SA1) ids to shapes
        };

        Main.main(args);
//
//        final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
//        long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
//        System.err.println("actual(events)="+actualEventsCRC) ;
//
//        long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
//        System.err.println("actual(plans)="+actualPlansCRC) ;
//
//        // ---
//
//        final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
//
//        // ---

        //TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 300.);
        //TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

    }
}
