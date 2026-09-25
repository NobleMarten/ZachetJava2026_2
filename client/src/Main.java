import ru.smak.net.Client;
import ru.smak.net.CommandType;
import ru.smak.net.ProtocolConstants;
import ru.smak.painting.DPoint;
import ru.smak.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

void main() {
    var rand = new Random();
    var myColor = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    try {
        var client = new Client("localhost", ProtocolConstants.DEFAULT_PORT);
        var wnd = new MainWindow();
        wnd.setLocationRelativeTo(null);
        // исправление 1: раньше клиент не знал про окно (window был null) и ничего не рисовал
        client.setWindow(wnd);
        wnd.addUserActionListener((type, point)->{
            var cmd = switch (type){
                case STOP_PAINT -> CommandType.FINISH_PAINT;
                case DO_PAINT -> CommandType.POINT;
            };
            var dataBuilder = cmd +
                    ProtocolConstants.COMMAND_SEPARATOR +
                    point.x() +
                    ProtocolConstants.PROPERTY_SEPARATOR +
                    point.y();
            client.sendData(dataBuilder);
        });
        client.start();
        client.sendData(CommandType.START + ProtocolConstants.COMMAND_SEPARATOR + myColor.getRGB());
        wnd.setVisible(true);
    } catch (Exception e){
        System.err.println(e.getMessage());
    }
}
