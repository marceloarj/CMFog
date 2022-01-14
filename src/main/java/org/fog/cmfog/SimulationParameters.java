/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog.cmfog;

import java.util.Calendar;
import java.util.Random;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.cmfog.enumerate.Technology;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.Policies;

/**
 *
 * @author Marcelo C. Araújo
 */
public class SimulationParameters {

    static public final double EEG_TRANSMISSION_TIME = 10;

    private static Integer mobileTechnology;
    private static Integer maxVMSize;
    private static Integer minVMSize;
    private static int stepPolicy; // Quantity of steps in the nextStep Function
    private static int numberOfUsers;
    private static Calendar calendar = Calendar.getInstance();
    private static boolean traceFlag = false;
    private static int migrationPointPolicy;
    private static int migrationStrategyPolicy;
    private static int apPositionPolicy;
    private static int cloudletPositionPolicy;
    private static int vmReplicaPolicy;
    private static int travelPredicTimeForST; //in seconds
    private static int mobilityPredictionError;//in meters
    private static int latencyBetweenCloudlets;
    private static int maxBandwidth;
    private static int maxMobileDevices;
    private static int seed;
    private static boolean migrationEnabled;
    private static Random rand;
    private static int layer;
    private static int monitoringWindow;
    private static boolean enabledVerticalMigration;

    public static int getMonitoringWindow() {
        return monitoringWindow;
    }

    public static void setMonitoringWindow(int monitoringWindow) {
        SimulationParameters.monitoringWindow = monitoringWindow;
    }

    public static void defineSimulationParameters(String[] parameters) {
        Log.disable();
        setNumberOfUsers(1);
        setCalendar(Calendar.getInstance());
        setTraceFlag(false);
        CloudSim.init(getNumberOfUsers(), getCalendar(), isTraceFlag());

        setApPositionPolicy(Policies.FIXED_AP_LOCATION);
        setCloudletPositionPolicy(Policies.FIXED_SC_LOCATION);
        setStepPolicy(1);

        if (Integer.parseInt(parameters[0]) == 0) {
            setMigrationEnabled(false);
        } else {
            setMigrationEnabled(true);
        }

        seed = Integer.parseInt(parameters[1]);
        rand = (new Random(seed * Integer.MAX_VALUE));
        if (seed < 1) {
            System.out.println("Seed cannot be less than 1");
            System.exit(0);
        }

        setMigrationPointPolicy(Integer.parseInt(parameters[2]));
        setMigrationStrategyPolicy(Integer.parseInt(parameters[3]));
        setMaxMobileDevices(Integer.parseInt(parameters[4]));
        setMaxBandwidth(Integer.parseInt(parameters[5]));
        setVmReplicaPolicy(Integer.parseInt(parameters[6]));
        setTravelPredicTimeForST(Integer.parseInt(parameters[7]));
        setMobilityPredictionError(Integer.parseInt(parameters[8]));
        setLatencyBetweenCloudlets(Integer.parseInt(parameters[9]));
        setMobileTechnology(Integer.parseInt(parameters[10]));
        setMaxVMSize(Integer.parseInt(parameters[11]));
        setMinVMSize(Integer.parseInt(parameters[12]));
        setLayer(Integer.parseInt(parameters[13]));
        setMonitoringWindow(Integer.parseInt(parameters[14]));
        if (Integer.parseInt(parameters[15]) == 0) {
            setEnabledVerticalMigration(false);
        } else {
            setEnabledVerticalMigration(true);
        }
    }

    public static boolean isEnabledVerticalMigration() {
        return enabledVerticalMigration;
    }

    public static void setEnabledVerticalMigration(boolean enabledVerticalMigration) {
        SimulationParameters.enabledVerticalMigration = enabledVerticalMigration;
    }

    public static double generateVMSize() {
        return minVMSize + (int) (new Random().nextFloat() * (maxVMSize - minVMSize));
    }

    public static double generateFogBW() {
        return MaxAndMin.BW_FOG_MIN + (int) (new Random().nextFloat() * (MaxAndMin.BW_FOG_MAX - MaxAndMin.BW_FOG_MIN));
    }

    public static double generateFogLT() {
        return MaxAndMin.LATENCY_FOG_MIN + (int) (new Random().nextFloat() * (MaxAndMin.LATENCY_FOG_MAX - MaxAndMin.LATENCY_FOG_MIN));
    }

