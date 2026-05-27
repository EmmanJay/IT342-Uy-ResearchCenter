package edu.cit.uy.researchcenter.shared.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api-key:default_key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email:noreply@researchcenter.com}")
    private String fromEmailStr;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public void sendRepositoryInviteEmail(String toEmail, String repoName, String inviterName, String inviteToken) {
        String subject = "You've been invited to join " + repoName + " on ResearchCenter";
        String htmlContent = buildHtmlTemplate(
            "Repository Invitation",
            inviterName + " has invited you to collaborate on the repository: <strong>" + repoName + "</strong>.",
            "Click the button below to accept the invitation and join the repository.",
            "Accept Invitation",
            frontendUrl + "/invite/accept?token=" + inviteToken
        );
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendRequestFulfilledEmail(String toEmail, String repoName, String materialTitle) {
        String subject = "Material Request Fulfilled in " + repoName;
        String htmlContent = buildHtmlTemplate(
            "Request Fulfilled",
            "Great news! The material <strong>" + materialTitle + "</strong> has been uploaded to <strong>" + repoName + "</strong>.",
            "You can now view this material in the repository.",
            "View Material",
            frontendUrl + "/dashboard"
        );
        sendEmail(toEmail, subject, htmlContent);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        Email from = new Email(fromEmailStr);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            logger.info("Sent email to: {}, Status Code: {}", to, response.getStatusCode());
        } catch (IOException ex) {
            logger.error("Failed to send email to: {}", to, ex);
        }
    }

    private String buildHtmlTemplate(String title, String mainText, String subText, String buttonText, String buttonLink) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<style>" +
            "  body { font-family: Arial, sans-serif; background-color: #f9fafb; margin: 0; padding: 20px; }" +
            "  .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }" +
            "  .header { background-color: #16a34a; padding: 30px 20px; text-align: center; }" +
            "  .header h1 { margin: 0; color: #ffffff; font-size: 24px; }" +
            "  .content { padding: 30px; text-align: center; color: #374151; }" +
            "  .content p { font-size: 16px; line-height: 1.5; margin-bottom: 20px; }" +
            "  .button { display: inline-block; padding: 12px 24px; background-color: #16a34a; color: #ffffff !important; text-decoration: none; border-radius: 6px; font-weight: bold; margin-top: 10px; }" +
            "  .footer { background-color: #f3f4f6; padding: 20px; text-align: center; color: #6b7280; font-size: 14px; border-top: 1px solid #e5e7eb; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "  <div class='container'>" +
            "    <div class='header'>" +
            "      <h1>" + title + "</h1>" +
            "    </div>" +
            "    <div class='content'>" +
            "      <p>" + mainText + "</p>" +
            "      <p>" + subText + "</p>" +
            "      <a href='" + buttonLink + "' class='button'>" + buttonText + "</a>" +
            "    </div>" +
            "    <div class='footer'>" +
            "      <p>&copy; " + java.time.Year.now().getValue() + " ResearchCenter. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }
}
