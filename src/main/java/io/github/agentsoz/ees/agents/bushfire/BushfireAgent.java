package io.github.agentsoz.ees.agents.bushfire;


/*
 * #%L
 * Jill Cognitive Agents Platform
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.EmergencyMessage;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

@AgentInfo(hasGoals={"io.github.agentsoz.ees.agents.bushfire.GoalDoNothing"})
public abstract class BushfireAgent extends  Agent implements io.github.agentsoz.bdiabm.Agent {

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

    static final String LOCATION_HOME = "home";
    static final String LOCATION_EVAC_PREFERRED = "evac";
    static final String LOCATION_INVAC_PREFERRED = "invac";

    private PrintStream writer = null;
    private QueryPerceptInterface queryInterface;
    private double time = -1;
    private BushfireAgent.Prefix prefix = new BushfireAgent.Prefix();

    // Defaults
    private DependentInfo dependentInfo = null;
    private double initialResponseThreshold = 0.5;
    private double finalResponseThreshold = 0.5;
    private double responseBarometerMessages = 0.0;
    private double responseBarometerFieldOfView = 0.0;

    private enum FieldOfViewPercept { // FIXME: move to config
        SMOKE_VISUAL(0.3),
        FIRE_VISUAL(0.4),
        NEIGHBOURS_LEAVING(0.5);

        private final double value;

        private FieldOfViewPercept(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    enum MemoryEventType {
        BELIEVED,
        PERCEIVED,
        DECIDED,
        ACTIONED
    }

    enum MemoryEventValue {
        DEPENDENTS_INFO,
        DONE_FOR_NOW,
        IS_PLAN_APPLICABLE,
        STATE_CHANGED,
        RESPONSE_BAROMETER_MESSAGES_CHANGED,
        RESPONSE_BAROMETER_FIELD_OF_VIEW_CHANGED,
        INITIAL_RESPONSE_THRESHOLD_BREACHED,
        FINAL_RESPONSE_THRESHOLD_BREACHED,
        INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER,
        TRIGGER_INITIAL_RESPONSE_NOW,
        TRIGGER_FINAL_RESPONSE_NOW,
        GOTO_LOCATION,
        DISTANCE_TO_LOCATION,
        SAFE,
        LAST_ENV_ACTION_STATE
    }

    // Internal variables
    private final String memory = "memory";
    private Map<String,Location> locations;
    private EnvironmentAction activeEnvironmentAction;
    private ActionContent.State lastEnvironmentActionStatus;



    public BushfireAgent(String id) {
        super(id);
        locations = new HashMap<>();
    }

    DependentInfo getDependentInfo() {
        return dependentInfo;
    }

    public Map<String, Location> getLocations() {
        return locations;
    }

    public void setLocations(Map<String, Location> locations) {
        this.locations = locations;
    }

    double getResponseBarometer() {
        return responseBarometerMessages + responseBarometerFieldOfView;
    }

    double getProbHomeAfterDependents() {
        return 0.5; // FIXME: should be configurable
    }
    double getProbHomeBeforeLeaving() {
        return 0.5; // FIXME: should be configurable
    }

    boolean isDriving() {
        return activeEnvironmentAction != null && activeEnvironmentAction.getActionID().equals(ActionList.DRIVETO);
    }

    void setActiveEnvironmentAction(EnvironmentAction activeEnvironmentAction) {
        this.activeEnvironmentAction = activeEnvironmentAction;
    }

    public void setLastEnvironmentActionState(ActionContent.State lastEnvironmentActionStatus) {
        this.lastEnvironmentActionStatus = lastEnvironmentActionStatus;
    }

    public ActionContent.State getLastEnvironmentActionState() {
        return lastEnvironmentActionStatus;
    }

    /**
     * Called by the Jill model when starting a new agent.
     * There is no separate initialisation call prior to this, so all
     * agent initialisation should be done here (using params).
     */
    @Override
    public void start(PrintStream writer, String[] params) {
        this.writer = writer;
        parseArgs(params);
        // Create a new belief set to store memory
        BeliefSetField[] fields = {
                new BeliefSetField("time", String.class, true),
                new BeliefSetField("event", String.class, false),
                new BeliefSetField("value", String.class, false),
        };
        try {
            // Attach this belief set to this agent
            this.createBeliefSet(memory, fields);

            memorise(BushfireAgent.MemoryEventType.BELIEVED.name(),
                    MemoryEventValue.DEPENDENTS_INFO.name() + ":" + getDependentInfo() );

        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called by the Jill model when terminating
     */
    @Override
    public void finish() {
//        writer.println(logPrefix() + "is terminating");
//        try {
//            if (eval("memory.event = *")) {
//                Set<String> memories = new TreeSet<>();
//                for (Belief belief : getLastResults()) {
//                    memories.add(logPrefix() + "memory : " + Arrays.toString(belief.getTuple()));
//                }
//                for (String belief : memories) {
//                    writer.println(belief);
//                }
//            }
//        } catch (BeliefBaseException e) {
//            throw new RuntimeException(e);
//        }
    }

        /**
     * Called by the Jill model with the status of a BDI percept
     * for this agent, coming from the ABM environment.
     */
    @Override
    public void handlePercept(String perceptID, Object parameters) {
        if (perceptID == null || perceptID.isEmpty()) {
            return;
        }
        if (perceptID.equals(PerceptList.TIME)) {
            if (parameters instanceof Double) {
                time = (double) parameters;
            }
            return;
        }

        // save it to memory
        memorise(MemoryEventType.PERCEIVED.name(), perceptID + ":" +parameters.toString());

        if (perceptID.equals(PerceptList.EMERGENCY_MESSAGE)) {
            updateResponseBarometerMessages(parameters);
        } else if (perceptID.equals(PerceptList.FIELD_OF_VIEW)) {
            updateResponseBarometerFieldOfViewPercept(parameters);
        } else if (perceptID.equals(PerceptList.ARRIVED)) {
            // do something
        } else if (perceptID.equals(PerceptList.BLOCKED)) {
            // do something
        } else if (perceptID.equals(PerceptList.FIRE_ALERT)) {
            // FIXME: using fire msg (global) as proxy for fire visual (localised)
            updateResponseBarometerFieldOfViewPercept(FieldOfViewPercept.FIRE_VISUAL);
        }

        // Now trigger a response as needed
        checkBarometersAndTriggerResponseAsNeeded();
    }

    protected void checkBarometersAndTriggerResponseAsNeeded() {
        try {
            // Nothing to do if already triggered responses once
            MemoryEventValue breach = null;
            if (!eval("memory.value = " + MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED.name())) {
                // initial response threshold not breached yet
                if (isInitialResponseThresholdBreached()) {
                    // initial response threshold breached for the first time
                    memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED.name());
                    breach= MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED;
                }
            }
            if (!eval("memory.value = " + MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED.name())) {
                // final response threshold not breached yet
                if (isFinalResponseThresholdBreached()) {
                    // final response threshold breached for the first time
                    memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED.name());
                    if (!isInitialResponseThresholdBreached()) {
                        // final breached bu not initial, so force initial breach now as well
                        logger.warn("{} had final threshold breached but not initial; will assume both have breached");
                        memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER.name());
                        breach = MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER;
                    } else if (breach==null) {
                        // only final response breached just now, not initial
                        breach = MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED;
                    } else {
                        // both thresholds breached together
                        memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER.name());
                        breach = MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER;
                    }
                }
            }
            if (breach != null) {
                triggerResponse(breach);
            }

        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    boolean isInitialResponseThresholdBreached() {
        return (getResponseBarometer() >= initialResponseThreshold);
    }

    boolean isFinalResponseThresholdBreached() {
        return (getResponseBarometer() >= finalResponseThreshold);
    }

    void memorise(String event, String data) {
        try {
            addBelief(memory, Double.toString(time), event, data);
            log("memory:" + String.format("%.0f", time) + ":" + event + ":" + data);
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called after a new percept has been processed
     * @param breach
     */
    private void triggerResponse(MemoryEventValue breach) {
        if (breach == MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER) {
            memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.TRIGGER_INITIAL_RESPONSE_NOW.name());
            memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.TRIGGER_FINAL_RESPONSE_NOW.name());
            // CAREFUL: Must use LIFO when pushing multiple goals that should be executed sequentially
            // onto the Jill stack
            post(new GoalActNow("ActNow")); // 1. will execute second
            post(new GoalInitialResponse("InitialResponse")); // 2. will execute first

        } else if (breach == MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED) {
            memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.TRIGGER_INITIAL_RESPONSE_NOW.name());
            post(new GoalInitialResponse("InitialResponse"));

        } else if (breach == MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED) {
            memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.TRIGGER_FINAL_RESPONSE_NOW.name());
            post(new GoalActNow("ActNow"));
        }
    }

    /**
     * Overwrites {@link #responseBarometerFieldOfView} with the value of the incoming visual
     * if the incoming value is higher.
     * @param view the incoming visual percept
     */
    private void updateResponseBarometerFieldOfViewPercept(Object view) {
        if (view == null) {
            return;
        }
        FieldOfViewPercept percept = null;
        if (PerceptList.SIGHTED_EMBERS.equalsIgnoreCase(view.toString())) {
            percept = FieldOfViewPercept.SMOKE_VISUAL;
        } else if (PerceptList.SIGHTED_FIRE.equalsIgnoreCase(view.toString())) {
            percept = FieldOfViewPercept.FIRE_VISUAL;
        } else {
            logger.warn("{} ignoring field of view percept: {}", logPrefix(), view);
            return;
        }
        double value = percept.getValue();
        if (value > responseBarometerFieldOfView) {
            responseBarometerFieldOfView = value;
            memorise(MemoryEventType.BELIEVED.name(), MemoryEventValue.RESPONSE_BAROMETER_FIELD_OF_VIEW_CHANGED.name() + "=" + Double.toString(value));
        }
    }

    /**
     * Overwrites {@link #responseBarometerMessages} with the value of the incoming message
     * if the incoming value is higher.
     * @param msg the incoming emergency message
     */
    private void updateResponseBarometerMessages(Object msg) {
        if (msg == null || !(msg instanceof String)) {
            return;
        }
        String[] tokens = ((String) msg).split(",");
        EmergencyMessage.EmergencyMessageType type = EmergencyMessage.EmergencyMessageType.valueOf(tokens[0]);
        try {
            String loc= tokens[1];
            double [] coords = new double[]{Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3])};
            locations.put(LOCATION_EVAC_PREFERRED, new Location(loc,coords[0], coords[1]));
            memorise(MemoryEventType.BELIEVED.name(), LOCATION_EVAC_PREFERRED + "=" + locations.get(LOCATION_EVAC_PREFERRED));
        } catch (Exception e) {}
        double value = type.getValue();
        // Allow the barometer to go down as well if the intensity of the situation (message) is reduced
        //if (value > responseBarometerMessages) {
            responseBarometerMessages = value;
            memorise(MemoryEventType.BELIEVED.name(), MemoryEventValue.RESPONSE_BAROMETER_MESSAGES_CHANGED.name() + "=" + Double.toString(value));
        //}
    }

    boolean startDrivingTo(Location location) {
        if (location == null) return false;
        memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.GOTO_LOCATION.name() + ":" + location.toString());
        double distToTravel = getTravelDistanceTo(location);
        if (distToTravel == 0.0) {
            // already there, so no need to drive
            return false;
        }
        Object[] params = new Object[4];
        params[0] = ActionList.DRIVETO;
        params[1] = location.getCoordinates();
        params[2] = getTime() + 5.0; // five secs from now;
        params[3] = MATSimModel.EvacRoutingMode.carFreespeed;
        memorise(MemoryEventType.ACTIONED.name(), ActionList.DRIVETO
                + ":"+ location + ":" + String.format("%.0f", distToTravel) + "m away");
        EnvironmentAction action = new EnvironmentAction(Integer.toString(getId()), ActionList.DRIVETO, params);
        setActiveEnvironmentAction(action); // will be reset by updateAction()
        post(action); // post should be last call in any plan step
        return true;
    }

    double getTravelDistanceTo(Location location) {
        return (double) getQueryPerceptInterface().queryPercept(
                    String.valueOf(getId()),
                    PerceptList.REQUEST_DRIVING_DISTANCE_TO,
                    location.getCoordinates());
    }

    /**
     * Called by the Jill model when this agent posts a new BDI action
     * to the ABM environment
     */
    @Override
    public void packageAction(String actionID, Object[] parameters) {
        logger.warn("{} ignoring action {}", logPrefix(), actionID);
    }

    /**
     * Called by the Jill model with the status of a BDI action previously
     * posted by this agent to the ABM environment.
     */
    @Override
    public void updateAction(String actionID, ActionContent content) {
        logger.debug("{} received action update: {}", logPrefix(), content);
        setLastEnvironmentActionState(content.getState()); // save the finish state of the action
        if (content.getAction_type().equals(ActionList.DRIVETO)) {
            ActionContent.State actionState = content.getState();
            if (actionState == ActionContent.State.PASSED ||
                    actionState == ActionContent.State.FAILED ||
                    actionState == ActionContent.State.DROPPED) {
                memorise(BushfireAgent.MemoryEventType.BELIEVED.name(), BushfireAgent.MemoryEventValue.LAST_ENV_ACTION_STATE.name() + "=" + actionState.name());
                setActiveEnvironmentAction(null); // remove the action
                // Wake up the agent that was waiting for external action to finish
                // FIXME: BDI actions put agent in suspend, which won't work for multiple intention stacks
                suspend(false);
            }
        }
    }

    /**
     * BDI-ABM agent init function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform any agent specific initialisation.
     */
    @Override
    public void init(String[] args) {
        parseArgs(args);
    }

    /**
     * BDI-ABM agent start function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform agent startup.
     */
    @Override
    public void start() {
        logger.warn("{} using a stub for io.github.agentsoz.bdiabm.Agent.start()", logPrefix());
    }

    /**
     * BDI-ABM agent kill function; Not used by Jill.
     * Use {@link #finish()} instead
     * to perform agent termination.
     */

    @Override
    public void kill() {
        logger.warn("{} using a stub for io.github.agentsoz.bdiabm.Agent.kill()", logPrefix());
    }

    @Override
    public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
        this.queryInterface = queryInterface;
    }

    @Override
    public QueryPerceptInterface getQueryPerceptInterface() {
        return queryInterface;
    }


    double getTime() {
        return time;
    }

    private void parseArgs(String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "hasDependentsAtLocation":
                        if (i + 1 < args.length) {
                            i++;
                            if (!args[i].isEmpty()) { // empty arg is ok, signifies no dependents
                                try {
                                    String[] vals = args[i].split(",");
                                    Location location = new Location("Dependent", Double.parseDouble(vals[0]), Double.parseDouble(vals[1]));
                                    dependentInfo = new DependentInfo();
                                    dependentInfo.setLocation(location);
                                } catch (Exception e) {
                                    System.err.println("Could not parse dependent's location '"
                                            + args[i] + "' : " + e.getMessage());
                                }
                            }
                        }
                        break;
                    case "InitialResponseThreshold":
                        if(i+1<args.length) {
                            i++ ;
                            try {
                                initialResponseThreshold = Double.parseDouble(args[i]);
                                // limit it to between 0 and 1
                                initialResponseThreshold =
                                        (initialResponseThreshold<0.0) ? 0.0 :
                                                (initialResponseThreshold>1.0) ? 1.0 :
                                                        initialResponseThreshold;
                            } catch (Exception e) {
                                logger.error("Could not parse double '"+ args[i] + "'", e);
                            }
                        }
                        break;
                    case "FinalResponseThreshold":
                        if(i+1<args.length) {
                            i++ ;
                            try {
                                finalResponseThreshold = Double.parseDouble(args[i]);
                                // limit it to between 0 and 1
                                finalResponseThreshold =
                                        (finalResponseThreshold<0.0) ? 0.0 :
                                                (finalResponseThreshold>1.0) ? 1.0 :
                                                        finalResponseThreshold;
                            } catch (Exception e) {
                                logger.error("Could not parse double '"+ args[i] + "'", e);
                            }

                        }
                        break;
                    case LOCATION_HOME:
                        if(i+1<args.length) {
                            i++;
                            try {
                                String[] vals = args[i].split(",");
                                Location location = new Location(LOCATION_HOME, Double.parseDouble(vals[0]), Double.parseDouble(vals[1]));
                                locations.put(LOCATION_HOME, location);
                            } catch (Exception e) {
                                System.err.println("Could not parse dependent's location '"
                                        + args[i] + "' : " + e.getMessage());
                            }
                        }
                        break;
                    case "EvacLocationPreference":
                    case "InvacLocationPreference":
                        if(i+1<args.length) {
                            i++;
                            try {
                                String[] vals = args[i].split(",");
                                Location location = new Location(vals[0], Double.parseDouble(vals[1]), Double.parseDouble(vals[2]));
                                String loc = args[i-1].equals("EvacLocationPreference") ?
                                        LOCATION_EVAC_PREFERRED : LOCATION_INVAC_PREFERRED;
                                locations.put(loc, location);
                            } catch (Exception e) {
                                System.err.println("Could not parse location '"
                                        + args[i] + "' : " + e.getMessage());
                            }
                        }
                        break;
                    default:
                        // ignore other options
                        break;
                }
            }
        }
    }

    class Prefix{
        public String toString() {
            return String.format("Time %05.0f BushfireAgent %-4s : ", getTime(), getId());
        }
    }

    String logPrefix() {
        return prefix.toString();
    }

    void log(String msg) {
        writer.println(logPrefix()+msg);
    }

    class DependentInfo {
        private Location location;
        private double lastVisitedAtTime = -1;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public double getLastVisitedAtTime() {
            return lastVisitedAtTime;
        }

        public void setLastVisitedAtTime(double lastVisitedAtTime) {
            this.lastVisitedAtTime = lastVisitedAtTime;
        }

        public boolean isVisited() {
            return getLastVisitedAtTime()==-1;
        }

        @Override
        public String toString() {
            String s = (location==null) ? "null" : location.toString();
            s += ", last visited at time="+ lastVisitedAtTime;
            return s;
        }
    }
}