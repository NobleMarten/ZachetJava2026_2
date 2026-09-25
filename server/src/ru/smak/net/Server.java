package ru.smak.net;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    private volatile boolean isActive;
    private final int port;
    public Server(int port) {
        this.port = port;
    }

    public void start() {
        isActive = true;
        new Thread(()->{
            try (var serverSocket = new ServerSocket(port)) {
                System.out.println("Сервер запущен");
                while (isActive) {
                    try{
                        var socket = serverSocket.accept();
                        System.out.println("Клиент подключен");
                        var connClient = new ConnectedClient(socket);
                        connClient.start();
                    } catch (Exception e) {
                        System.out.println("Ошибка подключения клиентов...");
                        System.out.println(e.getMessage());
                        // исправление 6: тут было isActive = false, из-за этого сервер больше никого не пускал
                    }
                }
            } catch (IOException e) {
                System.out.println("Ошибка включения сервера");
                // исправление 6: выводим причину, раньше ее не было видно
                System.out.println(e.getMessage());
            }
        }).start();
    }
}
