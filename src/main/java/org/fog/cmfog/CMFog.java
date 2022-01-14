package org.fog.cmfog;

import org.fog.cmfog.helpers.CMFogHelper;
import org.fog.cmfog.helpers.SimulationEntityHelper;
import org.fog.cmfog.helpers.MobilityPredictionHelper;
import org.fog.cmfog.helpers.NetworkHelper;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.ApDevice;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileActuator;
import org.fog.entities.MobileDevice;
import org.fog.entities.MobileSensor;
import org.fog.cmfog.mobilityprediction.MobilityPrediction;
import org.fog.placement.ModuleMapping;
import org.fog.scheduler.TupleScheduler;

/**
 *
 * @author Marcelo Araújo
 */
public class CMFog {

    public static CMFogMobileController mobileController;
    public static List<String> log = new ArrayList<>();
    public static Map<Integer, ArrayList<String>> mobilityLog = new HashMap<>();
    public static Map<String, Integer> algorithmMiss = new HashMap<>();
    public static MobilityPrediction mobilityPredictionL1;
    public static MobilityPrediction mobilityPredictionL2;

    public static FogDevice cloud;
    public static List<FogDevice> cloudletsL1 = new ArrayList<>();
    public static List<FogDevice> cloudletsL2 = new ArrayList<>();
    public static List<ApDevice> accessPoints = new ArrayList<>();
    public static List<MobileDevice> mobileDevices = new ArrayList<MobileDevice>();

    public static List<FogBroker> brokers = new ArrayList<>();
    public static List<String> appIdList = new ArrayList<>();
    public static List<Application> applicationList = new ArrayList<>();
    public static String temLog = "";
    public static List<Integer> msgQueue = new ArrayList<>();

    public static HashMap<String, FogDevice> modulesLocation = new HashMap<>();

    /**
     * @param args
     * @author Marcelo C. Araújo
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("RUNNING CMFog");
        /**
         * STEP 1: Configure/read simulation parameters and start MD logs
         */
        SimulationParameters.defineSimulationParameters(args);

        for (int anIndex = 0; anIndex < SimulationParameters.getMaxMobileDevices(); anIndex++) {
            mobilityLog.put(anIndex, new ArrayList<String>());
        }

        /**
         * STEP 2: Create simulation Devices and Network
         */
        cloud = SimulationEntityHelper.createCloud();
        SimulationEntityHelper.createFixedAPs(accessPoints);
        SimulationEntityHelper.createCloudlets(cloudletsL1, 1);
        SimulationEntityHelper.createCloudlets(cloudletsL2, 2);
        SimulationEntityHelper.createMobileDevices(mobileDevices);

        for (MobileDevice aMobileDevice : mobileDevices) {
            algorithmMiss.put(aMobileDevice.getName(), 0);
        }
        NetworkHelper.createInfraestructureNetwork();

        /**
         * STEP 3: Setting up mobility prediction and reading MD traces
         */
        mobilityPredictionL1 = new MobilityPrediction(cloudletsL1, mobileDevices);
        mobilityPredictionL2 = new MobilityPrediction(cloudletsL2, mobileDevices);
        MobilityPredictionHelper.readMobileDeviceTraces(mobilityPredictionL1);
        MobilityPredictionHelper.readMobileDeviceTraces(mobilityPredictionL2);