    public static double generateMobileDeviceBW() {
        switch (mobileTechnology) {
            case 1: //4G
                return MaxAndMin.T4G_BW_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T4G_BW_MD_MAX - MaxAndMin.T4G_BW_MD_MIN));
            case 2: //5G
                return MaxAndMin.T5G_BW_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T5G_BW_MD_MAX - MaxAndMin.T5G_BW_MD_MIN));
            default: //WiFi
                return MaxAndMin.WF_BW_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.WF_BW_MD_MAX - MaxAndMin.WF_BW_MD_MIN));
        }
    }

    public static double generateMobileDeviceBW(Technology aTechnology) {
        switch (aTechnology) {
            case TEC_4G: //4G
                return MaxAndMin.T4G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T4G_LATENCY_MD_MAX - MaxAndMin.T4G_LATENCY_MD_MIN));
            case TEC_5G: //5G
                return MaxAndMin.T5G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T5G_LATENCY_MD_MAX - MaxAndMin.T5G_LATENCY_MD_MIN));
            case TEC_WIFI: //WiFi
                return MaxAndMin.WF_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.WF_LATENCY_MD_MAX - MaxAndMin.WF_LATENCY_MD_MIN));
            default:
                return -1;
        }
    }

    public static double generateMobileDeviceLT() {
        switch (mobileTechnology) {
            case 1: //4G
                return MaxAndMin.T4G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T4G_LATENCY_MD_MAX - MaxAndMin.T4G_LATENCY_MD_MIN));
            case 2://5G
                return MaxAndMin.T5G_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.T5G_LATENCY_MD_MAX - MaxAndMin.T5G_LATENCY_MD_MIN));
            default: //WiFi
                return MaxAndMin.WF_LATENCY_MD_MIN + (int) (new Random().nextFloat() * (MaxAndMin.WF_LATENCY_MD_MAX - MaxAndMin.WF_LATENCY_MD_MIN));
        }
    }

    public static double generateAccessPointLT() {
        return MaxAndMin.LATENCY_AP_MIN + (int) (new Random().nextFloat() * (MaxAndMin.LATENCY_AP_MAX - MaxAndMin.LATENCY_AP_MIN));
    }

    public static double generateAccessPointBW() {
        return MaxAndMin.BW_AP_MIN + (int) (new Random().nextFloat() * (MaxAndMin.BW_AP_MAX - MaxAndMin.BW_AP_MIN));
    }

    public static int getSeed() {
        return seed;
    }

    public static void setSeed(int seed) {
        SimulationParameters.seed = seed;
    }

    public static boolean isMigrationEnabled() {
        return migrationEnabled;
    }

    public static void setMigrationEnabled(boolean migrationEnabled) {
        SimulationParameters.migrationEnabled = migrationEnabled;
    }

    public static Random getRand() {
        return rand;
    }

    public static void setRand(Random rand) {
        SimulationParameters.rand = rand;
    }

    public static Integer getMobileTechnology() {
        return mobileTechnology;
    }

    public static void setMobileTechnology(Integer mobileTechnology) {
        SimulationParameters.mobileTechnology = mobileTechnology;
    }

    public static Integer getMaxVMSize() {
        return maxVMSize;
    }

    public static void setMaxVMSize(Integer maxVMSize) {
        SimulationParameters.maxVMSize = maxVMSize;
    }

    public static Integer getMinVMSize() {
        return minVMSize;
    }

    public static void setMinVMSize(Integer minVMSize) {
        SimulationParameters.minVMSize = minVMSize;
    }

    public static int getNumberOfUsers() {
        return numberOfUsers;
    }

    public static void setNumberOfUsers(int numberOfUsers) {
        SimulationParameters.numberOfUsers = numberOfUsers;
    }

    public static Calendar getCalendar() {
        return calendar;
    }

    public static void setCalendar(Calendar calendar) {
        SimulationParameters.calendar = calendar;
    }

    public static boolean isTraceFlag() {
        return traceFlag;
    }

    public static void setTraceFlag(boolean traceFlag) {
        SimulationParameters.traceFlag = traceFlag;
    }

    public static int getStepPolicy() {
        return stepPolicy;
    }

    public static void setStepPolicy(int stepPolicy) {
        SimulationParameters.stepPolicy = stepPolicy;
    }

    public static int getMigrationPointPolicy() {
        return migrationPointPolicy;
    }

    public static void setMigrationPointPolicy(int migrationPointPolicy) {
        SimulationParameters.migrationPointPolicy = migrationPointPolicy;
    }

    public static int getMigrationStrategyPolicy() {
        return migrationStrategyPolicy;
    }

    public static void setMigrationStrategyPolicy(int migrationStrategyPolicy) {
        SimulationParameters.migrationStrategyPolicy = migrationStrategyPolicy;
    }

    public static int getApPositionPolicy() {
        return apPositionPolicy;
    }

    public static void setApPositionPolicy(int apPositionPolicy) {
        SimulationParameters.apPositionPolicy = apPositionPolicy;
    }

    public static int getCloudletPositionPolicy() {
        return cloudletPositionPolicy;
    }

    public static void setCloudletPositionPolicy(int cloudletPositionPolicy) {
        SimulationParameters.cloudletPositionPolicy = cloudletPositionPolicy;
    }

    public static int getVmReplicaPolicy() {
        return vmReplicaPolicy;
    }

    public static void setVmReplicaPolicy(int vmReplicaPolicy) {
        SimulationParameters.vmReplicaPolicy = vmReplicaPolicy;
    }

    public static int getTravelPredicTimeForST() {
        return travelPredicTimeForST;
    }

    public static void setTravelPredicTimeForST(int travelPredicTimeForST) {
        SimulationParameters.travelPredicTimeForST = travelPredicTimeForST;
    }

    public static int getMobilityPredictionError() {
        return mobilityPredictionError;
    }

    public static void setMobilityPredictionError(int mobilityPredictionError) {
        SimulationParameters.mobilityPredictionError = mobilityPredictionError;
    }

    public static int getLatencyBetweenCloudlets() {
        return latencyBetweenCloudlets;
    }

    public static void setLatencyBetweenCloudlets(int latencyBetweenCloudlets) {
        SimulationParameters.latencyBetweenCloudlets = latencyBetweenCloudlets;
    }

    public static int getMaxBandwidth() {
        return maxBandwidth;
    }

    public static void setMaxBandwidth(int maxBandwidth) {
        SimulationParameters.maxBandwidth = maxBandwidth;
    }

    public static int getMaxMobileDevices() {
        return maxMobileDevices;
    }

    public static void setMaxMobileDevices(int maxMobileDevices) {
        SimulationParameters.maxMobileDevices = maxMobileDevices;
    }

    public static int getLayer() {
        return layer;
    }

    public static void setLayer(int layer) {
        SimulationParameters.layer = layer;
    }

}
