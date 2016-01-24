package datalogger;

import java.io.*;

/**
 * Created by lars on 2016-01-24.
 */
public class ProcessManager implements Closeable {
    private ProcessBuilder b;
    private Process pr;
    private BufferedReader br;

    public ProcessManager(String... args) {
        b = new ProcessBuilder(args);
    }

    public BufferedReader open() {
        try {
            pr = b.start();
            br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            return br;
        } catch (IOException e) {
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


