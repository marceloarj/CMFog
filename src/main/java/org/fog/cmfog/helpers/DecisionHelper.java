package org.fog.cmfog.helpers;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.cmfog.CMFog;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Distances;
import org.fog.cmfog.mobilityprediction.Manager;
import org.fog.cmfog.mobilityprediction.MobilityPrediction;
import org.fog.vmmigration.ServiceAgreement;
import org.fog.vmmobile.AppExemplo2;
import org.fog.vmmobile.constants.MaxAndMin;

/**
 *
 * @author Marcelo C. Araújo
 */
public class DecisionHelper {

    public static boolean shouldHandoff(MobileDevice aMobileDevice) {
        boolean shouldHandoff = false;
        switch (aMobileDevice.getAlgorithm()) {
            case LL:
                break;
            case CCMFOG:
                break;
            case TCMFOG:
                shouldHandoff = shouldHandoffTCMFog(aMobileDevice);
                break;
            default:
                break;
        }
        if (shouldHandoff && (aMobileDevice.getVmLocalServerCloudlet().getId() == aMobileDevice.getNextServerClouletId())) {
            shouldHandoff = false;
        }
        return shouldHandoff;
    }

    public static boolean shouldMigrate(MobileDevice aMobileDevice) {
        boolean shouldMigrate = false;
        switch (aMobileDevice.getAlgorithm()) {
            case LL:
                break;
            case CCMFOG:
                break;
            case TCMFOG:
                shouldMigrate = shouldMigrateTCMFog(aMobileDevice);
                break;
            default:
                break;
        }
        if (shouldMigrate && (aMobileDevice.getVmLocalServerCloudlet().getId() == aMobileDevice.getNextServerClouletId())) {
            shouldMigrate = false;
        }
        return shouldMigrate;
    }

    private static boolean shouldMigrateTCMFog(MobileDevice aMobileDevice) {
        int cloudletToMigrate = -1;
        if (aMobileDevice.isVerticalMigration() && (!aMobileDevice.isLockedToMigration())) {
            cloudletToMigrate = ((FogDevice) CloudSim.getEntity(aMobileDevice.getSourceAp().getServerCloudlet().getParentId())).getMyId();
            System.out.println(">>>>>>>>>> Decide for vertical migration from L1->L2");
            return ServiceAgreement.serviceAgreement(CMFogHelper.getCloudletByMyId(cloudletToMigrate, CMFog.cloudletsL2), aMobileDevice);
        } else if (((!aMobileDevice.isLockedToMigration()) && (CloudSim.clock() >= aMobileDevice.getNextMigration()) && !aMobileDevice.isVerticalMigration()) || aMobileDevice.isEarlyMigration()) {
            System.out.println("-----> " + aMobileDevice.getName() + " meets the trigger requirements for migration");
            List<FogDevice> cloudlets = null;
            MobilityPrediction aMobilityPrediction = null;
            FogDevice selectedCloudlet = null;
            if (aMobileDevice.getVmLocalServerCloudlet().getLayer() == 1) {
                System.out.println("Decide for layer 1");
                cloudlets = CMFog.cloudletsL1;
                aMobilityPrediction = CMFog.mobilityPredictionL1;
            } else if (aMobileDevice.getVmLocalServerCloudlet().getLayer() == 2) {
                System.out.println("Decide for layer 2");
                cloudlets = CMFog.cloudletsL2;
                aMobilityPrediction = CMFog.mobilityPredictionL2;
            }
            cloudletToMigrate = Manager.predictMobility(aMobilityPrediction, aMobileDevice, cloudlets);
            selectedCloudlet = CMFogHelper.getCloudletByMyId(cloudletToMigrate, cloudlets);
            if (cloudletToMigrate < 0) {
                return false;
            }
            return ServiceAgreement.serviceAgreement(selectedCloudlet, aMobileDevice);
        } else {
            return false;
        }
    }

    private static boolean shouldHandoffTCMFog(MobileDevice aMobileDevice) { //1332  1349
        if (!aMobileDevice.isLockedToHandoff() && (aMobileDevice.getNextHandoff() - CloudSim.clock() <= 5000) && (aMobileDevice.getNextHandoff() != -1)) {
            System.out.println("-----> Handoff decision for " + aMobileDevice.getName());
            int apToHandoff = chooseHandoffAP2(CMFog.accessPoints, aMobileDevice);
            if (aMobileDevice.getDestinationServerCloudlet() != null) {
                if (aMobileDevice.getDestinationServerCloudlet().getMyId() == aMobileDevice.getSourceAp().getMyId()) {
                    aMobileDevice.setNextHandoff(-1);
                    System.out.println("Aborting handoff [VMs will arrive soon in the actual AP hierarchy]");
                    return false;
                }
            }
            if (apToHandoff >= 0 && apToHandoff != aMobileDevice.getSourceAp().getMyId()) {
                aMobileDevice.setDestinationAp(CMFog.accessPoints.get(apToHandoff));
                return true;
            }
        }
        return false;
    }

    public static int chooseHandoffAP(List<ApDevice> apDevices, MobileDevice aMobileDevice) {//verify what return type is better (int or ApDevice)	
        List<ApDevice> apDevicesCP = new ArrayList<>(apDevices);
        //apDevicesCP.remove(aMobileDevice.getSourceAp());
        ApDevice choose = null;
        double min = Double.MAX_VALUE;
        while (!apDevicesCP.isEmpty()) {
            for (ApDevice ap : apDevicesCP) {
                int destinationToCompare = aMobileDevice.getVmLocalServerCloudlet().getId();
                if (aMobileDevice.getDestinationServerCloudlet() != null) {
                    destinationToCompare = aMobileDevice.getDestinationServerCloudlet().getId();
                }
                double latency = ap.getUplinkLatency() + NetworkTopology.getDelay(ap.getServerCloudlet().getId(), destinationToCompare);
                if (latency < min) {
                    min = latency;
                    choose = ap;
                }
            }
            if (Distances.checkDistance(choose.getCoord(), aMobileDevice.getCoord()) > MaxAndMin.AP_COVERAGE) {
                apDevicesCP.remove(choose);
                // System.out.println("removind ap: " + choose.getName());
                min = Double.MAX_VALUE;
            } else {
                System.out.println("-> Best AP to connected: " + choose.getName());
                return choose.getMyId();
            }
        }
        return -1;
    }

    public static int chooseHandoffAP2(List<ApDevice> apDevices, MobileDevice aMobileDevice) {
        List<ApDevice> apDevicesCP = new ArrayList<>(apDevices);
        List<ApDevice> apsCovering = new ArrayList<>();
        //apDevicesCP.remove(aMobileDevice.getSourceAp());
        for (ApDevice anApDevice : apDevicesCP) {
            if (CMFogHelper.isInsideCoverage(aMobileDevice.getCoord(), anApDevice.getCoord())) {
                apsCovering.add(anApDevice);
            }
        }
        int cloudletWithVM = aMobileDevice.getVmLocalServerCloudlet().getId();
        ApDevice selectedAP = null;
        double minLatency = Double.MAX_VALUE;
        System.out.println("Checking handoff options");
        for (ApDevice anApDevice : apsCovering) {
            System.out.println(anApDevice.getName());
            double latency = NetworkTopology.getDelay(anApDevice.getServerCloudlet().getId(), cloudletWithVM);
            if (latency < minLatency) {
                minLatency = latency;
                selectedAP = anApDevice;
            }
        }
        if (selectedAP != null) {
            return selectedAP.getMyId();
        }
        return -1;
    }
}
