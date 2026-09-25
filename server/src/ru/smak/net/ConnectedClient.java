package ru.smak.net;

import java.awt.*;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectedClient {
    private final Communicator communicator;
    private final static List<ConnectedClient> clients = new ArrayList<>();
    private int color = Color.BLUE.getRGB();

    public ConnectedClient(Socket socket) throws IOException {
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
        synchronized (clients) {
            clients.add(this);
        }
    }
    public void start(){
        communicator.start();
    }

    public void sendData(String data){
        try {
            communicator.sendData(data);
        } catch (Exception e) {
            stop();
        }
    }

    private void parseData(String data){
        if (data == null) {
            stop();
            return;
        }
        var splitData = data.split(ProtocolConstants.COMMAND_SEPARATOR, 2);
        if (splitData.length == 2){
            CommandType command;
            try {
                command = CommandType.valueOf(splitData[0]);
            } catch (Exception e) {
                System.err.println("неизвестная команда " + splitData[0]);
                return;
            }
            switch (command){
                case POINT -> {
                    try {
                        var coords = splitData[1].split(ProtocolConstants.PROPERTY_SEPARATOR, 2);
                        if (coords.length == 2) {
                            var x = Double.parseDouble(coords[0]);
                            var y = Double.parseDouble(coords[1]);
                            var info = color
                                    + ProtocolConstants.OBJECT_SEPARATOR
                                    + x
                                    + ProtocolConstants.PROPERTY_SEPARATOR
                                    + y;
                            sendForAll(CommandType.POINT, info);
                        }
                    } catch (Exception e){
                        System.err.println(e.getMessage());
                    }
                }
                case FINISH_PAINT -> {
                    sendForAll(CommandType.FINISH_PAINT, String.valueOf(color));
                }
            }
        }
    }

    private void sendForAll(CommandType type, String data){
        List<ConnectedClient> snapshot;
        synchronized (clients) {
            snapshot = new ArrayList<>(clients);
        }
        for (var client : snapshot) {
            client.sendData(type + ProtocolConstants.COMMAND_SEPARATOR + data);
        }
    }

    public void stop(){
        if (communicator.isActive()) {
            communicator.stop();
        }
        synchronized (clients){
            clients.remove(this);
        }
    }
}
