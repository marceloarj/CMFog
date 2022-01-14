/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog.cmfog.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.SimulationParameters;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.MobileActuator;
import org.fog.entities.MobileDevice;
import org.fog.entities.MobileSensor;
import org.fog.mfmf.simulator.CMFogDecision;
import org.fog.cmfog.enumerate.Algorithm;
import org.fog.cmfog.enumerate.Technology;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.vmmigration.BeforeMigration;
import org.fog.vmmigration.CompleteVM;
import org.fog.vmmigration.ContainerVM;
import org.fog.vmmigration.DecisionMigration;
import org.fog.vmmigration.LiveMigration;
import org.fog.vmmigration.LowestLatency;
import org.fog.vmmigration.PrepareCompleteVM;
import org.fog.vmmigration.PrepareContainerVM;
import org.fog.vmmigration.PrepareLiveMigration;
import org.fog.vmmigration.Service;
import org.fog.vmmigration.VmMigrationTechnique;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.Policies;
import org.fog.vmmobile.constants.Services;

/**
 *
 * @author marce
 */
public class SimulationEntityHelper {

    static int cloudletSize = 0;

    public static FogDevice createCloud() {
        DecisionMigration migrationStrategy = null;
        switch (SimulationParameters.getMigrationStrategyPolicy()) { //CHANGE_HERE
            case Policies.LOWEST_LATENCY:
                migrationStrategy = new LowestLatency(CMFog.cloudletsL1,
                        CMFog.accessPoints, SimulationParameters.getMigrationPointPolicy(), SimulationParameters.getVmReplicaPolicy());
                break;
            case Policies.CMFOG:
                migrationStrategy = new CMFogDecision(CMFog.cloudletsL1,
                        CMFog.accessPoints, SimulationParameters.getMigrationPointPolicy(), SimulationParameters.getVmReplicaPolicy());
                break;
            default:
                break;
        }

        BeforeMigration beforeMigration = null;
        switch (SimulationParameters.getVmReplicaPolicy()) {
            case Policies.MIGRATION_COMPLETE_VM:
                beforeMigration = new PrepareCompleteVM();
                break;
            case Policies.MIGRATION_CONTAINER_VM:
                beforeMigration = new PrepareContainerVM();
                break;
            case Policies.LIVE_MIGRATION:
                beforeMigration = new PrepareLiveMigration();
                break;
            default:
                break;
        }

        FogLinearPowerModel powerModel = new FogLinearPowerModel(
                107.339d, 83.433d);// 10//maxPower

        List<Pe> peList = new ArrayList<>();
        int mips = 2800000 * 10;
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
                SimulationParameters.getRand().nextInt(10000) % MaxAndMin.MAX_SERVICES);
        if (serviceOffer.getType() == Services.HIBRID
                || serviceOffer.getType() == Services.PUBLIC) {
            serviceOffer.setValue(SimulationParameters.getRand().nextFloat() * 10);
        } else {
            serviceOffer.setValue(0);
        }
        try {

            double maxBandwidth = SimulationParameters.generateFogBW() * 10;// MaxAndMin.MAX_BANDWIDTH;
            double minBandwidth = maxBandwidth / 2;// MaxAndMin.MIN_BANDWIDTH;
            //double minBandwidth = (getMaxBandwidth() * 1024 * 1024);// MaxAndMin.MIN_BANDWIDTH;
            FogDevice cloud = new FogDevice("CLOUD" // name
                    ,
                     characteristics, vmAllocationPolicy// vmAllocationPolicy
                    ,
                     storageList, 10// schedulingInterval
                    ,
                     minBandwidth// uplinkBandwidth
                    ,
                     maxBandwidth// downlinkBandwidth
                    ,
                     SimulationParameters.generateFogLT()// rand.nextDouble()//uplinkLatency
                    ,
                     0.01// mipsPer..
                    ,
                     1250, 1250, 100000, serviceOffer,
                    migrationStrategy, SimulationParameters.getVmReplicaPolicy(),
                    beforeMigration);

            cloud.setParentId(-1);
            return cloud;
        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    public static int createCloudlets(List<FogDevice> cloudlets, int layer) {
        System.out.println("Creating cloudlets layer " + layer);
        double coverageArea = 0;
        double overlap = 0;
        if (layer == 1) {
            coverageArea = MaxAndMin.CLOUDLET_COVERAGE;
            overlap = MaxAndMin.MIG_POINT;
        } else if (layer == 2) {
            coverageArea = MaxAndMin.CLOUDLET_COVERAGE * 2;
            overlap = MaxAndMin.MIG_POINT * 2;
        }
        overlap = 0;
        double radius = coverageArea / 2;
        double coordX, coordY;

        for (coordX = radius; coordX < MaxAndMin.MAX_X; coordX += coverageArea - overlap) {
            for (coordY = radius; coordY < MaxAndMin.MAX_Y; coordY += coverageArea - overlap, cloudletSize++) {
                int xS = (int) (coordX - radius);
                int xE = (int) (coordX + radius);
                int yS = (int) (coordY - radius);
                int yE = (int) (coordY + radius);
                System.out.println("(" + xS + "," + xE + ") (" + yS + "," + yE + ")");
                DecisionMigration migrationStrategy = null;
                switch (SimulationParameters.getMigrationStrategyPolicy()) { //CHANGE_HERE
                    case Policies.LOWEST_LATENCY: //CMFOG HERE
                        migrationStrategy = new LowestLatency(cloudlets,
                                CMFog.accessPoints, SimulationParameters.getMigrationPointPolicy(), SimulationParameters.getVmReplicaPolicy());
                        break;
                    case Policies.CMFOG:
                        migrationStrategy = new CMFogDecision(cloudlets,
                                CMFog.accessPoints, SimulationParameters.getMigrationPointPolicy(), SimulationParameters.getVmReplicaPolicy());
                        break;
                    default:
                        break;
                }

                BeforeMigration beforeMigration = null;
                switch (SimulationParameters.getVmReplicaPolicy()) {
                    case Policies.MIGRATION_COMPLETE_VM:
                        beforeMigration = new PrepareCompleteVM();
                        break;
                    case Policies.MIGRATION_CONTAINER_VM:
                        beforeMigration = new PrepareContainerVM();
                        break;
                    case Policies.LIVE_MIGRATION:
                        beforeMigration = new PrepareLiveMigration();
                        break;
                    default:
                        break;
                }

                FogLinearPowerModel powerModel = new FogLinearPowerModel(
                        107.339d, 83.433d);// 10//maxPower

                List<Pe> peList = new ArrayList<>();
                int mips = 2800000;
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
                        SimulationParameters.getRand().nextInt(10000) % MaxAndMin.MAX_SERVICES);
                if (serviceOffer.getType() == Services.HIBRID
                        || serviceOffer.getType() == Services.PUBLIC) {
                    serviceOffer.setValue(SimulationParameters.getRand().nextFloat() * 10);
                } else {
                    serviceOffer.setValue(0);
                }
                try {

                    double maxBandwidth = SimulationParameters.generateFogBW();// MaxAndMin.MAX_BANDWIDTH;
                    if (layer == 2) {
                        maxBandwidth = maxBandwidth * 2;
                    }
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
                             SimulationParameters.generateFogLT() * ((layer * 2) - 1)// rand.nextDouble()//uplinkLatency
                            ,
                             0.01// mipsPer..
                            ,
                             (int) coordX, (int) coordY, cloudletSize, serviceOffer,
                            migrationStrategy, SimulationParameters.getVmReplicaPolicy(),
                            beforeMigration, layer);
                    cloudlets.add(cloudlets.size(), sc);
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

    public static void createFixedAPs(List<ApDevice> apDevices) {
        int i = 0;
        double radius = MaxAndMin.AP_COVERAGE / 2;
        double overlap = MaxAndMin.MAX_DISTANCE_TO_HANDOFF;
        double coordX, coordY;
        boolean control = true;
        System.out.println("Creating Ap devices - New Class");
        for (coordX = radius; coordX < MaxAndMin.MAX_X; coordX += MaxAndMin.AP_COVERAGE - overlap) {
            for (coordY = radius; coordY < MaxAndMin.MAX_Y; coordY += MaxAndMin.AP_COVERAGE - overlap, i++) {
                int xS = (int) (coordX - radius);
                int xE = (int) (coordX + radius);
                int yS = (int) (coordY - radius);
                int yE = (int) (coordY + radius);
                System.out.println("(" + xS + "," + xE + ") (" + yS + "," + yE + ")");
                double bw = SimulationParameters.generateAccessPointBW();
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

    public static void createMobileDevices(List<MobileDevice> mobileDevices) {
        MobileDevice aMobileDevice = null;
        for (int anIndex = 0; anIndex < SimulationParameters.getMaxMobileDevices(); anIndex++) {
            System.out.println("creating MD " + anIndex);
            aMobileDevice = createMobileDevice(anIndex, Algorithm.TCMFOG, Technology.TEC_5G);
            mobileDevices.add(aMobileDevice);
        }
    }

    private static MobileDevice createMobileDevice(int anIndex, Algorithm algorithm, Technology technology) {
        // Random rand = new Random((Integer.MAX_VALUE*getSeed())/(i+1));
        int coordX = 0, coordY = 0;
        int direction, speed;
        direction = SimulationParameters.getRand().nextInt(MaxAndMin.MAX_DIRECTION - 1) + 1;
        speed = SimulationParameters.getRand().nextInt(MaxAndMin.MAX_SPEED - 1) + 1;
        /**
         * ************* Start set of Mobile Sensors ***************
         */
        VmMigrationTechnique migrationTechnique = null;

        if (SimulationParameters.getVmReplicaPolicy() == Policies.MIGRATION_COMPLETE_VM) {
            migrationTechnique = new CompleteVM(SimulationParameters.getMigrationPointPolicy());
        } else if (SimulationParameters.getVmReplicaPolicy() == Policies.MIGRATION_CONTAINER_VM) {
            migrationTechnique = new ContainerVM(SimulationParameters.getMigrationPointPolicy());
        } else if (SimulationParameters.getVmReplicaPolicy() == Policies.LIVE_MIGRATION) {
            migrationTechnique = new LiveMigration(SimulationParameters.getMigrationPointPolicy());

        }

        DeterministicDistribution distribution0 = new DeterministicDistribution(
                SimulationParameters.EEG_TRANSMISSION_TIME);// +(i*getRand().nextDouble()));

        Set<MobileSensor> sensors = new HashSet<>();

        MobileSensor sensor = new MobileSensor("Sensor" + anIndex // Tuple's name ->
                // ACHO QUE DÁ PARA
                // USAR ESTE
                // CONSTRUTOR
                ,
                 "EEG" + anIndex // Tuple's type
                ,
                 anIndex // User Id
                ,
                 "MyApp_vr_game" + anIndex // app's name
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
        MobileActuator actuator0 = new MobileActuator("Actuator" + anIndex, anIndex,
                "MyApp_vr_game" + anIndex, "DISPLAY" + anIndex);

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
        float maxServiceValue = SimulationParameters.getRand().nextFloat() * 100;
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
            double download = SimulationParameters.generateMobileDeviceBW();
            st = new MobileDevice("SmartThing" + Integer.toString(anIndex),
                    characteristics, vmAllocationPolicy// - seria a maquina
                    // que executa
                    // dentro do
                    // fogDevice?
                    ,
                     storageList, 2// schedulingInterval
                    ,
                     (download / 2)// uplinkBandwidth - 1 Mbit
                    ,
                     download// downlinkBandwidth - 2 Mbits
                    ,
                     SimulationParameters.generateMobileDeviceLT()// uplinkLatency
                    ,
                     0.01// mipsPer..
                    ,
                     coordX, coordY, anIndex// id
                    ,
                     direction, speed, maxServiceValue, vmSize,
                    migrationTechnique);
            st.setTempSimulation(0);
            st.setTimeFinishDeliveryVm(-1);
            st.setTimeFinishHandoff(0);
            st.setSensors(sensors);
            st.setActuators(actuators);
            st.setTravelPredicTime(SimulationParameters.getTravelPredicTimeForST());
            st.setMobilityPredictionError(SimulationParameters.getMobilityPredictionError());
            st.setAlgorithm(algorithm);
            st.setTechnology(technology);
            st.setLayer(SimulationParameters.getLayer());
            return st;
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
        return null;
    }

    public static boolean isPartOfMD(int srcID, int destID) {
        boolean partOfMD = false;
        SimEntity aSrcEntity = CloudSim.getEntity(srcID);
        SimEntity aDestEntity = CloudSim.getEntity(destID);
        if (aSrcEntity.getName().startsWith("Sensor") || aSrcEntity.getName().startsWith("Actuator") || aDestEntity.getName().startsWith("Sensor") || aDestEntity.getName().startsWith("Actuator")) {
            partOfMD = true;
        }
        return partOfMD;
    }

}
