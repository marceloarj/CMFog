package org.fog.cmfog.helpers;

import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.SimulationParameters;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Distances;
import org.fog.vmmigration.LatencyByDistance;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.Policies;

/**
 *
 * @author Marcelo C. Araújo
 */
public class NetworkHelper {

    private static void connectFogLayers() {
        System.out.println("==================== Connecting Fog Layers L1/L2 ====================");
        for (FogDevice cloudletL1 : CMFog.cloudletsL1) {
            for (FogDevice cloudletL2 : CMFog.cloudletsL2) {
                if (CMFogHelper.isCoveredByL2(cloudletL1.getCoord(), cloudletL2.getCoord())) {
                    cloudletL1.setParentId(cloudletL2.getId());
                    cloudletL2.getChildrenCloudlets().add(cloudletL1);
                    double temp = SimulationParameters.getRand().nextDouble() + SimulationParameters.generateFogLT();
                    cloudletL1.getNetServerCloudlets().put(cloudletL2, cloudletL1.getUplinkBandwidth());
                    NetworkTopology.addLink(cloudletL1.getId(), cloudletL2.getId(),
                            cloudletL1.getUplinkBandwidth(), temp);

                    cloudletL2.getNetServerCloudlets().put(cloudletL1, cloudletL1.getDownlinkBandwidth());
                    NetworkTopology.addLink(cloudletL2.getId(), cloudletL1.getId(),
                            cloudletL1.getDownlinkBandwidth(), temp);
                    //System.out.println(cloudletL1.getName() + " to " + cloudletL2.getName() + " with latency = " + temp);
                    break;
                }
            }
        }
        for (FogDevice cloudletL2 : CMFog.cloudletsL2) {
            System.out.print(cloudletL2.getName() + " -> ");
            for (FogDevice childrenCloudlets : cloudletL2.getChildrenCloudlets()) {
                System.out.print(childrenCloudlets.getName() + " ");
            }
            System.out.println("");
        }
    }

    public static double getUplinkBW(MobileDevice aMobileDevice) {
        double bandwidth = 0;
        if (aMobileDevice.getLayer() == 1) {
            bandwidth = aMobileDevice.getSourceAp().getServerCloudlet().getUplinkBandwidth();
        } else if (aMobileDevice.getLayer() == 2) {
            FogDevice aCloudlet = CMFogHelper.getCloudletById(aMobileDevice.getSourceAp().getServerCloudlet().getParentId(), CMFog.cloudletsL2);
            bandwidth = aCloudlet.getUplinkBandwidth();
        }
        return bandwidth;
    }

    private static void connectFogToCloud() {
        System.out.println("==================== Connecting Fog Layer L2 to Cloud ====================");
        for (FogDevice cloudletL2 : CMFog.cloudletsL2) {
            cloudletL2.setParentId(CMFog.cloud.getId());
            CMFog.cloud.getChildrenCloudlets().add(cloudletL2);
            cloudletL2.getNetServerCloudlets().put(CMFog.cloud, cloudletL2.getUplinkBandwidth());
            double latency = SimulationParameters.getRand().nextDouble() + cloudletL2.getUplinkLatency();
            System.out.println(cloudletL2.getName() + " to CLOUD with latency = " + latency);
            NetworkTopology.addLink(cloudletL2.getId(), CMFog.cloud.getId(),
                    cloudletL2.getUplinkBandwidth(), latency);

            CMFog.cloud.getNetServerCloudlets().put(cloudletL2, cloudletL2.getDownlinkBandwidth());
            NetworkTopology.addLink(CMFog.cloud.getId(), cloudletL2.getId(),
                    cloudletL2.getDownlinkBandwidth(), latency);
        }
    }

    private static void connectMDtoAP() {
        for (MobileDevice aMobileDevice : CMFog.mobileDevices) {
            if (!ApDevice.connectApSmartThing(CMFog.accessPoints, aMobileDevice, SimulationParameters.getRand().nextDouble())) {
                LogMobile.debug("NetworkHelper.java", aMobileDevice.getName() + " isn't connected");
            }
        }
    }

