package org.fog.cmfog;

import org.fog.cmfog.helpers.CMFogHelper;
import org.fog.cmfog.helpers.LocationHelper;
import org.fog.cmfog.helpers.NetworkHelper;
import org.fog.cmfog.helpers.LogHelper;
import org.fog.placement.*;
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

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.ApDevice;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.entities.Sensor;
import org.fog.localization.Distances;
import org.fog.cmfog.enumerate.Algorithm;

import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.vmmigration.Migration;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.NextStep;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.MobileEvents;

public class CMFogMobileController extends SimEntity {

    private static boolean migrationEnabled;
    private static int migrationPointPolicy;
    private static int stepPolicy; //Quantity of steps in the nextStep Function
    private static int migrationStrategyPolicy;
    private static List<FogDevice> cloudletsL1;
    private static List<FogDevice> cloudletsL2;
    private static List<MobileDevice> mobileDevices;
    private static List<ApDevice> accessPoints;
    private static List<FogBroker> brokers;
    private Map<String, Application> applications;
    private Map<String, Integer> appLaunchDelays;
    private ModuleMapping moduleMapping;
    private Map<Integer, Double> globalCurrentCpuLoad;
    private double[] latencyAverage;

    public CMFogMobileController() {

    }

    public CMFogMobileController(String name, List<FogDevice> cloudletsL1, List<FogDevice> cloudletsL2, List<ApDevice> apDevices, List<MobileDevice> smartThings, List<FogBroker> brokers, ModuleMapping moduleMapping,
            int migPointPolicy, int migStrategyPolicy, int stepPolicy, boolean migrationAble) {
        // TODO Auto-generated constructor stub
        super(name);
        this.applications = new HashMap<String, Application>();
        this.globalCurrentCpuLoad = new HashMap<Integer, Double>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setModuleMapping(moduleMapping);
        for (FogDevice sc : cloudletsL1) {
            sc.setControllerId(getId());
        }
        for (FogDevice sc : cloudletsL2) {
            sc.setControllerId(getId());
        }
        setCloudletsL1(cloudletsL1);
        setCloudletsL2(cloudletsL2);
        setAccessPoints(apDevices);
        setMobileDevices(smartThings);
        setBrokers(brokers);
        setMigrationPointPolicy(migPointPolicy);
        setMigrationStrategyPolicy(migStrategyPolicy);
        setStepPolicy(stepPolicy);

        connectWithLatencies();
        initializeCPULoads();

        setMigrationEnabled(migrationAble);
    }

    private void connectWithLatencies() {
        for (FogDevice st : getMobileDevices()) {
            FogDevice parent = CMFogHelper.getCloudletById(st.getParentId(), getCloudletsL1());
            if (parent == null) {
                continue;
            }
            double latency = st.getUplinkLatency();
            parent.getChildToLatencyMap().put(st.getId(), latency);
            parent.getChildrenIds().add(st.getId());
        }
    }

