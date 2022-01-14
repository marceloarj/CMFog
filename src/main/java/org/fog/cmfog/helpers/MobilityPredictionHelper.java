/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog.cmfog.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.fog.cmfog.CMFog;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;
import org.fog.cmfog.mobilityprediction.MobilityPrediction;

/**
 *
 * @author marce
 */
public class MobilityPredictionHelper {

    public static void readMobileDeviceTraces(MobilityPrediction aMobilityPrediction) {
        for (int anIndex = 0; anIndex < CMFog.mobileDevices.size(); anIndex++) {
            readHistoricalData(CMFog.mobileDevices.get(anIndex), "input/" + anIndex, aMobilityPrediction);
            readSimulationPath(CMFog.mobileDevices.get(anIndex), "input/9.txt");
        }
    }

    private static void readSimulationPath(MobileDevice mobildeDevice, String filename) {
        String line;
        String cvsSplitBy = "\t";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while ((line = br.readLine()) != null) {
                String[] position = line.split(cvsSplitBy);
                mobildeDevice.getPath().add(position);
            }
            Coordinate coordinate = new Coordinate();
            coordinate.setInitialCoordinate(mobildeDevice);
            //saveMobility(st);
        } catch (IOException e) {
        }
    }

    private static void readHistoricalData(MobileDevice mobileDevice, String filename, MobilityPrediction aMobilityPrediction) {
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
                aMobilityPrediction.parseTraces(mobileDevice, entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
