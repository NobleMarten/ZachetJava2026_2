package ru.smak.net;

import ru.smak.painting.DPoint;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Client {
    private final Communicator communicator;
    // исправление 1: клиент больше не хранит окно, вместо него списки слушателей
    private final List<BiConsumer<Integer, DPoint>> pointListeners = new ArrayList<>();
    private final List<Runnable> stopListeners = new ArrayList<>();

    public Client(String host, int port) throws IOException {
        var socket = new Socket(host, port);
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
    }

    public void start(){
        communicator.start();
    }

    // исправление 1: сюда добавляется тот, кто хочет получать точки (цвет и точку)
    public void addPointListener(BiConsumer<Integer, DPoint> l){
        pointListeners.add(l);
    }

    // исправление 1: сюда добавляется тот, кто хочет узнать что пришла команда STOP
    public void addStopListener(Runnable l){
        stopListeners.add(l);
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
                switch (type){
                    case CommandType.POINT -> {
                        // исправление 2: было data.split, а надо делить только то что после команды
                        var colorPoint = fullInfo[1].split(ProtocolConstants.OBJECT_SEPARATOR, 2);
                        if (colorPoint.length == 2){
                            try {
                                var color = Integer.parseInt(colorPoint[0]);
                                var pointXY = colorPoint[1].split(ProtocolConstants.PROPERTY_SEPARATOR, 2);
                                if (pointXY.length == 2) {
                                    var x = Double.parseDouble(pointXY[0]);
                                    var y = Double.parseDouble(pointXY[1]);
                                    var pt = new DPoint(x, y);
                                    // исправление 1: раньше было window.addPoint, теперь говорим всем слушателям
                                    for (var l : pointListeners) {
                                        l.accept(color, pt);
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Ошибка преобразования принятых данных");
                            }
                        }
                    }
                    case FINISH_PAINT -> {
                        try {
                            // исправление 3: было parseInt(data), а там еще имя команды, поэтому была ошибка
                            var color = Integer.parseInt(fullInfo[1]);
                            // исправление 1: null значит что линия закончилась
                            for (var l : pointListeners) {
                                l.accept(color, null);
                            }
                        } catch (Exception e){
                            System.err.println("Ошибка преобразования принятых данных");
                        }
                    }
                    case STOP -> {
                        // исправление 1: раньше было window.dispose(), теперь говорим слушателям
                        for (var l : stopListeners) {
                            l.run();
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
