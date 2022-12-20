package com.brunomilitzer.quartzschedulers.web;

import com.brunomilitzer.quartzschedulers.payload.EmailRequest;
import com.brunomilitzer.quartzschedulers.payload.EmailResponse;
import com.brunomilitzer.quartzschedulers.quartz.job.EmailJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class EmailSchedulerController {

    private final Scheduler scheduler;

    @Autowired
    public EmailSchedulerController( final Scheduler scheduler ) {
        this.scheduler = scheduler;
    }

    @PostMapping("/schedule/email")
    public ResponseEntity<EmailResponse> scheduleEmail( @Valid @RequestBody EmailRequest emailRequest ) {
        try {
            final ZonedDateTime dateTime = ZonedDateTime.of( emailRequest.getDateTime(), emailRequest.getTimeZone() );
            if ( dateTime.isBefore( ZonedDateTime.now() ) ) {
                final EmailResponse emailResponse = new EmailResponse( false,
                        "dateTime must be after current time." );

                return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( emailResponse );
            }

            final JobDetail jobDetail = this.buildJobDetail( emailRequest );
            final Trigger trigger = this.buildTrigger( jobDetail, dateTime );

            this.scheduler.scheduleJob( jobDetail, trigger );

            final EmailResponse response = new EmailResponse( true, jobDetail.getKey().getName(),
                    jobDetail.getKey().getGroup(), "E-mail scheduled successfully!" );

            return ResponseEntity.ok( response );

        } catch ( final SchedulerException exception ) {
            log.error( "Error while scheduling email: ", exception );
            EmailResponse response = new EmailResponse( false,
                    "Error while scheduling email. Please try again later!" );
            return ResponseEntity.status( HttpStatus.INTERNAL_SERVER_ERROR ).body( response );
        }
    }

    @GetMapping("/get")
    public ResponseEntity<String> getApiTest() {
        return ResponseEntity.ok( "Get API Test pass!!!" );
    }

    private JobDetail buildJobDetail( final EmailRequest scheduledEmailRequest ) {
        final JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put( "email", scheduledEmailRequest.getEmail() );
        jobDataMap.put( "subject", scheduledEmailRequest.getSubject() );
        jobDataMap.put( "body", scheduledEmailRequest.getBody() );

        return JobBuilder.newJob( EmailJob.class )
                .withIdentity( UUID.randomUUID().toString(), "email-jobs" )
                .withDescription( "Send E-mail Job" )
                .usingJobData( jobDataMap )
                .storeDurably()
                .build();
    }

    private Trigger buildTrigger( final JobDetail jobDetail, ZonedDateTime startAt ) {
        return TriggerBuilder.newTrigger().forJob( jobDetail )
                .withIdentity( jobDetail.getKey().getName(), "email-triggers" )
                .withDescription( "Send Email Trigger" ).startAt( Date.from( startAt.toInstant() ) )
                .withSchedule( SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow() )
                .build();

    }

}
