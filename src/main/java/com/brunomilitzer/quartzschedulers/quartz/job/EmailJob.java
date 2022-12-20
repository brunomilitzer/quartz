package com.brunomilitzer.quartzschedulers.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class EmailJob extends QuartzJobBean {

    private final JavaMailSender mailSender;

    private final MailProperties mailProperties;

    @Autowired
    public EmailJob( final JavaMailSender mailSender, final MailProperties mailProperties ) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    protected void executeInternal( JobExecutionContext context ) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getMergedJobDataMap();

        final String subject = jobDataMap.getString( "subject" );
        final String body = jobDataMap.getString( "body" );
        final String recipientEmail = jobDataMap.getString( "email" );

        this.sendMail( this.mailProperties.getUsername(), recipientEmail, subject, body );

    }

    private void sendMail( final String fromEmail, String toEmail, String subject, String body ) {
        try {
            final MimeMessage message = this.mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper( message, StandardCharsets.UTF_8.toString() );

            helper.setSubject( subject );
            helper.setText( body, true );
            helper.setFrom( fromEmail );
            helper.setTo( toEmail );

            this.mailSender.send( message );

        } catch ( final MessagingException exception ) {
            log.error( "Error sending email: ", exception );
        }
    }

}