    private void initializeCPULoads() {
        for (FogDevice aCloudlet : getCloudletsL1()) {
            this.globalCurrentCpuLoad.put(aCloudlet.getId(), 0.0);
        }
        for (FogDevice aCloudlet : getCloudletsL2()) {
            this.globalCurrentCpuLoad.put(aCloudlet.getId(), 0.0);
        }
        for (MobileDevice aMobileDevice : getMobileDevices()) {
            this.globalCurrentCpuLoad.put(aMobileDevice.getId(), 0.0);
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

        for (MobileDevice aMobileDevice : getMobileDevices()) {
            System.out.println("MobileDevice(" + aMobileDevice.getName() + ") starting @ " + aMobileDevice.getStartTravelTime() * 1000 + "s");
            send(getId(), aMobileDevice.getStartTravelTime() * 1000, MobileEvents.CREATE_NEW_SMARTTHING, aMobileDevice);
        }
        //?
        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
        //?
        for (FogDevice aCloudlet : getCloudletsL1()) {
            sendNow(aCloudlet.getId(), FogEvents.RESOURCE_MGMT);
        }
        for (FogDevice aCloudlet : getCloudletsL2()) {
            sendNow(aCloudlet.getId(), FogEvents.RESOURCE_MGMT);
        }
        //Stop simulation
        send(getId(), MaxAndMin.MAX_SIMULATION_TIME, MobileEvents.STOP_SIMULATION);

        System.out.println("=================================================================================================================================");
        System.out.println("=================================================================================================================================");
        System.out.println("=============================================== STARTING SIMULATION =============================================================");
        System.out.println("=================================================================================================================================");
        System.out.println("=================================================================================================================================");
        for (MobileDevice aMobileDevice : mobileDevices) {
            aMobileDevice.getMigrationTrack().add(aMobileDevice.getVmLocalServerCloudlet());
            if (aMobileDevice.getAlgorithm() == Algorithm.TCMFOG) {
                if (aMobileDevice.getNextHandoff() == 0 || aMobileDevice.getNextMigration() == 0) {
                    //Set next handoff
                    String toSearch = String.valueOf(aMobileDevice.getSourceServerCloudlet().getMyId());
                    double estimatedTime = CMFog.mobilityPredictionL1.predictedTime(aMobileDevice.getMyId(), toSearch) * 1000;
                    //aMobileDevice.setNextHandoff(CloudSim.clock() + estimatedTime);
                    //System.out.println("=================================================================================================================================");
                    //System.out.println("Initial Handoff setup for: " + (CloudSim.clock() + estimatedTime));

                    //Set next migration
                    toSearch = String.valueOf(aMobileDevice.getVmLocalServerCloudlet().getMyId());
                    if (aMobileDevice.getVmLocalServerCloudlet().getLayer() == 1) {
                        estimatedTime = CMFog.mobilityPredictionL1.predictedTime(aMobileDevice.getMyId(), toSearch) * 1000;
                    } else {
                        estimatedTime = CMFog.mobilityPredictionL2.predictedTime(aMobileDevice.getMyId(), toSearch) * 1000;
                    }
                    double estimatedMigTime = NetworkHelper.calculateInitialMigTime(aMobileDevice); //2021
                    estimatedMigTime += estimatedMigTime * 0.25;
                    aMobileDevice.setNextMigration(CloudSim.clock() + estimatedTime - estimatedMigTime);
                    System.out.println("Initial Migration setup for: " + (CloudSim.clock() + estimatedTime - estimatedMigTime));
                    System.out.println("=================================================================================================================================");

                }
            }
        }
        MonitoringAgent.configureAgent();
    }

    private void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        processAppSubmit(app);
    }

