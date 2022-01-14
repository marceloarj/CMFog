/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog.cmfog;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.fog.entities.MobileDevice;
import org.fog.utils.TimeKeeper;
import org.fog.vmmigration.MyStatistics;

/**
 *
 * @author Marcelo Araújo
 */
public class MonitoringAgent {

    private static Double ALPHA = 0.004;

    private static List<Double> latencyTS = new ArrayList<>();
    private static List<Double> lostTuplesTS = new ArrayList<>();
    private static List<Double> latencyEWA = new ArrayList<>();
    private static List<Double> lostTuplesEWA = new ArrayList<>();
    private static List<Double> latencyW = new ArrayList<>();
    private static List<Double> lostTuplesW = new ArrayList<>();
    private static List<Double> latencyAW = new ArrayList<>();
    private static List<Double> lostTuplesAW = new ArrayList<>();
    public static List<Integer> pairingCheck = new ArrayList<>();
    private static Long lostTuples;
    private static DecimalFormat decimalFormat;
    private static Integer latencyThreshold;
    private static Integer losTuplesThreshold;

    public static void configureAgent() {
        latencyTS = new ArrayList<>();
        lostTuplesTS = new ArrayList<>();
        lostTuples = 0L;
        decimalFormat = new DecimalFormat("#.####");
        latencyThreshold = 20;
        losTuplesThreshold = 20;
    }

    public static void checkVariation(MobileDevice aMobileDevice) {
        int aMonitoringWindow = SimulationParameters.getMonitoringWindow();
        if (latencyTS.size() >= aMonitoringWindow && lostTuplesTS.size() >= aMonitoringWindow) {
            List<Double> auxLatency = latencyTS.subList(latencyTS.size() - aMonitoringWindow, latencyTS.size());
            List<Double> auxLostTuples = lostTuplesTS.subList(lostTuplesTS.size() - aMonitoringWindow, lostTuplesTS.size());
            Double latencyVariation = calculatePositiveVariation(auxLatency);
            Double lostTuplesVariation = calculateNegativeVariation(auxLostTuples);
            latencyW.add(Double.valueOf(decimalFormat.format(latencyVariation)));
            lostTuplesW.add(Double.valueOf(decimalFormat.format(lostTuplesVariation)));

            latencyAW.add(Double.valueOf(decimalFormat.format((auxLatency.stream().mapToDouble(Double::doubleValue).sum() / 300))));
            lostTuplesAW.add(Double.valueOf(decimalFormat.format((auxLostTuples.stream().mapToDouble(Double::doubleValue).sum() / 300))));

            System.out.println("LV: " + latencyVariation + " - LTV: " + lostTuplesVariation + " - VertMig: " + SimulationParameters.isEnabledVerticalMigration());
            if ((latencyVariation > latencyThreshold) && (aMobileDevice.getLayer() == 1) && SimulationParameters.isEnabledVerticalMigration()) {
                aMobileDevice.setVerticalMigration(true);
                if (aMobileDevice.isLockedToMigration()) {
                    System.out.println(">>> Latency Threshold (LOCKED) <<<");
                } else {
                    System.out.println(">>> Latency Threshold <<<");
                }

            }
            if (lostTuplesVariation > losTuplesThreshold) {
                System.out.println(">>> Lost Tuples Threshold <<<");
            }
        } else {
            latencyW.add(Double.valueOf(decimalFormat.format(0D)));
            lostTuplesW.add(Double.valueOf(decimalFormat.format(0D)));

            latencyAW.add(Double.valueOf(decimalFormat.format(0D)));
            lostTuplesAW.add(Double.valueOf(decimalFormat.format(0D)));
        }
    }

    public static Double calculatePositiveVariation(List<Double> auxLatency) {
        Double aVariation = 0D;
        Double toCompare = auxLatency.get(auxLatency.size() - 1);
        for (int anIndex = 0; anIndex < auxLatency.size() - 1; anIndex++) {
            Double aDiff = (toCompare - auxLatency.get(anIndex));
            aDiff = (aDiff / toCompare) * 100;
            if (aDiff > aVariation) {
                aVariation = aDiff;
            }
        }
        return aVariation;
    }

