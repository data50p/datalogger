/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

import com.femtioprocent.fpd2.util.MilliTimer;
import datalogger.server.Main;
import datalogger.server.db.PersistingService.TransactionJob;
import datalogger.server.db.entity.LogCurrentData;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.LogDevice;
import datalogger.server.db.entity.LogType;
import datalogger.server.db.entity.Unit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class BootstrapDB {

    String bootstrapUnits[][] = new String[][] {
        {"", "<unknown>", null, "1.0"},
        {"=", "identity", null, "1.0"},
        {"*", "Any", null, "1.0"},
        {"B", "boolean", null, "1.0"},
        {"I", "counter", null, "1.0"},
        {"D", "number", null, "1.0"},
        {"S", "alpha-numeric", null, "1.0"},
        {"%", "percent", "D", "0.01"},
        {"m", "length", null, "1.0"},
        {"s", "time", null, "1.0"},
        {"ms", "time", "s", "0.001"},
        {"mm", "length", "m", "0.001"},
        {"°", "temp", null, "1.0"},
        {"Pa", "pressure", null, "1.0"},
        {"bar", "pressure", "Pa", "100000"},
	{"millibar", "pressure", "Pa", "100"},
	{"mmHg", "pressure", "Pa", "133.322"},
    };

    String bootstrapTypes[][] = new String[][]{
        {"test", "Test any", "*"},
        {"ute-temp", "temp outside", "°"},
        {"ute-humidity", "humidity outside", "%"},};

    String bootstrapDevs[][] = new String[][]{
        {"unknown", "unknown"},
        {"test", "Test any"},
        {"ute1", "ute sovrummet"},};

    public void update() {
        try {
            final DataLoggerService dls = new DataLoggerService();

            Integer n = dls.withTransaction(() -> {
                    try {
                        int nn = 0;
                        for (String[] bu : bootstrapUnits) {
                            final Unit u = dls.getUnitByName(bu[0]);
                            if (u == null) {
                                final Unit bUnit = bu[2] == null ? null : dls.getUnitByName(bu[2]);
                                Unit nu = new Unit(bu[0], bu[1], bUnit, Double.parseDouble(bu[3]));
                                dls.saveUnit(nu);
                                nn++;
                            }
                        }
                        for (String[] bt : bootstrapTypes) {
                            final LogType t = dls.getLogTypeByName(bt[0]);
                            if (t == null) {
                                final Unit bUnit = bt[2] == null ? null : dls.getUnitByName(bt[2]);
                                LogType nt = new LogType(bt[0], bt[1], bUnit);
                                dls.saveLogType(nt);
                                nn++;
                            }
                        }
                        for (String[] bt : bootstrapDevs) {
                            final LogDevice t = dls.getLogDeviceByName(bt[0]);
                            if (t == null) {
                                LogDevice nt = new LogDevice(bt[0], bt[1]);
                                dls.saveLogDevice(nt);
                                nn++;
                            }
                        }
                        List<Unit> allUnits = dls.getAllUnits();
                        for (Unit u : allUnits) {
                            System.err.println("Found: " + u);
                        }
                        return nn;
                    } catch (PersistingService.TransactionJobException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return null;
            });
            System.out.println("sd " + n);
        } catch (PersistingService.TransactionJobException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void test(int loop) {
        try {
            final DataLoggerService dls = new DataLoggerService();

	    MilliTimer mt = new MilliTimer();
	    
            Integer n = dls.withTransaction(() -> {
                    try {
			int nn = 0;
                        final LogType t = dls.getLogTypeByName("test");
                        final LogDevice d = dls.getLogDeviceByName("test");
                        for (int i = 0; i < loop; i++) {
                            LogCurrentData cd = dls.getLogCurrentData(t, d);
                            LogCurrentData ncd;
                            if (cd == null) {
                                ncd = new LogCurrentData(d, t, "1000");
                                ncd.setNote("This is testing");
                            } else {
                                ncd = new LogCurrentData(cd, d, t, cd.getValue() + 1, "" + (Integer.parseInt(cd.getSvalue()) + 1));
                                ncd.setNote("This is more testing");
                            }
                            dls.save(ncd);
                            LogData ld = new LogData(ncd);
                            dls.save(ld);
			    nn++;
                        }
			return nn;
                    } catch (PersistingService.TransactionJobException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return null;
            });
	    final double ms = mt.pollValue() / n;
            System.out.println(mt.getString("sd " + n + ": ", " ms") + ' ' + ms);
        } catch (PersistingService.TransactionJobException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
