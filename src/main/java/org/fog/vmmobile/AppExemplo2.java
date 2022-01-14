package org.fog.vmmobile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.ApDevice;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.MobileActuator;
import org.fog.entities.MobileDevice;
import org.fog.entities.MobileSensor;
import org.fog.entities.Tuple;
import org.fog.localization.Coordinate;
import org.fog.localization.Distances;
import org.fog.cmfog.mobilityprediction.MobilityPrediction;
import org.fog.mfmf.simulator.CMFogDecision;
import org.fog.cmfog.enumerate.Algorithm;
import org.fog.cmfog.enumerate.Technology;
import org.fog.placement.MobileController2;
import org.fog.placement.ModuleMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.vmmigration.BeforeMigration;
import org.fog.vmmigration.CompleteVM;
import org.fog.vmmigration.ContainerVM;
import org.fog.vmmigration.DecisionMigration;
import org.fog.vmmigration.LiveMigration;
import org.fog.vmmigration.LowestDistBwSmartThingAP;
import org.fog.vmmigration.LowestDistBwSmartThingServerCloudlet;
import org.fog.vmmigration.LowestLatency;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.PrepareCompleteVM;
import org.fog.vmmigration.PrepareContainerVM;
import org.fog.vmmigration.PrepareLiveMigration;
import org.fog.vmmigration.Service;
import org.fog.vmmigration.VmMigrationTechnique;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.Policies;
import org.fog.vmmobile.constants.Services;

public class AppExemplo2 {

    public static List<String> log = new ArrayList<>();
    public static Map<Integer, ArrayList<String>> mobilityLog = new HashMap<>();
    public static String temLog = "";
    public static Integer mobileTec;
    public static Integer vmSizeMax;
    public static Integer vmSizeMin;

    public static MobilityPrediction mobilityPrediction;

    private static int stepPolicy; // Quantity of steps in the nextStep Function
    private static List<MobileDevice> smartThings = new ArrayList<MobileDevice>();
    private static List<FogDevice> serverCloudlets = new ArrayList<>();
    private static List<FogDevice> serverCloudletsL2 = new ArrayList<>();
    private static FogDevice cloud;
    private static List<ApDevice> apDevices = new ArrayList<>();
    private static List<FogBroker> brokerList = new ArrayList<>();
    private static List<String> appIdList = new ArrayList<>();
    private static List<Application> applicationList = new ArrayList<>();

    private static boolean migrationAble;

    private static int migPointPolicy;
    private static int migStrategyPolicy;
    private static int positionApPolicy;
    private static int positionScPolicy;
    private static int policyReplicaVM;
    private static int travelPredicTimeForST; //in seconds
    private static int mobilityPrecitionError;//in meters
    private static int latencyBetweenCloudlets;
    private static int maxBandwidth;
    private static int maxSmartThings;
    private static Coordinate coordDevices;// =new Coordinate(MaxAndMin.MAX_X,
    // MaxAndMin.MAX_Y);//Grid/Map
    private static int seed;
    private static Random rand;
    static final boolean CLOUD = true;

    static final int numOfDepts = 1;
    static final int numOfMobilesPerDept = 4;
    static final double EEG_TRANSMISSION_TIME = 10;

    public static List<FogDevice> getServerCloudletsL2() {
        return serverCloudletsL2;
    }

    public static FogDevice getCloud() {
        return cloud;
    }

    /**
     * @param args
     * @author Marcio Moraes Lopes
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
//        double[][] mBenef = new double[2][2];
//        double[][] mCost = new double[2][2];
//        RMiddleware.callMethodical(mBenef, mCost);
        // Proximos steps:
        // - verificar e talvez configurar a rede entre os ServerCloudlets
        // - Buscar os valores reais de acordo com a literatura e colocar o link
        // dos artigos como comentario na atribuição destes valores
        // - pensar nos pontos de mediçao

        /*
		 * Alguns artigos sobre o projeto
		 * https://arxiv.org/pdf/1611.05539.pdf (Fog Computing: A Taxonomy,
		 * Survey and Future Directions)
		 * http://www.cs.wm.edu/~liqun/paper/hotweb15.pdf (Fog Computing:
		 * Platform and Applications)
		 * http://sbrc2016.ufba.br/downloads/WoCCES/155119.pdf (Avaliacao de
		 * desempenho de procedimentos de handoff em redes IPv6 e uma discussao
		 * sobre a viabilidade de aplicacao em sistemas criticos)
		 * http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=7098039 (Fog
		 * Computing Micro Datacenter Based Dynamic Resource Estimation and
		 * Pricing Model for IoT)
         */
        /**
         * ********
         * It's necessary to CloudSim.java for working correctly ********
         */
        Log.disable();

        // Logger.ENABLED=true;
        // LogMobile.ENABLED=true;
        int numUser = 1; //CHANGE_HERE
        Calendar calendar = Calendar.getInstance();
        boolean traceFlag = false; // mean trace events
        CloudSim.init(numUser, calendar, traceFlag);
        /**
         * ***********************************************************************
         */
        setPositionApPolicy(Policies.FIXED_AP_LOCATION);
        setPositionScPolicy(Policies.FIXED_SC_LOCATION);
        setSeed(Integer.parseInt(args[1]));
        setStepPolicy(1);
        // ENABLED = 1  / DISABLED = 0
        if (Integer.parseInt(args[0]) == 0) {
            setMigrationAble(false);
        } else {
            setMigrationAble(true);
        }
        if (getSeed() < 1) {
            System.out.println("Seed cannot be less than 1");
            System.exit(0);
        }
        setRand(new Random(getSeed() * Integer.MAX_VALUE));
        //FIXED_MIGRATION_POINT = 0;
        //SPEED_MIGRATION_POINT = 1;
        setMigPointPolicy(Integer.parseInt(args[2]));
        //LOWEST_LATENCY = 0;
        //LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET = 1;
        //LOWEST_DIST_BW_SMARTTING_AP = 2;
        //F-FMF = 10;
        //
        setMigStrategyPolicy(Integer.parseInt(args[3]));
        //setMaxSmartThings(Integer.parseInt(args[4]));
        setMaxSmartThings(2);
        setMaxBandwidth(Integer.parseInt(args[5]));
        //MIGRATION_COMPLETE_VM = 0;
        //MIGRATION_CONTAINER_VM = 1;
        //LIVE_MIGRATION = 2;
        setPolicyReplicaVM(Integer.parseInt(args[6]));
        setTravelPredicTimeForST(Integer.parseInt(args[7]));
        setMobilityPredictionError(Integer.parseInt(args[8]));
        setLatencyBetweenCloudlets(Integer.parseInt(args[9]));
        mobileTec = Integer.parseInt(args[10]);
        vmSizeMax = Integer.parseInt(args[11]);
        vmSizeMin = Integer.parseInt(args[12]);

        for (int i = 0; i < getMaxSmartThings(); i++) {
            mobilityLog.put(i, new ArrayList<String>());
        }
        /**
         * STEP 2: CREATE ALL DEVICES -> example from: CloudSim - example5.java
         *
         */
        createCloud();
        addApDevicesFixed2(apDevices, coordDevices);
        /* It is creating Server Cloudlets. */
        addServerCloudletGRID(serverCloudlets, coordDevices, 1);
        addServerCloudletGRID(serverCloudletsL2, coordDevices, 2);

        /* It is creating Smart Things. */
        int totalST = 0;
        //for (int t = 1; t <= 3; t++) {
        for (int alg = 1; alg <= 2; alg++) { //CHANGE_HERE
            addSmartThing(smartThings, coordDevices, totalST, Algorithm.setValue(alg), Technology.TEC_5G);
            totalST++;
        }
        //  }
//
        // addSmartThing(smartThings, coordDevices, totalST++, Algorithm.setValue(2), Technology.setValue(2));
//        for (int i = 0; i < getMaxSmartThings(); i++) {//CHANGE_HERE
//            addSmartThing(smartThings, coordDevices, i);
//        }
        System.out.println("---------------------------------------------------------------------------------------------------");

        createServerCloudletsNetwork();
        //  createServerCloudletsNetwork(getServerCloudlets());
        System.out.println("-------------- Layer 1 --------------");
        for (FogDevice cloudletL1 : serverCloudlets) {
            for (FogDevice cloudletL2 : serverCloudletsL2) {
                if (isLayerOverlaped(cloudletL1.getCoord(), cloudletL2.getCoord())) {
                    System.out.println("[" + cloudletL1.getName() + " , " + cloudletL2.getName() + "] - (" + NetworkTopology.getDelay(cloudletL1.getId(), cloudletL2.getId()) + " , " + cloudletL1.getUplinkBandwidth() / 1048576 + ")");
                    break;
                }
            }
        }

        System.out.println("\n -------------- Layer 2 --------------");
        for (FogDevice cloudletL2 : serverCloudletsL2) {
            for (FogDevice cloudletL1 : serverCloudlets) {
                if (cloudletL1.getParentId() == cloudletL2.getId()) {
                    System.out.println("[" + cloudletL2.getName() + " , " + cloudletL1.getName() + "] - (" + NetworkTopology.getDelay(cloudletL2.getId(), cloudletL1.getId()) + " , " + cloudletL1.getDownlinkBandwidth() / 1048576 + ")");
                }
            }
        }

        System.out.println("\n -------------- From L2 to Cloud--------------");
        for (FogDevice cloudletL2 : serverCloudletsL2) {
            System.out.println("[" + cloudletL2.getName() + " , " + cloud.getName() + "] - (" + NetworkTopology.getDelay(cloudletL2.getId(), cloud.getId()) + " , " + cloudletL2.getUplinkBandwidth() / 1048576 + ")");
        }

        System.out.println("\n -------------- From Cloud to L2--------------");
        for (FogDevice cloudletL2 : serverCloudletsL2) {
            System.out.println("[" + cloud.getName() + " , " + cloudletL2.getName() + "] - (" + NetworkTopology.getDelay(cloud.getId(), cloudletL2.getId()) + " , " + cloudletL2.getDownlinkBandwidth() / 1048576 + ")");
        }

//        for (FogDevice cloudletsL2 : serverCloudletsL2) {
//            System.out.println("-------------" + cloudletsL2.getName() + "-------------");
//            for (FogDevice cloudletsL1 : getServerCloudlets()) {
//                System.out.println("Delay from " + cloudletsL1.getName() + ": " + NetworkTopology.getDelay(cloudletsL1.getId(), cloudletsL2.getId()));
//                System.out.println("Delay to " + cloudletsL1.getName() + ": " + NetworkTopology.getDelay(cloudletsL2.getId(), cloudletsL1.getId()));
//                //System.out.println(sc.getName() + ": " + (sc.getDownlinkBandwidth() / 1048576) + " | " + (sc.getUplinkBandwidth() / 1048576));
//            }
//        }
        mobilityPrediction = new MobilityPrediction(serverCloudlets, smartThings);
        readMoblityData();

        int index;
        int myCount = 0;
        for (MobileDevice st : getSmartThings()) {// it makes the connection between SmartThing and the closest AccessPoint
            if (!ApDevice.connectApSmartThing(getApDevices(), st, getRand().nextDouble())) {
                myCount++;
                LogMobile.debug("AppExemplo2.java", st.getName() + " isn't connected");
            }
        }
        LogMobile.debug("AppExemplo2.java", "total no connection: " + myCount);

        for (ApDevice ap : getApDevices()) { // it makes the connection between AccessPoint and the closestServerCloudlet
            index = Distances.theClosestServerCloudletToAp(getServerCloudlets(), ap);
            ap.setServerCloudlet(getServerCloudlets().get(index));
            ap.setParentId(getServerCloudlets().get(index).getId());
            getServerCloudlets().get(index).setApDevices(ap, Policies.ADD);
            NetworkTopology.addLink(serverCloudlets.get(index).getId(), ap.getId(), ap.getDownlinkBandwidth(), getRand().nextDouble());
            for (MobileDevice st : ap.getSmartThings()) {// it makes the symbolic link between smartThing and ServerCloudlet
                getServerCloudlets().get(index).connectServerCloudletSmartThing(st);
                getServerCloudlets().get(index).setSmartThingsWithVm(st, Policies.ADD);
            }
        }
        /**
         * STEP 3: CREATE BROKER -> example from: CloudSim - example5.java *
         */

