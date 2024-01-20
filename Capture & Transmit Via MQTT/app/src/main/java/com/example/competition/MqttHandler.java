package com.example.competition;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

public class MqttHandler {

    private MqttClient client;
    //private String Mqtt_username = "emqx";
    //private String Mqtt_password = "public";
    public void connect(String brokerUrl, String clientId) {
        try {
            // Set up the persistence layer
            MemoryPersistence persistence = new MemoryPersistence();
            // Set up the SSL context
            //SSLContext sslContext = SSLContext.getInstance("TLS");
            //sslContext.init(null, null, null);

            // Initialize the MQTT client
            client = new MqttClient(brokerUrl, clientId, persistence);

            // Set up the connection options
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            //connectOptions.setUserName(Mqtt_username);
            //connectOptions.setPassword(Mqtt_password.toCharArray());
            //connectOptions.setSocketFactory(sslContext.getSocketFactory());

            // Connect to the broker
            client.connect(connectOptions);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            client.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void setCallback(MqttCallback mqttCallback) {
        client.setCallback(mqttCallback);
    }

}