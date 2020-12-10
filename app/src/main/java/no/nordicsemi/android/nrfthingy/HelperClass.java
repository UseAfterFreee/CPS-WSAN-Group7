package no.nordicsemi.android.nrfthingy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class HelperClass {
    private HashMap<String, HashMap<String, Integer>> thingyMap = new HashMap<String, HashMap<String, Integer>>();

    public void HelperFunction()
    {

        // Input here
        String phonename = new String();
        String thingyID = new String();
        Integer RSSI = new Integer(0);


        String[] phones = {"iPhone1", "iPhone2", "iPhone3"};
        String[] thingyNumbers = {"25", "25", "25",         "3", "3", "3",          "53", "53", "53",           "8", "8", "8",              "7", "7", "7",              "69", "69", "69"};
        Integer[] RSSItest = {-65, -68, -24,             -56, -57, -32,             -28, -35, -47,               -43, -64, -77,             -44, -62, -82,              -85, -95, -49};
        for (int i = 0; i < 18; ++i)
        {
            insertRSSI(phones[i % 3], "Thingy" + thingyNumbers[i], RSSItest[i]);
        }
    }

    public void insertRSSI(String phoneName, String thingyID, Integer RSSI)
    {
        if(!thingyMap.containsKey(phoneName))
        {
            thingyMap.put(phoneName, new HashMap<String, Integer>());
        }
        if(thingyMap.get(phoneName).containsKey(thingyID))
        {
            thingyMap.get(phoneName).replace(thingyID, RSSI);
        }
        else
        {
            thingyMap.get(phoneName).put(thingyID, RSSI);
        }
    }

    public HashMap<String, HashSet> getThingyDivision()
    {
        HashMap<String, Boolean> allThingys = new HashMap<String, Boolean>();
        for (HashMap<String, Integer> i : thingyMap.values())
        {
            for (String j : i.keySet())
            {
                if(!allThingys.containsKey(j))
                {
                    allThingys.put(j, false);
                }
            }
        }

        HashMap<String, HashSet> finalDivision = new HashMap<String, HashSet>();
        for (String i : thingyMap.keySet())
        {
            finalDivision.put(i, new HashSet<String>());
        }
        Boolean stillChanging = new Boolean(true);
        while (stillChanging)
        {
            stillChanging = false;
            for (Map.Entry<String, HashMap<String, Integer>> entry: thingyMap.entrySet())
            {
                HashMap<String, Integer> i = entry.getValue();
                String s = entry.getKey();
                Integer maxPower = new Integer(-999999);
                String maxPowerName = new String();
                for (Map.Entry<String, Integer> j : i.entrySet())
                {
                    String name = j.getKey();
                    Integer value = j.getValue();
                    if (!allThingys.get(name))
                    {
                        if (value > maxPower)
                        {
                            maxPower = value;
                            maxPowerName = name;
                        }
                    } 
                }
                if (maxPower > -999999)
                {
                    finalDivision.get(s).add(maxPowerName);
                    allThingys.replace(maxPowerName, true);
                    stillChanging = true;
                }
            }
        }
        return finalDivision;
    }

    public int getThingyMapSize()
    {
        int thingys = 0;
        for (HashMap<String, Integer> i : thingyMap.values())
        {
            thingys += i.size();
        }
        return thingys;
    }

}