        for (MobileDevice st : getSmartThings()) {
            getBrokerList().add(new FogBroker("My_broker" + Integer.toString(st.getMyId())));
        }
        /**
         * STEP 4: CREATE ONE VIRTUAL MACHINE FOR EACH BROKER/USER -> example
         * from: CloudSim - example5.java
         *
         */

        for (MobileDevice st : getSmartThings()) {// It only creates the virtual machine for each smartThing
            if (st.getSourceAp() != null) {
                CloudletScheduler cloudletScheduler = new TupleScheduler(500, 1);
                //long sizeVm = (MaxAndMin.MIN_VM_SIZE + (long) ((MaxAndMin.MAX_VM_SIZE - MaxAndMin.MIN_VM_SIZE) * (getRand().nextDouble())));
                AppModule vmSmartThingTest = new AppModule(st.getMyId(), // id
                        "AppModuleVm_" + st.getName(), "MyApp_vr_game" + st.getMyId(), getBrokerList().get(st.getMyId()).getId(),
                        2000, 64, 1000, (long) generateVMSize(), "Vm_" + st.getName(), cloudletScheduler, new HashMap<Pair<String, String>, SelectivityModel>());
                st.setVmMobileDevice(vmSmartThingTest);
                st.getSourceServerCloudlet().getHost().vmCreate(vmSmartThingTest);
                st.setVmLocalServerCloudlet(st.getSourceServerCloudlet());
                System.out.println(st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: "
                        + st.getDirection() + " Speed: " + st.getSpeed());
                System.out.println("Source AP: " + st.getSourceAp() + " Dest AP: " + st.getDestinationAp() + " Host: " + st.getHost().getId());
                System.out.println("Local server: " + st.getVmLocalServerCloudlet().getName() + " Apps " + st.getVmLocalServerCloudlet().getActiveApplications()
                        + " Map " + st.getVmLocalServerCloudlet().getApplicationMap());
                if (st.getDestinationServerCloudlet() == null) {
                    System.out.println("Dest server: null Apps: null Map: null");
                } else {
                    System.out.println("Dest server: " + st.getDestinationServerCloudlet().getName() + " Apps: " + st.getDestinationServerCloudlet()
                            .getActiveApplications() + " Map " + st.getDestinationServerCloudlet().getApplicationMap());
                }
            }
        }
        int i = 0;
        for (FogBroker br : getBrokerList()) {// Each broker receives one smartThing's VM
            List<Vm> tempVmList = new ArrayList<>();
            tempVmList.add(getSmartThings().get(i++).getVmMobileDevice());
            br.submitVmList(tempVmList);
        }
        /**
         * STEP 5: CREATE THE APPLICATION -> example from: CloudSim and iFogSim
         *
         */
        i = 0;
        for (FogBroker br : getBrokerList()) {
            getAppIdList().add("MyApp_vr_game" + Integer.toString(i));
            Application myApp = createApplication(getAppIdList().get(i), br.getId(), i, (AppModule) getSmartThings().get(i).getVmMobileDevice());
            getApplicationList().add(myApp);
            i++;
        }
        /**
         * STEP 5.1: IT LINKS SENSORS AND ACTUATORS FOR EACH BROKER -> example
         * from: CloudSim and iFogSim
         *
         */
        for (MobileDevice st : getSmartThings()) {
            int brokerId = getBrokerList().get(st.getMyId()).getId();
            String appId = getAppIdList().get(st.getMyId());
            if (st.getSourceAp() != null) {
                for (MobileSensor s : st.getSensors()) {
                    s.setAppId(appId);
                    s.setUserId(brokerId);
                    s.setGatewayDeviceId(st.getId());
                    s.setLatency(6.0);
                }
                for (MobileActuator a : st.getActuators()) {
                    a.setUserId(brokerId);
                    a.setAppId(appId);
                    a.setGatewayDeviceId(st.getId());
                    a.setLatency(1.0);
                    a.setActuatorType("DISPLAY" + st.getMyId());
                }
            }
        }

        /**
         * STEP 6: CREATE MAPPING, CONTROLLER, AND SUBMIT APPLICATION -> example
         * from: iFogSim - Position3.java
         *
         */
        MobileController2 mobileController = null;
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
        for (Application app : getApplicationList()) {
            app.setPlacementStrategy("Mapping");
        }
        i = 0;
        for (FogDevice sc : getServerCloudlets()) {
            i = 0;
            for (MobileDevice st : getSmartThings()) {
                if (st.getApDevices() != null) {
                    if (sc.equals(st.getSourceServerCloudlet())) {
                        moduleMapping.addModuleToDevice(((AppModule) st.getVmMobileDevice()).getName(), sc.getName(), 1);// MaxAndMin.MAX_SMART_THING);//
                        moduleMapping.addModuleToDevice("client" + st.getMyId(), st.getName(), 1);
                    }
                }
                i++;
            }
        }
        mobileController = new MobileController2("MobileController",
                getServerCloudlets(), getApDevices(), getSmartThings(),
                getBrokerList(), moduleMapping, getMigPointPolicy(),
                getMigStrategyPolicy(), getStepPolicy(), getCoordDevices(),
                getSeed(), isMigrationAble());
        i = 0;
        for (Application app : applicationList) {
            mobileController.submitApplication(app, 1);
        }
        ////////////////////////////////////////////////////
        mobilityPrediction.generateTransictionMatrix();
        ////////////////////////////////////////////////////
        TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
        MyStatistics.getInstance().setSeed(getSeed());
        for (MobileDevice st : getSmartThings()) {
            System.out.println(st.getMyId() + " - " + getMigStrategyPolicy());
            if (getMigPointPolicy() == Policies.FIXED_MIGRATION_POINT || getMigPointPolicy() == Policies.TIME_MIGRATION_POINT) {
                switch (getMigStrategyPolicy()) {
                    case Policies.LOWEST_LATENCY:
                        System.out.println("----------------------------> LOWEST LATENCY");
                        MyStatistics.getInstance()
                                .setFileMap("./outputLatencies/" + st.getMyId()
                                        + "/latencies_FIXED_MIGRATION_POINT_with_LOWEST_LATENCY_seed_"
                                        + getSeed() + "_st_" + st.getMyId()
                                        + ".txt", st.getMyId());
                        MyStatistics.getInstance().putLantencyFileName(
                                "FIXED_MIGRATION_POINT_with_LOWEST_LATENCY_seed_"
                                + getSeed() + "_st_" + st.getMyId(),
                                st.getMyId());
                        MyStatistics.getInstance().setToPrint(
                                "FIXED_MIGRATION_POINT_with_LOWEST_LATENCY");
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_AP:
                        MyStatistics.getInstance()
                                .setFileMap("./outputLatencies/" + st.getMyId()
                                        + "/latencies_FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_AP_seed_"
                                        + getSeed() + "_st_" + st.getMyId()
                                        + ".txt", st.getMyId());
                        MyStatistics.getInstance().putLantencyFileName(
                                "FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_AP_seed_"
                                + getSeed() + "_st_" + st.getMyId(),
                                st.getMyId());
                        MyStatistics.getInstance().setToPrint(
                                "FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_AP");
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET:
                        MyStatistics.getInstance()
                                .setFileMap("./outputLatencies/" + st.getMyId()
                                        + "/latencies_FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET_seed_"
                                        + getSeed() + "_st_" + st.getMyId()
                                        + ".txt", st.getMyId());
                        MyStatistics.getInstance().putLantencyFileName(
                                "FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET_seed_"
                                + getSeed() + "_st_" + st.getMyId(),
                                st.getMyId());
                        MyStatistics.getInstance().setToPrint(
                                "FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET");
                        break;
                    case Policies.CMFOG:
                        System.out.println("HEEEEEEEEEEEEEEEEEEEEEEEEEEEEERE");
                        MyStatistics.getInstance()
                                .setFileMap("./outputLatencies/" + st.getMyId()
                                        + "/latencies_FIXED_MIGRATION_POINT_with_FM_seed_"
                                        + getSeed() + "_st_" + st.getMyId()
                                        + ".txt", st.getMyId());
                        MyStatistics.getInstance().putLantencyFileName(
                                "FIXED_MIGRATION_POINT_with_FM_seed_"
                                + getSeed() + "_st_" + st.getMyId(),
                                st.getMyId());
                        MyStatistics.getInstance().setToPrint(
                                "FIXED_MIGRATION_POINT_with_FM");
                        break;
                    default:
                        break;
                }
            } else if (getMigPointPolicy() == Policies.SPEED_MIGRATION_POINT) {
                switch (getMigStrategyPolicy()) {
                    case Policies.LOWEST_LATENCY:
                        MyStatistics.getInstance()
                                .setFileMap("./outputLatencies/" + st.getMyId()
                                        + "/latencies_SPEED_MIGRATION_POINT_with_LOWEST_LATENCY_seed_"
                                        + getSeed() + "_st_" + st.getMyId()
                                        + ".txt", st.getMyId());
                        MyStatistics.getInstance().putLantencyFileName(
                                "SPEED_MIGRATION_POINT_with_LOWEST_LATENCY_seed_"
                                + getSeed() + "_st_" + st.getMyId(),
                                st.getMyId());
                        MyStatistics.getInstance().setToPrint(
                                "SPEED_MIGRATION_POINT_with_LOWEST_LATENCY");
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_AP:
                        MyStatistics.getInstance()
                                .setFileMap("./outputLatencies/" + st.getMyId()
                                        + "/latencies_SPEED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_AP_seed_"
                                        + getSeed() + "_st_" + st.getMyId()
                                        + ".txt", st.getMyId());
                        MyStatistics.getInstance().putLantencyFileName(
                                "SPEED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_AP_seed_"
                                + getSeed() + "_st_" + st.getMyId(),
                                st.getMyId());
                        MyStatistics.getInstance().setToPrint(
                                "SPEED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_AP");
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET:
                        MyStatistics.getInstance()
                                .setFileMap("./outputLatencies/" + st.getMyId()
                                        + "/latencies_SPEED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET_seed_"
                                        + getSeed() + "_st_" + st.getMyId()
                                        + ".txt", st.getMyId());
                        MyStatistics.getInstance().putLantencyFileName(
                                "SPEED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET_seed_"
                                + getSeed() + "_st_" + st.getMyId(),
                                st.getMyId());
                        MyStatistics.getInstance().setToPrint(
                                "SPEED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET");
                        break;
                    default:
                        break;
                }
            }
            System.out.println("ID: " + st.getMyId());
            MyStatistics.getInstance().putLantencyFileName("Time-latency", st.getMyId());
            MyStatistics.getInstance().getMyCount().put(st.getMyId(), 0);
        }

        myCount = 0;

        for (MobileDevice st : getSmartThings()) {
            if (st.getSourceAp() != null) {
                System.out.println("Distance between " + st.getName() + " and "
                        + st.getSourceAp().getName() + ": "
                        + Distances.checkDistance(st.getCoord(),
                                st.getSourceAp().getCoord()));
            }
        }
        for (MobileDevice st : getSmartThings()) {
            System.out.println(
                    st.getName() + "- X: " + st.getCoord().getCoordX() + " Y: "
                    + st.getCoord().getCoordY() + " Direction: "
                    + st.getDirection() + " Speed: " + st.getSpeed()
                    + " VmSize: " + st.getVmMobileDevice().getSize());
        }
        System.out.println("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");
        for (FogDevice sc : getServerCloudlets()) {
            System.out
                    .println(sc.getName() + "(" + sc.getId() + ")" + " - X: " + sc.getCoord().getCoordX()
                            + " Y: " + sc.getCoord().getCoordY()
                            + " UpLinkLatency: " + sc.getUplinkLatency());
        }
        System.out.println("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");
        for (ApDevice ap : getApDevices()) {
            System.out.println(
                    ap.getName() + "- X: " + ap.getCoord().getCoordX() + " Y: "
                    + ap.getCoord().getCoordY() + " connected to "
                    + ap.getServerCloudlet().getName());
        }

        System.setOut(new PrintStream("out.txt"));
        System.out.println("Starting: " + Calendar.getInstance().getTime());
        CloudSim.startSimulation();
        System.out.println("Simulation over");

