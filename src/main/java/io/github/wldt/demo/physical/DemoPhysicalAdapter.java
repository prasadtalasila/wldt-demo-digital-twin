package io.github.wldt.demo.physical;

import it.wldt.adapter.physical.*;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent;
import it.wldt.exception.EventBusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Authors:
 *          Marco Picone, Ph.D. (picone.m@gmail.com)
 * Date: 01/09/2023
 * Project: White Label Digital Twin Java Framework - (whitelabel-digitaltwin)
 */
public class DemoPhysicalAdapter extends PhysicalAdapter {

    private final static String TEMPERATURE_PROPERTY_KEY = "temperature-property-key";
    private final static String OVERHEATING_EVENT_KEY = "overheating-event-key";
    private final static String SET_TEMPERATURE_ACTION_KEY = "set-temperature-action-key";

    private final static int MESSAGE_UPDATE_TIME = 1000;
    private final static int MESSAGE_UPDATE_NUMBER = 10;
    private final static double TEMPERATURE_MIN_VALUE = 20;
    private final static double TEMPERATURE_MAX_VALUE = 30;

    private PhysicalAssetRelationship<String> insideInRelationship = null;

    public DemoPhysicalAdapter(String id) {
        super(id);
    }

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> physicalAssetActionWldtEvent) {
        try{

            if(physicalAssetActionWldtEvent != null
                    && physicalAssetActionWldtEvent.getActionKey().equals(SET_TEMPERATURE_ACTION_KEY)
                    && physicalAssetActionWldtEvent.getBody() instanceof Double) {

                System.out.println("[DemoPhysicalAdapter] -> Received Action Request: " + physicalAssetActionWldtEvent.getActionKey()
                        + " with Body: " + physicalAssetActionWldtEvent.getBody());
            }
            else
                System.err.println("[DemoPhysicalAdapter] -> Wrong Action Received !");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onAdapterStart() {
        try {

            //Start Physical Asset Description Publication
            new Thread(publishPhysicalAssetDescription()).start();

            //Start Device Emulation
            new Thread(deviceEmulation()).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAdapterStop() {

    }

    private Runnable publishPhysicalAssetDescription(){
        return () -> {
            try {

                System.out.println("[DemoPhysicalAdapter] -> Sleeping before Publishing Physical Asset Description ...");

                //Emulate a Startup delay of 5 seconds to emulate device startup
                Thread.sleep(5000);

                System.out.println("[DemoPhysicalAdapter] -> Publishing Physical Asset Description ...");

                //Create an empty PAD
                PhysicalAssetDescription pad = new PhysicalAssetDescription();

                //Add a new Property associated to the target PAD with a key and a default value
                PhysicalAssetProperty<Double> temperatureProperty = new PhysicalAssetProperty<Double>(TEMPERATURE_PROPERTY_KEY, 0.0);
                pad.getProperties().add(temperatureProperty);

                //Add the declaration of a new type of generated event associated to a event key
                // and the content type of the generated payload
                PhysicalAssetEvent overheatingEvent = new PhysicalAssetEvent(OVERHEATING_EVENT_KEY, "text/plain");
                pad.getEvents().add(overheatingEvent);

                //Declare the availability of a target action characterized by a Key, an action type
                // and the expected content type and the request body
                PhysicalAssetAction setTemperatureAction = new PhysicalAssetAction(SET_TEMPERATURE_ACTION_KEY, "temperature.actuation", "text/plain");
                pad.getActions().add(setTemperatureAction);

                //Create Test Relationship to describe that the Physical Device is inside a building
                this.insideInRelationship = new PhysicalAssetRelationship<>("insideId");
                pad.getRelationships().add(insideInRelationship);

                //Notify the new PAD to the DT's Shadowing Function
                this.notifyPhysicalAdapterBound(pad);

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private Runnable deviceEmulation(){
        return () -> {
            try {


                System.out.println("[DemoPhysicalAdapter] -> Sleeping before Starting Physical Device Emulation ...");

                //Sleep 5 seconds to emulate device startup
                Thread.sleep(10000);

                System.out.println("[DemoPhysicalAdapter] -> Starting Physical Device Emulation ...");

                //Create a new random object to emulate temperature variations
                Random r = new Random();

                //Publish an initial Event for a normal condition
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(OVERHEATING_EVENT_KEY, "normal"));

                //Emulate the generation on 'n' temperature measurements
                for(int i = 0; i < MESSAGE_UPDATE_NUMBER; i++){

                    //Sleep to emulate sensor measurement
                    Thread.sleep(MESSAGE_UPDATE_TIME);

                    //Update the
                    double randomTemperature = TEMPERATURE_MIN_VALUE + (TEMPERATURE_MAX_VALUE - TEMPERATURE_MIN_VALUE) * r.nextDouble();

                    //Create a new event to notify the variation of a Physical Property
                    PhysicalAssetPropertyWldtEvent<Double> newPhysicalPropertyEvent = new PhysicalAssetPropertyWldtEvent<>(TEMPERATURE_PROPERTY_KEY, randomTemperature);

                    //Publish the WLDTEvent associated to the Physical Property Variation
                    publishPhysicalAssetPropertyWldtEvent(newPhysicalPropertyEvent);
                }

                //Publish a demo Physical Event associated to a 'critical' overheating condition
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(OVERHEATING_EVENT_KEY, "critical"));

            } catch (EventBusException | InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    private void publishPhysicalRelationshipInstance() {
        try{

            String relationshipTarget = "building-hq";

            Map<String, Object> relationshipMetadata = new HashMap<>();
            relationshipMetadata.put("floor", "f0");
            relationshipMetadata.put("room", "r0");

            PhysicalAssetRelationshipInstance<String> relInstance = this.insideInRelationship.createRelationshipInstance(relationshipTarget, relationshipMetadata);

            PhysicalAssetRelationshipInstanceCreatedWldtEvent<String> relInstanceEvent = new PhysicalAssetRelationshipInstanceCreatedWldtEvent<>(relInstance);
            publishPhysicalAssetRelationshipCreatedWldtEvent(relInstanceEvent);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