        /**
         * STEP 4: Create Brokers and VMs *
         */
        for (MobileDevice aMobileDevice : mobileDevices) {
            brokers.add(new FogBroker("My_broker" + Integer.toString(aMobileDevice.getMyId())));
        }
        System.out.println("-----------------------------------------");
        for (MobileDevice aMobileDevice : mobileDevices) {// It only creates the virtual machine for each smartThing
            if (aMobileDevice.getSourceAp() != null) {
                CloudletScheduler cloudletScheduler = new TupleScheduler(500, 1);
                AppModule vmSmartThingTest = new AppModule(aMobileDevice.getMyId(), // id
                        "AppModuleVm_" + aMobileDevice.getName(), "MyApp_vr_game" + aMobileDevice.getMyId(), brokers.get(aMobileDevice.getMyId()).getId(),
                        2000, 64, 1000, (long) SimulationParameters.generateVMSize(), "Vm_" + aMobileDevice.getName(), cloudletScheduler, new HashMap<Pair<String, String>, SelectivityModel>());
                aMobileDevice.setVmMobileDevice(vmSmartThingTest);
                if (aMobileDevice.getLayer() == 1) {
                    aMobileDevice.getSourceServerCloudlet().getHost().vmCreate(vmSmartThingTest);
                    aMobileDevice.setVmLocalServerCloudlet(aMobileDevice.getSourceServerCloudlet());
                } else if (aMobileDevice.getLayer() == 2) {
                    CMFogHelper.getCloudletById(aMobileDevice.getSourceServerCloudlet().getParentId(), cloudletsL2).getHost().vmCreate(vmSmartThingTest);
                    aMobileDevice.setVmLocalServerCloudlet(CMFogHelper.getCloudletById(aMobileDevice.getSourceServerCloudlet().getParentId(), cloudletsL2));
                }
                modulesLocation.put("AppModuleVm_" + aMobileDevice.getName(), aMobileDevice.getVmLocalServerCloudlet());
                modulesLocation.put("client" + aMobileDevice.getMyId(), aMobileDevice.getSourceServerCloudlet());
                //aMobileDevice.getSourceServerCloudlet().getHost().vmCreate(vmSmartThingTest);
                //aMobileDevice.setVmLocalServerCloudlet(aMobileDevice.getSourceServerCloudlet());

                System.out.println(aMobileDevice.getMyId() + " Position: " + aMobileDevice.getCoord().getCoordX() + ", " + aMobileDevice.getCoord().getCoordY() + " Direction: "
                        + aMobileDevice.getDirection() + " Speed: " + aMobileDevice.getSpeed());
                System.out.println("Source AP: " + aMobileDevice.getSourceAp() + " Dest AP: " + aMobileDevice.getDestinationAp() + " Host: " + aMobileDevice.getHost().getId());
                System.out.println("Local server: " + aMobileDevice.getVmLocalServerCloudlet().getName() + " Apps " + aMobileDevice.getVmLocalServerCloudlet().getActiveApplications()
                        + " Map " + aMobileDevice.getVmLocalServerCloudlet().getApplicationMap());
                if (aMobileDevice.getDestinationServerCloudlet() == null) {
                    System.out.println("Dest server: null Apps: null Map: null");
                } else {
                    System.out.println("Dest server: " + aMobileDevice.getDestinationServerCloudlet().getName() + " Apps: " + aMobileDevice.getDestinationServerCloudlet()
                            .getActiveApplications() + " Map " + aMobileDevice.getDestinationServerCloudlet().getApplicationMap());
                }
            }
        }
        int i = 0;
        for (FogBroker br : brokers) {// Each broker receives one smartThing's VM
            List<Vm> tempVmList = new ArrayList<>();
            tempVmList.add(mobileDevices.get(i++).getVmMobileDevice());
            br.submitVmList(tempVmList);
        }
        /**
         * STEP 5: Create APP
         *
         */
        i = 0;
        for (FogBroker br : brokers) {
            appIdList.add("MyApp_vr_game" + Integer.toString(i));
            Application myApp = CMFogHelper.createApplication(appIdList.get(i), br.getId(), i, (AppModule) mobileDevices.get(i).getVmMobileDevice());
            applicationList.add(myApp);
            i++;
        }
        /**
         * STEP 5.1: Links sensors and brokers for each broker from: CloudSim
         * and iFogSim
         *
         */
        for (MobileDevice aMobileDevice : mobileDevices) {
            int brokerId = brokers.get(aMobileDevice.getMyId()).getId();
            String appId = appIdList.get(aMobileDevice.getMyId());
            if (aMobileDevice.getSourceAp() != null) {
                for (MobileSensor s : aMobileDevice.getSensors()) {
                    s.setAppId(appId);
                    s.setUserId(brokerId);
                    s.setGatewayDeviceId(aMobileDevice.getId());
                    s.setLatency(6.0);
                }
                for (MobileActuator a : aMobileDevice.getActuators()) {
                    a.setUserId(brokerId);
                    a.setAppId(appId);
                    a.setGatewayDeviceId(aMobileDevice.getId());
                    a.setLatency(1.0);
                    a.setActuatorType("DISPLAY" + aMobileDevice.getMyId());
                }
            }
        }

        /**
         * STEP 6: CREATE MAPPING, CONTROLLER, AND SUBMIT APPLICATION
         */
        CMFogMobileController mobileController = null;
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
        for (Application app : applicationList) {
            app.setPlacementStrategy("Mapping");
        }
        i = 0;
        if (SimulationParameters.getLayer() == 1) {
            for (FogDevice afogDevice : cloudletsL1) {
                i = 0;
                for (MobileDevice aMobileDevice : mobileDevices) {
                    if (aMobileDevice.getApDevices() != null) {
                        if (afogDevice.equals(aMobileDevice.getSourceServerCloudlet())) {
                            moduleMapping.addModuleToDevice(((AppModule) aMobileDevice.getVmMobileDevice()).getName(), afogDevice.getName(), 1);// MaxAndMin.MAX_SMART_THING);//
                            moduleMapping.addModuleToDevice("client" + aMobileDevice.getMyId(), aMobileDevice.getName(), 1);
                        }
                    }
                    i++;
                }
            }
        } else {
            for (FogDevice afogDevice : cloudletsL2) {
                i = 0;
                for (MobileDevice aMobileDevice : mobileDevices) {
                    if (aMobileDevice.getApDevices() != null) {
                        if (afogDevice.equals(aMobileDevice.getVmLocalServerCloudlet())) {
                            moduleMapping.addModuleToDevice(((AppModule) aMobileDevice.getVmMobileDevice()).getName(), afogDevice.getName(), 1);// MaxAndMin.MAX_SMART_THING);//
                            moduleMapping.addModuleToDevice("client" + aMobileDevice.getMyId(), aMobileDevice.getName(), 1);
                        }
                    }
                    i++;
                }
            }
        }
        mobileController = new CMFogMobileController("MobileController",
                cloudletsL1, cloudletsL2, accessPoints, mobileDevices,
                brokers, moduleMapping, SimulationParameters.getMigrationPointPolicy(),
                SimulationParameters.getMigrationStrategyPolicy(), SimulationParameters.getStepPolicy(),
                SimulationParameters.isMigrationEnabled());
        i = 0;
        for (Application app : applicationList) {
            mobileController.submitApplication(app, 1);

        }

        /**
         * STEP 7: Generate Markov Chain
         */
        mobilityPredictionL1.generateTransictionMatrix();
        mobilityPredictionL2.generateTransictionMatrix();

        CMFogHelper.preSimulation();
        System.setOut(new PrintStream("out.txt"));
        System.out.println("Starting: " + Calendar.getInstance().getTime());
        CloudSim.startSimulation();
        System.out.println("Simulation over");

        CloudSim.stopSimulation();

    }
}
