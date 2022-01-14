package org.fog.cmfog.rmodule;

import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCode;
import com.github.rcaller.util.Globals;

public class RMiddleware {

    //static double wBenef[] = new double[]{10};
    //static double wCost[] = new double[]{10};
    static double wBenef[] = new double[]{5};
    static double wCost[] = new double[]{5};

    public static double[][] callMethodical(double mBenef[][], double mCost[][]) {
        return callFuction("FMC_MADM(mBenef,mCost,wBenef,wCost)", mBenef, mCost);
    }

    public static void callDia() {
    }

    public static void callVikor() {
    }

    private static double[][] callFuction(String functionCall, double mBenef[][], double mCost[][]) {
        Globals.RScript_Windows = "C:\\Program Files\\R\\R-4.0.3\\bin\\Rscript.exe";
        Globals.R_Windows = "C:\\Program Files\\R\\R-4.0.3\\bin\\R.exe";
        RCaller rCaller = RCaller.create();
        RCode code = RCode.create();
        rCaller.setRCode(code);
        code.R_source("input/METH_BSousa_v10.r");
        code.addDoubleMatrix("mBenef", mBenef);
        code.addDoubleMatrix("mCost", mCost);
        code.addDoubleArray("wBenef", wBenef);
        code.addDoubleArray("wCost", wCost);
        code.addRCode("output <- " + functionCall);
        rCaller.runAndReturnResult("output");
        double[] result = rCaller.getParser().getAsDoubleArray("output");
        double probabilityList[][] = new double[mBenef.length][2];
        for (int i = 0; i < mBenef.length; i++) {
            probabilityList[i][0] = result[i];
            probabilityList[i][1] = result[i + mBenef.length];
        }
        return probabilityList;
    }

}
