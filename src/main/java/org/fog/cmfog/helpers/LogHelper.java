/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog.cmfog.helpers;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author marce
 */
public class LogHelper {

    public static Map<String, Integer> totalTuples = new HashMap<>();
    public static Map<String, Integer> lostTuples = new HashMap<>();

    public static void addTotalTuples(String anApp, int toAdd) {
        if (totalTuples.containsKey(anApp)) {
            int actualValue = totalTuples.get(anApp);
            totalTuples.put(anApp, actualValue + toAdd);
        } else {
            totalTuples.put(anApp, toAdd);
        }
    }
    
    
    
       public static void addLostTuples(String anApp, int toAdd) {
        if (lostTuples.containsKey(anApp)) {
            int actualValue = lostTuples.get(anApp);
            lostTuples.put(anApp, actualValue + toAdd);
        } else {
            lostTuples.put(anApp, toAdd);
        }
    }
}
