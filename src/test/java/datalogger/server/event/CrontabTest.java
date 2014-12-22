/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.event;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author lars
 */
public class CrontabTest {
    
    public CrontabTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of init method, of class Crontab.
     */
    @Test
    public void testInit() throws Exception {
        assertTrue(true);
    }

    /**
     * Test of register method, of class Crontab.
     */
    @Test
    @Ignore
    public void testRegister() throws Exception {
        System.out.println("register");
        String id = "junit1";
        String crontab = "* * * * * ?";
        AtomicInteger cnt = new AtomicInteger();
        Crontab instance = new Crontab();
        instance.register(id, crontab, () -> {System.err.println("R " + cnt.incrementAndGet());});
        System.out.println("zzz...");
        for(int i = 0; i < 5; i++) {
            TimeUnit.SECONDS.sleep(1);
            instance.getNextFireTime(id);
        }
        System.out.println("...ZZZ");
        instance.stop();
    }
    
}
