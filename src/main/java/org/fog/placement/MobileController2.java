package org.fog.placement;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.cmfog.helpers.LocationHelper;
import org.fog.entities.Actuator;
import org.fog.entities.ApDevice;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.entities.Sensor;
import org.fog.localization.Coordinate;
import org.fog.localization.Distances;
import org.fog.cmfog.enumerate.Algorithm;
import static org.fog.placement.MobileController2.isMigrationAble;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.vmmigration.Migration;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.NextStep;
import org.fog.vmmobile.AppExemplo2;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.MobileEvents;
import org.fog.vmmobile.constants.Policies;

public class MobileController2 extends SimEntity {

    private static boolean migrationAble;
    private static int migPointPolicy;
    private static int stepPolicy; //Quantity of steps in the nextStep Function
    private static Coordinate coordDevices;//=new Coordinate(MaxAndMin.MAX_X, MaxAndMin.MAX_Y);//Grid/Map
    private static int migStrategyPolicy;
    private static int seed;
    private static List<FogDevice> serverCloudlets;
    private static List<MobileDevice> smartThings;
    private static List<ApDevice> apDevices;
    private static List<FogBroker> brokerList;
    private Map<String, Application> applications;
    private Map<String, Integer> appLaunchDelays;
    private ModuleMapping moduleMapping;
    private Map<Integer, Double> globalCurrentCpuLoad;
    static final int numOfDepts = 1;
    static final int numOfMobilesPerDept = 4;
    private static Random rand;

    public MobileController2() {

    }

    public MobileController2(String name, List<FogDevice> serverCloudlets, List<ApDevice> apDevices, List<MobileDevice> smartThings, List<FogBroker> brokers, ModuleMapping moduleMapping,
            int migPointPolicy, int migStrategyPolicy, int stepPolicy, Coordinate coordDevices, int seed, boolean migrationAble) {
        // TODO Auto-generated constructor stub
        super(name);
        this.applications = new HashMap<String, Application>();
        this.globalCurrentCpuLoad = new HashMap<Integer, Double>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setModuleMapping(moduleMapping);
        for (FogDevice sc : serverCloudlets) {
            sc.setControllerId(getId());
        }
        setSeed(seed);
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setSmartThings(smartThings);
        setBrokerList(brokers);
        setMigPointPolicy(migPointPolicy);
        setMigStrategyPolicy(migStrategyPolicy);
        setStepPolicy(stepPolicy);
        setCoordDevices(coordDevices);
        connectWithLatencies();
        initializeCPULoads();
        setRand(new Random(getSeed() * Long.MAX_VALUE));
        setMigrationAble(migrationAble);
    }

    
    public MobileController2(String name, List<FogDevice> serverCloudlets, List<ApDevice> apDevices, List<MobileDevice> smartThings, List<FogBroker> brokers, ModuleMapping moduleMapping,
            int migPointPolicy, int migStrategyPolicy, int stepPolicy, int seed, boolean migrationAble) {
        // TODO Auto-generated constructor stub
        super(name);
        this.applications = new HashMap<String, Application>();
        this.globalCurrentCpuLoad = new HashMap<Integer, Double>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setModuleMapping(moduleMapping);
        for (FogDevice sc : serverCloudlets) {
            sc.setControllerId(getId());
        }
        setSeed(seed);
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setSmartThings(smartThings);
        setBrokerList(brokers);
        setMigPointPolicy(migPointPolicy);
        setMigStrategyPolicy(migStrategyPolicy);
        setStepPolicy(stepPolicy);
        setCoordDevices(coordDevices);
        connectWithLatencies();
        initializeCPULoads();
        setRand(new Random(getSeed() * Long.MAX_VALUE));
        setMigrationAble(migrationAble);
    }
    
    public MobileController2(String name, List<FogDevice> serverCloudlets,
            List<ApDevice> apDevices, List<MobileDevice> smartThings,
            int migPointPolicy, int migStrategyPolicy, int stepPolicy,
            Coordinate coordDevices, int seed) {
        // TODO Auto-generated constructor stub
        super(name);
        this.applications = new HashMap<String, Application>();
        this.globalCurrentCpuLoad = new HashMap<Integer, Double>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setModuleMapping(moduleMapping);
        for (FogDevice sc : serverCloudlets) {
            sc.setControllerId(getId());
        }
        setSeed(seed);
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setSmartThings(smartThings);
        setMigPointPolicy(migPointPolicy);
        setMigStrategyPolicy(migStrategyPolicy);
        setStepPolicy(stepPolicy);
        setCoordDevices(coordDevices);
        connectWithLatencies();
        initializeCPULoads();
        setRand(new Random(getSeed() * Long.MAX_VALUE));

    }
    
    

    private void connectWithLatencies() {
        for (FogDevice st : getSmartThings()) {
            FogDevice parent = getFogDeviceById(st.getParentId());
            if (parent == null) {
                continue;
            }
            double latency = st.getUplinkLatency();
            parent.getChildToLatencyMap().put(st.getId(), latency);
            parent.getChildrenIds().add(st.getId());
        }
    }

    private FogDevice getFogDeviceById(int id) {
        for (FogDevice sc : getServerCloudlets()) {
            if (id == sc.getId()) {
                return sc;
            }
        }
        return null;
    }

    private void initializeCPULoads() {
        //		Map<String, Map<String, Integer>> mapping = moduleMapping.getModuleMapping();
        //		for(String deviceName : mapping.keySet()){
        //			FogDevice device = getDeviceByName(deviceName);
        //			for(String moduleName : mapping.get(deviceName).keySet()){
        //
        //				AppModule module = getApplication().getModuleByName(moduleName);
        //				if(module == null)
        //					continue;
        //				getCurrentCpuLoad().put(device.getId(), getCurrentCpuLoad().get(device.getId()).doubleValue() + module.getMips());
        //			}
        //		}
        for (FogDevice sc : getServerCloudlets()) {
            this.globalCurrentCpuLoad.put(sc.getId(), 0.0);
        }
        for (MobileDevice st : getSmartThings()) {
            this.globalCurrentCpuLoad.put(st.getId(), 0.0);
        }
    }

    @Override
    public void startEntity() {
        for (String appId : applications.keySet()) {
            LogMobile.debug("MobileController.java", appId + " - " + getAppLaunchDelays().get(appId));
            processAppSubmit(applications.get(appId));
        }
        //Define check events for the whole simulation
        for (int simulationClock = 0; simulationClock < MaxAndMin.MAX_SIMULATION_TIME; simulationClock += 1000) {
            send(getId(), simulationClock, MobileEvents.NEXT_STEP);
            send(getId(), simulationClock, MobileEvents.CHECK_NEW_STEP);
        }
        //If simulation is configured to migrate VMs
        if (isMigrationAble()) {
            for (FogDevice aCloudlet : getServerCloudlets()) {
                for (int simulationClock = 0; simulationClock < MaxAndMin.MAX_SIMULATION_TIME; simulationClock += 1000) {
                    send(aCloudlet.getId(), simulationClock, MobileEvents.MAKE_DECISION_MIGRATION, aCloudlet.getSmartThings());
                }
            }
        }
        //Create mobile device
        for (MobileDevice aMobileDevice : getSmartThings()) {
            System.out.println("MobileDevice(" + aMobileDevice.getName() + ")starting @" + aMobileDevice.getStartTravelTime() * 1000 + "s");
            send(getId(), aMobileDevice.getStartTravelTime() * 1000, MobileEvents.CREATE_NEW_SMARTTHING, aMobileDevice);
        }
        //?
        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
        //?
        for (FogDevice aCloudlet : getServerCloudlets()) {
            sendNow(aCloudlet.getId(), FogEvents.RESOURCE_MGMT);
        }
        //Stop simulation
        send(getId(), MaxAndMin.MAX_SIMULATION_TIME, MobileEvents.STOP_SIMULATION);

        System.out.println("=================================================================================================================================");
        System.out.println("=================================================================================================================================");
        System.out.println("=============================================== STARTING SIMULATION =============================================================");
        System.out.println("=================================================================================================================================");
        System.out.println("=================================================================================================================================");
    }

