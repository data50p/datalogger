/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.activation;

import com.femtioprocent.fpd.appl.Appl;
import com.femtioprocent.fpd.util.Ansi;
import com.femtioprocent.propaganda.client.PropagandaClient;
import com.femtioprocent.propaganda.connector.Connector_Tcp;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class ProcessActivation extends Activation {

    class ProcessClient extends PropagandaClient {

        ProcessClient() {
            super("ProcessClient");
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
                        new Message("dl-process-" + hostname
                                + "@DATALOGGER")));
                for (;;) {
                    Datagram datagram = getConnector().recvMsg();
                    System.err.println(name + " got: " + datagram);
                    if (datagram == null) {
                        break;
                    }
                    if (standardProcessMessage(datagram, MessageType.plain) == PropagandaClient.MessageTypeFilter.FILTERED) {
                        System.err.println("got datagram: " + name + " =----> " + datagram);
                        String msg = datagram.getMessage().getMessage();
                        String msgArr[] = datagram.getMessage().getAddendum().split(" ");
                        if ("logged".equals(msg)) {
                            System.err.println(name + " got datagram: _ " + name + " =----> " + datagram);
                        } else {
                            System.err.println(name + " got datagram: _ " + name + " =----> " + datagram);
                        }
                    }
                }
            } catch (PropagandaException ex) {
                System.err.println(name + ": " + ex);
            }
        }
    }

    protected void doProcess() {
        final Connector_Tcp conn = (Connector_Tcp) PropagandaConnectorFactory.create("Tcp", "Process", null, null);
        final ProcessClient client = new ProcessClient();
        System.err.println("ProcessActivation " + " Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
        if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {

            client.setConnectorAndAttach(conn);
            System.err.println("conn " + conn);

            (new Thread(() -> client.start())).start();
            (new Thread(() -> doProcessScanning(client))).start();
        } else {
            System.err.println("ProcessActivation " + "No connection to propaganda: " 
		    + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
            System.exit(1);
        }
    }

    private Message logMessage(String a) {
        return new Message("log", a);
    }

    static class Mapper {

        int id;
        String dest_spec;
        String hwid;

        public Mapper(int id, String dest_spec, String hwid) {
            this.id = id;
            this.dest_spec = dest_spec;
            this.hwid = hwid;
        }

    }

    public void doProcessScanning(ProcessClient client) {
        System.err.println("ProcessActivation " + " scanning ");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
        }

        for (;;) {
            try {

                TimeUnit.SECONDS.sleep(5);
                String w = "ute2 alarm";   // dev type
                String hvalue = "humidity > 80%";
                Message rmsg = logMessage("add " + " " + w + " " + hvalue);
                System.err.println(Ansi.green("send H " + rmsg));

                client.sendMsg(new Datagram(client.getDefaultAddrType(), AddrType.createAddrType("dl-collector-" + hostname
                        + "@DATALOGGER"), MessageType.plain, rmsg));
            } catch (PropagandaException | InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Date parseDate(String cd) {
        try {
            return Date.from(Instant.from(LocalDateTime.parse(cd, formatter).atZone(ZoneId.systemDefault())));
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void start() {
        doProcess();
    }
}
