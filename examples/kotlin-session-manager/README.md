# Kotlin Session Manager Example

Basead on Java Session Manager Example by [@domschoen](https://github.com/domschoen/cloudstate-samples-java-session-manager)

# Cloudstate Session Manager
## Description
Example of a Session Manager which manage sessions for accounts like you may have when using a video streaming platform.
In such platform, depending on your subscription, you may have more or less simultaneous viewings (= sessions).
Here we represent subscription as Home entity and the number of simultaneous viewing by the max number of sessions.

We have 2 services:

| Service   | Entity    | Entity Key    |
| --------- | --------- | ------------- |
| SM        | Home      | Account ID    |
| Device    | Device    | Device ID     |

Those 2 services runs in the same Cloudstate user function because we want to forward from one service to the other (and this is only possible inside the same user function).

### Standard use case: the device knows the account ID
In this standard use case, the device uses the first service "Home".
For each video the user start to watch from a device, the device needs a session. It means that the device send to the "Home" service the command:

- CreateSession

Then the device will regularly ask for session renewal with command:

- HeartBeat

When the user has finished watching a video, the device will terminate the session sending command:

- TearDown

Note: If a device tries to ask for an extra session above the max number of sessions of the Home account, then you will have an error.

### Why do we need a Device service ?
When you use the Home service you are using the account ID but we wanted to cover a special use case: The device doesn't always knows the account ID and may ask for a session with only the device ID. In that case, the session manager should use another service to get the account ID from Device ID and then proceed as usual.

At first we wanted to have a service able to represent the Account - Devices relationship i.e the Account having multiple Devices registered but what we need is this:
- getDeviceID(deviceID) returning AccountID

That command is not possible if we represent account because the key of an account is the account ID. If you look at the command above we cannot pass the account ID because it is what we want in the response ! This command would be possible with a projection (read side) but this functionality is not available for the moment in CloudState.

To solve the problem we decided to use the Device ID as the key and have a Device service even if it not exactly what we wanted.

### What happened if the device want a session knowing only the Device ID ?
In such case the device will not ask the Home service but the Device service for the session creation. Here the steps:

1. The device ask the Device service for the session passing the device ID
2. The "Device" service knows the account ID for the device ID (it should have been set beforehand to the "Device" service with the CreateDevice command)
3. The "Device" service takes the account ID and forward the session creation command with the account ID to the "Home" service which will return the new session created to the device.

## Run on Minikube with Cassandra
To run this example, you need to run some command in this project: https://github.com/cloudstateio/cloudstate

1. `minikube start --vm-driver=hyperkit --memory 8192 --cpus 2`
2. ```eval $(minikube docker-env)```
3. ```
   cd couldstate-master
   sbt -Ddocker.tag=dev
   operator/docker:publishLocal
   dockerBuildCassandra publishLocal
   exit
   ```
5. ```kubectl create namespace cloudstate```
6. ```kubectl apply -n cloudstate -f operator/cloudstate-dev.yaml```
7. Edit config to remove "native-" in images:```kubectl edit -n cloudstate configmap cloudstate-operator-config```
8. ```cd cloudstate-samples-java-session-manager```
9. ```kubectl apply -f descriptors/store/cassandra-store.yaml```
10. ```kubectl apply -f descriptors/cassandra```
11. ```sbt -Ddocker.tag=dev
    project java-session-manager
    docker:publishLocal
    exit
    ```
12. ```kubectl apply -f descriptors/java-session-manager/java-session-manager.yaml```
13. ```
    kubectl expose deployment sm-deployment --port=8013 --type=NodePort
    minikube service sm-deployment --url
    http://192.168.64.35:30320
    grpcurl -plaintext 192.168.64.35:30320 describe
    ```
14. To try it, launch Postman with for example:

    |        | Value                                                  |
    | ------ | ------------------------------------------------------ |
    | Method | POST                                                   |
    | URL    | http://192.168.64.35:30320/home/MyHome/sessions/create |
    | Body   | {<br/>	"device_id" = "Android Phone"<br/>}           |

    and you will get session for "MyHome":

    ```
    {
        "accountId": "MyHome",
        "sessionId": "1b387cfb-aabe-4b94-ae7e-0f31fc6f909b",
        "expiration": "1575996502"
    }
    ```

    
 