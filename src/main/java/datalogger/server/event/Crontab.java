/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.event;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import org.quartz.CronTrigger;
import org.quartz.Job;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import static org.quartz.TriggerBuilder.newTrigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

/**
 *
 * @author lars
 */
public class Crontab extends Timer {

    Scheduler sched;

    static class CrontabJob implements Job {
        @Override
        public void execute(JobExecutionContext jec) throws JobExecutionException {
            System.err.println("running...");
//            Runnable r = (Runnable) jec.getJobDetail().getJobDataMap().get("Runnable");
 //           r.run();
        }
    }

    public Crontab() {
        try {
            init();
        } catch (SchedulerException ex) {
            Logger.getLogger(Crontab.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void init() throws SchedulerException {
        SchedulerFactory sf = new StdSchedulerFactory();
        sched = sf.getScheduler();
    }

    void stop() throws SchedulerException {
        sched.shutdown(true);
    }

    public void register(String id, String crontab, Runnable r) throws SchedulerException {

        JobDataMap jdm = new JobDataMap();
        jdm.put("Runnable", r);

        JobDetail job = newJob(CrontabJob.class)
                .withIdentity(id, id)
                .usingJobData(jdm)
                .build();

        CronTrigger trigger = newTrigger()
                .withIdentity("trigger-crontab." + id, "CrontabTriggerGroup")
                .withSchedule(cronSchedule(crontab))
                .build();

        sched.scheduleJob(job, trigger);
        sched.start();

        getNextFireTime(id);
    }
    
    Date getNextFireTime(String id) {
        try {
            final Trigger trigger = sched.getTrigger(TriggerKey.triggerKey("trigger-crontab." + id, "CrontabTriggerGroup"));
            final Date nextFireTime = trigger.getNextFireTime();
            System.err.println("nft " + nextFireTime + ' ' + new Date());
            return nextFireTime;
        } catch (SchedulerException ex) {
            Logger.getLogger(Crontab.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
