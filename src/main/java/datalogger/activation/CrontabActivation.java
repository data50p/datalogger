/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.activation;

import com.femtioprocent.fpd.appl.Appl;
import com.femtioprocent.fpd.sundry.S;
import com.femtioprocent.propaganda.client.PropagandaClient;
import com.femtioprocent.propaganda.connector.Connector_Plain;
import com.femtioprocent.propaganda.connector.PropagandaConnectorFactory;
import com.femtioprocent.propaganda.data.AddrType;

import static com.femtioprocent.propaganda.data.AddrType.anonymousAddrType;
import static com.femtioprocent.propaganda.data.AddrType.serverAddrType;

import com.femtioprocent.propaganda.data.Datagram;
import com.femtioprocent.propaganda.data.Message;
import com.femtioprocent.propaganda.data.MessageType;

import static com.femtioprocent.propaganda.data.MessageType.register;

import com.femtioprocent.propaganda.exception.PropagandaException;
import datalogger.server.Main;
import datalogger.server.event.Crontab;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.SchedulerException;

/**
 * @author lars
 */
public class CrontabActivation extends Activation {

    class CrontabClient extends PropagandaClient {

        CrontabClient() {
            super("CrontabClient");
        }

        void start() {
            try {
                String hostname = "unknown";
                try {
                    hostname = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException ex) {
                }
                sendMsg(new Datagram(anonymousAddrType,
                        serverAddrType,
                        register,
                        new Message("dl-crontab-" + hostname
                                + "@DATALOGGER")));

                try {
                    AtomicInteger cnt = new AtomicInteger();
                    Crontab instance = new Crontab();
                    instance.register("datalogger", "0/10 * * * * ?", () -> {
                        try {
                            sendMsg(new Datagram(getDefaultAddrType(), AddrType.createAddrType("*@DATALOGGER"), MessageType.plain, new Message("Crontab trigger")));
                        } catch (PropagandaException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    });
                } catch (SchedulerException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

                for (;;) {
                    Datagram datagram = getConnector().recvMsg();
                    S.pL("Crontab got: " + datagram);
                    if (datagram == null) {
                        break;
                    }
                    if (standardProcessMessage(datagram, MessageType.plain) == MessageTypeFilter.FILTERED) {
                        System.err.println("got datagram: " + name + " =----> " + datagram);
                        String msg = datagram.getMessage().getMessage();
                        String msgArr[] = datagram.getMessage().getAddendum().split(" ");
                        if ("xxxxxxxxx".equals(msg)) {
                        } else {
                            System.err.println("got datagram: _ " + name + " =----> " + datagram);
                        }
                    }
                }
            } catch (PropagandaException ex) {
                S.pL("CrontabClient: " + ex);
            }
        }
    }

    void doCrontab() {
        final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "Crontab", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
        final CrontabClient client = new CrontabClient();
        System.err.println("Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
        if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {

            client.setConnectorAndAttach(conn);
            S.pL("conn " + conn);

            Thread th2 = new Thread(() -> client.start());
            th2.start();
        } else {
            System.err.println("No connection to propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
            System.exit(1);
        }
    }

    public void start() {
        doCrontab();
    }

}
