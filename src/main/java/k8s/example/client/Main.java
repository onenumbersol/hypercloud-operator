package k8s.example.client;

import java.io.IOException;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ProgressResponseBody;
import k8s.example.client.handler.UserDeleteJob;
import k8s.example.client.k8s.K8sApiCaller;
import k8s.example.client.metering.MeteringJob;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class Main {
	public static Logger logger = LoggerFactory.getLogger("K8SOperator");
	public static void main(String[] args) {
		try {
			// Start webhook server
			logger.info("[Main] Start webhook server");
			new WebHookServer();
			
			// Start Metering
			logger.info("[Main] Start Metering per 30 mins");
			startMeteringTimer();
			
			// Start UserDelete
			logger.info("[Main] Start User Delete per Week");
			startUserDeleteTimer();

			logger.info("[Main] Init & start K8S watchers");
			while (true) {
				try {
					// Start Controllers
					K8sApiCaller.initK8SClient();
					K8sApiCaller.startWatcher(); // Infinite loop
				}catch( Exception e ) {
					logger.info("[Main] Init & restart K8S watchers");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void startMeteringTimer() throws SchedulerException {
		JobDetail job = JobBuilder.newJob( MeteringJob.class )
				.withIdentity( "MeteringJob" ).build();

		CronTrigger cronTrigger = TriggerBuilder
				.newTrigger()
				.withIdentity( "MeteringCronTrigger" )
				.withSchedule(
				CronScheduleBuilder.cronSchedule( Constants.METERING_CRON_EXPRESSION ))
				.build();

		Scheduler sch = new StdSchedulerFactory().getScheduler();
		sch.start(); sch.scheduleJob( job, cronTrigger );
	}
	
	private static void startUserDeleteTimer() throws SchedulerException {
		JobDetail job = JobBuilder.newJob( UserDeleteJob.class )
				.withIdentity( "UserDeleteJob" ).build();

		CronTrigger cronTrigger = TriggerBuilder
				.newTrigger()
				.withIdentity( "UserDeleteCronTrigger" )
				.withSchedule(
				CronScheduleBuilder.cronSchedule( Constants.USER_DELETE_CRON_EXPRESSION ))
				.build();

		Scheduler sch = new StdSchedulerFactory().getScheduler();
		sch.start(); sch.scheduleJob( job, cronTrigger );
	}
}