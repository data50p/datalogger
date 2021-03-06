/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

import com.femtioprocent.fpd.util.Ansi;
import com.femtioprocent.fpd2.util.Counter;
import com.femtioprocent.fpd2.util.MilliTimer;
import datalogger.server.Main;
import datalogger.server.db.PersistingService.TransactionJob;
import datalogger.server.db.entity.LogCurrentData;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.LogDevice;
import datalogger.server.db.entity.LogType;
import datalogger.server.db.entity.Unit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author lars
 */
public class BootstrapDB {

    String bootstrapUnits[][] = new String[][]{
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
	{"°C", "temp", null, "1.0"},
	{"Pa", "pressure", null, "1.0"},
	//{"bar", "pressure", "Pa", "100000"},  why
	{"mbar", "pressure", "Pa", "100"},
	{"mmHg", "pressure", "Pa", "133.322"}, //
	{"W", "power", null, "1.0"}, //
    };

    String bootstrapTypes[][] = new String[][]{
	{"test", "Test any", "*"},
	{"temp:out", "temp outside", "°C"},
	{"humidity:out", "humidity outside", "%"}, //
	{"temp:in", "temp inside", "°C"},
	{"humidity:in", "humidity inside", "%"}, //
	{"el", "el-effekt", "W"}, //
	{"alarm", "alarm", "*"}, //
    };

    String bootstrapDevs[][] = new String[][]{
	{"unknown", "unknown"},
	{"test", "Test any"},
	{"ute1", "ute sovrummet"}, //
	{"inne1", "inne sovrummet"}, //
	{"elmät", "Elmätare skåpet"}, //
	{"ute2", "Uterummet"}, //
	{"inne2", "Datahallen vinden"}, //
    };

    public void update() {
	try {
	    final DataLoggerService dls = new DataLoggerService();

	    Integer n;
	    n = dls.withTransaction(new TransactionJob<Integer>() {
		@Override
		public Integer exec() throws PersistingService.TransactionJobException {
		    try {
			Counter c = new Counter();
			
			Stream.of(bootstrapUnits).map((String[] spec) -> {
			    try {
				if (dls.getUnitByName(spec[0]) == null) {
				    final Unit bUnit = spec[2] == null ? null : dls.getUnitByName(spec[2]);
				    return new Unit(spec[0], spec[1], bUnit, Double.parseDouble(spec[3]));
				}
			    } catch (PersistingService.TransactionJobException ex) {
			    }
			    return null;
			}).filter(u -> u != null).forEach(u -> {
			    try {
				dls.save(u);
				c.inc();
			    } catch (PersistingService.TransactionJobException ex) {
			    }
			});
			
			Stream.of(bootstrapTypes).map((String[] spec) -> {
			    try {
				if (dls.getLogTypeByName(spec[0]) == null) {
				    final Unit bUnit = spec[2] == null ? null : dls.getUnitByName(spec[2]);
				    return new LogType(spec[0], spec[1], bUnit);
				}
			    } catch (PersistingService.TransactionJobException ex) {
			    }
			    return null;
			}).filter(lt -> lt != null).forEach(lt -> {
			    try {
				dls.save(lt);
				c.inc();
			    } catch (PersistingService.TransactionJobException ex) {
			    }
			});
			
			Stream.of(bootstrapDevs).map((String[] spec) -> {
			    try {
				if (dls.getLogDeviceByName(spec[0]) == null) {
				    return new LogDevice(spec[0], spec[1]);
				}
			    } catch (PersistingService.TransactionJobException ex) {
			    }
			    return null;
			}).filter(ld -> ld != null).forEach(ld -> {
			    try {
				dls.save(ld);
				c.inc();
			    } catch (PersistingService.TransactionJobException ex) {
			    }
			});
			
			dls.getAllUnits().stream().map(u -> Ansi.blue("Found: ") + u).forEach(System.err::println);
			
			return c.val();
		    } catch (PersistingService.TransactionJobException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		    }
		    return null;
		}
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
		    Logger.getLogger(Main.class
			    .getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	    });
	    final double ms = mt.pollValue() / n;
	    System.out.println(mt.getString("sd " + n + ": ", " ms") + ' ' + ms);

	} catch (PersistingService.TransactionJobException ex) {
	    Logger.getLogger(Main.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
    }
}
