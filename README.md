- CMFog v2

    Commandline example:  java -jar -Xmx6g .\cmfog.jar 1 1 2 3 1 0 1 1 1 0 1 250 150 1 > outfile.txt

    Arguments:
    	00 - Migration Enabled 		(1: Enabled , 0: Disabled)
    	01 - Seed for Rand function*
    	02 - Migration Policy 		(0: Lowest Latency, 1: C-CMFog, 2: T-CMFog)*
    	03 - Migration Strategy		(0: Lowest Latency, 3: CMFog, 4: Mix)*
    	04 - Number of Mobile Devices*
    	05 - Max Bandwidth
    	06 - Vm Replica Policy		(0: VM migration, 1: Container Migration, 2: Live Migration)
    	07 - Travel Predict Time
    	08 - Mobility Prediction Error
    	09 - Latency Between Cloudlets
    	10 - Mobile Technology 		(1: 4G, 2: 5G, 3: Wifi)*
    	11 - Max Vm Size*
    	12 - Min Vm Size*
    	13 - Layer 					(1: Simulation on Layer 1, 2: Simulation on Layer 2)*


