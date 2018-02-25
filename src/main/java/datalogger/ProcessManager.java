package datalogger;

import java.io.*;
import java.util.*;

/**
 * Created by lars on 2016-01-24.
 */
public class ProcessManager implements Closeable {
    private ProcessBuilder b;
    private Process pr;
    private BufferedReader br;

    public ProcessManager(String... args) {
        b = new ProcessBuilder(args);
	Map<String, String> env = b.environment();
 	env.put("PS1", "");
    }

    public BufferedReader open() {
        try {
            pr = b.start();
	    System.err.println("Process starrted: " + pr);
            InputStream ins = pr.getInputStream();
            br = new BufferedReader(new InputStreamReader(ins));
	    System.err.println("Process starrted: " + pr + ' ' + ins + ' ' + br);
            return br;
        } catch (IOException e) {
	    System.err.println("Can't fork " + e);
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            br.close();
            System.err.println("------------ br closed");
        } catch (Exception ex) {
            System.err.println("------------! br closed " + ex);
        }

        try {
            pr.destroy();
            System.err.println("------------ pr destroyed");
        } catch (Exception ex) {
            System.err.println("------------! pr destroyed " + ex);
        }
    }
}


