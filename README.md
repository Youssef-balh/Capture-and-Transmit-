# Capture-and-Transmit Via MQTT

## Overview

This is an Android application developed in Java, functioning as an MQTT client for capturing and sending photos in response to requests. My app seamlessly integrates with any MQTT client capable of sending requests.

*Note: During my search, I couldn't find an existing app on the App Store or Google Play that provides similar functionality, so I decided to create this application.*

## Key Concepts

### Publish

- **Definition:** Sending a message (photo payload) to a specific topic.
- **How it Works:** The Android device, acting as a publisher, sends a captured photo to a designated MQTT topic on the broker.

### Subscribe

- **Definition:** Indicating interest in receiving messages (photo responses) on a particular topic.
- **How it Works:** The Android device, acting as a subscriber, expresses interest in a specific MQTT topic to receive photo responses.

### Topic

- **Definition:** A string that categorizes and filters messages.
- **How it Works:** The MQTT topics serve as channels for communication. The Android device can publish photos to a topic or subscribe to receive responses from a specific topic.

## Technologies Used

- **MQTT:** The application leverages the MQTT (Message Queuing Telemetry Transport) protocol for efficient and reliable communication between devices.

- **Java:** The app is developed using Java programming language, ensuring compatibility and flexibility.

- **Android Studio:** The development environment used for creating the Android application, providing tools and resources for Android app development.

## Subscription and Publishing in My APP

### Photo Publishing

1. The Android device captures a photo in response to an MQTT request.
2. The app constructs an MQTT PUBLISH packet, including the photo payload and a designated topic.
3. The PUBLISH packet is sent to the MQTT broker.

### Request Subscription

1. The Android device subscribes to a specific MQTT topic, indicating its interest in receiving photo requests.
2. The SUBSCRIBE packet is sent to the broker.

### Message Transmission

- When a photo request is received on the subscribed topic, the Android device captures a photo and sends it as a response.
- The broker forwards the photo response to all relevant subscribers, such as Raspberry Pi or any MQTT client.

In summary, this app leverages the publish/subscribe model for asynchronous communication, allowing the Android device to capture and send photos in response to MQTT requests from various clients.

---