    public static Double calculateNegativeVariation(List<Double> auxLatency) {
        Double aVariation = 0D;
        Double toCompare = auxLatency.get(auxLatency.size() - 1);
        for (int anIndex = 0; anIndex < auxLatency.size() - 1; anIndex++) {
            Double aDiff = (toCompare - auxLatency.get(anIndex));
            aDiff = (aDiff / toCompare) * 100;
            if (aDiff < aVariation) {
                aVariation = aDiff;
            }
        }
        return aVariation;
    }

    public static void addlostTuple() {
        lostTuples = lostTuples + 1;
    }

    private static void updateLatencyTS() {
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
            Double value = Double.valueOf(decimalFormat.format(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)));
            latencyTS.add(value);
            if (latencyTS.size() == 1) {
                latencyEWA.add(latencyTS.get(0));
                return;
            }
            Double newValue = latencyTS.get(latencyTS.size() - 1);
            Double lastEWA = latencyEWA.get(latencyEWA.size() - 1);
            latencyEWA.add(calculateEWA(lastEWA, newValue));
        }
    }

    private static void updateLostTuplesTS() {
        Double averageLostTuples;
        if (lostTuples == 0 || MyStatistics.getInstance().getMyCountTotalTuple() == 0) {
            averageLostTuples = 0.0;
        } else {
            averageLostTuples = Double.valueOf(((double) lostTuples / MyStatistics.getInstance().getMyCountTotalTuple()));
            averageLostTuples = Double.valueOf(decimalFormat.format(averageLostTuples * 100));
        }
        lostTuplesTS.add(averageLostTuples);
        if (lostTuplesTS.size() == 1) {
            lostTuplesEWA.add(lostTuplesTS.get(0));
            return;
        }
        Double newValue = lostTuplesTS.get(lostTuplesTS.size() - 1);
        Double lastEWA = lostTuplesEWA.get(lostTuplesEWA.size() - 1);
        lostTuplesEWA.add(calculateEWA(lastEWA, newValue));
    }

    private static void updateLatencyEWA() {
        if (latencyTS.size() == 1) {
            latencyEWA.add(latencyTS.get(0));
            return;
        }
        Double newValue = latencyTS.get(latencyTS.size() - 1);
        Double lastEWA = latencyEWA.get(latencyEWA.size() - 1);
        latencyEWA.add(calculateEWA(lastEWA, newValue));
    }

    private static void updateLostTuplesEWA() {
        if (lostTuplesTS.size() == 1) {
            lostTuplesEWA.add(lostTuplesTS.get(0));
            return;
        }
        Double newValue = lostTuplesTS.get(lostTuplesTS.size() - 1);
        Double lastEWA = lostTuplesEWA.get(lostTuplesEWA.size() - 1);
        lostTuplesEWA.add(calculateEWA(lastEWA, newValue));
    }

    private static Double calculateEWA(Double lastEWA, Double value) {
        Double aResult = value * ALPHA;
        aResult = aResult + ((1 - ALPHA) * lastEWA);
        return aResult;
    }

    public static void calculateTSPoint(MobileDevice aMobileDevice) {
        updateLatencyTS();
        updateLostTuplesTS();
        //
        //updateLatencyEWA();
        //updateLostTuplesEWA();

    }

    public static void printReport() {
        for (int anIndex = 0; anIndex < latencyTS.size(); anIndex++) {
            printResults(latencyTS.get(anIndex) + "\t" + lostTuplesTS.get(anIndex) + "\t" + latencyEWA.get(anIndex) + "\t" + lostTuplesEWA.get(anIndex)
                    + "\t" + latencyW.get(anIndex + 1) + "\t" + lostTuplesW.get(anIndex + 1)+ "\t" + pairingCheck.get(anIndex), "TimeSeries.txt");
        }
//        for (int anIndex = 0; anIndex < latencyW.size(); anIndex++) {
//            printResults(latencyW.get(anIndex) + "\t" + lostTuplesW.get(anIndex), "MetricsByW.txt");
//        }
//
//        for (int anIndex = 0; anIndex < latencyAW.size(); anIndex++) {
//            printResults(latencyAW.get(anIndex) + "\t" + (lostTuplesAW.get(anIndex) * (-1)), "AvgMetricsByW.txt");
//        }
    }

    public static boolean verticalMigration() {
        return false;
    }

    public static void printResults(String a, String filename) {
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

}
