package com.miternos.signalr;

import java.util.HashMap;
import java.util.Map;

public class ResourceCache {

    private static ResourceCache instance;

    private static Map<String,String> resources;


    private ResourceCache(){
        resources = new HashMap<String,String>();
    }

    private static ResourceCache getInstance(){
        if ( instance == null )
            instance = new ResourceCache();

        return instance;
    }

    public Map<String,String> getResources(){
        return resources;
    }
}