        CloudSim.stopSimulation();

    }

    public static double calculateMigTime(MobileDevice aMobileDevice) {
        //double distance = Distances.checkDistance(aMobileDevice.getSourceAp().getCoord(), aMobileDevice.getCoord());
        double bandwidth = aMobileDevice.getVmLocalServerCloudlet().getUplinkBandwidth();
        double time = ((double) ((aMobileDevice.getVmMobileDevice().getSize() * 8 * 1024 * 1024) * MaxAndMin.SIZE_CONTAINER) / bandwidth) * 1000.0;
        time += aMobileDevice.getVmLocalServerCloudlet().getUplinkLatency() //Link Latency
                + NetworkTopology.getDelay(aMobileDevice.getId(), aMobileDevice.getVmLocalServerCloudlet().getId());
        //+ LatencyByDistance.latencyConnection(aMobileDevice.getVmLocalServerCloudlet(), aMobileDevice);
        return time;
    }

    public static double generateVMSize() {
        return vmSizeMin + (int) (new Random().nextFloat() * (vmSizeMax - vmSizeMin));
    }

    private static double generateFogBW() {
        return MaxAndMin.BW_FOG_MIN + (int) (new Random().nextFloat() * (MaxAndMin.BW_FOG_MAX - MaxAndMin.BW_FOG_MIN));
    }

    private static double generateFogLT() {
        return MaxAndMin.LATENCY_FOG_MIN + (int) (new Random().nextFloat() * (MaxAndMin.LATENCY_FOG_MAX - MaxAndMin.LATENCY_FOG_MIN));
    }

    private static double generateMDBW() {
        if (mobileTec == 1) { //4G
            return MaxAndMin.T4G_BW_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T4G_BW_MD_MAX - MaxAndMin.T4G_BW_MD_MIN));
        } else if (mobileTec == 2) { //5G
            return MaxAndMin.T5G_BW_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T5G_BW_MD_MAX - MaxAndMin.T5G_BW_MD_MIN));
        } else { //WiFi
            return MaxAndMin.WF_BW_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.WF_BW_MD_MAX - MaxAndMin.WF_BW_MD_MIN));
        }
    }

    private static double generateMDBW(Technology aTechnology) {
        if (aTechnology == Technology.TEC_4G) { //4G
            return MaxAndMin.T4G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T4G_LATENCY_MD_MAX - MaxAndMin.T4G_LATENCY_MD_MIN));
        } else if (aTechnology == Technology.TEC_5G) { //5G
            return MaxAndMin.T5G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T5G_LATENCY_MD_MAX - MaxAndMin.T5G_LATENCY_MD_MIN));
        } else { //WiFi
            return MaxAndMin.WF_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.WF_LATENCY_MD_MAX - MaxAndMin.WF_LATENCY_MD_MIN));
        }
    }

    private static double generateMDLT() {
        if (mobileTec == 1) { //4G
            return MaxAndMin.T4G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T4G_LATENCY_MD_MAX - MaxAndMin.T4G_LATENCY_MD_MIN));
        } else if (mobileTec == 2) { //5G
            return MaxAndMin.T5G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T5G_LATENCY_MD_MAX - MaxAndMin.T5G_LATENCY_MD_MIN));
        } else { //WiFi
            return MaxAndMin.WF_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.WF_LATENCY_MD_MAX - MaxAndMin.WF_LATENCY_MD_MIN));
        }
    }

    private static double generateApLT() {
        return MaxAndMin.LATENCY_AP_MIN + (int) (new Random().nextFloat() * (MaxAndMin.LATENCY_AP_MAX - MaxAndMin.LATENCY_AP_MIN));
    }

    private static double generateApBW() {
        return MaxAndMin.BW_AP_MIN + (int) (new Random().nextFloat() * (MaxAndMin.BW_AP_MAX - MaxAndMin.BW_AP_MIN));
    }

    private static void readDevicePath2(MobileDevice st, String filename) {
        //File folder = new File("input/" + st.getMyId());//CHANGE_HERE
        File folder = new File("input/" + 0);//CHANGE_HERE
        String line = "";
        String cvsSplitBy = "\t";
        File[] listOfFiles = folder.listFiles();
        for (File aFile : listOfFiles) {
            ArrayList<String[]> entry = new ArrayList<>();
            System.out.println(aFile.getAbsolutePath());
            try (BufferedReader br = new BufferedReader(new FileReader(aFile))) {
                while ((line = br.readLine()) != null) {
                    String[] position = line.split(cvsSplitBy);
                    entry.add(position);
                }
                mobilityPrediction.parseTraces(st, entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void trainingMoblityData() {
        System.out.println("--------> Training");
        for (int i = 0; i < getSmartThings().size(); i++) {
            readDevicePath2(getSmartThings().get(i), "input/" + i);
        }
    }

    private static void readMoblityData() {
        trainingMoblityData();
        //  File folder = new File("input");
        // File[] listOfFiles = folder.listFiles();
        System.out.println("REAAADING--------------------");
        //    Arrays.sort(listOfFiles);
        //  int[] ordem = readDevicePathOrder(listOfFiles[listOfFiles.length - 1]);
        for (int i = 0; i < getSmartThings().size(); i++) {
            readDevicePath(getSmartThings().get(i), "input/9.txt");
        }
//		for (MobileDevice st : getSmartThings()) {
//			readDevicePath(st, "input/"+listOfFiles[i++].getName());
//		}
    }

    private static int[] readDevicePathOrder(File filename) {

        String line = "";
        String cvsSplitBy = "\t";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            int i = 1;
            while (((line = br.readLine()) != null)) {
//				if(i == getSeed()) {
                if (i == 1) {
                    break;
                }
                i++;
            }
            // use comma as separator
            String[] position = line.split(cvsSplitBy);
            int ordem[] = new int[getSmartThings().size()];
            for (int j = 0; j < getSmartThings().size(); j++) {
                ordem[j] = Integer.valueOf(position[j]);
            }
            Arrays.sort(ordem);
            return ordem;
            // System.out.println(country[0]+"
            // "+Double.parseDouble(country[0])*(180/Math.PI)+"
            // "+country[1]+" "+country[2]+" "+country[3]);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void readDevicePath(MobileDevice st, String filename) {

        String line = "";
        String cvsSplitBy = "\t";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] position = line.split(cvsSplitBy);
//                position[0] = String.valueOf(Double.valueOf(position[0]) - 90000.0);
//                position[2] = String.valueOf(Double.valueOf(position[2]) - 3500.0);
//                position[3] = String.valueOf(Double.valueOf(position[3]) - 1500.0);
                // System.out.println("---------------->>>> " + position[0] + " ------------->" + (Double.valueOf(position[0])+90000));
                // System.out.println(country[0]+"
                // "+Double.parseDouble(country[0])*(180/Math.PI)+"
                // "+country[1]+" "+country[2]+" "+country[3]);

                st.getPath().add(position);
            }
            //    mobilityPrediction.parseTraces(st, st.getPath());
            // fill = st.getPath();
            Coordinate coordinate = new Coordinate();
            coordinate.setInitialCoordinate(st);
            saveMobility(st);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static ArrayList<String[]> fill;

    private static void saveMobility(MobileDevice st) {
//		System.out.println(st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
//		System.out.println("Source AP: " + st.getSourceAp() + " Dest AP: " + st.getDestinationAp() + " Host: " + st.getHost().getId());
//		System.out.println("Local server: " + st.getVmLocalServerCloudlet().getName() + " Apps " + st.getVmLocalServerCloudlet().getActiveApplications() + " Map " + st.getVmLocalServerCloudlet().getApplicationMap());
//		if(st.getDestinationServerCloudlet() == null){
//			System.out.println("Dest server: null Apps: null Map: null");
//		}
//		else{
//			System.out.println("Dest server: " + st.getDestinationServerCloudlet().getName() + " Apps: " + st.getDestinationServerCloudlet().getActiveApplications() +  " Map " + st.getDestinationServerCloudlet().getApplicationMap());
//		}
        try (FileWriter fw1 = new FileWriter(st.getMyId() + "out.txt", true);
                BufferedWriter bw1 = new BufferedWriter(fw1);
                PrintWriter out1 = new PrintWriter(bw1)) {
            out1.println(st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
            out1.println("Source AP: " + st.getSourceAp() + " Dest AP: " + st.getDestinationAp() + " Host: " + st.getHost().getId());
            out1.println("Local server: null  Apps null Map null");
            if (st.getDestinationServerCloudlet() == null) {
                out1.println("Dest server: null Apps: null Map: null");
            } else {
                out1.println("Dest server: " + st.getDestinationServerCloudlet().getName() + " Apps: " + st.getDestinationServerCloudlet().getActiveApplications() + " Map " + st.getDestinationServerCloudlet().getApplicationMap());
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

        try (FileWriter fw = new FileWriter(st.getMyId() + "route.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println(st.getMyId() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY() + "\t" + st.getDirection() + "\t" + st.getSpeed() + "\t" + CloudSim.clock());
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

//    public static boolean isCovered(FogDevice aFogDevice, MobileDevice aMobileDevice) {
//        int radius = MaxAndMin.CLOUDLET_COVERAGE / 2;
//        int entityX = aFogDevice.getCoord().getCoordX();
//        int entityY = aFogDevice.getCoord().getCoordY();
//        int coordX = aMobileDevice.getCoord().getCoordX();
//        int coordY = aMobileDevice.getCoord().getCoordY();
//        boolean xCovered = false, yCovered = false;
//        // System.out.println("Fog DEV: " + entityX + " / " + entityY);
//        // System.out.println("MD: " + coordX + " / " + coordY);
//        // System.out.println("RADIUS " + radius);
//        if (coordX >= entityX - radius && entityX + radius >= coordX) {
//
//            xCovered = true;
//        }
//        if (coordY >= entityY - radius && entityY + radius >= coordY) {
//
//            yCovered = true;
//        }
//        if (xCovered == true && yCovered == true) {
//            // System.out.println("#### is covered");
//            return true;
//        } else {
//            // System.out.println("#### is NOT covered");
//            return false;
//        }
//    }
//
//    public static boolean isInZone(FogDevice aFogDevice, MobileDevice aMobileDevice, int zone) {
//        if (isBordeline(aFogDevice.getCoord(), aMobileDevice.getCoord(), zone)) {
//            return false;
//        }
//        int radius = MaxAndMin.CLOUDLET_COVERAGE / 2;
//        int entityX = aFogDevice.getCoord().getCoordX();
//        int entityY = aFogDevice.getCoord().getCoordY();
//        int coordX = aMobileDevice.getCoord().getCoordX();
//        int coordY = aMobileDevice.getCoord().getCoordY();
//        boolean xNucled = false, yNucled = false;
//        //System.out.println(aFogDevice.getName() + ": " + entityX + " / " + entityY);
//        // System.out.println("MD: " + coordX + " / " + coordY);
//        //System.out.println("RADIUS " + radius);
//        if (coordX >= entityX - radius + zone && entityX + radius - zone >= coordX) {
//            //  System.out.println("x nucled");
//            xNucled = true;
//        }
//        if (coordY >= entityY - radius + zone && entityY + radius - zone >= coordY) {
//            //System.out.println("y nucled");
//            yNucled = true;
//        }
//        if ((xNucled == false || yNucled == false) && isCovered(aFogDevice, aMobileDevice)) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    private static boolean isBordeline(Coordinate FDCoord, Coordinate MDCoord, int zone) {
//        if (isCorner(MDCoord)) {
//            return true;
//        }
//        if (isInGridBord(MDCoord)) {
//            int radius = MaxAndMin.CLOUDLET_COVERAGE / 2;
//            int entityX = FDCoord.getCoordX();
//            int entityY = FDCoord.getCoordY();
//            int coordX = MDCoord.getCoordX();
//            int coordY = MDCoord.getCoordY();
//            boolean xNucled = false, yNucled = false;
//            if (coordX >= entityX - radius + zone && entityX + radius - zone >= coordX) {
//                xNucled = true;
//            }
//            if (coordY >= entityY - radius + zone && entityY + radius - zone >= coordY) {
//                yNucled = true;
//            }
//            if (xNucled == false && yNucled == false) {
//                return false;
//            } else {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private static boolean isInGridBord(Coordinate MDCoord) {
//        if (MDCoord.getCoordX() >= 0 && MDCoord.getCoordX() <= MaxAndMin.MIG_POINT) {
//            return true;
//        }
//        if (MDCoord.getCoordX() >= (MaxAndMin.MAX_X - MaxAndMin.MIG_POINT) && MDCoord.getCoordX() <= MaxAndMin.MAX_X) {
//            return true;
//        }
//        if (MDCoord.getCoordY() >= 0 && MDCoord.getCoordY() <= MaxAndMin.MIG_POINT) {
//            return true;
//        }
//        if (MDCoord.getCoordY() >= (MaxAndMin.MAX_Y - MaxAndMin.MIG_POINT) && MDCoord.getCoordY() <= MaxAndMin.MAX_Y) {
//            return true;
//        }
//        return false;
//    }
//
//    private static boolean isCorner(Coordinate MDCoord) {
//        if (MDCoord.getCoordX() >= 0 && MDCoord.getCoordX() <= MaxAndMin.MIG_POINT) {
//            if (MDCoord.getCoordY() >= 0 && MDCoord.getCoordY() <= MaxAndMin.MIG_POINT) {
//                return true;
//            }
//            if (MDCoord.getCoordY() >= (MaxAndMin.MAX_Y - MaxAndMin.MIG_POINT) && MDCoord.getCoordY() <= MaxAndMin.MAX_Y) {
//                return true;
//            }
//        }
//        if (MDCoord.getCoordX() >= (MaxAndMin.MAX_X - MaxAndMin.MIG_POINT) && MDCoord.getCoordX() <= MaxAndMin.MAX_X) {
//            if (MDCoord.getCoordY() >= 0 && MDCoord.getCoordY() <= MaxAndMin.MIG_POINT) {
//                return true;
//            }
//            if (MDCoord.getCoordY() >= (MaxAndMin.MAX_Y - MaxAndMin.MIG_POINT) && MDCoord.getCoordY() <= MaxAndMin.MAX_Y) {
//                return true;
//            }
//        }
//        return false;
//    }

    public static MobilityPrediction getMobilityPrediction() {
        return mobilityPrediction;
    }

    private static void addApDevicesFixed2(List<ApDevice> apDevices, Coordinate coordDevices) {
        int i = 0;
        double radius = MaxAndMin.AP_COVERAGE / 2;
        double overlap = MaxAndMin.MAX_DISTANCE_TO_HANDOFF;
        double coordX, coordY;
        boolean control = true;
        System.out.println("Creating Ap devices");
        for (coordX = radius; coordX < MaxAndMin.MAX_X; coordX += MaxAndMin.AP_COVERAGE - overlap) {
            for (coordY = radius; coordY < MaxAndMin.MAX_Y; coordY += MaxAndMin.AP_COVERAGE - overlap, i++) {
                double bw = AppExemplo2.generateApBW() * 1024 * 1024;
                ApDevice ap = new ApDevice("AccessPoint" + Integer.toString(i), // name
                        (int) coordX, (int) coordY, i// ap.set//id
                        ,
                         bw// downLinkBandwidth - 100Mbits
                        ,
                         200// engergyConsuption
                        ,
                         MaxAndMin.MAX_ST_IN_AP// maxSmartThing
                        ,
                         bw / 2// upLinkBandwidth - 100Mbits
                        ,
                         4// upLinkLatency
                );// ver valores reais e melhores
                apDevices.add(i, ap);
                // coordDevices.setPositions(ap.getId(),
                // ap.getCoord().getCoordX(), ap.getCoord().getCoordY());
            }
        }
        LogMobile.debug("AppExemplo2.java", "Total of accessPoints: " + i);

    }

    private static void addApDevicesFixed(List<ApDevice> apDevices, Coordinate coordDevices) {
        int i = 0;
        boolean control = true;
        int coordY = 0;
//		 for(int coordX=1050; coordX<MaxAndMin.MAX_X-990; coordX+=2001){
        for (int coordX = 0; coordX < MaxAndMin.MAX_X; coordX += (2
                * MaxAndMin.AP_COVERAGE
                - (2 * MaxAndMin.AP_COVERAGE / 3))) {
            /* evenly distributed */
            System.out.println("Creating Ap devices");
            // if(control){
            // coordY=4000;
            // }
            // else{
            // coordY = 10500;
            // }
            // control=!control;
            // for(coordY=1050; coordY<MaxAndMin.MAX_Y-990; coordY+=2001, i++){
            for (coordY = 0; coordY < MaxAndMin.MAX_Y; coordY += (2
                    * MaxAndMin.AP_COVERAGE
                    - (2 * MaxAndMin.AP_COVERAGE / 3)), i++) {
                // if(coordDevices.getPositions(coordX, coordY)==-1){
                // ApDevice ap = new
                // ApDevice("AccessPoint"+Integer.toString(i),coordX,coordY,i);//my
                // construction
                ApDevice ap = new ApDevice("AccessPoint" + Integer.toString(i), // name
                        coordX, coordY, i// ap.set//id
                        ,
                         100 * 1024 * 1024// downLinkBandwidth - 100Mbits
                        ,
                         200// engergyConsuption
                        ,
                         MaxAndMin.MAX_ST_IN_AP// maxSmartThing
                        ,
                         100 * 1024 * 1024// upLinkBandwidth - 100Mbits
                        ,
                         4// upLinkLatency
                );// ver valores reais e melhores
                apDevices.add(i, ap);
                // coordDevices.setPositions(ap.getId(),
                // ap.getCoord().getCoordX(), ap.getCoord().getCoordY());
            }
        }
        LogMobile.debug("AppExemplo2.java", "Total of accessPoints: " + i);

    }

    private static void addApDevicesRandon(List<ApDevice> apDevices,
            Coordinate coordDevices, int i) {
        // Random rand = new Random((Integer.MAX_VALUE/getSeed())*(i+1));
        int coordX, coordY;
        // while(true){
        coordX = getRand().nextInt(MaxAndMin.MAX_X);
        coordY = getRand().nextInt(MaxAndMin.MAX_Y);
        // if(coordDevices.getPositions(coordX, coordY)==-1){//verify if it is
        // empty
        // ApDevice ap = new
        // ApDevice("AccessPoint"+Integer.toString(i),coordX,coordY,i);//my
        // construction
        ApDevice ap = new ApDevice("AccessPoint" + Integer.toString(i), // name
                coordX, coordY, i// id
                ,
                 100 * 1024 * 1024// downLinkBandwidth - 100 Mbits
                ,
                 200// engergyConsuption
                ,
                 MaxAndMin.MAX_ST_IN_AP// maxSmartThing
                ,
                 100 * 1024 * 1024// upLinkBandwidth 100 Mbits
                ,
                 4// upLinkLatency
        );// ver valores reais e melhores
        apDevices.add(i, ap);
        // coordDevices.setPositions(ap.getId(), ap.getCoord().getCoordX(),
        // ap.getCoord().getCoordY());

        // System.out.println("i: "+i);
        // break;
        // }
        // else {
        // LogMobile.debug("AppExemplo2.java", "POSITION ISN'T AVAILABLE... (AP)
        // X ="+ coordX+ " Y = " +coordY+" Reallocating..." );
        // }
        // }
    }

    public static void addSmartThing(List<MobileDevice> smartThing,
            Coordinate coordDevices, int i) {

        // Random rand = new Random((Integer.MAX_VALUE*getSeed())/(i+1));
        int coordX = 0, coordY = 0;
        int direction, speed;
        direction = getRand().nextInt(MaxAndMin.MAX_DIRECTION - 1) + 1;
        speed = getRand().nextInt(MaxAndMin.MAX_SPEED - 1) + 1;
        /**
         * ************* Start set of Mobile Sensors ***************
         */
        VmMigrationTechnique migrationTechnique = null;

        if (getPolicyReplicaVM() == Policies.MIGRATION_COMPLETE_VM) {
            migrationTechnique = new CompleteVM(getMigPointPolicy());
        } else if (getPolicyReplicaVM() == Policies.MIGRATION_CONTAINER_VM) {
            migrationTechnique = new ContainerVM(getMigPointPolicy());
        } else if (getPolicyReplicaVM() == Policies.LIVE_MIGRATION) {
            migrationTechnique = new LiveMigration(getMigPointPolicy());

        }

        DeterministicDistribution distribution0 = new DeterministicDistribution(
                EEG_TRANSMISSION_TIME);// +(i*getRand().nextDouble()));

        Set<MobileSensor> sensors = new HashSet<>();

        MobileSensor sensor = new MobileSensor("Sensor" + i // Tuple's name ->
                // ACHO QUE DÁ PARA
                // USAR ESTE
                // CONSTRUTOR
                ,
                 "EEG" + i // Tuple's type
                ,
                 i // User Id
                ,
                 "MyApp_vr_game" + i // app's name
                ,
                 distribution0);
        sensors.add(sensor);

        // MobileSensor sensor1 = new MobileSensor("Sensor1" //Tuple's name ->
        // ACHO QUE DÁ PARA USAR ESTE CONSTRUTOR
        // ,"EEG" //Tuple's type
        // ,i //User Id
        // ,"appId1" //app's name
        // ,distribution0 );
        // sensors.add(sensor1);
        // MobileSensor sensor0 = new MobileSensor("Sensor0" //Tuple's name ->
        // ACHO QUE DÁ PARA USAR ESTE CONSTRUTOR
        // ,"EEG" //Tuple's type
        // ,i //User Id
        // ,"appId0" //app's name
        // ,distribution0);// find into the paper about tuples and distribution
        // MobileSensor sensor1 = new
        // MobileSensor("Sensor1","EEG",i,"appId1",distribution1);
        // MobileSensor sensor2 = new MobileSensor("Sensor2"// tuple's name
        // , i// userId
        // , "EEG"//Tuple's name
        // , -1//gatewayDeviceId - I think it is the ServerCloudlet id - as it
        // not creation yet, -1
        // , 20//latency
        // , null//geoLocation - it uses the MobileDevice's localization
        // , distribution2//transmitDistribution
        // , 2000//cpuLength
        // , 10//nwLength
        // , "EEG"
        // , "destModuleName2");
        //
        // MobileSensor sensor3 = new MobileSensor("Sensor3"
        // , i// userId
        // , "EEG"
        // , -1//gatewayDeviceId - I think it is the ServerCloudlet id - as it
        // not creation yet, -1
        // , 20//latency
        // , null//geoLocation - it uses the MobileDevice's localization
        // , distribution3//transmitDistribution
        // , 2000//cpuLength
        // , 10//nwLength
        // , "EEG"
        // , "destModuleName3");
        // Set<MobileSensor> sensors = new HashSet<>();
        // sensors.add(sensor0);
        // sensors.add(sensor1);
        // sensors.add(sensor2);
        // sensors.add(sensor3);
        /**
         * ************* End set of Mobile Sensors ***************
         */
        /**
         * ************* Start set of Mobile Actuators ***************
         */
        MobileActuator actuator0 = new MobileActuator("Actuator" + i, i,
                "MyApp_vr_game" + i, "DISPLAY" + i);

        // MobileActuator actuator1 = new MobileActuator("Actuator1", i,
        // "appId1", "actuatorType1");
        // MobileActuator actuator2 = new MobileActuator("Actuator2"
        // , i// userId
        // , "appId2"
        // , -1 //gatewayDeviceId
        // , 2 //latency
        // , null //geoLocation
        // , "actuatorType2"
        // , "srcModuleName2");
        // MobileActuator actuator3 = new MobileActuator("Actuator"+i
        // , i// userId
        // , "MyApp_vr_game"+i
        // , -1 //gatewayDeviceId
        // , 2 //latency
        // , null //geoLocation
        // , "DISPLAY"+i
        // , "DISPLAY"+i); // ../..PAREI AQUI: POR QUE O ATUADOR ESTA RECEBENDO
        // EM UM DISPLAY DIFERENTE
        Set<MobileActuator> actuators = new HashSet<>();
        // actuators.add(actuator0);
        // actuators.add(actuator1);
        // actuators.add(actuator2);
        actuators.add(actuator0);

        /**
         * ************* End set of Mobile Actuators ***************
         */
        /**
         * ************* Start MobileDevice Configurations ***************
         */
        FogLinearPowerModel powerModel = new FogLinearPowerModel(87.53d,
                82.44d);// 10//maxPower

        List<Pe> peList = new ArrayList<>();
        int mips = 50000;
        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to
        // storage
        // Pe id and
        // MIPS
        // Rating -
        // to
        // CloudSim

        int hostId = FogUtils.generateEntityId();
        long storage = 140000;
        // host storage
        int bw = 2048;
        int ram = 1024;
        PowerHost host = new PowerHost(// To the hardware's characteristics
                // (MobileDevice) - to CloudSim
                hostId, new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw), storage, peList,
                new StreamOperatorScheduler(peList), powerModel);

        List<Host> hostList = new ArrayList<Host>();// why to create a list?
        hostList.add(host);

        String arch = "x86"; // system architecture
        String os = "Android"; // operating system
        String vmm = "empty";// Empty
        double vmSize = MaxAndMin.MAX_VM_SIZE;
        double time_zone = 10.0; // time zone this resource located
        double cost = 1.0; // the cost of using processing in this resource
        double costPerMem = 0.005; // the cost of using memory in this resource
        double costPerStorage = 0.0001; // the cost of using storage in this
        // resource
        double costPerBw = 0.001; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are
        // not
        // adding
        // SAN
        // devices by now

        // for Characteristics
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        AppModuleAllocationPolicy vmAllocationPolicy = new AppModuleAllocationPolicy(
                hostList);

        MobileDevice st = null;
        // Vm vmTemp = new Vm(id, userId, mips, numberOfPes, ram, bw, size, vmm,
        // cloudletScheduler);
        float maxServiceValue = getRand().nextFloat() * 100;
        try {

//			while (true) {
//				coordX = getRand().nextInt((int) (MaxAndMin.MAX_X * 0.8));
//				coordY = getRand().nextInt((int) (MaxAndMin.MAX_Y * 0.8));
//				if ((coordX < MaxAndMin.MAX_X * 0.2)
//						|| (coordY < MaxAndMin.MAX_Y * 0.2)) {
//					continue;
//				}
//				// if(coordDevices.getPositions(coordX, coordY)==-1){//verify if
//				// it is empty
            double download = generateMDBW();
            st = new MobileDevice("SmartThing" + Integer.toString(i),
                    characteristics, vmAllocationPolicy// - seria a maquina
                    // que executa
                    // dentro do
                    // fogDevice?
                    ,
                     storageList, 2// schedulingInterval
                    ,
                     (download / 2) * 1024 * 1024// uplinkBandwidth - 1 Mbit
                    ,
                     download * 1024 * 1024// downlinkBandwidth - 2 Mbits
                    ,
                     generateMDLT()// uplinkLatency
                    ,
                     0.01// mipsPer..
                    ,
                     coordX, coordY, i// id
                    ,
                     direction, speed, maxServiceValue, vmSize,
                    migrationTechnique);
            st.setTempSimulation(0);
            st.setTimeFinishDeliveryVm(-1);
            st.setTimeFinishHandoff(0);
            st.setSensors(sensors);
            st.setActuators(actuators);
            st.setTravelPredicTime(getTravelPredicTimeForST());
            st.setMobilityPredictionError(getMobilityPrecitionError());
            smartThing.add(i, st);
            // coordDevices.setPositions(st.getId(),
            // st.getCoord().getCoordX(), st.getCoord().getCoordY());
//				break;
            // }
            // else{
            // LogMobile.debug("AppExemplo2.java","POSITION ISN'T
            // AVAILABLE... (ST)X ="+ coordX+ " Y = " +coordY+"
            // Reallocating..." );
            // }
//			}
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void addSmartThing(List<MobileDevice> smartThing,
            Coordinate coordDevices, int i, Algorithm algorithm, Technology technology) {

        // Random rand = new Random((Integer.MAX_VALUE*getSeed())/(i+1));
        int coordX = 0, coordY = 0;
        int direction, speed;
        direction = getRand().nextInt(MaxAndMin.MAX_DIRECTION - 1) + 1;
        speed = getRand().nextInt(MaxAndMin.MAX_SPEED - 1) + 1;
        /**
         * ************* Start set of Mobile Sensors ***************
         */
        VmMigrationTechnique migrationTechnique = null;

        if (getPolicyReplicaVM() == Policies.MIGRATION_COMPLETE_VM) {
            migrationTechnique = new CompleteVM(getMigPointPolicy());
        } else if (getPolicyReplicaVM() == Policies.MIGRATION_CONTAINER_VM) {
            migrationTechnique = new ContainerVM(getMigPointPolicy());
        } else if (getPolicyReplicaVM() == Policies.LIVE_MIGRATION) {
            migrationTechnique = new LiveMigration(getMigPointPolicy());

        }

        DeterministicDistribution distribution0 = new DeterministicDistribution(
                EEG_TRANSMISSION_TIME);// +(i*getRand().nextDouble()));

        Set<MobileSensor> sensors = new HashSet<>();

        MobileSensor sensor = new MobileSensor("Sensor" + i // Tuple's name ->
                // ACHO QUE DÁ PARA
                // USAR ESTE
                // CONSTRUTOR
                ,
                 "EEG" + i // Tuple's type
                ,
                 i // User Id
                ,
                 "MyApp_vr_game" + i // app's name
                ,
                 distribution0);
        sensors.add(sensor);

        // MobileSensor sensor1 = new MobileSensor("Sensor1" //Tuple's name ->
        // ACHO QUE DÁ PARA USAR ESTE CONSTRUTOR
        // ,"EEG" //Tuple's type
        // ,i //User Id
        // ,"appId1" //app's name
        // ,distribution0 );
        // sensors.add(sensor1);
        // MobileSensor sensor0 = new MobileSensor("Sensor0" //Tuple's name ->
        // ACHO QUE DÁ PARA USAR ESTE CONSTRUTOR
        // ,"EEG" //Tuple's type
        // ,i //User Id
        // ,"appId0" //app's name
        // ,distribution0);// find into the paper about tuples and distribution
        // MobileSensor sensor1 = new
        // MobileSensor("Sensor1","EEG",i,"appId1",distribution1);
        // MobileSensor sensor2 = new MobileSensor("Sensor2"// tuple's name
        // , i// userId
        // , "EEG"//Tuple's name
        // , -1//gatewayDeviceId - I think it is the ServerCloudlet id - as it
        // not creation yet, -1
        // , 20//latency
        // , null//geoLocation - it uses the MobileDevice's localization
        // , distribution2//transmitDistribution
        // , 2000//cpuLength
        // , 10//nwLength
        // , "EEG"
        // , "destModuleName2");
        //
        // MobileSensor sensor3 = new MobileSensor("Sensor3"
        // , i// userId
        // , "EEG"
        // , -1//gatewayDeviceId - I think it is the ServerCloudlet id - as it
        // not creation yet, -1
        // , 20//latency
        // , null//geoLocation - it uses the MobileDevice's localization
        // , distribution3//transmitDistribution
        // , 2000//cpuLength
        // , 10//nwLength
        // , "EEG"
        // , "destModuleName3");
        // Set<MobileSensor> sensors = new HashSet<>();
        // sensors.add(sensor0);
        // sensors.add(sensor1);
        // sensors.add(sensor2);
        // sensors.add(sensor3);
        /**
         * ************* End set of Mobile Sensors ***************
         */
        /**
         * ************* Start set of Mobile Actuators ***************
         */
        MobileActuator actuator0 = new MobileActuator("Actuator" + i, i,
                "MyApp_vr_game" + i, "DISPLAY" + i);

        // MobileActuator actuator1 = new MobileActuator("Actuator1", i,
        // "appId1", "actuatorType1");
        // MobileActuator actuator2 = new MobileActuator("Actuator2"
        // , i// userId
        // , "appId2"
        // , -1 //gatewayDeviceId
        // , 2 //latency
        // , null //geoLocation
        // , "actuatorType2"
        // , "srcModuleName2");
        // MobileActuator actuator3 = new MobileActuator("Actuator"+i
        // , i// userId
        // , "MyApp_vr_game"+i
        // , -1 //gatewayDeviceId
        // , 2 //latency
        // , null //geoLocation
        // , "DISPLAY"+i
        // , "DISPLAY"+i); // ../..PAREI AQUI: POR QUE O ATUADOR ESTA RECEBENDO
        // EM UM DISPLAY DIFERENTE
        Set<MobileActuator> actuators = new HashSet<>();
        // actuators.add(actuator0);
        // actuators.add(actuator1);
        // actuators.add(actuator2);
        actuators.add(actuator0);

        /**
         * ************* End set of Mobile Actuators ***************
         */
        /**
         * ************* Start MobileDevice Configurations ***************
         */
        FogLinearPowerModel powerModel = new FogLinearPowerModel(87.53d,
                82.44d);// 10//maxPower

        List<Pe> peList = new ArrayList<>();
        int mips = 50000;
        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to
        // storage
        // Pe id and
        // MIPS
        // Rating -
        // to
        // CloudSim

        int hostId = FogUtils.generateEntityId();
        long storage = 140000;
        // host storage
        int bw = 2048;
        int ram = 1024;
        PowerHost host = new PowerHost(// To the hardware's characteristics
                // (MobileDevice) - to CloudSim
                hostId, new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw), storage, peList,
                new StreamOperatorScheduler(peList), powerModel);

        List<Host> hostList = new ArrayList<Host>();// why to create a list?
        hostList.add(host);

        String arch = "x86"; // system architecture
        String os = "Android"; // operating system
        String vmm = "empty";// Empty
        double vmSize = MaxAndMin.MAX_VM_SIZE;
        double time_zone = 10.0; // time zone this resource located
        double cost = 1.0; // the cost of using processing in this resource
        double costPerMem = 0.005; // the cost of using memory in this resource
        double costPerStorage = 0.0001; // the cost of using storage in this
        // resource
        double costPerBw = 0.001; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are
        // not
        // adding
        // SAN
        // devices by now

        // for Characteristics
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        AppModuleAllocationPolicy vmAllocationPolicy = new AppModuleAllocationPolicy(
                hostList);

        MobileDevice st = null;
        // Vm vmTemp = new Vm(id, userId, mips, numberOfPes, ram, bw, size, vmm,
        // cloudletScheduler);
        float maxServiceValue = getRand().nextFloat() * 100;
        try {

//			while (true) {
//				coordX = getRand().nextInt((int) (MaxAndMin.MAX_X * 0.8));
//				coordY = getRand().nextInt((int) (MaxAndMin.MAX_Y * 0.8));
//				if ((coordX < MaxAndMin.MAX_X * 0.2)
//						|| (coordY < MaxAndMin.MAX_Y * 0.2)) {
//					continue;
//				}
//				// if(coordDevices.getPositions(coordX, coordY)==-1){//verify if
//				// it is empty
            double download = generateMDBW();
            st = new MobileDevice("SmartThing" + Integer.toString(i),
                    characteristics, vmAllocationPolicy// - seria a maquina
                    // que executa
                    // dentro do
                    // fogDevice?
                    ,
                     storageList, 2// schedulingInterval
                    ,
                     (download / 2) * 1024 * 1024// uplinkBandwidth - 1 Mbit
                    ,
                     download * 1024 * 1024// downlinkBandwidth - 2 Mbits
                    ,
                     generateMDLT()// uplinkLatency
                    ,
                     0.01// mipsPer..
                    ,
                     coordX, coordY, i// id
                    ,
                     direction, speed, maxServiceValue, vmSize,
                    migrationTechnique);
            st.setTempSimulation(0);
            st.setTimeFinishDeliveryVm(-1);
            st.setTimeFinishHandoff(0);
            st.setSensors(sensors);
            st.setActuators(actuators);
            st.setTravelPredicTime(getTravelPredicTimeForST());
            st.setMobilityPredictionError(getMobilityPrecitionError());
            st.setAlgorithm(algorithm);
            st.setTechnology(technology);
            smartThing.add(i, st);
            // coordDevices.setPositions(st.getId(),
            // st.getCoord().getCoordX(), st.getCoord().getCoordY());
//				break;
            // }
            // else{
            // LogMobile.debug("AppExemplo2.java","POSITION ISN'T
            // AVAILABLE... (ST)X ="+ coordX+ " Y = " +coordY+"
            // Reallocating..." );
            // }
//			}
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static FogDevice getFogDeviceById(int id, List<FogDevice> fognodes) {
        if (fognodes == null) {
            fognodes = fognodes;
        }
        for (FogDevice sc : fognodes) {
            if (id == sc.getId()) {
                return sc;
            }
        }
        return null;
    }

    public static FogDevice getFogDeviceByMyId(int id, List<FogDevice> fognodes) {
        for (FogDevice sc : fognodes) {
            if (id == sc.getMyId()) {
                return sc;
            }
        }
        return null;
    }

    public static void addServerCloudlet(List<FogDevice> serverCloudlets, Coordinate coordDevices) {
        int i = 0;
        int coordX, coordY;

        // for(coordX=1001; coordX<MaxAndMin.MAX_X-990; coordX+=2001){ /*evenly
        // distributed*/
        for (coordX = 0; coordX < MaxAndMin.MAX_X; coordX += (2 * MaxAndMin.CLOUDLET_COVERAGE - (2 * MaxAndMin.CLOUDLET_COVERAGE / 3))) {
            /* evenly distributed */
            System.out.println("Creating Server cloudlets - FIXED");
            //System.out.println("choosing - " + getMigStrategyPolicy());
            // for(coordY=1001; coordY<MaxAndMin.MAX_Y-990; coordY+=2001, i++){
            // /*evenly distributed*/
            for (coordY = 0; coordY < MaxAndMin.MAX_X; coordY += (2
                    * MaxAndMin.CLOUDLET_COVERAGE
                    - (2 * MaxAndMin.CLOUDLET_COVERAGE
                    / 3)), i++) {
                /* evenly distributed */
                // Random rand = new
                // Random(getSeed()*(i+1));//((Long.MAX_VALUE/getSeed())/(i+1))*2);
                DecisionMigration migrationStrategy = null;
                switch (getMigStrategyPolicy()) {
                    case Policies.LOWEST_LATENCY:
                        migrationStrategy = new LowestLatency(getServerCloudlets(),
                                getApDevices(), getMigPointPolicy(), getPolicyReplicaVM());
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET:
                        migrationStrategy = new LowestDistBwSmartThingServerCloudlet(
                                getServerCloudlets(), getApDevices(), getMigPointPolicy(),
                                getPolicyReplicaVM());
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_AP:
                        //Policies.LOWEST_DIST_BW_SMARTTING_AP
                        migrationStrategy = new LowestDistBwSmartThingAP(
                                getServerCloudlets(), getApDevices(), getMigPointPolicy(),
                                getPolicyReplicaVM());
                        break;
                    case Policies.CMFOG:
                        migrationStrategy = new CMFogDecision(getServerCloudlets(),
                                getApDevices(), getMigPointPolicy(), getPolicyReplicaVM());
                        break;
                    default:
                        break;
                }

                BeforeMigration beforeMigration = null;
                if (getPolicyReplicaVM() == Policies.MIGRATION_COMPLETE_VM) {
                    beforeMigration = new PrepareCompleteVM();
                } else if (getPolicyReplicaVM() == Policies.MIGRATION_CONTAINER_VM) {
                    beforeMigration = new PrepareContainerVM();
                } else if (getPolicyReplicaVM() == Policies.LIVE_MIGRATION) {
                    beforeMigration = new PrepareLiveMigration();
                }

                FogLinearPowerModel powerModel = new FogLinearPowerModel(
                        107.339d, 83.433d);// 10//maxPower

                List<Pe> peList = new ArrayList<>();// CloudSim Pe (Processing
                // Element) class represents
                // CPU unit, defined in
                // terms of Millions
                // * Instructions Per Second (MIPS) rating
                int mips = 2800000 * (i + 1);
                // 3. Create PEs and add these into a list.
                peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need
                // to
                // store
                // Pe
                // id
                // and
                // MIPS
                // Rating
                // -
                // to
                // CloudSim

                int hostId = FogUtils.generateEntityId();
                long storage = 1000 * 1024 * 1024;// Long.MAX_VALUE; // host
                // storage
                int bw = 1000 * 1024 * 1024;
                int ram = 25000;// host memory (MB)
                PowerHost host = new PowerHost(// To the hardware's
                        // characteristics
                        // (MobileDevice) - to CloudSim
                        hostId, new RamProvisionerSimple(ram),
                        new BwProvisionerOverbooking(bw), storage, peList,
                        new StreamOperatorScheduler(peList), powerModel);

                List<Host> hostList = new ArrayList<Host>();// why to create a
                // list?
                hostList.add(host);

                String arch = "x86"; // system architecture
                String os = "Linux"; // operating system
                String vmm = "Empty";// Empty
                double time_zone = 10.0; // time zone this resource located
                double cost = 3.0; // the cost of using processing in this
                // resource
                double costPerMem = 0.05; // the cost of using memory in this
                // resource
                double costPerStorage = 0.001; // the cost of using storage in
                // this
                // resource
                double costPerBw = 0.0; // the cost of using bw in this resource
                LinkedList<Storage> storageList = new LinkedList<Storage>(); // we
                // are
                // not
                // adding
                // SAN
                // devices by now
                FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                        arch, os, vmm, host, time_zone, cost, costPerMem,
                        costPerStorage, costPerBw);

                AppModuleAllocationPolicy vmAllocationPolicy = new AppModuleAllocationPolicy(
                        hostList);
                FogDevice sc = null;
                Service serviceOffer = new Service();
                serviceOffer.setType(Services.PUBLIC);
                // if (serviceOffer.getType() == Services.HIBRID || serviceOffer.getType() == Services.PUBLIC) {
                serviceOffer.setValue(getRand().nextFloat() * 10);
                //  } else {
                //      serviceOffer.setValue(0);
                //   }
                try {
                    // if(coordDevices.getPositions(coordX, coordY)==-1){
                    // ApDevice ap = new
                    // ApDevice("AccessPoint"+Integer.toString(i),coordX,coordY,i);//my
                    // construction
                    double maxBandwidth = getMaxBandwidth() * 1024 * 1024;// MaxAndMin.MAX_BANDWIDTH;
                    double minBandwidth = (getMaxBandwidth() * 1024 * 1024);// MaxAndMin.MIN_BANDWIDTH;

//                    double upLinkRandom = minBandwidth
//                            + (maxBandwidth - minBandwidth)
//                            * getRand().nextDouble();
//                    double downLinkRandom = minBandwidth
//                            + (maxBandwidth - minBandwidth)
//                            * getRand().nextDouble();
                    sc = new FogDevice("ServerCloudlet" + Integer.toString(i) // name
                            ,
                             characteristics, vmAllocationPolicy// vmAllocationPolicy
                            ,
                             storageList, 10// schedulingInterval
                            ,
                             minBandwidth// uplinkBandwidth
                            ,
                             maxBandwidth// downlinkBandwidth
                            ,
                             4// rand.nextDouble()//uplinkLatency
                            ,
                             0.01// mipsPer..
                            ,
                             coordX, coordY, i, serviceOffer,
                            migrationStrategy, getPolicyReplicaVM(),
                            beforeMigration);
                    serverCloudlets.add(i, sc);
                    //   System.out.println("MY ID CLOUDLET: "+sc.getMyId()+" / "+sc.getId());
                    // coordDevices.setPositions(sc.getId(),
                    // sc.getCoord().getCoordX(), sc.getCoord().getCoordY());
                    sc.setParentId(-1);

                    // }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } // id

            }
        }
        LogMobile.debug("AppExemplo2.java", "Total of serverCloudlets: " + i);
    }

    public static int cloudletSize = 0;

    public static void createCloud() {
        DecisionMigration migrationStrategy = null;
        switch (getMigStrategyPolicy()) { //CHANGE_HERE
            case Policies.LOWEST_LATENCY:
                migrationStrategy = new LowestLatency(getServerCloudlets(),
                        getApDevices(), getMigPointPolicy(), getPolicyReplicaVM());
                break;
            case Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET:
                migrationStrategy = new LowestDistBwSmartThingServerCloudlet(
                        getServerCloudlets(), getApDevices(), getMigPointPolicy(),
                        getPolicyReplicaVM());
                break;
            case Policies.LOWEST_DIST_BW_SMARTTING_AP:
                migrationStrategy = new LowestDistBwSmartThingAP(
                        getServerCloudlets(), getApDevices(), getMigPointPolicy(),
                        getPolicyReplicaVM());
                break;
            case Policies.CMFOG:
                migrationStrategy = new CMFogDecision(getServerCloudlets(),
                        getApDevices(), getMigPointPolicy(), getPolicyReplicaVM());
                break;
            default:
                break;
        }

        BeforeMigration beforeMigration = null;
        if (getPolicyReplicaVM() == Policies.MIGRATION_COMPLETE_VM) {
            beforeMigration = new PrepareCompleteVM();
        } else if (getPolicyReplicaVM() == Policies.MIGRATION_CONTAINER_VM) {
            beforeMigration = new PrepareContainerVM();
        } else if (getPolicyReplicaVM() == Policies.LIVE_MIGRATION) {
            beforeMigration = new PrepareLiveMigration();
        }

        FogLinearPowerModel powerModel = new FogLinearPowerModel(
                107.339d, 83.433d);// 10//maxPower

        List<Pe> peList = new ArrayList<>();
        int mips = 2800000 * (cloudletSize + 1);
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
        int hostId = FogUtils.generateEntityId();
        long storage = 10000 * 1024 * 1024;
        int bw = 10000 * 1024 * 1024;
        int ram = 250000;
        PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw), storage, peList,
                new StreamOperatorScheduler(peList), powerModel);

        List<Host> hostList = new ArrayList<Host>();// why to create a
        // list?
        hostList.add(host);

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Empty";// Empty
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this
        double costPerMem = 0.05; // the cost of using memory in this
        double costPerStorage = 0.001; // the cost of using storage in
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);
        AppModuleAllocationPolicy vmAllocationPolicy = new AppModuleAllocationPolicy(
                hostList);
        Service serviceOffer = new Service();
        serviceOffer.setType(
                getRand().nextInt(10000) % MaxAndMin.MAX_SERVICES);
        if (serviceOffer.getType() == Services.HIBRID
                || serviceOffer.getType() == Services.PUBLIC) {
            serviceOffer.setValue(getRand().nextFloat() * 10);
        } else {
            serviceOffer.setValue(0);
        }
        try {

            double maxBandwidth = generateFogBW() * 10 * 1024 * 1024;// MaxAndMin.MAX_BANDWIDTH;
            double minBandwidth = maxBandwidth / 2;// MaxAndMin.MIN_BANDWIDTH;
            //double minBandwidth = (getMaxBandwidth() * 1024 * 1024);// MaxAndMin.MIN_BANDWIDTH;
            cloud = new FogDevice("CLOUD" // name
                    ,
                     characteristics, vmAllocationPolicy// vmAllocationPolicy
                    ,
                     storageList, 10// schedulingInterval
                    ,
                     minBandwidth// uplinkBandwidth
                    ,
                     maxBandwidth// downlinkBandwidth
                    ,
                     generateFogLT()// rand.nextDouble()//uplinkLatency
                    ,
                     0.01// mipsPer..
                    ,
                     1250, 1250, 100000, serviceOffer,
                    migrationStrategy, getPolicyReplicaVM(),
                    beforeMigration);

            cloud.setParentId(-1);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public static int addServerCloudletGRID(List<FogDevice> serverCloudlets, Coordinate coordDevices, int layer) {
        double coverageArea = 0;
        double overlap = 0;
        if (layer == 1) {
            coverageArea = MaxAndMin.CLOUDLET_COVERAGE;
            overlap = MaxAndMin.MIG_POINT;
        } else if (layer == 2) {
            coverageArea = MaxAndMin.CLOUDLET_COVERAGE * 2;
            overlap = MaxAndMin.MIG_POINT * 2;
        }
        double radius = coverageArea / 2;
        double coordX, coordY;
        System.out.println("Creating Server cloudlets L2 - GRID");
        for (coordX = radius; coordX < MaxAndMin.MAX_X; coordX += coverageArea - overlap) {
            for (coordY = radius; coordY < MaxAndMin.MAX_Y; coordY += coverageArea - overlap, cloudletSize++) {
                DecisionMigration migrationStrategy = null;
                switch (getMigStrategyPolicy()) { //CHANGE_HERE
                    case Policies.LOWEST_LATENCY:
                        migrationStrategy = new LowestLatency(getServerCloudlets(),
                                getApDevices(), getMigPointPolicy(), getPolicyReplicaVM());
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET:
                        migrationStrategy = new LowestDistBwSmartThingServerCloudlet(
                                getServerCloudlets(), getApDevices(), getMigPointPolicy(),
                                getPolicyReplicaVM());
                        break;
                    case Policies.LOWEST_DIST_BW_SMARTTING_AP:
                        migrationStrategy = new LowestDistBwSmartThingAP(
                                getServerCloudlets(), getApDevices(), getMigPointPolicy(),
                                getPolicyReplicaVM());
                        break;
                    case Policies.CMFOG:
                        migrationStrategy = new CMFogDecision(getServerCloudlets(),
                                getApDevices(), getMigPointPolicy(), getPolicyReplicaVM());
                        break;
                    default:
                        break;
                }

                BeforeMigration beforeMigration = null;
                if (getPolicyReplicaVM() == Policies.MIGRATION_COMPLETE_VM) {
                    beforeMigration = new PrepareCompleteVM();
                } else if (getPolicyReplicaVM() == Policies.MIGRATION_CONTAINER_VM) {
                    beforeMigration = new PrepareContainerVM();
                } else if (getPolicyReplicaVM() == Policies.LIVE_MIGRATION) {
                    beforeMigration = new PrepareLiveMigration();
                }

                FogLinearPowerModel powerModel = new FogLinearPowerModel(
                        107.339d, 83.433d);// 10//maxPower

                List<Pe> peList = new ArrayList<>();
                int mips = 2800000 * (cloudletSize + 1);
                peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
                int hostId = FogUtils.generateEntityId();
                long storage = 1000 * 1024 * 1024;
                int bw = 1000 * 1024 * 1024;
                int ram = 25000;
                PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram),
                        new BwProvisionerOverbooking(bw), storage, peList,
                        new StreamOperatorScheduler(peList), powerModel);

                List<Host> hostList = new ArrayList<Host>();// why to create a
                // list?
                hostList.add(host);

                String arch = "x86"; // system architecture
                String os = "Linux"; // operating system
                String vmm = "Empty";// Empty
                double time_zone = 10.0; // time zone this resource located
                double cost = 3.0; // the cost of using processing in this
                double costPerMem = 0.05; // the cost of using memory in this
                double costPerStorage = 0.001; // the cost of using storage in
                double costPerBw = 0.0; // the cost of using bw in this resource
                LinkedList<Storage> storageList = new LinkedList<Storage>();
                FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                        arch, os, vmm, host, time_zone, cost, costPerMem,
                        costPerStorage, costPerBw);
                AppModuleAllocationPolicy vmAllocationPolicy = new AppModuleAllocationPolicy(
                        hostList);
                FogDevice sc = null;
                Service serviceOffer = new Service();
                serviceOffer.setType(
                        getRand().nextInt(10000) % MaxAndMin.MAX_SERVICES);
                if (serviceOffer.getType() == Services.HIBRID
                        || serviceOffer.getType() == Services.PUBLIC) {
                    serviceOffer.setValue(getRand().nextFloat() * 10);
                } else {
                    serviceOffer.setValue(0);
                }
                try {

                    double maxBandwidth = (generateFogBW() * (layer * 2 - 1)) * 1024 * 1024;// MaxAndMin.MAX_BANDWIDTH;
                    double minBandwidth = maxBandwidth / 2;// MaxAndMin.MIN_BANDWIDTH;
                    //double minBandwidth = (getMaxBandwidth() * 1024 * 1024);// MaxAndMin.MIN_BANDWIDTH;
                    sc = new FogDevice("ServerCloudlet" + Integer.toString(cloudletSize) // name
                            ,
                             characteristics, vmAllocationPolicy// vmAllocationPolicy
                            ,
                             storageList, 10// schedulingInterval
                            ,
                             minBandwidth// uplinkBandwidth
                            ,
                             maxBandwidth// downlinkBandwidth
                            ,
                             generateFogLT() * ((layer * 2) - 1)// rand.nextDouble()//uplinkLatency
                            ,
                             0.01// mipsPer..
                            ,
                             (int) coordX, (int) coordY, cloudletSize, serviceOffer,
                            migrationStrategy, getPolicyReplicaVM(),
                            beforeMigration);
                    serverCloudlets.add(serverCloudlets.size(), sc);
                    sc.setParentId(-1);

                } catch (Exception e) {

                    e.printStackTrace();
                }

            }
        }
        System.out.println("------------------------>TOTAL CLOUDLETS: " + cloudletSize);
        LogMobile.debug("AppExemplo2.java", "Total of serverCloudlets: " + cloudletSize);
        return cloudletSize;
    }

//    private static void createServerCloudletsNetwork(List<FogDevice> serverCloudlets) {
//        // for no full graph, use -1 to link
//        HashMap<FogDevice, Double> net = new HashMap<>();
//        // Random rand = new Random(100*getSeed());
//        for (FogDevice sc : serverCloudlets) {// It makes a full graph
//            for (FogDevice sc1 : serverCloudlets) {
//                if (sc.equals(sc1)) {
//                    break;
//                }
//                // if(rand.nextInt(100)%20 == 0){
//                // break;
//                // }
//                // net.keySet().add(sc1);
//                if (sc.getUplinkBandwidth() < sc1.getDownlinkBandwidth()) {
//                    net.put(sc1, sc.getUplinkBandwidth());
//                    NetworkTopology.addLink(sc.getId(), sc1.getId(),
//                            sc.getUplinkBandwidth(), getRand().nextDouble() + (AppExemplo2.generateFogLT() + AppExemplo2.generateFogLT()));
//                } else {
//                    net.put(sc1, sc1.getDownlinkBandwidth());
//                    NetworkTopology.addLink(sc.getId(), sc1.getId(),
//                            sc1.getDownlinkBandwidth(), getRand().nextDouble() + (AppExemplo2.generateFogLT() + AppExemplo2.generateFogLT()));
//                }
//            }
//            sc.setNetServerCloudlets(net);
//        }
//    }
    private static boolean isLayerOverlaped(Coordinate L1, Coordinate L2) {
        if (L1.getCoordX() <= (L2.getCoordX() + (MaxAndMin.CLOUDLET_COVERAGE * 2)) && L1.getCoordX() >= (L2.getCoordX() - (MaxAndMin.CLOUDLET_COVERAGE * 2))) {
            if (L1.getCoordY() <= (L2.getCoordY() + (MaxAndMin.CLOUDLET_COVERAGE * 2)) && L1.getCoordY() >= (L2.getCoordY() - (MaxAndMin.CLOUDLET_COVERAGE * 2))) {
                return true;
            }
        }
        return false;
    }

    private static void createServerCloudletsNetwork() {
        System.out.println("starting network builder");
//        FogDevice parentL2 = null;
//        for (FogDevice cloudletL1 : serverCloudlets) {
//            parentL2 = Distances.closestUpperCloudlet(serverCloudletsL2, cloudletL1);
//            cloudletL1.setParentId(parentL2.getId());
//
//            cloudletL1.getNetServerCloudlets().put(parentL2, cloudletL1.getUplinkBandwidth());
//            NetworkTopology.addLink(cloudletL1.getId(), parentL2.getId(),
//                    cloudletL1.getUplinkBandwidth(), getRand().nextDouble() + AppExemplo2.generateFogLT());
//
//            parentL2.getNetServerCloudlets().put(cloudletL1, cloudletL1.getDownlinkBandwidth());
//            NetworkTopology.addLink(parentL2.getId(), cloudletL1.getId(),
//                    cloudletL1.getDownlinkBandwidth(), getRand().nextDouble() + AppExemplo2.generateFogLT());
//            //System.out.println("[" + cloudletL1.getName() + " , " + cloudletL2.getName() + "]");
//            //System.out.println("[" + parentL2.getName() + " , " + cloudletL1.getName() + "]");
//
//        }
        for (FogDevice cloudletL1 : serverCloudlets) {
            for (FogDevice cloudletL2 : serverCloudletsL2) {
                if (isLayerOverlaped(cloudletL1.getCoord(), cloudletL2.getCoord())) {
                    cloudletL1.setParentId(cloudletL2.getId());
                    double temp = getRand().nextDouble() + AppExemplo2.generateFogLT();
                    cloudletL1.getNetServerCloudlets().put(cloudletL2, cloudletL1.getUplinkBandwidth());
                    NetworkTopology.addLink(cloudletL1.getId(), cloudletL2.getId(),
                            cloudletL1.getUplinkBandwidth(), temp);

                    cloudletL2.getNetServerCloudlets().put(cloudletL1, cloudletL1.getDownlinkBandwidth());
                    NetworkTopology.addLink(cloudletL2.getId(), cloudletL1.getId(),
                            cloudletL1.getDownlinkBandwidth(), temp);
                    break;
                }
            }
        }

        for (FogDevice cloudletL2 : serverCloudletsL2) {
            cloudletL2.setParentId(cloud.getId());
            cloudletL2.getNetServerCloudlets().put(cloud, cloudletL2.getUplinkBandwidth());
            NetworkTopology.addLink(cloudletL2.getId(), cloud.getId(),
                    cloudletL2.getUplinkBandwidth(), getRand().nextDouble() + cloudletL2.getUplinkLatency());

            cloud.getNetServerCloudlets().put(cloudletL2, cloudletL2.getDownlinkBandwidth());
            NetworkTopology.addLink(cloud.getId(), cloudletL2.getId(),
                    cloudletL2.getDownlinkBandwidth(), getRand().nextDouble() + cloudletL2.getUplinkLatency());
        }
    }

    @SuppressWarnings("unused")
    private static Application createApplication(String appId, int userId,
            int myId, AppModule userVm) {

        Application application = Application.createApplication(appId, userId); // creates
        // an
        // empty
        // application
        // model
        // (empty
        // directed
        // graph)
        application.addAppModule(userVm); // adding module Client to the
        // application model
        application.addAppModule("client" + myId, "appModuleClient" + myId, 10);
        // application.addAppModule("connector"+myId, 10);

        /*
		 * Connecting the application modules (vertices) in the application
		 * model (directed graph) with edges
         */
        if (EEG_TRANSMISSION_TIME >= 10) {
            application.addAppEdge("EEG" + myId, "client" + myId, 2000, 500,
                    "EEG" + myId, Tuple.UP, AppEdge.SENSOR); // adding edge from
        } // EEG (sensor)
        // to Client
        // module
        // carrying
        // tuples of
        // type EEG
        else {
            application.addAppEdge("EEG" + myId, "client" + myId, 3000, 500,
                    "EEG" + myId, Tuple.UP, AppEdge.SENSOR);
        }

        // application.addAppEdge(source, destination, tupleCpuLength,
        // tupleNwLength, tupleType, direction, edgeType)
        application.addAppEdge("client" + myId, userVm.getName(), 3500, 500,
                "_SENSOR" + myId, Tuple.UP, AppEdge.MODULE); // adding edge from
        // Client to
        // Concentration
        // Calculator
        // module
        // carrying
        // tuples of
        // type _SENSOR
        // application.addAppEdge(source, destination, periodicity,
        // tupleCpuLength, tupleNwLength, tupleType, direction, edgeType)?

        application.addAppEdge(userVm.getName(), userVm.getName(), 1000, 1000,
                1000, "PLAYER_GAME_STATE" + myId, Tuple.UP, AppEdge.MODULE); // adding
        // periodic
        // edge
        // (period=1000ms)
        // from
        // Concentration
        // Calculator
        // to
        // Connector
        // module
        // carrying
        // tuples
        // of
        // type
        // PLAYER_GAME_STATE
        application.addAppEdge(userVm.getName(), "client" + myId, 14, 500,
                "CONCENTRATION" + myId, Tuple.DOWN, AppEdge.MODULE); // adding
        // edge
        // from
        // Concentration
        // Calculator
        // to
        // Client
        // module
        // carrying
        // tuples
        // of
        // type
        // CONCENTRATION
        application.addAppEdge(userVm.getName(), "client" + myId, 1000, 28,
                1000, "GLOBAL_GAME_STATE" + myId, Tuple.DOWN, AppEdge.MODULE); // adding
        // periodic
        // edge
        // (period=1000ms)
        // from
        // Connector
        // to
        // Client
        // module
        // carrying
        // tuples
        // of
        // type
        // GLOBAL_GAME_STATE
        application.addAppEdge("client" + myId, "DISPLAY" + myId, 1000, 500,
                "SELF_STATE_UPDATE" + myId, Tuple.DOWN, AppEdge.ACTUATOR); // adding
        // edge
        // from
        // Client
        // module
        // to
        // Display
        // (actuator)
        // carrying
        // tuples
        // of
        // type
        // SELF_STATE_UPDATE
        application.addAppEdge("client" + myId, "DISPLAY" + myId, 1000, 500,
                "GLOBAL_STATE_UPDATE" + myId, Tuple.DOWN, AppEdge.ACTUATOR); // adding
        // edge
        // from
        // Client
        // module
        // to
        // Display
        // (actuator)
        // carrying
        // tuples
        // of
        // type
        // GLOBAL_STATE_UPDATE

        /*
		 * Defining the input-output relationships (represented by selectivity)
		 * of the application modules.
         */
        application.addTupleMapping("client" + myId, "EEG" + myId,
                "_SENSOR" + myId, new FractionalSelectivity(0.9)); // 0.9 tuples
        // of type
        // _SENSOR
        // are
        // emitted
        // by Client
        // module
        // per
        // incoming
        // tuple of
        // type EEG
        application.addTupleMapping("client" + myId, "CONCENTRATION" + myId,
                "SELF_STATE_UPDATE" + myId, new FractionalSelectivity(1.0)); // 1.0
        // tuples
        // of
        // type
        // SELF_STATE_UPDATE
        // are
        // emitted
        // by
        // Client
        // module
        // per
        // incoming
        // tuple
        // of
        // type
        // CONCENTRATION
        application.addTupleMapping(userVm.getName(), "_SENSOR" + myId,
                "CONCENTRATION" + myId, new FractionalSelectivity(1.0)); // 1.0
        // tuples
        // of
        // type
        // CONCENTRATION
        // are
        // emitted
        // by
        // Concentration
        // Calculator
        // module
        // per
        // incoming
        // tuple
        // of
        // type
        // _SENSOR
        application.addTupleMapping("client" + myId, "GLOBAL_GAME_STATE" + myId,
                "GLOBAL_STATE_UPDATE" + myId, new FractionalSelectivity(1.0)); // 1.0
        // tuples
        // of
        // type
        // GLOBAL_STATE_UPDATE
        // are
        // emitted
        // by
        // Client
        // module
        // per
        // incoming
        // tuple
        // of
        // type
        // GLOBAL_GAME_STATE

        /*
		 * Defining application loops to monitor the latency of.
		 * Here, we add only one loop for monitoring : EEG(sensor) -> Client ->
		 * Concentration Calculator -> Client -> DISPLAY (actuator)
         */
        final String client = "client" + myId;// userVm.getName();
        final String concentration = userVm.getName();
        final String eeg = "EEG" + myId;
        final String display = "DISPLAY" + myId;
        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add(eeg);
                add(client);
                add(concentration);
                add(client);
                add(display);
            }
        });
        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };
        application.setLoops(loops);

        return application;
    }

    @SuppressWarnings("unused")
    private static Application createApplication(String appId, int userId,
            int myId) {

        Application application = Application.createApplication(appId, userId); // creates
        // an
        // empty
        // application
        // model
        // (empty
        // directed
        // graph)

        application.addAppModule("client" + myId, 10); // adding module Client
        // to the application
        // model
        application.addAppModule("concentration_calculator" + myId, 10); // adding
        // module
        // Concentration
        // Calculator
        // to
        // the
        // application
        // model
        application.addAppModule("connector" + myId, 10); // adding module
        // Connector to the
        // application model

        /*
		 * Connecting the application modules (vertices) in the application
		 * model (directed graph) with edges
         */
        if (EEG_TRANSMISSION_TIME == 10) {
            application.addAppEdge("EEG" + myId, "client" + myId, 2000, 500,
                    "EEG" + myId, Tuple.UP, AppEdge.SENSOR); // adding edge from
        } // EEG (sensor)
        // to Client
        // module
        // carrying
        // tuples of
        // type EEG
        else {
            application.addAppEdge("EEG" + myId, "client" + myId, 3000, 500,
                    "EEG" + myId, Tuple.UP, AppEdge.SENSOR);
        }

        // application.addAppEdge(source, destination, tupleCpuLength,
        // tupleNwLength, tupleType, direction, edgeType)
        application.addAppEdge("client" + myId,
                "concentration_calculator" + myId, 3500, 500, "_SENSOR",
                Tuple.UP, AppEdge.MODULE); // adding edge from Client to
        // Concentration Calculator module
        // carrying tuples of type _SENSOR
        // application.addAppEdge(source, destination, periodicity,
        // tupleCpuLength, tupleNwLength, tupleType, direction, edgeType)?
        application.addAppEdge("concentration_calculator" + myId,
                "connector" + myId, 1000, 1000, 1000, "PLAYER_GAME_STATE",
                Tuple.UP, AppEdge.MODULE); // adding periodic edge
        // (period=1000ms) from
        // Concentration Calculator to
        // Connector module carrying tuples
        // of type PLAYER_GAME_STATE

        application.addAppEdge("concentration_calculator" + myId,
                "client" + myId, 14, 500, "CONCENTRATION", Tuple.DOWN,
                AppEdge.MODULE); // adding edge from Concentration Calculator to
        // Client module carrying tuples of type
        // CONCENTRATION
        application.addAppEdge("connector" + myId, "client" + myId, 1000, 28,
                1000, "GLOBAL_GAME_STATE", Tuple.DOWN, AppEdge.MODULE); // adding
        // periodic
        // edge
        // (period=1000ms)
        // from
        // Connector
        // to
        // Client
        // module
        // carrying
        // tuples
        // of
        // type
        // GLOBAL_GAME_STATE
        application.addAppEdge("client" + myId, "DISPLAY" + myId, 1000, 500,
                "SELF_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR); // adding
        // edge from
        // Client
        // module to
        // Display
        // (actuator)
        // carrying
        // tuples of
        // type
        // SELF_STATE_UPDATE
        application.addAppEdge("client" + myId, "DISPLAY" + myId, 1000, 500,
                "GLOBAL_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR); // adding
        // edge
        // from
        // Client
        // module
        // to
        // Display
        // (actuator)
        // carrying
        // tuples
        // of
        // type
        // GLOBAL_STATE_UPDATE

        /*
		 * Defining the input-output relationships (represented by selectivity)
		 * of the application modules.
         */
        application.addTupleMapping("client" + myId, "EEG" + myId, "_SENSOR",
                new FractionalSelectivity(0.9)); // 0.9 tuples of type _SENSOR
        // are emitted by Client
        // module per incoming tuple
        // of type EEG
        application.addTupleMapping("client" + myId, "CONCENTRATION",
                "SELF_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0
        // tuples
        // of
        // type
        // SELF_STATE_UPDATE
        // are
        // emitted
        // by
        // Client
        // module
        // per
        // incoming
        // tuple
        // of
        // type
        // CONCENTRATION
        application.addTupleMapping("concentration_calculator" + myId,
                "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0)); // 1.0
        // tuples
        // of
        // type
        // CONCENTRATION
        // are
        // emitted
        // by
        // Concentration
        // Calculator
        // module
        // per
        // incoming
        // tuple
        // of
        // type
        // _SENSOR
        application.addTupleMapping("client" + myId, "GLOBAL_GAME_STATE",
                "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0
        // tuples
        // of
        // type
        // GLOBAL_STATE_UPDATE
        // are
        // emitted
        // by
        // Client
        // module
        // per
        // incoming
        // tuple
        // of
        // type
        // GLOBAL_GAME_STATE

        /*
		 * Defining application loops to monitor the latency of.
		 * Here, we add only one loop for monitoring : EEG(sensor) -> Client ->
		 * Concentration Calculator -> Client -> DISPLAY (actuator)
         */
        final String client = "client" + myId;
        final String concentration = "concentration_calculator" + myId;
        final String eeg = "EEG" + myId;
        final String display = "DISPLAY" + myId;
        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add(eeg);
                add(client);
                add(concentration);
                add(client);
                add(display);
            }
        });
        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };
        application.setLoops(loops);

        return application;
    }

    public static int getPolicyReplicaVM() {
        return policyReplicaVM;
    }

    public static void setPolicyReplicaVM(int policyReplicaVM) {
        AppExemplo2.policyReplicaVM = policyReplicaVM;
    }

    public static int getTravelPredicTimeForST() {
        return travelPredicTimeForST;
    }

    public static void setTravelPredicTimeForST(int travelPredicTimeForST) {
        AppExemplo2.travelPredicTimeForST = travelPredicTimeForST;
    }

    public static int getMobilityPrecitionError() {
        return mobilityPrecitionError;
    }

    public static void setMobilityPredictionError(int mobilityPrecitionError) {
        AppExemplo2.mobilityPrecitionError = mobilityPrecitionError;
    }

    public static int getLatencyBetweenCloudlets() {
        return latencyBetweenCloudlets;
    }

    public static void setLatencyBetweenCloudlets(int latencyBetweenCloudlets) {
        AppExemplo2.latencyBetweenCloudlets = latencyBetweenCloudlets;
    }

    public static int getStepPolicy() {
        return stepPolicy;
    }

    public static void setStepPolicy(int stepPolicy) {
        AppExemplo2.stepPolicy = stepPolicy;
    }

    public static List<MobileDevice> getSmartThings() {
        return smartThings;
    }

    public static void setSmartThings(List<MobileDevice> smartThings) {
        AppExemplo2.smartThings = smartThings;
    }

    public static List<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }

    public static void setServerCloudlets(List<FogDevice> serverCloudlets) {
        AppExemplo2.serverCloudlets = serverCloudlets;
    }

    public static List<ApDevice> getApDevices() {
        return apDevices;
    }

    public static void setApDevices(List<ApDevice> apDevices) {
        AppExemplo2.apDevices = apDevices;
    }

    public static int getMigPointPolicy() {
        return migPointPolicy;
    }

    public static void setMigPointPolicy(int migPointPolicy) {
        AppExemplo2.migPointPolicy = migPointPolicy;
    }

    public static int getMigStrategyPolicy() {
        return migStrategyPolicy;
    }

    public static void setMigStrategyPolicy(int migStrategyPolicy) {
        AppExemplo2.migStrategyPolicy = migStrategyPolicy;
    }

    public static int getPositionApPolicy() {
        return positionApPolicy;
    }

    public static void setPositionApPolicy(int positionApPolicy) {
        AppExemplo2.positionApPolicy = positionApPolicy;
    }

    public static Coordinate getCoordDevices() {
        return coordDevices;
    }

    public static void setCoordDevices(Coordinate coordDevices) {
        AppExemplo2.coordDevices = coordDevices;
    }

    public static List<FogBroker> getBrokerList() {
        return brokerList;
    }

    public static void setBrokerList(List<FogBroker> brokerList) {
        AppExemplo2.brokerList = brokerList;
    }

    public static List<String> getAppIdList() {
        return appIdList;
    }

    public static void setAppIdList(List<String> appIdList) {
        AppExemplo2.appIdList = appIdList;
    }

    public static List<Application> getApplicationList() {
        return applicationList;
    }

    public static void setApplicationList(List<Application> applicationList) {
        AppExemplo2.applicationList = applicationList;
    }

    public static int getSeed() {
        return seed;
    }

    public static void setSeed(int seed) {
        AppExemplo2.seed = seed;
    }

    public static int getPositionScPolicy() {
        return positionScPolicy;
    }

    public static void setPositionScPolicy(int positionScPolicy) {
        AppExemplo2.positionScPolicy = positionScPolicy;
    }

    public static int getMaxSmartThings() {
        return maxSmartThings;
    }

    public static void setMaxSmartThings(int maxSmartThings) {
        AppExemplo2.maxSmartThings = maxSmartThings;
    }

    public static Random getRand() {
        return rand;
    }

    public static void setRand(Random rand) {
        AppExemplo2.rand = rand;
    }

    public static int getMaxBandwidth() {
        return maxBandwidth;
    }

    public static void setMaxBandwidth(int maxBandwidth) {
        AppExemplo2.maxBandwidth = maxBandwidth;
    }

    public static boolean isMigrationAble() {
        return migrationAble;
    }

    public static void setMigrationAble(boolean migrationAble) {
        AppExemplo2.migrationAble = migrationAble;
    }

}