    private void processAppSubmit(Application application) {

        System.out.println("Submitting App" + application.getAppId());
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        List<FogDevice> tempAllDevices = new ArrayList<>();
        for (FogDevice sc : getCloudletsL1()) {
            tempAllDevices.add(sc);
        }
        for (FogDevice sc : getCloudletsL2()) {
            tempAllDevices.add(sc);
        }
        for (MobileDevice st : getMobileDevices()) {
            tempAllDevices.add(st);
        }

        ModulePlacement modulePlacement = new ModulePlacementMapping(tempAllDevices//getServerCloudlets()
                ,
                 application, getModuleMapping(), globalCurrentCpuLoad);

        for (FogDevice fogDevice : getCloudletsL1()) {
            sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }
        for (MobileDevice st : getMobileDevices()) {
            sendNow(st.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }

        Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
        Map<Integer, Map<String, Integer>> instanceCountMap = modulePlacement.getModuleInstanceCountMap();
        for (Integer deviceId : deviceToModuleMap.keySet()) {
            for (AppModule module : deviceToModuleMap.get(deviceId)) {
                //System.out.println("MobileController 240 ProcessAppSubmit");
                System.out.println(CMFogHelper.getCloudletById(deviceId).getName() + " - " + module.getName());
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
                NextStep.nextStep(getCloudletsL1(), getAccessPoints(), getMobileDevices(), getStepPolicy());
                break;
            case MobileEvents.CREATE_NEW_SMARTTHING:
                createNewSmartThing(event);
                break;
            case MobileEvents.CHECK_NEW_STEP:
                for (MobileDevice aMobileDevice : CMFog.mobileDevices) {
                    switch (aMobileDevice.getAlgorithm()) {
                        case LL:
                            System.out.println("Checking LOWEST_LATENCY");
                            checkLL();
                            break;
                        case CCMFOG:
                            System.out.println("Checking CCMFOG for " + aMobileDevice.getName());
                            checkCCMFog(aMobileDevice);
                            break;
                        case TCMFOG:
                            if (CMFogHelper.isConnectedToNetwork(aMobileDevice)) {
                                System.out.println("Checking TCMFOG for " + aMobileDevice.getName() + "[" + aMobileDevice.getSourceAp().getName() + "] - [" + aMobileDevice.isHandoffStatus() + "," + aMobileDevice.isMigStatus() + "]");
                            } else {
                                System.out.println("Checking TCMFOG for " + aMobileDevice.getName() + "[NULL] - [" + aMobileDevice.isHandoffStatus() + "," + aMobileDevice.isMigStatus() + "]");
                            }
                            checkTCMFog(aMobileDevice);
                            break;
                        default:
                            break;
                    }
                    MonitoringAgent.calculateTSPoint(aMobileDevice);
                    MonitoringAgent.checkVariation(aMobileDevice);
                }

                if (CMFog.mobileDevices.isEmpty()) {
                    System.out.println("Ending request: " + CloudSim.clock());
                    sendNow(getId(), MobileEvents.STOP_SIMULATION);
                }
                break;
            case MobileEvents.STOP_SIMULATION:
                System.out.println("*********************myStopSimulation MobilieController 149 ***********");
                System.out.println("CloudSim.clock(): " + CloudSim.clock());
                System.out.println("Size SmartThings: " + getMobileDevices().size());
                CloudSim.stopSimulation();
                printTimeDetails();
                // printPowerDetails();
                // printCostDetails();
                printNetworkUsageDetails();
                printMigrationsDetalis();
                printAlgorithmHits();
                MonitoringAgent.printReport();
                System.exit(0);
                break;

        }
    }

    public void finishSimulation() {
        sendNow(getId(), MobileEvents.STOP_SIMULATION);
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

    // Marcelo C. S. Araujo
    //
    private void checkLL() {
        System.out.println("-------------------------------------------------> NEW STEP LOWEST LATENCY");
        int index = 0;
        for (MobileDevice st : getMobileDevices()) {
            if (st.getSourceAp() != null && st.getVmLocalServerCloudlet() != null) {
                CMFog.log.add(CloudSim.clock() + " | " + st.getSourceAp().getName() + " - " + st.getVmLocalServerCloudlet().getName());
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
                        index = Migration.nextAp(getAccessPoints(), st);
                        if (index >= 0) {
                            st.setDestinationAp(getAccessPoints().get(index));
                            st.setHandoffStatus(true);
                            st.setLockedToHandoff(true);
                            double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * SimulationParameters.getRand().nextDouble(); //"Maximo" tempo para handoff
                            float handoffLocked = (float) (handoffTime * 20);
                            int delayConnection = 100; //connection between SmartT and ServerCloudlet
                            if (!st.getDestinationAp().getServerCloudlet().equals(st.getSourceServerCloudlet())) {
                                System.out.println("-------------------------------------------------------> CLOUDLET DIFF");
                                if (isMigrationEnabled()) {
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
                if (ApDevice.connectApSmartThing(getAccessPoints(), st, SimulationParameters.getRand().nextDouble())) {
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

    private void mobilityLog(MobileDevice aMobileDevice) {
        boolean algorithmMiss = false;
        if (aMobileDevice.getSourceAp() != null && aMobileDevice.getVmLocalServerCloudlet() != null) {
            String complement = ")";
            if (aMobileDevice.getVmLocalServerCloudlet().getLayer() == 1 && aMobileDevice.getSourceAp().getMyId() != aMobileDevice.getVmLocalServerCloudlet().getMyId()) {
                complement = ",xxxxx)";
                algorithmMiss = true;
            }
            if (aMobileDevice.getVmLocalServerCloudlet().getLayer() == 2) {
                if (aMobileDevice.getSourceAp().getServerCloudlet().getParentId() != aMobileDevice.getVmLocalServerCloudlet().getId()) {
                    complement = ",xxxxx)";
                    algorithmMiss = true;
                }
            }
            if (algorithmMiss) {
                CMFog.algorithmMiss.put(aMobileDevice.getName(), (CMFog.algorithmMiss.get(aMobileDevice.getName()) + 1));
                MonitoringAgent.pairingCheck.add(0);
            } else {
                MonitoringAgent.pairingCheck.add(1);
            }
            CMFog.mobilityLog.get(aMobileDevice.getMyId()).add("@" + CloudSim.clock() + " | " + aMobileDevice.getName() + "(" + aMobileDevice.getSourceAp().getName() + "," + aMobileDevice.getVmLocalServerCloudlet().getName() + complement);
        }
    }

    private void checkCCMFog(MobileDevice aMobileDevice) {
        int index = 0;
        mobilityLog(aMobileDevice);
        MyStatistics.getInstance().getEnergyHistory().put(aMobileDevice.getMyId(), aMobileDevice.getEnergyConsumption());
        MyStatistics.getInstance().getPowerHistory().put(aMobileDevice.getMyId(), aMobileDevice.getHost().getPower());
        if (aMobileDevice.getSourceAp() != null && !aMobileDevice.isLockedToHandoff()) {
            double distance = Distances.checkDistance(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord());
            if (LocationHelper.isInZone(aMobileDevice.getSourceAp(), aMobileDevice, MaxAndMin.MAX_DISTANCE_TO_HANDOFF)) { //Handoff Zone
                System.out.println("-----> Searching handoff option");
                index = Migration.nextAp(getAccessPoints(), aMobileDevice);
                if (index >= 0) {
                    aMobileDevice.setDestinationAp(getAccessPoints().get(index));
                    aMobileDevice.setHandoffStatus(true);
                    aMobileDevice.setLockedToHandoff(true);
                    double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * SimulationParameters.getRand().nextDouble(); //"Maximo" tempo para handoff
                    float handoffLocked = (float) (handoffTime * 20);
                    int delayConnection = 100; //connection between SmartT and ServerCloudlet
                    if (!aMobileDevice.getDestinationAp().getServerCloudlet().equals(aMobileDevice.getSourceServerCloudlet())) {
                        //System.out.println("-------------------------------------------------------> CLOUDLET DIFF");
                        if (isMigrationEnabled()) {
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
        } else {
            if (ApDevice.connectApSmartThing(getAccessPoints(), aMobileDevice, SimulationParameters.getRand().nextDouble())) {
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

    private void checkTCMFog(MobileDevice aMobileDevice) {
        aMobileDevice.checkStatusAP();
        if (CMFogHelper.isConnectedToNetwork(aMobileDevice)) {
            mobilityLog(aMobileDevice);
            sendNow(aMobileDevice.getSourceAp().getServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, aMobileDevice);
            //sendNow(aMobileDevice.getId(), MobileEvents.MAKE_DECISION_HANDOFF, aMobileDevice);
            System.out.println("Time for the next migration: " + (int) (aMobileDevice.getNextMigration() - CloudSim.clock()));
//            if ((aMobileDevice.getNextMigration() - CloudSim.clock()) <= 5000 && !aMobileDevice.isMigStatus()) {
//                System.out.println("fix fast foward error");
//                aMobileDevice.performHandoff();
//            }
        }
    }

    private static void saveHandOff(MobileDevice st) {
        //System.out.println("HANDOFF " + st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
        try ( FileWriter fw = new FileWriter(st.getMyId() + "handoff.txt", true);  BufferedWriter bw = new BufferedWriter(fw);  PrintWriter out = new PrintWriter(bw)) {
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

    public void printResults(String a, String filename) {
        try ( FileWriter fw1 = new FileWriter(filename, true);  BufferedWriter bw1 = new BufferedWriter(fw1);  PrintWriter out1 = new PrintWriter(bw1)) {
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
        for (FogDevice fogDevice : getCloudletsL1()) {
            System.out.println(fogDevice.getName() + ": Power = " + fogDevice.getHost().getPower());
            System.out.println(fogDevice.getName() + ": Energy Consumed = " + fogDevice.getEnergyConsumption());
        }
        for (int i = 0; i < MyStatistics.getInstance().getPowerHistory().size(); i++) {
            System.out.println("SmartThing" + i + ": Power = " + MyStatistics.getInstance().getPowerHistory().get(i));

        }
        double energyConsumedMean = 0.0;
        for (int i = 0; i < MyStatistics.getInstance().getEnergyHistory().size(); i++) {
            System.out.println("SmartThing" + i + ": Energy Consumed = " + MyStatistics.getInstance().getEnergyHistory().get(i));
            //printResults(String.valueOf(MyStatistics.getInstance().getEnergyHistory().get(i)), "resultados.txt");
            energyConsumedMean += MyStatistics.getInstance().getEnergyHistory().get(i);
        }
        //printResults(String.valueOf(energyConsumedMean / MyStatistics.getInstance().getEnergyHistory().size()), "averageEnergyHistory.txt");
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

    private void printTimeDetails() {
        latencyAverage = new double[getMobileDevices().size()];
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
        printResults("================== RESULTS ==================", "results.txt");
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
            printResults(String.valueOf(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)), "resultSummary" + (loopId - 1) + ".txt");//<<<<<<
            printResults(getStringForLoopId(loopId) + " = " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId) + " | " + TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId), "results.txt");
            System.out.println(loopId + " - " + getStringForLoopId(loopId) + " ---> " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId) + " MaxExecutionTime: " + TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId));
            latencyAverage[latencyIndex++] = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
            //printResults(String.valueOf(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)), "resultados.txt");
            //printResults(String.valueOf(TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId)), "resultados.txt");
            mediaLatencia += TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
            mediaLatenciaMax += TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId);
        }
        //printResults(String.valueOf(mediaLatencia / TimeKeeper.getInstance().getLoopIdToCurrentAverage().keySet().size()), "averageLoopIdToCurrentAverage.txt");
        //printResults(String.valueOf(mediaLatenciaMax / TimeKeeper.getInstance().getMaxLoopExecutionTime().keySet().size()), "averageMaxLoopExecutionTime.txt");
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
        //printResults(String.valueOf((int) NetworkUsageMonitor.getNetworkUsage() / CloudSim.clock()) + '\t' + String.valueOf((int) NetworkUsageMonitor.getNetworkUsage()) + '\t' + CloudSim.clock(), "resultados.txt");
        //printResults(String.valueOf((int) NetworkUsageMonitor.getNetworkUsage() / CloudSim.clock()) + '\t' + String.valueOf((int) NetworkUsageMonitor.getNetworkUsage()) + '\t' + CloudSim.clock(), "totalNetworkUsage.txt");
    }

    private void printMigrationsDetalis() {
        System.out.println("=========================================");
        System.out.println("==============MIGRATIONS=================");
        System.out.println("=========================================");
        for (int i = 0; i < this.mobileDevices.size(); i++) {
            //System.out.println(smartThings.get(i).getName() + "| " + smartThings.get(i).getAlgorithm() + " - " + smartThings.get(i).getMigrations());
            System.out.println(mobileDevices.get(i).getName() + " \t " + mobileDevices.get(i).getMigrations() + " \t " + mobileDevices.get(i).getTotalTuples() + " \t " + (mobileDevices.get(i).getTotalLostTuples() / mobileDevices.get(i).getTotalTuples()) + "%");
            //   printResults(mobileDevices.get(i).getName() + "\t" + mobileDevices.get(i).getMigrations() + "\t" + mobileDevices.get(i).getTotalTuples() + "\t" + mobileDevices.get(i).getTotalLostTuples() + "", "results.txt");
        }

        for (Map.Entry<String, Integer> anEntry : LogHelper.totalTuples.entrySet()) {
            Integer anID = Integer.valueOf(anEntry.getKey().replace("MyApp_vr_game", ""));
            double lostPercentage = Double.valueOf(LogHelper.lostTuples.get(anEntry.getKey()) / Double.valueOf(LogHelper.totalTuples.get(anEntry.getKey()))) * 100;
            printResults(anEntry.getKey() + " \t " + mobileDevices.get(anID).getMigrations() + " \t " + LogHelper.totalTuples.get(anEntry.getKey()) + " \t " + LogHelper.lostTuples.get(anEntry.getKey()) + " \t " + lostPercentage + "%", "results.txt");
            printResults(String.valueOf(mobileDevices.get(anID).getMigrations()), "resultSummary" + anID + ".txt");
            printResults(String.valueOf(LogHelper.totalTuples.get(anEntry.getKey())), "resultSummary" + anID + ".txt");
            printResults(String.valueOf(lostPercentage), "resultSummary" + anID + ".txt");
            double lostByMigration = Double.valueOf(LogHelper.lostTuples.get(anEntry.getKey())) / mobileDevices.get(anID).getMigrations();
            printResults(String.valueOf(lostByMigration), "resultSummary" + anID + ".txt");
        }
        System.out.println("Total of migrations: " + MyStatistics.getInstance().getTotalMigrations());
        System.out.println("Total of handoff: " + MyStatistics.getInstance().getTotalHandoff());
        System.out.println("Total of migration to differents SC: " + MyStatistics.getInstance().getMyCountLowestLatency());

        //printResults(String.valueOf(MyStatistics.getInstance().getTotalMigrations()), "totalMigrations.txt");
        //printResults(String.valueOf(MyStatistics.getInstance().getMyCountLowestLatency()), "totalMyCountLowestLatency.txt");
        //printResults(String.valueOf(MyStatistics.getInstance().getTotalHandoff()), "totalHandoff.txt");
        MyStatistics.getInstance().printResults();
        System.out.println("***Last time without connection***");

        for (Entry<Integer, Double> test : MyStatistics.getInstance().getWithoutConnectionTime().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getWithoutConnectionTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxWithoutConnectionTime().get(test.getKey()));
        }

        System.out.println("Average of without connection: " + MyStatistics.getInstance().getAverageWithoutConnection());

        //printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutConnection()), "resultados.txt");
        System.out.println("***Last time without Vm***");

        for (Entry<Integer, Double> test : MyStatistics.getInstance().getWithoutVmTime().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getWithoutVmTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxWithoutVmTime().get(test.getKey()));
        }

        System.out.println("Average of without Vm: " + MyStatistics.getInstance().getAverageWithoutVmTime());
        //printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutVmTime()), "resultados.txt");
        //printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutVmTime()), "averageWithoutVmTime.txt");

        System.out.println("===Last delay after connection===");
        for (Entry<Integer, Double> test : MyStatistics.getInstance().getDelayAfterNewConnection().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getDelayAfterNewConnection().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxDelayAfterNewConnection().get(test.getKey()));
        }
        System.out.println("Average of delay after new Connection: " + MyStatistics.getInstance().getAverageDelayAfterNewConnection());
        //printResults(String.valueOf(MyStatistics.getInstance().getAverageDelayAfterNewConnection()), "resultados.txt");
        //printResults(String.valueOf(MyStatistics.getInstance().getAverageDelayAfterNewConnection()), "averageDelayAfterNewConnection.txt");

        System.out.println("---Average of Time of Migrations---");
        double totalTempoMigracaoMax = 0.0;
        for (Entry<Integer, Double> test : MyStatistics.getInstance().getMigrationTime().entrySet()) {
            System.out.println("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getMigrationTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey()));
            //printResults(String.valueOf(MyStatistics.getInstance().getMigrationTime().get(test.getKey())), "resultados.txt");
            //printResults(String.valueOf(MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey())), "resultados.txt");
            totalTempoMigracaoMax += MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey());
        }
        System.out.println("Average of Time of Migrations: " + MyStatistics.getInstance().getAverageMigrationTime());
        //printResults(String.valueOf(MyStatistics.getInstance().getAverageMigrationTime()), "resultados.txt");
        //printResults(String.valueOf(MyStatistics.getInstance().getAverageMigrationTime()), "averageMigrationTime.txt");
        //printResults(String.valueOf(totalTempoMigracaoMax / MyStatistics.getInstance().getMigrationTime().entrySet().size()), "averageMigrationMaxTime.txt");
        System.out.println("Tuple lost: " + (((double) MyStatistics.getInstance().getMyCountLostTuple() / MyStatistics.getInstance().getMyCountTotalTuple())) * 100 + "%");
        System.out.println("Tuple lost: " + MyStatistics.getInstance().getMyCountLostTuple());
        System.out.println("Total tuple: " + MyStatistics.getInstance().getMyCountTotalTuple());
        System.out.println("Unfinished msg: " + CMFog.msgQueue.size());
        //System.out.println(CMFog.msgQueue);
        System.out.println("------------------------------------------");
        for (Map.Entry<Integer, ArrayList<String>> entry : CMFog.mobilityLog.entrySet()) {
            System.out.println("-------------------" + entry.getKey() + "-----------------------");
            for (String line : entry.getValue()) {
                System.out.println(line);
            }
            System.out.println(" ");
        }
//        for (String a : CMFog.log) {
//            System.out.println(a);
//        }

//        printResults("===============================================================", "resultados.txt");
//        for (int i = 0; i < this.mobileDevices.size(); i++) {
//            printResults(latencyAverage[i] + "\t" + mobileDevices.get(i).getMigrations() + "\t" + mobileDevices.get(i).getTotalTuples() + "\t" + mobileDevices.get(i).getTotalLostTuples(), "resultados.txt");
//        }
    }

    public void submitApplication(Application application, int delay) {
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        getAppLaunchDelays().put(application.getAppId(), delay);
        for (MobileDevice st : getMobileDevices()) {
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
                for (MobileDevice st : getMobileDevices()) {
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
                for (MobileDevice st : getMobileDevices()) {
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
        for (FogDevice device : getCloudletsL1()) {
            this.globalCurrentCpuLoad.put(device.getId(), currentCpuLoad.get(device.getId()));
        }
    }

    public static int getMigrationPointPolicy() {
        return migrationPointPolicy;
    }

    public static void setMigrationPointPolicy(int migrationPointPolicy) {
        CMFogMobileController.migrationPointPolicy = migrationPointPolicy;
    }

    public static int getMigrationStrategyPolicy() {
        return migrationStrategyPolicy;
    }

    public static void setMigrationStrategyPolicy(int migrationStrategyPolicy) {
        CMFogMobileController.migrationStrategyPolicy = migrationStrategyPolicy;
    }

    public static int getStepPolicy() {
        return stepPolicy;
    }

    public static void setStepPolicy(int stepPolicy) {
        CMFogMobileController.stepPolicy = stepPolicy;
    }

    public List<FogBroker> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<FogBroker> brokers) {
        this.brokers = brokers;
    }

    public static List<FogDevice> getCloudletsL1() {
        return cloudletsL1;
    }

    public static void setCloudletsL1(List<FogDevice> cloudletsL1) {
        CMFogMobileController.cloudletsL1 = cloudletsL1;
    }

    public static List<MobileDevice> getMobileDevices() {
        return mobileDevices;
    }

    public static void setMobileDevices(List<MobileDevice> mobileDevices) {
        CMFogMobileController.mobileDevices = mobileDevices;
    }

    public static List<ApDevice> getAccessPoints() {
        return accessPoints;
    }

    public static void setAccessPoints(List<ApDevice> accessPoints) {
        CMFogMobileController.accessPoints = accessPoints;
    }

    public static boolean isMigrationEnabled() {
        return migrationEnabled;
    }

    public static void setMigrationEnabled(boolean migrationEnabled) {
        CMFogMobileController.migrationEnabled = migrationEnabled;
    }

    public void setCloudletsL2(List<FogDevice> cloudletsL2) {
        this.cloudletsL2 = cloudletsL2;
    }

    public List<FogDevice> getCloudletsL2() {
        return cloudletsL2;
    }

    private void printAlgorithmHits() {
        for (MobileDevice aMobileDevice : CMFog.mobileDevices) {
            String aMobileName = aMobileDevice.getName();
            Integer algorithmMiss = CMFog.algorithmMiss.get(aMobileName);
            Integer simulationTime = CMFog.mobilityLog.get(aMobileDevice.getMyId()).size();
            double result = (((double) (100 * algorithmMiss)) / simulationTime);
            printResults(aMobileName + ": " + algorithmMiss + " - " + simulationTime, "results.txt");
            printResults(aMobileName + ": " + result, "results.txt");
            printResults(String.valueOf(result), "resultSummary" + aMobileDevice.getMyId() + ".txt");

        }
    }

}
