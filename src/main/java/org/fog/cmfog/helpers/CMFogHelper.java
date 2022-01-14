package org.fog.cmfog.helpers;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.SimulationParameters;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.entities.MobileSensor;
import org.fog.entities.Tuple;
import org.fog.localization.Coordinate;
import org.fog.localization.Distances;
import org.fog.utils.TimeKeeper;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmobile.constants.MaxAndMin;

/**
 *
 * @author Marcelo C. Araújo
 */
public class CMFogHelper {

    public static boolean isConnectedToNetwork(MobileDevice aMobileDevice) {
        if (aMobileDevice.getSourceAp() == null) {
            return false;
        } else {
            return true;
        }
    }

    static boolean isAbleToMigrate(MobileDevice aMobileDevice) {
        if ((!aMobileDevice.isLockedToMigration()) && (CloudSim.clock() >= aMobileDevice.getNextMigration())) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isAbleToHandoff(MobileDevice aMobileDevice) {
        if (aMobileDevice.getSourceAp() == null) {
            return false;
        } else {
            return true;
        }
    }

    static boolean isCoveredByL2(Coordinate L1, Coordinate L2) {
        if (L1.getCoordX() < (L2.getCoordX() + MaxAndMin.CLOUDLET_COVERAGE) && L1.getCoordX() > (L2.getCoordX() - MaxAndMin.CLOUDLET_COVERAGE)) {
            if (L1.getCoordY() < (L2.getCoordY() + MaxAndMin.CLOUDLET_COVERAGE) && L1.getCoordY() > (L2.getCoordY() - MaxAndMin.CLOUDLET_COVERAGE)) {
                return true;
            }
        }
        return false;
    }

    public static Application createApplication(String appId, int userId, int myId, AppModule userVm) {
        Application application = Application.createApplication(appId, userId); // creates

        application.addAppModule(userVm); // adding module Client to the

        application.addAppModule("client" + myId, "appModuleClient" + myId, 10);
        if (SimulationParameters.EEG_TRANSMISSION_TIME >= 10) {
            application.addAppEdge("EEG" + myId, "client" + myId, 2000, 500,"EEG" + myId, Tuple.UP, AppEdge.SENSOR); // adding edge from
        } else {
            application.addAppEdge("EEG" + myId, "client" + myId, 3000, 500,"EEG" + myId, Tuple.UP, AppEdge.SENSOR);
        }

        application.addAppEdge("client" + myId, userVm.getName(), 3500, 500,"_SENSOR" + myId, Tuple.UP, AppEdge.MODULE); // adding edge from

        application.addAppEdge(userVm.getName(), userVm.getName(), 1000, 1000, 1000, "PLAYER_GAME_STATE" + myId, Tuple.UP, AppEdge.MODULE); // adding

        //#NEW ROUTING TUPLES IS SETTED TO UP
        application.addAppEdge(userVm.getName(), "client" + myId, 14, 500, "CONCENTRATION" + myId, Tuple.UP, AppEdge.MODULE); // adding

        application.addAppEdge(userVm.getName(), "client" + myId, 1000, 28,1000, "GLOBAL_GAME_STATE" + myId, Tuple.UP, AppEdge.MODULE); // adding

        application.addAppEdge("client" + myId, "DISPLAY" + myId, 1000, 500,"SELF_STATE_UPDATE" + myId, Tuple.DOWN, AppEdge.ACTUATOR); // adding

        application.addAppEdge("client" + myId, "DISPLAY" + myId, 1000, 500,
                "GLOBAL_STATE_UPDATE" + myId, Tuple.DOWN, AppEdge.ACTUATOR); // adding

        application.addTupleMapping("client" + myId, "EEG" + myId,
                "_SENSOR" + myId, new FractionalSelectivity(0.9)); // 0.9 tuples

        application.addTupleMapping("client" + myId, "CONCENTRATION" + myId,
                "SELF_STATE_UPDATE" + myId, new FractionalSelectivity(1.0)); // 1.0

        application.addTupleMapping(userVm.getName(), "_SENSOR" + myId,
                "CONCENTRATION" + myId, new FractionalSelectivity(1.0)); // 1.0

        application.addTupleMapping("client" + myId, "GLOBAL_GAME_STATE" + myId,
                "GLOBAL_STATE_UPDATE" + myId, new FractionalSelectivity(1.0)); // 1.0

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

    public static boolean isInsideCoverage(Coordinate L1, Coordinate L2) {
        if (L1.getCoordX() <= (L2.getCoordX() + (MaxAndMin.CLOUDLET_COVERAGE / 2)) && L1.getCoordX() >= (L2.getCoordX() - (MaxAndMin.CLOUDLET_COVERAGE / 2))) {
            if (L1.getCoordY() <= (L2.getCoordY() + (MaxAndMin.CLOUDLET_COVERAGE / 2)) && L1.getCoordY() >= (L2.getCoordY() - (MaxAndMin.CLOUDLET_COVERAGE / 2))) {
                return true;
            }
        }
        return false;
    }

    public static double calculateDistance(Coordinate firstCoordinate, Coordinate secondCoordinate) {
        return Point2D.distance(firstCoordinate.getCoordX(), firstCoordinate.getCoordY(), secondCoordinate.getCoordX(), secondCoordinate.getCoordY());
    }

    public static void preSimulation() {
        TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
        MyStatistics.getInstance().setSeed(SimulationParameters.getSeed());
        for (MobileDevice aMobileDevice : CMFog.mobileDevices) {
//            System.out.println(aMobileDevice.getMyId() + " - " + SimulationParameters.getMigrationStrategyPolicy());
//            if (SimulationParameters.getMigrationPointPolicy() == Policies.FIXED_MIGRATION_POINT || SimulationParameters.getMigrationPointPolicy() == Policies.TIME_MIGRATION_POINT) {
            MyStatistics.getInstance()
                    .setFileMap("./outputLatencies/" + aMobileDevice.getMyId()
                            + "/latencies_FIXED_MIGRATION_POINT_with_LOWEST_LATENCY_seed_"
                            + SimulationParameters.getSeed() + "_st_" + aMobileDevice.getMyId()
                            + ".txt", aMobileDevice.getMyId());
            MyStatistics.getInstance().putLantencyFileName(
                    "FIXED_MIGRATION_POINT_with_LOWEST_LATENCY_seed_"
                    + SimulationParameters.getSeed() + "_st_" + aMobileDevice.getMyId(),
                    aMobileDevice.getMyId());
            MyStatistics.getInstance().setToPrint(
                    "FIXED_MIGRATION_POINT_with_LOWEST_LATENCY");

            System.out.println("ID: " + aMobileDevice.getMyId());
            MyStatistics.getInstance().putLantencyFileName("Time-latency", aMobileDevice.getMyId());
            MyStatistics.getInstance().getMyCount().put(aMobileDevice.getMyId(), 0);
        }

        int myCount = 0;

        for (MobileDevice st : CMFog.mobileDevices) {
            if (st.getSourceAp() != null) {
                System.out.println("Distance between " + st.getName() + " and "
                        + st.getSourceAp().getName() + ": "
                        + Distances.checkDistance(st.getCoord(),
                                st.getSourceAp().getCoord()));
            }
        }
        for (MobileDevice st : CMFog.mobileDevices) {
            System.out.println(
                    st.getName() + "- X: " + st.getCoord().getCoordX() + " Y: "
                    + st.getCoord().getCoordY() + " Direction: "
                    + st.getDirection() + " Speed: " + st.getSpeed()
                    + " VmSize: " + st.getVmMobileDevice().getSize());
        }
        System.out.println("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");
        for (FogDevice sc : CMFog.cloudletsL1) {
            System.out
                    .println(sc.getName() + "(" + sc.getId() + ")" + " - X: " + sc.getCoord().getCoordX()
                            + " Y: " + sc.getCoord().getCoordY()
                            + " UpLinkLatency: " + sc.getUplinkLatency());
        }
        System.out.println("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");
        for (ApDevice ap : CMFog.accessPoints) {
            System.out.println(
                    ap.getName() + "- X: " + ap.getCoord().getCoordX() + " Y: "
                    + ap.getCoord().getCoordY() + " connected to "
                    + ap.getServerCloudlet().getName());
        }
    }

    public static FogDevice getCloudletById(int id, List<FogDevice> fognodes) {
        for (FogDevice sc : fognodes) {
            if (id == sc.getId()) {
                return sc;
            }
        }
        return null;
    }

    public static FogDevice getCloudletByMyId(int myId, List<FogDevice> fognodes) {
        for (FogDevice sc : fognodes) {
            if (myId == sc.getMyId()) {
                return sc;
            }
        }
        return null;
    }

    public static String generateSearchTag(List<FogDevice> cloudlets) {
        for (FogDevice aFogDevice : cloudlets) {
            System.out.print(aFogDevice.getMyId() + "-> ");
        }
        System.out.println("");
        FogDevice lastCloudlet = null;
        FogDevice secondToLastCloudlet = null;
        if (SimulationParameters.getLayer() == 1) {
            lastCloudlet = cloudlets.get(cloudlets.size() - 1);
            secondToLastCloudlet = cloudlets.get(cloudlets.size() - 2);
        } else if (SimulationParameters.getLayer() == 2) {
            lastCloudlet = CMFogHelper.getCloudletById(cloudlets.get(cloudlets.size() - 1).getParentId(), CMFog.cloudletsL2);
            secondToLastCloudlet = CMFogHelper.getCloudletById(cloudlets.get(cloudlets.size() - 2).getParentId(), CMFog.cloudletsL2);
        }
        if (secondToLastCloudlet.getMyId() != lastCloudlet.getMyId()) {
            return String.valueOf(secondToLastCloudlet.getMyId()).concat(String.valueOf(lastCloudlet.getMyId()));
        } else {
            return String.valueOf(secondToLastCloudlet.getMyId());
        }
    }

    public static String generateSearchTag(FogDevice actualCloudlet, FogDevice cloudletToPair) {
        if (cloudletToPair == null) {
            if (SimulationParameters.getLayer() == 2) {
                actualCloudlet = CMFogHelper.getCloudletById(actualCloudlet.getParentId());
            }
            return String.valueOf(actualCloudlet.getMyId());
        }
        if (SimulationParameters.getLayer() == 2) {
            actualCloudlet = CMFogHelper.getCloudletById(actualCloudlet.getParentId());
            cloudletToPair = CMFogHelper.getCloudletById(cloudletToPair.getParentId());
        }
        if (actualCloudlet.getMyId() == cloudletToPair.getMyId()) {
            return String.valueOf(cloudletToPair.getMyId());
        }
        return String.valueOf(cloudletToPair.getMyId()).concat("/").concat(String.valueOf(actualCloudlet.getMyId()));
    }

    public static String generateSearchTag(MobileDevice aMobileDevice) {
        String searchTag = null;
        FogDevice aFogDevice = null;
        if (SimulationParameters.getLayer() == 1) {
            aFogDevice = CMFogHelper.getCloudletById(aMobileDevice.getSourceAp().getServerCloudlet().getId(), CMFog.cloudletsL1);
        } else if (SimulationParameters.getLayer() == 2) {
            aFogDevice = CMFogHelper.getCloudletById(aMobileDevice.getSourceAp().getServerCloudlet().getParentId(), CMFog.cloudletsL2);
        }

        if (aMobileDevice.getLastFN() == null) {
            searchTag = String.valueOf(aFogDevice.getMyId());
        } else {
            FogDevice lastCloudlet = aMobileDevice.getLastFN();
            if (SimulationParameters.getLayer() == 2) {
                lastCloudlet = CMFogHelper.getCloudletById(aMobileDevice.getLastFN().getParentId(), CMFog.cloudletsL2);
            }
            if (lastCloudlet.getMyId() != aFogDevice.getMyId()) {
                searchTag = String.valueOf(lastCloudlet.getMyId()).concat(String.valueOf(aFogDevice.getMyId()));
            } else {
                searchTag = String.valueOf(aFogDevice.getMyId());
            }
        }
        for (FogDevice aFogDevice1 : aMobileDevice.getMobilityTrack()) {
            System.out.print(aFogDevice1.getMyId() + "/" + CMFogHelper.getCloudletById(aFogDevice1.getParentId(), CMFog.cloudletsL2).getMyId() + " -> ");
        }
        System.out.println("");
        return searchTag;
    }

    public static int traceConvergence(List<Integer> originTrace, List<Integer> destinationTrace) {
        for (int anIndex = 0; anIndex < originTrace.size(); anIndex++) {
            for (int aSecondIndex = 0; aSecondIndex < destinationTrace.size(); aSecondIndex++) {
                if (Objects.equals(originTrace.get(anIndex), destinationTrace.get(aSecondIndex))) {
                    return aSecondIndex;
                }
            }
        }
        return -1;
    }

    public static List<Integer> generateCloudletTrace(FogDevice aFogDevice) {
        //System.out.println(aFogDevice.getName());
        List<Integer> traces = new ArrayList<>();
        if (aFogDevice.getParentId() == -1) {
            traces.add(aFogDevice.getId());
            return traces;
        }
        FogDevice aParent = CMFogHelper.getCloudletById(aFogDevice.getParentId());
        traces = generateCloudletTrace(aParent);
        traces.add(0, aFogDevice.getId());
        return traces;
    }

    public static FogDevice getCloudletById(int parentId) {
        FogDevice aFogDevice = getCloudletById(parentId, CMFog.cloudletsL1);
        if (aFogDevice == null) {
            aFogDevice = getCloudletById(parentId, CMFog.cloudletsL2);
        }
        if (aFogDevice == null) {
            aFogDevice = CMFog.cloud;
        }
        return aFogDevice;
    }

    public static FogDevice getCloudletByMyId(int parentId) {
        FogDevice aFogDevice = getCloudletByMyId(parentId, CMFog.cloudletsL1);
        if (aFogDevice == null) {
            aFogDevice = getCloudletByMyId(parentId, CMFog.cloudletsL2);
        }
        if (aFogDevice == null) {
            aFogDevice = CMFog.cloud;
        }
        return aFogDevice;
    }

    public static void emitTuple(MobileDevice aMobileDevice) {
        System.out.println("----> EMITING TUPLE FROM HANDOFF EVENT");
        for (MobileSensor aMobileSensor : aMobileDevice.getSensors()) {
            System.out.println(aMobileSensor.getName());
            aMobileSensor.emiteTuple();
        }
    }

}
