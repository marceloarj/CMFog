package org.fog.vmmobile.constants;

public final class MaxAndMin {

    public static final int AP_COVERAGE = 250; //Max Ap coverage distance - It should modify
    public static final int CLOUDLET_COVERAGE = 250; //Max Ap coverage distance - It should modify
    public static final int MAX_DISTANCE_TO_HANDOFF = 0; //It cannot be less than Max_SPEED
    public static final int MIG_POINT = 30;//(int) (MAX_DISTANCE_TO_HANDOFF*1.3);//	best try was 40 zone
    public static final int LIVE_MIG_POINT = 200;//(int) (MAX_DISTANCE_TO_HANDOFF*20.0);//It can be based on the Network's Bandwidth Marcerl
    public static final int MAX_HANDOFF_TIME = 1200;
    public static final int MIN_HANDOFF_TIME = 700;
    public static final int MAX_AP_DEVICE = 15000;
    public static final int MAX_SMART_THING = 7;
    public static final int MAX_SERVER_CLOUDLET = 10000;
    public static final int MAX_X = 2500;
    public static final int MAX_Y = 2500;
    public static final int MAX_SPEED = 10;
    public static final int MAX_DIRECTION = 9;
    public static final int MAX_SERVICES = 3;
    public static final float MAX_VALUE_SERVICE = 1.1f;

    public static final float MAX_VALUE_AGREE = 70f;
    public static final int MAX_ST_IN_AP = 500;
    public static final int MAX_SIMULATION_TIME = 3098 * 60 * 30;
            //3872 * 60 * 30; 10
    //2927 * 60 * 30;
    //2445 * 60 * 30;
    // 3254 * 60 * 30;//3645 * 60 * 30;//2445 * 60 * 30;//3645 * 60 * 30;
    //1000 * 60 * 30; //30 minutes 7250
    public static final int MAX_VM_SIZE = 200; //200MB 
    public static final int MIN_VM_SIZE = 150; //100MB
    public static final int MAX_BANDWIDTH = 15 * 1024 * 1024;
    public static final int MIN_BANDWIDTH = 5 * 1024 * 1024;
    public static final int DELAY_PROCESS = 500;
    public static final double SIZE_CONTAINER = 0.6;
    public static final double PROCESS_CONTAINER = 1.3;

    /////////////////////////// 918000.0    1053000.0   1465000.0                          1156000.0
    public static final double LATENCY_AP_MIN = 1;
    public static final double LATENCY_AP_MAX = 3;
    public static final double BW_AP_MIN = 280;
    public static final double BW_AP_MAX = 320;
    public static final double BW_AP = 100;

    public static final double LATENCY_FOG_MIN = 30;
    public static final double LATENCY_FOG_MAX = 40;
    public static final double BW_FOG_MIN = 130;
    public static final double BW_FOG_MAX = 170;
    //4G
    public static final double T4G_LATENCY_MD_MIN = 8;
    public static final double T4G_LATENCY_MD_MAX = 12;
    public static final double T4G_BW_MD_MIN = 8;
    public static final double T4G_BW_MD_MAX = 12;

    //5G
    public static final double T5G_LATENCY_MD_MIN = 1;
    public static final double T5G_LATENCY_MD_MAX = 4;
    public static final double T5G_BW_MD_MIN = 80;
    public static final double T5G_BW_MD_MAX = 120;

    public static final double WF_LATENCY_MD_MIN = 4;
    public static final double WF_LATENCY_MD_MAX = 8;
    public static final double WF_BW_MD_MIN = 120;
    public static final double WF_BW_MD_MAX = 180;
//        4G	[8,12]	[8,12]	wireless
//5G	[80,120]	[1,4]	wireless
//WiFi (802.11n)	[120,180]	[4,8]	wireless
//Ap	[280,320]	[1,3]	ethernet
//Fog	[130,170]	[30,40]	
}