    private static void connectAPtoFog() {
        System.out.println("==================== Connecting AP to Cloudlets L1 ====================");
        int cloudletIndex = -1;
        for (ApDevice anAccessPoint : CMFog.accessPoints) { // it makes the connection between AccessPoint and the closestServerCloudlet
            cloudletIndex = Distances.theClosestServerCloudletToAp(CMFog.cloudletsL1, anAccessPoint);
            anAccessPoint.setServerCloudlet(CMFog.cloudletsL1.get(cloudletIndex));
            anAccessPoint.setParentId(CMFog.cloudletsL1.get(cloudletIndex).getId());
            CMFog.cloudletsL1.get(cloudletIndex).setApDevices(anAccessPoint, Policies.ADD);
            double latency = SimulationParameters.getRand().nextDouble();
            NetworkTopology.addLink(CMFog.cloudletsL1.get(cloudletIndex).getId(), anAccessPoint.getId(), anAccessPoint.getUplinkBandwidth(), latency);
            System.out.println(anAccessPoint.getName() + " to " + CMFog.cloudletsL1.get(cloudletIndex).getName() + " with latency = " + latency);
            for (MobileDevice st : anAccessPoint.getSmartThings()) {// it makes the symbolic link between smartThing and ServerCloudlet
                CMFog.cloudletsL1.get(cloudletIndex).connectServerCloudletSmartThing(st);
                CMFog.cloudletsL1.get(cloudletIndex).setSmartThingsWithVm(st, Policies.ADD);
            }
        }
    }

    public static void createInfraestructureNetwork() {
        connectMDtoAP();
        connectAPtoFog();
        connectFogLayers();
        connectFogToCloud();
    }

    public static double calculateMigrationTime(MobileDevice aMobileDevice) {
        double bandwidth = aMobileDevice.getVmLocalServerCloudlet().getUplinkBandwidth();
        double time = ((double) (aMobileDevice.getVmMobileDevice().getSize() / bandwidth) * 1000.0);
        double loopCost = NetworkHelper.multiplicativeMigrationDelay(aMobileDevice.getVmLocalServerCloudlet().getId(), aMobileDevice.getDestinationServerCloudlet().getId()) * 0.5;
        System.out.println("MIG TIME[bw " + bandwidth + " time " + time + " LC " + loopCost + "] = " + (time + (time * loopCost)));
        time += time * loopCost;
        return time;
    }

    public static double calculateInitialMigTime(MobileDevice aMobileDevice) {
        double bandwidth = aMobileDevice.getVmLocalServerCloudlet().getUplinkBandwidth();
        double time = ((double) (aMobileDevice.getVmMobileDevice().getSize() / bandwidth)) * 1000.0;
        return time;
    }

    public static double multiplicativeMigrationDelay(int actualCloudletID, int destinationCloudletID) {
        FogDevice actualCloudlet = CMFogHelper.getCloudletByMyId(actualCloudletID);
        FogDevice destinationCloudlet = CMFogHelper.getCloudletByMyId(destinationCloudletID);
        double multiplicativeRate = 0.0;
        if (actualCloudlet.getParentId() == destinationCloudlet.getId() || destinationCloudlet.getParentId() == actualCloudlet.getId()) {
            multiplicativeRate = actualCloudlet.getMigrationRate();
        } else {
            multiplicativeRate = actualCloudlet.getMigrationRate() + destinationCloudlet.getMigrationRate();
            if (actualCloudlet.getParentId() != destinationCloudlet.getParentId()) {
                actualCloudlet = CMFogHelper.getCloudletById(actualCloudlet.getParentId());
                destinationCloudlet = CMFogHelper.getCloudletById(destinationCloudlet.getParentId());
                if (actualCloudlet.getParentId() != -1) {
                    multiplicativeRate = multiplicativeRate + actualCloudlet.getMigrationRate();
                }
                if (destinationCloudlet.getParentId() != -1) {
                    multiplicativeRate = multiplicativeRate + destinationCloudlet.getMigrationRate();
                }
            }
        }
        return multiplicativeRate;
    }
}
