/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server;

import datalogger.server.db.PersistingService;
import datalogger.server.db.PersistingService.TransactionJob;
import datalogger.server.db.TestService;
import datalogger.server.db.entity.Unit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class Main {

    public static void main(String[] args) {
	Main m = new Main();
	m.test();
    }

    private void test() {
	try {
	    TestService ts = new TestService();

	    Integer n = ts.withTransaction(new TransactionJob<Integer>() {
		@Override
		public Integer perform() {
		    try {
			Unit u = ts.getSettingsData(3);
			return u.getId();
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
}
