/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

import datalogger.server.Main;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PersistingService {

    static String PU_NAME = "dataloggerserver-1.0-mysql-$ENV";
    
    protected static EntityManagerFactory emf = javax.persistence.Persistence.createEntityManagerFactory(PU_NAME.replace("$ENV", Main.env));
    protected EntityManager em;
    private AtomicInteger counter = new AtomicInteger();
    protected boolean fail = false;
    private boolean inTransactionJob = false;

    public void setFailed() {
        fail = true;
    }

    synchronized private void startEM() {
        if (em == null) {
            em = emf.createEntityManager();
            counter.set(0);
        } else {
        }
        counter.addAndGet(1);
    }

    synchronized private void closeEM() {
        if (counter.addAndGet(-1) <= 0) {
            if (em.isOpen()) {
                em.close();
            }
            em = null;
        }
    }

    public EntityManager getEntityManager() {
	return em;
    }
    
    @FunctionalInterface
    static public interface TransactionJob<T> {
        public T exec() throws TransactionJobException;
    }

    static public class TransactionJobException extends Exception {

        public TransactionJobException(String string) {
            super(string);
        }
    }

    public <T> T withTransaction(TransactionJob<T> j) throws TransactionJobException {
        startEM();
        try {
            boolean isActive = em.getTransaction().isActive();
            
            if (isActive) {
                
                try {
                    return j.exec();
                } catch (Exception ex) {
                    throw new TransactionJobException("failed job: " + ex);
                } catch (Error ex) {
                    throw new TransactionJobException("failed job: " + ex);
                } finally {
                }
            } else {

                em.getTransaction().begin();
                try {
                    inTransactionJob = true;
                    return j.exec();
                } catch (Exception ex) {
                    em.getTransaction().rollback();
                    ex.printStackTrace();
                    throw new TransactionJobException("failed job: " + ex);
                } catch (Error ex) {
                    em.getTransaction().rollback();
                    throw new TransactionJobException("failed job: " + ex);
                } finally {
                    inTransactionJob = false;
                    if (em.getTransaction().isActive()) {
                        if (fail) {
                            em.getTransaction().rollback();
                        } else {
                            em.getTransaction().commit();
                        }
                    }
                }
            }

        } finally {
            closeEM();
        }
    }

    public void isInTransaction() throws TransactionJobException {
        if (!inTransactionJob) {
            throw new TransactionJobException("Not called inside withTransaction");
        }
    }
}
