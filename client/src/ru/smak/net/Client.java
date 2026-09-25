package ru.smak.net;

import ru.smak.painting.DPoint;
import ru.smak.ui.MainWindow;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Client {
    private final Communicator communicator;
    private MainWindow window;
    public Client(String host, int port) throws IOException {
        var socket = new Socket(host, port);
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
    }

    public void start(){
        communicator.start();
    }

    public void setWindow(MainWindow w){
        window = w;
    }

    private void parseData(String data){
        if (data == null) {
            stop();
            return;
        }
        var fullInfo = data.split(ProtocolConstants.COMMAND_SEPARATOR, 2);
        if (fullInfo.length == 2) {
            try {
                var type = CommandType.valueOf(fullInfo[0]);
                if (window != null ){
                    switch (type){
                        case CommandType.POINT -> {
                            var colorPoint = fullInfo[1].split(ProtocolConstants.OBJECT_SEPARATOR, 2);
                            if (colorPoint.length == 2){
                                try {
                                    var color = Integer.parseInt(colorPoint[0]);
                                    var pointXY = colorPoint[1].split(ProtocolConstants.PROPERTY_SEPARATOR, 2);
                                    if (pointXY.length == 2) {
                                        var x = Double.parseDouble(pointXY[0]);
                                        var y = Double.parseDouble(pointXY[1]);
                                        var pt = new DPoint(x, y);
                                        window.addPoint(color, pt);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Ошибка преобразования принятых данных");
                                }
                            }
                        }
                        case FINISH_PAINT -> {
                            try {
                                var color = Integer.parseInt(fullInfo[1]);
                                window.addPoint(color, null);
                            } catch (Exception e){
                                System.err.println("Ошибка преобразования принятых данных");
                            }
                        }
                        case STOP -> {
                            window.dispose();
                        }
                    }
                }
            } catch (Exception e){
                System.err.println("Неизвестная команда. " + e.getMessage());
            }
        }
    }

    public void sendData(String data){
        try {
            communicator.sendData(data);
        } catch (Exception e) {
            stop();
        }
    }

    public void stop(){
        if (communicator.isActive())
            communicator.stop();
    }
}