    private void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        processAppSubmit(app);
    }

    private void processAppSubmit(Application application) {
        //System.out.println("MobileController 213 processAppSubmit " + CloudSim.clock() + " Submitted application " + application.getAppId());
        System.out.println("Submitting App" + application.getAppId());
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        List<FogDevice> tempAllDevices = new ArrayList<>();
        for (FogDevice sc : getServerCloudlets()) {
            tempAllDevices.add(sc);
        }

        for (MobileDevice st : getSmartThings()) {
            tempAllDevices.add(st);
        }

        ModulePlacement modulePlacement = new ModulePlacementMapping(tempAllDevices//getServerCloudlets()
                ,
                 application, getModuleMapping(), globalCurrentCpuLoad);

        for (FogDevice fogDevice : getServerCloudlets()) {
            sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }
        for (MobileDevice st : getSmartThings()) {
            sendNow(st.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }

        Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
        Map<Integer, Map<String, Integer>> instanceCountMap = modulePlacement.getModuleInstanceCountMap();
        for (Integer deviceId : deviceToModuleMap.keySet()) {
            for (AppModule module : deviceToModuleMap.get(deviceId)) {
                //System.out.println("MobileController 240 ProcessAppSubmit");
                sendNow(deviceId, FogEvents.APP_SUBMIT, application);
                sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
                sendNow(deviceId, FogEvents.LAUNCH_MODULE_INSTANCE,
                        new ModuleLaunchConfig(module, instanceCountMap.get(deviceId).get(module.getName())));
            }

        }
    }

    private void processAppSubmitMigration(SimEvent ev) {
        Application application = (Application) ev.getData();
        //  System.out.println(CloudSim.clock() + " Submitted application after migration " + application.getAppId());
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        FogDevice sc = (FogDevice) CloudSim.getEntity(ev.getSource());
        List<FogDevice> tempList = new ArrayList<>();
        tempList.add(sc);
        ModulePlacement modulePlacement = new ModulePlacementMapping(tempList//getServerCloudlets()
                ,
                 application, getModuleMapping(), globalCurrentCpuLoad, true);

        //		for(FogDevice fogDevice : getServerCloudlets()){
        sendNow(sc.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        //		}

        Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
        Map<Integer, Map<String, Integer>> instanceCountMap = modulePlacement.getModuleInstanceCountMap();
        //		for(Integer deviceId : deviceToModuleMap.keySet()){
        for (AppModule module : deviceToModuleMap.get(sc.getId())) {
            // System.out.println("MobileController 268 processAppSubmitMigration");
            sendNow(sc.getId(), FogEvents.APP_SUBMIT, application);
            sendNow(sc.getId(), FogEvents.LAUNCH_MODULE, module);
            sendNow(sc.getId(), FogEvents.LAUNCH_MODULE_INSTANCE,
                    new ModuleLaunchConfig(module, instanceCountMap.get(sc.getId()).get(module.getName())));
        }

        //		}
    }

    private void processTupleFinished(SimEvent ev) {
    }

    protected void manageResources() {
        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
    }

    @Override
    public void processEvent(SimEvent event) {
        switch (event.getTag()) {
            case FogEvents.APP_SUBMIT:
                System.out.println("APP_SUBMIT");
                processAppSubmit(event);
                break;
            case MobileEvents.APP_SUBMIT_MIGRATE:
                processAppSubmitMigration(event);
                break;

            case FogEvents.TUPLE_FINISHED:
                System.out.println("TUPLE_FINISHED");
                processTupleFinished(event);
                break;
            case FogEvents.CONTROLLER_RESOURCE_MANAGE:
                manageResources();
                break;
            case MobileEvents.NEXT_STEP:
                NextStep.nextStep(getServerCloudlets(),
                        getApDevices(),
                        getSmartThings(),
                        getCoordDevices(),
                        getStepPolicy(),
                        getSeed());

                break;
            case MobileEvents.CREATE_NEW_SMARTTHING:
                createNewSmartThing(event);
                break;
            case MobileEvents.CHECK_NEW_STEP:
                switch (AppExemplo2.getMigStrategyPolicy()) {
                    case Policies.CMFOG:
                        for (MobileDevice aMobileDevice : getSmartThings()) {
                            if (aMobileDevice.getAlgorithm() == Algorithm.CCMFOG) {
                                System.out.println("Checking CCMFOG-NEW_STEP for " + aMobileDevice.getName());
                                checkNewStepC(aMobileDevice);
                            } else if (aMobileDevice.getAlgorithm() == Algorithm.TCMFOG) {
                                System.out.println("Checking TCMFOG-NEW_STEP for " + aMobileDevice.getName());
                                checkNS_TCMFOG(aMobileDevice);
                            }
                        }
//                        if (AppExemplo2.getMigPointPolicy() == Policies.TIME_MIGRATION_POINT) {
//                            checkNewStepTT();
//                        } else {
//                            checkNewStepFMFM();
//                        }
                        break;
                    case Policies.LOWEST_LATENCY:
                        System.out.println("Checking LOWEST_LATENCY");
                        checkNewStep();
                        break;
                    default:
                        break;
                }
                //System.out.println("SmartThingListSize: " + getSmartThings().size());
                if (getSmartThings().isEmpty()) {
                    sendNow(getId(), MobileEvents.STOP_SIMULATION);
                }
                break;
            case MobileEvents.STOP_SIMULATION:
                System.out.println("*********************myStopSimulation MobilieController 149 ***********");
                System.out.println("CloudSim.clock(): " + CloudSim.clock());
                System.out.println("Size SmartThings: " + getSmartThings().size());
                CloudSim.stopSimulation();
                printTimeDetails();
                // printPowerDetails();
                // printCostDetails();
                printNetworkUsageDetails();
                printMigrationsDetalis();
                System.exit(0);
                break;

        }
    }

    private void createNewSmartThing(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        System.out.println("Creating " + aMobileDevice.getName());
        aMobileDevice.setTravelTimeId(0);
//		if(ApDevice.connectApSmartThing(getApDevices(), st, getRand().nextDouble())){
//			st.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(st);
//			System.out.println("conectado... "+st.getSourceServerCloudlet().getName());
//		}

    }

    private double migrationTimeToLiveMigration(MobileDevice smartThing) {
        // TODO Auto-generated method stub
        double runTime = CloudSim.clock() - smartThing.getTimeStartLiveMigration();
        if (smartThing.getMigTime() > runTime) {
            runTime = smartThing.getMigTime() - runTime;
            return runTime;
        } else {
            return 0;
        }

    }

    private void checkNewStep() {
        System.out.println("-------------------------------------------------> NEW STEP LOWEST LATENCY");
        int index = 0;
        for (MobileDevice st : getSmartThings()) {
            if (st.getSourceAp() != null && st.getVmLocalServerCloudlet() != null) {
                AppExemplo2.log.add(CloudSim.clock() + " | " + st.getSourceAp().getName() + " - " + st.getVmLocalServerCloudlet().getName());
            }
            MyStatistics.getInstance().getEnergyHistory().put(st.getMyId(), st.getEnergyConsumption());
            MyStatistics.getInstance().getPowerHistory().put(st.getMyId(), st.getHost().getPower());
            if (st.getSourceAp() != null) {
                System.out.println(st.getName() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY());
                System.out.println(st.getSourceAp().getName() + "\t" + st.getSourceAp().getCoord().getCoordX() + "\t" + st.getSourceAp().getCoord().getCoordY());
                System.out.println(Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord()));
                if (!st.isLockedToHandoff()) {
                    double distance = Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord());
                    if (LocationHelper.isInZone(st.getSourceAp(), st, MaxAndMin.MAX_DISTANCE_TO_HANDOFF)) { //Handoff Zone
                        System.out.println("---------------------------------------------> SEARCHING HANDOFF OPTION");
                        index = Migration.nextAp(getApDevices(), st);
                        if (index >= 0) {
                            st.setDestinationAp(getApDevices().get(index));
                            st.setHandoffStatus(true);
                            st.setLockedToHandoff(true);
                            double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * getRand().nextDouble(); //"Maximo" tempo para handoff
                            float handoffLocked = (float) (handoffTime * 20);
                            int delayConnection = 100; //connection between SmartT and ServerCloudlet
                            if (!st.getDestinationAp().getServerCloudlet().equals(st.getSourceServerCloudlet())) {
                                System.out.println("-------------------------------------------------------> CLOUDLET DIFF");
                                if (isMigrationAble()) {
                                    System.out.println("-------------------------------------------------------> MIG ABLED");
                                    LogMobile.debug("MobileController.java", st.getName() + " will be desconnected from " + st.getSourceServerCloudlet().getName() + " by handoff");
                                    sendNow(st.getSourceServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, st);
                                    sendNow(st.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, st);
                                    send(st.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, st);
                                }
                            }
                            send(st.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, st);
                            send(st.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, st);
                            MyStatistics.getInstance().setTotalHandoff(1);
                            saveHandOff(st);
                            LogMobile.debug("MobileController.java", st.getName() + " handoff was scheduled! " + "SourceAp: " + st.getSourceAp().getName()
                                    + " NextAp: " + st.getDestinationAp().getName() + "\n");
                            LogMobile.debug("MobileController.java", "Distance between " + st.getName() + " and " + st.getSourceAp().getName() + ": "
                                    + Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord()));
                        } else {
                            LogMobile.debug("MobileController.java", st.getName() + " can't make handoff because don't exist closest nextAp");
                        }
                    } else if (!LocationHelper.isCovered(st.getSourceAp(), st)) {
                        st.getSourceAp().desconnectApSmartThing(st);
                        st.getSourceServerCloudlet().desconnectServerCloudletSmartThing(st);
                        System.out.println("--------------------------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> DC");
//                        if (st.isLockedToMigration() || st.isMigStatus()) {
//                            System.out.println("TA FORA DE COBERTURA E ESTA LOCKADO PRA MIG OU EM ESTADO DE MIG");
//                            sendNow(st.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, st);
//                        }
                        LogMobile.debug("MobileController.java", st.getName() + " desconnected by AP_COVERAGE - Distance: " + distance);
                        LogMobile.debug("MobileController.java", st.getName() + " X: " + st.getCoord().getCoordX() + " Y: " + st.getCoord().getCoordY());
                    }
                }
            } else {
                if (ApDevice.connectApSmartThing(getApDevices(), st, getRand().nextDouble())) {
                    System.out.println("--------------------------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> RE-C");
                    st.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(st);
                    MyStatistics.getInstance().setTotalHandoff(1);
                    LogMobile.debug("MobileController.java", st.getName() + " has a new connection - SourceAp: " + st.getSourceAp().getName()
                            + " SourceServerCouldlet: " + st.getSourceServerCloudlet().getName());
                } else {
                    //To do something
                }
            }
        }
    }

    private void checkNewStepC(MobileDevice aMobileDevice) {
        //System.out.println("-------------------------------------------------> NEW STEP LOWEST LATENCY");
        int index = 0;

        if (aMobileDevice.getSourceAp() != null && aMobileDevice.getVmLocalServerCloudlet() != null) {
            AppExemplo2.mobilityLog.get(aMobileDevice.getMyId()).add("@" + CloudSim.clock() + " | " + aMobileDevice.getName() + "(" + aMobileDevice.getSourceAp().getName() + "," + aMobileDevice.getVmLocalServerCloudlet().getName() + ")");
            //AppExemplo2.log.add("@" + CloudSim.clock() + " | " + aMobileDevice.getName() + "(" + aMobileDevice.getSourceAp().getName() + "," + aMobileDevice.getVmLocalServerCloudlet().getName() + ")");
        }
        MyStatistics.getInstance().getEnergyHistory().put(aMobileDevice.getMyId(), aMobileDevice.getEnergyConsumption());
        MyStatistics.getInstance().getPowerHistory().put(aMobileDevice.getMyId(), aMobileDevice.getHost().getPower());
        if (aMobileDevice.getSourceAp() != null) {
            //System.out.println(st.getName() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY());
            //System.out.println(st.getSourceAp().getName() + "\t" + st.getSourceAp().getCoord().getCoordX() + "\t" + st.getSourceAp().getCoord().getCoordY());
            // System.out.println(Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord()));
            if (!aMobileDevice.isLockedToHandoff()) {
                double distance = Distances.checkDistance(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord());
                if (LocationHelper.isInZone(aMobileDevice.getSourceAp(), aMobileDevice, MaxAndMin.MAX_DISTANCE_TO_HANDOFF)) { //Handoff Zone
                    System.out.println("-----> Searching handoff option");
                    index = Migration.nextAp(getApDevices(), aMobileDevice);
                    if (index >= 0) {
                        aMobileDevice.setDestinationAp(getApDevices().get(index));
                        aMobileDevice.setHandoffStatus(true);
                        aMobileDevice.setLockedToHandoff(true);
                        double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * getRand().nextDouble(); //"Maximo" tempo para handoff
                        float handoffLocked = (float) (handoffTime * 20);
                        int delayConnection = 100; //connection between SmartT and ServerCloudlet
                        if (!aMobileDevice.getDestinationAp().getServerCloudlet().equals(aMobileDevice.getSourceServerCloudlet())) {
                            //System.out.println("-------------------------------------------------------> CLOUDLET DIFF");
                            if (isMigrationAble()) {
                                //System.out.println("-------------------------------------------------------> MIG ABLED");
                                LogMobile.debug("MobileController.java", aMobileDevice.getName() + " will be desconnected from " + aMobileDevice.getSourceServerCloudlet().getName() + " by handoff");
                                System.out.println("--> " + aMobileDevice.getName() + " will be desconnected from " + aMobileDevice.getSourceServerCloudlet().getName() + " to Cloudlet " + index + " by handoff");
                                sendNow(aMobileDevice.getSourceServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, aMobileDevice);
                                sendNow(aMobileDevice.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, aMobileDevice);
                                send(aMobileDevice.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, aMobileDevice);
                            }
                        }
                        send(aMobileDevice.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, aMobileDevice);
                        send(aMobileDevice.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, aMobileDevice);
                        MyStatistics.getInstance().setTotalHandoff(1);
                        saveHandOff(aMobileDevice);
                        LogMobile.debug("MobileController.java", aMobileDevice.getName() + " handoff was scheduled! " + "SourceAp: " + aMobileDevice.getSourceAp().getName()
                                + " NextAp: " + aMobileDevice.getDestinationAp().getName() + "\n");
                        LogMobile.debug("MobileController.java", "Distance between " + aMobileDevice.getName() + " and " + aMobileDevice.getSourceAp().getName() + ": "
                                + Distances.checkDistance(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord()));
                    } else {
                        LogMobile.debug("MobileController.java", aMobileDevice.getName() + " can't make handoff because don't exist closest nextAp");
                    }
                } else if (!LocationHelper.isCovered(aMobileDevice.getSourceAp(), aMobileDevice)) {
                    aMobileDevice.getSourceAp().desconnectApSmartThing(aMobileDevice);
                    aMobileDevice.getSourceServerCloudlet().desconnectServerCloudletSmartThing(aMobileDevice);
                    System.out.println("--------------------------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> DC");
//                        if (st.isLockedToMigration() || st.isMigStatus()) {
//                            System.out.println("TA FORA DE COBERTURA E ESTA LOCKADO PRA MIG OU EM ESTADO DE MIG");
//                            sendNow(st.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, st);
//                        }
                    LogMobile.debug("MobileController.java", aMobileDevice.getName() + " desconnected by AP_COVERAGE - Distance: " + distance);
                    LogMobile.debug("MobileController.java", aMobileDevice.getName() + " X: " + aMobileDevice.getCoord().getCoordX() + " Y: " + aMobileDevice.getCoord().getCoordY());
                }
            }
        } else {
            if (ApDevice.connectApSmartThing(getApDevices(), aMobileDevice, getRand().nextDouble())) {
                System.out.println("--------------------------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> RE-C");
                aMobileDevice.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(aMobileDevice);
                MyStatistics.getInstance().setTotalHandoff(1);
                LogMobile.debug("MobileController.java", aMobileDevice.getName() + " has a new connection - SourceAp: " + aMobileDevice.getSourceAp().getName()
                        + " SourceServerCouldlet: " + aMobileDevice.getSourceServerCloudlet().getName());
            } else {
                //To do something
            }

        }
    }

    private void checkNewStepTT() {
        for (MobileDevice aMobileDevice : getSmartThings()) {
            MyStatistics.getInstance().getEnergyHistory().put(aMobileDevice.getMyId(), aMobileDevice.getEnergyConsumption());
            MyStatistics.getInstance().getPowerHistory().put(aMobileDevice.getMyId(), aMobileDevice.getHost().getPower());
            //IF MD IS CONNECTED TO AN AP
            if (aMobileDevice.getSourceAp() != null) {
                if (aMobileDevice.getNextHandoff() == 0 || aMobileDevice.getNextMigration() == 0) {
                    String toSearch = String.valueOf(aMobileDevice.getSourceServerCloudlet().getMyId());
                    System.out.println(toSearch);
                    //aMobileDevice.setToSearch(toSearch);
                    double estimatedTime = AppExemplo2.mobilityPrediction.predictedTime(aMobileDevice.getMyId(), toSearch) * 1000;
                    aMobileDevice.setNextHandoff(CloudSim.clock() + estimatedTime);
                    double estimatedMigTime = AppExemplo2.calculateMigTime(aMobileDevice);
                    aMobileDevice.setNextMigration(CloudSim.clock() + estimatedTime - estimatedMigTime);
                    System.out.println("=============================>>>>> NEXT MIG: " + aMobileDevice.getNextMigration());
                    System.out.println("=============================>>>>> NEXT HANDOFF: " + aMobileDevice.getNextHandoff());
                }

                if (aMobileDevice.getSourceAp() != null && aMobileDevice.getVmLocalServerCloudlet() != null) {
                    AppExemplo2.log.add("@" + CloudSim.clock() + " | " + aMobileDevice.getName() + "(" + aMobileDevice.getSourceAp().getName() + "," + aMobileDevice.getVmLocalServerCloudlet().getName() + ")");
                }
                if ((!aMobileDevice.isLockedToMigration()) && (CloudSim.clock() >= aMobileDevice.getNextMigration())) {
                    System.out.println("================================= DECISION MIGRATION EVENT FIRED =================================");
                    sendNow(aMobileDevice.getSourceAp().getServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, aMobileDevice);
                }

                if (!aMobileDevice.isLockedToHandoff()) {
                    if ((aMobileDevice.getNextHandoff() - CloudSim.clock() <= 5000) && (aMobileDevice.getNextHandoff() != -1)) { //Handoff Zone
                        System.out.println("================================= DECISION HANDOFF EVENT FIRED =================================");
                        int nextAPId = Migration.nextAp(getApDevices(), aMobileDevice, true);
                        if (aMobileDevice.getDestinationServerCloudlet() != null) {
                            if (aMobileDevice.getDestinationServerCloudlet().getMyId() == aMobileDevice.getSourceAp().getMyId()) {
                                nextAPId = -1;
                                aMobileDevice.setNextHandoff(-1);
                            }
                        }
                        if (nextAPId >= 0 && nextAPId != aMobileDevice.getSourceAp().getMyId()) {
                            System.out.println("================================= FOUND NEXT AP =================================");
                            aMobileDevice.setDestinationAp(getApDevices().get(nextAPId));
                            aMobileDevice.setHandoffStatus(true);
                            aMobileDevice.setLockedToHandoff(true);
                            double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * getRand().nextDouble(); //"Maximo" tempo para handoff
                            float handoffLocked = (float) (handoffTime * 10);
                            int delayConnection = 100;
                            //Handoff will be always between cloudlets, so you have to disconect the smarthing every handoff
                            sendNow(aMobileDevice.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, aMobileDevice);
                            send(aMobileDevice.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, aMobileDevice);
                            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " will be desconnected from " + aMobileDevice.getSourceServerCloudlet().getName() + " by handoff");
                            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                            send(aMobileDevice.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, aMobileDevice);
                            send(aMobileDevice.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, aMobileDevice);
                            MyStatistics.getInstance().setTotalHandoff(1);
                            saveHandOff(aMobileDevice);
                           // aMobileDevice.getMobilityTrack().add(nextAPId);
                            aMobileDevice.setNextHandoff(-1);
                            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " handoff was scheduled! " + "SourceAp: " + aMobileDevice.getSourceAp().getName() + " NextAp: " + aMobileDevice.getDestinationAp().getName() + "\n");
                            LogMobile.debug("MobileController.java", "Distance between " + aMobileDevice.getName() + " and " + aMobileDevice.getSourceAp().getName() + ": " + Distances.checkDistance(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord()));
                        } else {
                            aMobileDevice.setNextHandoff(-1);
                            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " can't make handoff because don't exist closest nextAp");
                        }
                    } else {
                        if (aMobileDevice.isHandoffArea()) {
                            aMobileDevice.setHandoffArea(false);
                            //FAZ ALGO
                        }
                        if (!LocationHelper.isCovered(aMobileDevice.getSourceAp(), aMobileDevice)) {
                           // aMobileDevice.getMobilityTrack().add(aMobileDevice.getSourceAp().getServerCloudlet().getMyId());
                            System.out.println("================================= OUT OF COVERAGE -> DISCONECTING MD =================================");
                            aMobileDevice.setLastFN(aMobileDevice.getSourceAp().getServerCloudlet());
                            aMobileDevice.getSourceAp().desconnectApSmartThing(aMobileDevice);
                            aMobileDevice.getSourceServerCloudlet().desconnectServerCloudletSmartThing(aMobileDevice);
                            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " X: " + aMobileDevice.getCoord().getCoordX() + " Y: " + aMobileDevice.getCoord().getCoordY());
                        }
                    }

                }
            } else {
                if (ApDevice.connectApSmartThing(getApDevices(), aMobileDevice, getRand().nextDouble())) {
                    System.out.println("================================= NEW CONNECTION TO MOBILE DEVICE =================================");
                    aMobileDevice.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(aMobileDevice);
                    MyStatistics.getInstance().setTotalHandoff(1);
                    saveHandOff(aMobileDevice);
                } else {
                }
            }
        }

    }

    private void checkNS_TCMFOG(MobileDevice aMobileDevice) {
        MyStatistics.getInstance().getEnergyHistory().put(aMobileDevice.getMyId(), aMobileDevice.getEnergyConsumption());
        MyStatistics.getInstance().getPowerHistory().put(aMobileDevice.getMyId(), aMobileDevice.getHost().getPower());
        //If the MD is connected to an AP
        if (aMobileDevice.getSourceAp() != null) {
            //If MD is not migrating or handoffing
            if (aMobileDevice.getNextHandoff() == 0 || aMobileDevice.getNextMigration() == 0) {
                String toSearch = String.valueOf(aMobileDevice.getSourceServerCloudlet().getMyId());
                double estimatedTime = AppExemplo2.mobilityPrediction.predictedTime(aMobileDevice.getMyId(), toSearch) * 1000;
                aMobileDevice.setNextHandoff(CloudSim.clock() + estimatedTime);
                double estimatedMigTime = AppExemplo2.calculateMigTime(aMobileDevice);
                aMobileDevice.setNextMigration(CloudSim.clock() + estimatedTime - estimatedMigTime);
                //System.out.println("=======================================================");
                //System.out.println("Search string:" + toSearch + " | Estimated Time: " + estimatedTime);
                //aMobileDevice.setToSearch(toSearch);
                //System.out.println("Next migration: " + aMobileDevice.getNextMigration());
                //System.out.println("Next handoff: " + aMobileDevice.getNextHandoff());
                //System.out.println("=======================================================");
            }

            if (aMobileDevice.getVmLocalServerCloudlet() != null) {
                AppExemplo2.mobilityLog.get(aMobileDevice.getMyId()).add("@" + CloudSim.clock() + " | " + aMobileDevice.getName() + "(" + aMobileDevice.getSourceAp().getName() + "," + aMobileDevice.getVmLocalServerCloudlet().getName() + ")");
//                AppExemplo2.log.add("@" + CloudSim.clock() + " | " + aMobileDevice.getName() + "(" + aMobileDevice.getSourceAp().getName() + "," + aMobileDevice.getVmLocalServerCloudlet().getName() + ")");
            }
            //If MD is not migrating and its in the predicted time window send a DECICION_MIGRATION
            if ((!aMobileDevice.isLockedToMigration()) && (CloudSim.clock() >= aMobileDevice.getNextMigration())) {
                System.out.println("===== Sending DECISION_MIGRATION for " + aMobileDevice.getName() + "=====");
                sendNow(aMobileDevice.getSourceAp().getServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, aMobileDevice);
            }
            //If MD not locked to handoff
            if (!aMobileDevice.isLockedToHandoff()) {
                //If MD is in the predicted time window to handoff
                if ((aMobileDevice.getNextHandoff() - CloudSim.clock() <= 5000) && (aMobileDevice.getNextHandoff() != -1)) { //Handoff Zone
                    System.out.println("-----> Handoff decision for " + aMobileDevice.getName());
                    int nextAPId = Migration.nextAp(getApDevices(), aMobileDevice, true);
                    //If MD's VM is migrating
                    if (aMobileDevice.getDestinationServerCloudlet() != null) {
                        //If VM is migration to my actual AP abort handoff
                        if (aMobileDevice.getDestinationServerCloudlet().getMyId() == aMobileDevice.getSourceAp().getMyId()) {
                            nextAPId = -1;
                            aMobileDevice.setNextHandoff(-1);
                        }
                    }
                    //If next AP is defined and its not the MD actual AP
                    if (nextAPId >= 0 && nextAPId != aMobileDevice.getSourceAp().getMyId()) {

                        aMobileDevice.setDestinationAp(getApDevices().get(nextAPId));
                        aMobileDevice.setHandoffStatus(true);
                        aMobileDevice.setLockedToHandoff(true);
                        //System.out.println("=====" + aMobileDevice.getName() + " HANDOFF for " + " from " + aMobileDevice.getSourceAp().getName()
                        //      + " to " + aMobileDevice.getDestinationAp().getName() + "=====");
                        System.out.println("-->" + aMobileDevice.getName() + " is handoffing from " + aMobileDevice.getSourceAp().getName() + " to " + aMobileDevice.getDestinationAp().getName());
                        double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * getRand().nextDouble(); //"Maximo" tempo para handoff
                        float handoffLocked = (float) (handoffTime * 10);
                        int delayConnection = 100;
                        //Handoff will be always between cloudlets, so you have to disconect the smarthing every handoff
                        sendNow(aMobileDevice.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, aMobileDevice);
                        send(aMobileDevice.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, aMobileDevice);
                        LogMobile.debug("MobileController.java", aMobileDevice.getName() + " will be desconnected from " + aMobileDevice.getSourceServerCloudlet().getName() + " by handoff");
                        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        send(aMobileDevice.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, aMobileDevice);
                        send(aMobileDevice.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, aMobileDevice);
                        MyStatistics.getInstance().setTotalHandoff(1);
                        saveHandOff(aMobileDevice);
                      //  aMobileDevice.getMobilityTrack().add(nextAPId);
                        aMobileDevice.setNextHandoff(-1);
                        LogMobile.debug("MobileController.java", aMobileDevice.getName() + " handoff was scheduled! " + "SourceAp: " + aMobileDevice.getSourceAp().getName() + " NextAp: " + aMobileDevice.getDestinationAp().getName() + "\n");
                        LogMobile.debug("MobileController.java", "Distance between " + aMobileDevice.getName() + " and " + aMobileDevice.getSourceAp().getName() + ": " + Distances.checkDistance(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord()));
                    } else {
                        aMobileDevice.setNextHandoff(-1);
                        LogMobile.debug("MobileController.java", aMobileDevice.getName() + " can't make handoff because don't exist closest nextAp");
                    }
                } else {
                    if (aMobileDevice.isHandoffArea()) {
                        aMobileDevice.setHandoffArea(false);
                        //FAZ ALGO
                    }
                    if (!LocationHelper.isCovered(aMobileDevice.getSourceAp(), aMobileDevice)) {
                      //  aMobileDevice.getMobilityTrack().add(aMobileDevice.getSourceAp().getServerCloudlet().getMyId());
                        System.out.println("================================= OUT OF COVERAGE -> DISCONECTING MD =================================");
                        aMobileDevice.setLastFN(aMobileDevice.getSourceAp().getServerCloudlet());
                        aMobileDevice.getSourceAp().desconnectApSmartThing(aMobileDevice);
                        aMobileDevice.getSourceServerCloudlet().desconnectServerCloudletSmartThing(aMobileDevice);
                        LogMobile.debug("MobileController.java", aMobileDevice.getName() + " X: " + aMobileDevice.getCoord().getCoordX() + " Y: " + aMobileDevice.getCoord().getCoordY());
                    }
                }

            }
        } else {
            if (ApDevice.connectApSmartThing(getApDevices(), aMobileDevice, getRand().nextDouble())) {
                System.out.println("================================= NEW CONNECTION TO MOBILE DEVICE =================================");
                aMobileDevice.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(aMobileDevice);
                MyStatistics.getInstance().setTotalHandoff(1);
                saveHandOff(aMobileDevice);
            } else {
            }

        }

    }

    private void checkNewStepFMFM() {
        System.out.println("-------------------------------------------------> NEW STEP FMFM");
        int index;
        for (MobileDevice st : getSmartThings()) {
            if (st.getSourceAp() != null && st.getVmLocalServerCloudlet() != null) {
                AppExemplo2.log.add(CloudSim.clock() + " | " + st.getSourceAp().getName() + " - " + st.getVmLocalServerCloudlet().getName());
            }
            MyStatistics.getInstance().getEnergyHistory().put(st.getMyId(), st.getEnergyConsumption());
            MyStatistics.getInstance().getPowerHistory().put(st.getMyId(), st.getHost().getPower());
            //  System.out.println("----------------------------------------------------------------------------------> IS AP NULL?");
            if (st.getSourceAp() != null) {
                //    System.out.println("----------------------------------------------------------------------------------> NOT NULL!");
                System.out.println(st.getName() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY());
                System.out.println(st.getSourceAp().getName() + "\t" + st.getSourceAp().getCoord().getCoordX() + "\t" + st.getSourceAp().getCoord().getCoordY());
                System.out.println(Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord()));
                System.out.println("----------------------------------------------------------------------------------> LOCKED TO MIG??");
                if (!st.isLockedToMigration()) {
                    System.out.println("----------------------------------------------------------------------------------> IS ABLE MIG");
                    //CHANGED HERE
                    if (st.getSourceServerCloudlet() != null && st.getSourceAp() != null) {
                        System.out.println("----------------------------------------------------------------------------------> CLOUDLET NOT NULL");
                        //double distanceFN = Distances.checkDistance(st.getCoord(), st.getSourceServerCloudlet().getCoord());
                        //System.out.println("----------------------------------------------------------------------------------> IS IN MIG ZONE?");
                        //CHANGED HERE
                        if (LocationHelper.isInZone(st.getSourceAp().getServerCloudlet(), st, MaxAndMin.MIG_POINT)) { //MIG Zone
                            System.out.println("----------------------------------------------------------------------------------> IS IN MIG ZONE!");
                            sendNow(st.getSourceAp().getServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, st);
                        }
                    }
                }
                //System.out.println("----------------------------------------------------------------------------------> LOCKED TO HANDOFF?");
                if (!st.isLockedToHandoff()) {
                    System.out.println("----------------------------------------------------------------------------------> ST NOT LOCKED HANDOFF");
                    // double distance = Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord());
                    //System.out.println("Distance " + distance + "Diff " + (MaxAndMin.AP_COVERAGE - MaxAndMin.MAX_DISTANCE_TO_HANDOFF) + " max " + MaxAndMin.AP_COVERAGE);
                    //if (distance >= MaxAndMin.AP_COVERAGE - MaxAndMin.MAX_DISTANCE_TO_HANDOFF && distance < MaxAndMin.AP_COVERAGE) { //Handoff Zone
                    // System.out.println("----------------------------------------------------------------------------------> HANDOFF ZONE?");
                    if (LocationHelper.isInZone(st.getSourceAp(), st, MaxAndMin.MAX_DISTANCE_TO_HANDOFF)) { //Handoff Zone
                        if (!st.isHandoffArea()) {
                            st.setHandoffArea(true);
                        }
                        System.out.println("---------------------------------------------> SEARCHING HANDOFF OPTION");
                        index = Migration.nextAp(getApDevices(), st, true);
                        if (st.getDestinationServerCloudlet() != null) {
                            if (st.getDestinationServerCloudlet().getMyId() == st.getSourceAp().getMyId()) {
                                index = -1;
                            }
                        }
                        if (index >= 0 && index != st.getSourceAp().getMyId()) {
                            System.out.println("---------------------------------->LH: " + index + " - " + getApDevices().size());
                            st.setDestinationAp(getApDevices().get(index));
                            st.setHandoffStatus(true);
                            st.setLockedToHandoff(true);
                            double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * getRand().nextDouble(); //"Maximo" tempo para handoff
                            float handoffLocked = (float) (handoffTime * 10);
                            int delayConnection = 100;
                            //Handoff will be always between cloudlets, so you have to disconect the smarthing every handoff
                            sendNow(st.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, st);
                            send(st.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, st);
                            LogMobile.debug("MobileController.java", st.getName() + " will be desconnected from " + st.getSourceServerCloudlet().getName() + " by handoff");
                            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                            send(st.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, st);
                            send(st.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, st);
                            MyStatistics.getInstance().setTotalHandoff(1);
                            saveHandOff(st);

                         //   st.getMobilityTrack().add(index);

                            LogMobile.debug("MobileController.java", st.getName() + " handoff was scheduled! " + "SourceAp: " + st.getSourceAp().getName()
                                    + " NextAp: " + st.getDestinationAp().getName() + "\n");
                            LogMobile.debug("MobileController.java", "Distance between " + st.getName() + " and " + st.getSourceAp().getName() + ": "
                                    + Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord()));
                        } else {
                            LogMobile.debug("MobileController.java", st.getName() + " can't make handoff because don't exist closest nextAp");
                        }
                    } else {
                        if (st.isHandoffArea()) {
                            st.setHandoffArea(false);
                            //FAZ ALGO
                        }
                        if (!LocationHelper.isCovered(st.getSourceAp(), st)) {
                            System.out.println("------------------------------------------->>>>>>>>>> DESCONECTANDO");
                            st.setLastFN(st.getSourceAp().getServerCloudlet());
                            st.getSourceAp().desconnectApSmartThing(st);
                            st.getSourceServerCloudlet().desconnectServerCloudletSmartThing(st);
                            // LogMobile.debug("MobileController.java", st.getName() + " desconnected by AP_COVERAGE - Distance: " + distance);
                            LogMobile.debug("MobileController.java", st.getName() + " X: " + st.getCoord().getCoordX() + " Y: " + st.getCoord().getCoordY());
                        }
                    }
                }
            } else {
                if (ApDevice.connectApSmartThing(getApDevices(), st, getRand().nextDouble())) {
                    System.out.println("------------------------------------------> FAKE - CONNECTION AGAIN AND CREATING NEW APPLICATION");
                    st.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(st);
                    MyStatistics.getInstance().setTotalHandoff(1);
                } else {
                    //To do something
                }
            }
        }
    }

    private static void saveHandOff(MobileDevice st) {
        //System.out.println("HANDOFF " + st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
        try (FileWriter fw = new FileWriter(st.getMyId() + "handoff.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            if (st.getDestinationAp() == null) {
                out.println(st.getMyId() + "\t" + CloudSim.clock() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY() + "\t" + st.getDirection() + "\t" + st.getSpeed() + "\t" + st.getSourceAp() + "\t" + " - FROM DC");
            } else {
                out.println(st.getMyId() + "\t" + CloudSim.clock() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY() + "\t" + st.getDirection() + "\t" + st.getSpeed() + "\t" + st.getSourceAp() + "\t" + st.getDestinationAp());
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void shutdownEntity() {
        // TODO Auto-generated method stub

    }

    private void printCostDetails() {
        //System.out.println("Cost of execution in cloud = "+getCloud().getTotalCost());
    }

    private FogDevice getCloud() {
        for (FogDevice dev : getServerCloudlets()) {
            if (dev.getName().equals("cloud")) {
                return dev;
            }
        }
        return null;
    }

    public void printResults(String a, String filename) {
        try (FileWriter fw1 = new FileWriter(filename, true);
                BufferedWriter bw1 = new BufferedWriter(fw1);
                PrintWriter out1 = new PrintWriter(bw1)) {
            out1.println(a);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void printPowerDetails() {
        // TODO Auto-generated method stub
        for (FogDevice fogDevice : getServerCloudlets()) {
            System.out.println(fogDevice.getName() + ": Power = " + fogDevice.getHost().getPower());
            System.out.println(fogDevice.getName() + ": Energy Consumed = " + fogDevice.getEnergyConsumption());
        }
        for (int i = 0; i < MyStatistics.getInstance().getPowerHistory().size(); i++) {
            System.out.println("SmartThing" + i + ": Power = " + MyStatistics.getInstance().getPowerHistory().get(i));

        }
        double energyConsumedMean = 0.0;
        for (int i = 0; i < MyStatistics.getInstance().getEnergyHistory().size(); i++) {
            System.out.println("SmartThing" + i + ": Energy Consumed = " + MyStatistics.getInstance().getEnergyHistory().get(i));
            printResults(String.valueOf(MyStatistics.getInstance().getEnergyHistory().get(i)), "resultados.txt");
            energyConsumedMean += MyStatistics.getInstance().getEnergyHistory().get(i);
        }
        printResults(String.valueOf(energyConsumedMean / MyStatistics.getInstance().getEnergyHistory().size()), "averageEnergyHistory.txt");
    }

    private String getStringForLoopId(int loopId) {
        for (String appId : getApplications().keySet()) {
            Application app = getApplications().get(appId);
            for (AppLoop loop : app.getLoops()) {
                if (loop.getLoopId() == loopId) {
                    return loop.getModules().toString();
                }
            }
        }
        return null;
    }

    private double[] latencyAverage;

    private void printTimeDetails() {
        latencyAverage = new double[getSmartThings().size()];
        int latencyIndex = 0;
        System.out.println("=========================================");
        System.out.println("============== RESULTS ==================");
        System.out.println("=========================================");
        System.out.println("EXECUTION TIME : " + (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
        System.out.println("=========================================");
        System.out.println("APPLICATION LOOP DELAYS");
        System.out.println("=========================================");
        double mediaLatencia = 0.0;
        double mediaLatenciaMax = 0.0;
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
            //			double average = 0, count = 0;
            //			for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
            //				Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
            //				Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
            //				if(startTime == null || endTime == null)
            //					break;
            //				average += endTime-startTime;
            //				count += 1;
            //			}
            //			System.out.println(getStringForLoopId(loopId) + " ---> "+(average/count));
            System.out.println(loopId + " - " + getStringForLoopId(loopId) + " ---> " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId) + " MaxExecutionTime: " + TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId));
            latencyAverage[latencyIndex++] = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
            printResults(String.valueOf(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)), "resultados.txt");
            printResults(String.valueOf(TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId)), "resultados.txt");
            mediaLatencia += TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
            mediaLatenciaMax += TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId);
        }
        printResults(String.valueOf(mediaLatencia / TimeKeeper.getInstance().getLoopIdToCurrentAverage().keySet().size()), "averageLoopIdToCurrentAverage.txt");
        printResults(String.valueOf(mediaLatenciaMax / TimeKeeper.getInstance().getMaxLoopExecutionTime().keySet().size()), "averageMaxLoopExecutionTime.txt");
        System.out.println("=========================================");
        System.out.println("TUPLE CPU EXECUTION DELAY");
        System.out.println("=========================================");

        for (String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
            System.out.println(tupleType + " ---> " + TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
        }

        System.out.println("=========================================");
    }

    private void printNetworkUsageDetails() {
        System.out.println("Total network usage = " + (int) NetworkUsageMonitor.getNetworkUsage() / CloudSim.clock());//Config.MAX_SIMULATION_TIME);
        printResults(String.valueOf((int) NetworkUsageMonitor.getNetworkUsage() / CloudSim.clock()) + '\t' + String.valueOf((int) NetworkUsageMonitor.getNetworkUsage()) + '\t' + CloudSim.clock(), "resultados.txt");
        printResults(String.valueOf((int) NetworkUsageMonitor.getNetworkUsage() / CloudSim.clock()) + '\t' + String.valueOf((int) NetworkUsageMonitor.getNetworkUsage()) + '\t' + CloudSim.clock(), "totalNetworkUsage.txt");
    }

    private void printMigrationsDetalis() {
        System.out.println("=========================================");
        System.out.println("==============MIGRATIONS=================");
        System.out.println("=========================================");
        for (int i = 0; i < this.smartThings.size(); i++) {
            //System.out.println(smartThings.get(i).getName() + "| " + smartThings.get(i).getAlgorithm() + " - " + smartThings.get(i).getMigrations());
            System.out.println(smartThings.get(i).getMigrations() + "\t" + smartThings.get(i).getTotalTuples() + "\t" + smartThings.get(i).getTotalLostTuples());
        }
        System.out.println("=========================================");
        System.out.println("Total of migrations: " + MyStatistics.getInstance().getTotalMigrations());
        System.out.println("Total of handoff: " + MyStatistics.getInstance().getTotalHandoff());
        System.out.println("Total of migration to differents SC: " + MyStatistics.getInstance().getMyCountLowestLatency());

        printResults(String.valueOf(MyStatistics.getInstance().getTotalMigrations()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getTotalHandoff()), "resultados.txt");

        printResults(String.valueOf(MyStatistics.getInstance().getTotalMigrations()), "totalMigrations.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getMyCountLowestLatency()), "totalMyCountLowestLatency.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getTotalHandoff()), "totalHandoff.txt");

        MyStatistics.getInstance().printResults();
        System.out.println("***Last time without connection***");

        for (Entry<Integer, Double> test : MyStatistics.getInstance().getWithoutConnectionTime().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getWithoutConnectionTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxWithoutConnectionTime().get(test.getKey()));
        }

        System.out.println("Average of without connection: " + MyStatistics.getInstance().getAverageWithoutConnection());

        printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutConnection()), "resultados.txt");

        System.out.println("***Last time without Vm***");

        for (Entry<Integer, Double> test : MyStatistics.getInstance().getWithoutVmTime().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getWithoutVmTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxWithoutVmTime().get(test.getKey()));
        }

        System.out.println("Average of without Vm: " + MyStatistics.getInstance().getAverageWithoutVmTime());
        printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutVmTime()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutVmTime()), "averageWithoutVmTime.txt");

        System.out.println("===Last delay after connection===");
        for (Entry<Integer, Double> test : MyStatistics.getInstance().getDelayAfterNewConnection().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getDelayAfterNewConnection().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxDelayAfterNewConnection().get(test.getKey()));
        }
        System.out.println("Average of delay after new Connection: " + MyStatistics.getInstance().getAverageDelayAfterNewConnection());
        printResults(String.valueOf(MyStatistics.getInstance().getAverageDelayAfterNewConnection()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getAverageDelayAfterNewConnection()), "averageDelayAfterNewConnection.txt");

        System.out.println("---Average of Time of Migrations---");
        double totalTempoMigracaoMax = 0.0;
        for (Entry<Integer, Double> test : MyStatistics.getInstance().getMigrationTime().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getMigrationTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey()));
            printResults(String.valueOf(MyStatistics.getInstance().getMigrationTime().get(test.getKey())), "resultados.txt");
            printResults(String.valueOf(MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey())), "resultados.txt");
            totalTempoMigracaoMax += MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey());
        }
        System.out.println("Average of Time of Migrations: " + MyStatistics.getInstance().getAverageMigrationTime());
        printResults(String.valueOf(MyStatistics.getInstance().getAverageMigrationTime()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getAverageMigrationTime()), "averageMigrationTime.txt");
        printResults(String.valueOf(totalTempoMigracaoMax / MyStatistics.getInstance().getMigrationTime().entrySet().size()), "averageMigrationMaxTime.txt");
        System.out.println("Tuple lost: " + (((double) MyStatistics.getInstance().getMyCountLostTuple() / MyStatistics.getInstance().getMyCountTotalTuple())) * 100 + "%");
        System.out.println("Tuple lost: " + MyStatistics.getInstance().getMyCountLostTuple());
        System.out.println("Total tuple: " + MyStatistics.getInstance().getMyCountTotalTuple());
        System.out.println("------------------------------------------");
        for (Map.Entry<Integer, ArrayList<String>> entry : AppExemplo2.mobilityLog.entrySet()) {
            System.out.println("-------------------" + entry.getKey() + "-----------------------");
            for (String line : entry.getValue()) {
                System.out.println(line);
            }
            System.out.println(" ");
        }
