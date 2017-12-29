package com.miternos.signalr;

import microsoft.aspnet.signalr.client.*;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import microsoft.aspnet.signalr.client.hubs.HubProxy;

public class SignalRClient {


    @Value("${network}")
    private String network;

    @Value("${authToken}")
    public String authToken ;

    @Value("${hubUrl}")
    public String hubUrl ;

    @Value("${restApiUrl}")
    public String restApiUrl;

    @Value("${resourceIdFile:}")  /* cant say required=false , set default to empty as w/a be careful*/
    private String resourceIdFile ;

    @Value("${odataUrl}")
    private String odataUrl;

    @Value("${resourceRefreshPeriod}")
    private Long resourceRefreshPeriod ;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SignalRClient.class);

    private Map<String,Integer> measurementMap= new HashMap<String,Integer>();
    private List<String> resourceIdList = new ArrayList<>();

    HubConnection connection;
    HubProxy hub;

    public void disconnect(){
        logger.info("Stopping connection to SignalR");
        connection.stop();
    }


    private Logger signalrLogger;

    public void connect() {


        long start = System.currentTimeMillis();

        // Create a new console signalrLogger
        signalrLogger = (message, level) -> {};


        String realTimeToken = getRealTimeToken();

        connection = new HubConnection(hubUrl, "", true, signalrLogger);

        hub = connection.createHubProxy("measurementHub");


        hub.on("NewMeasurement", p1 -> {
            logger.debug("Measurement = "+p1.toString());
            Map data = (Map)p1 ;
            String resourceId = (String)data.get("r");
            Double timeStamp = (Double)data.get("UnixTimestamp");
            Double value = (Double)data.get("v");
            logger.info("Measurement Received Resource,"+resourceId+",value,"+value.longValue());

            Integer measurement =  measurementMap.get(resourceId);
            if ( measurement == null ){
                measurement = 0;
            }
            measurement = measurement + 1;
            measurementMap.put(resourceId,measurement);

        }, Object.class);


        // Subscribe to the connected event
        connection.connected(() -> logger.info("CONNECTED"));

        // Start the connection
        SignalRFuture<Void> awaitingConn =  connection.start();


        SignalRFuture<Void> doneConnect = awaitingConn.done(obj -> {
            hub.invoke("authenticate", network, realTimeToken).done(hobj -> logger.info("Authenticated"));
            logger.info("Done Connecting!") ;

        });



        logger.info("Waiting for connection.");
        while ( connection.getState().equals(ConnectionState.Connecting) ){
            try {
                long millis = 5000;
                logger.debug("Wait connection for "+millis+" ms .");
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        try {
            doneConnect.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (StringUtils.isEmpty(getResourceIdFile())) {

            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            ResourceRegistrar registrar = new ResourceRegistrar(hub, odataUrl, authToken, network);
            executorService.scheduleAtFixedRate(registrar, 0, resourceRefreshPeriod, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override public void run() {
                    try {
                        logger.error("attempt to shutdown executor");
                        executorService.shutdown();
                        executorService.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.error("tasks interrupted");
                    } finally {
                        if (!executorService.isTerminated()) {
                            logger.error("cancel non-finished tasks");
                        }
                        executorService.shutdownNow();
                        logger.error("shutdown finished");
                    }
                }
            });
        } else {
            resourceIdList = readResourceIdsFromFile(resourceIdFile);

            for (String r : resourceIdList) {
                hub.invoke("addResource", r).done(hobj -> logger.info("Added resource " + hobj.toString()));
            }

        }


        logger.info("Wait for data");
        while ( measurementMap.size() < 100000 ){
            try {
                long millis = 5000;
                logger.debug("Wait data for "+millis+" ms .");
                Thread.sleep(millis);
                long seconds =  ( System.currentTimeMillis() - start ) / 1000;


                int resourceTotal = measurementMap.size();

                int totalMeasurementsCount = measurementMap.values().stream().mapToInt(value -> value).sum();

                logger.info("Total Resources = "+resourceTotal+", Total measures = "+totalMeasurementsCount+" in "+seconds+" secs, TPS = "+totalMeasurementsCount / seconds );

            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.info("Stopping");
                break;
            }
        }


        connection.stop();



    }



    public String getRealTimeToken() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + authToken);
        headers.set("X-DeviceNetwork", network);

        String requestJson = "";

        logger.info(requestJson);

        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Object> a = restTemplate
                    .exchange(restApiUrl, HttpMethod.POST, entity, Object.class);
            String token = ((HashMap) ((ResponseEntity) a).getBody()).get("Token").toString();
            // ToDo: check object mapping


            logger.info("RT token is " + token);
            return token;
        } catch (RestClientResponseException e) {
            logger.error("Exception occured during real time token retrieval "+e.getMessage());
            throw e;
        }


    }

    /**
     * Read resource ids from file and return list
     */
    private List<String> readResourceIdsFromFile(String sourceFile) {
        List resourceIdList = new ArrayList();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(sourceFile));
            String line = reader.readLine();
            while (!StringUtils.isEmpty(line)) {
                resourceIdList.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resourceIdList;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getOdataUrl() {
        return odataUrl;
    }

    public void setOdataUrl(String odataUrl) {
        this.odataUrl = odataUrl;
    }

    public String getResourceIdFile() {
        return resourceIdFile;
    }

    public void setResourceIdFile(String resourceIdFile) {
        this.resourceIdFile = resourceIdFile;
    }

    public Long getResourceRefreshPeriod() {
        return resourceRefreshPeriod;
    }

    public void setResourceRefreshPeriod(Long resourceRefreshPeriod) {
        this.resourceRefreshPeriod = resourceRefreshPeriod;
    }
}