//        for (String a : AppExemplo2.log) {
//            System.out.println(a);
//        }

        printResults("===============================================================", "resultados.txt");
        for (int i = 0; i < this.smartThings.size(); i++) {
            printResults(latencyAverage[i] + "\t" + smartThings.get(i).getMigrations() + "\t" + smartThings.get(i).getTotalTuples() + "\t" + smartThings.get(i).getTotalLostTuples(), "resultados.txt");
        }

    }

    public void submitApplication(Application application, int delay) {
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        getAppLaunchDelays().put(application.getAppId(), delay);
        for (MobileDevice st : getSmartThings()) {
            for (Sensor s : st.getSensors()) {
                if (s.getAppId().equals(application.getAppId())) {
                    s.setApp(application);
                }
            }
            for (Actuator a : st.getActuators()) {
                if (a.getAppId().equals(application.getAppId())) {
                    a.setApp(application);
                }
            }
        }
        for (AppEdge edge : application.getEdges()) {
            if (edge.getEdgeType() == AppEdge.ACTUATOR) {
                String moduleName = edge.getSource();
                for (MobileDevice st : getSmartThings()) {
                    for (Actuator actuator : st.getActuators()) {
                        if (actuator.getActuatorType().equalsIgnoreCase(edge.getDestination())) {
                            application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
                        }
                    }
                }
            }
        }

    }

    public void submitApplicationMigration(MobileDevice smartThing, Application application, int delay) {
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        getAppLaunchDelays().put(application.getAppId(), delay);

        //			for(Sensor s : smartThing.getSensors()){
        ////				if(s.getAppId().equals(application.getAppId()))
        //					s.setApp(application);
        //			}
        //			for(Actuator a : smartThing.getActuators()){
        ////				if(a.getAppId().equals(application.getAppId()))
        //					a.setApp(application);
        //			}
        //
        for (AppEdge edge : application.getEdges()) {
            if (edge.getEdgeType() == AppEdge.ACTUATOR) {
                String moduleName = edge.getSource();
                for (MobileDevice st : getSmartThings()) {
                    for (Actuator actuator : st.getActuators()) {
                        if (actuator.getActuatorType().equalsIgnoreCase(edge.getDestination())) {
                            application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
                        }
                    }
                }
            }
        }

    }

    public Map<String, Application> getApplications() {
        return applications;
    }

    public void setApplications(Map<String, Application> applications) {
        this.applications = applications;
    }

    public Map<String, Integer> getAppLaunchDelays() {
        return appLaunchDelays;
    }

    public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
        this.appLaunchDelays = appLaunchDelays;
    }

    public ModuleMapping getModuleMapping() {
        return moduleMapping;
    }

    public void setModuleMapping(ModuleMapping moduleMapping) {
        this.moduleMapping = moduleMapping;
    }

    public Map<Integer, Double> getGlobalCurrentCpuLoad() {
        return globalCurrentCpuLoad;
    }

    public void setGlobalCurrentCpuLoad(Map<Integer, Double> globalCurrentCpuLoad) {
        this.globalCurrentCpuLoad = globalCurrentCpuLoad;
    }

    public void setGlobalCPULoad(Map<Integer, Double> currentCpuLoad) {
        for (FogDevice device : getServerCloudlets()) {
            this.globalCurrentCpuLoad.put(device.getId(), currentCpuLoad.get(device.getId()));
        }
    }

    public static int getMigPointPolicy() {
        return migPointPolicy;
    }

    public static void setMigPointPolicy(int migPointPolicy) {
        MobileController2.migPointPolicy = migPointPolicy;
    }

    public static int getMigStrategyPolicy() {
        return migStrategyPolicy;
    }

    public static void setMigStrategyPolicy(int migStrategyPolicy) {
        MobileController2.migStrategyPolicy = migStrategyPolicy;
    }

    public static int getStepPolicy() {
        return stepPolicy;
    }

    public static void setStepPolicy(int stepPolicy) {
        MobileController2.stepPolicy = stepPolicy;
    }

    public static Coordinate getCoordDevices() {
        return coordDevices;
    }

    public static void setCoordDevices(Coordinate coordDevices) {
        MobileController2.coordDevices = coordDevices;
    }

    public List<FogBroker> getBrokerList() {
        return brokerList;
    }

    public void setBrokerList(List<FogBroker> brokerList) {
        this.brokerList = brokerList;
    }

    public static int getSeed() {
        return seed;
    }

    public static void setSeed(int seed) {
        MobileController2.seed = seed;
    }

    public static List<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }

    public static void setServerCloudlets(List<FogDevice> serverCloudlets) {
        MobileController2.serverCloudlets = serverCloudlets;
    }

    public static List<MobileDevice> getSmartThings() {
        return smartThings;
    }

    public static void setSmartThings(List<MobileDevice> smartThings) {
        MobileController2.smartThings = smartThings;
    }

    public static List<ApDevice> getApDevices() {
        return apDevices;
    }

    public static void setApDevices(List<ApDevice> apDevices) {
        MobileController2.apDevices = apDevices;
    }

    public static Random getRand() {
        return rand;
    }

    public static void setRand(Random rand) {
        MobileController2.rand = rand;
    }

    public static boolean isMigrationAble() {
        return migrationAble;
    }

    public static void setMigrationAble(boolean migrationAble) {
        MobileController2.migrationAble = migrationAble;
    }

}
