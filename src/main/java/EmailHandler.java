import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailHandler {
    private final String registrationText = "<h1>Registrierungsbestätigung</h1><p>Sie haben sich erfolgreich registriert und können unter folgendem Link einen Termin für Ihren kostenlosen COVID-19 Test buchen.</p><a href=\"https://htlgkr-testet.web.app/registration/start/{registrationId}\" target=\"_blank\" style=\"padding: 0.6rem 1rem; color: ffffff\">Termin buchen</a><p>-------------------------------------</p><p>Bundesministerium</p></p><p>Soziales, Gesundheit, Pflege</p><p>und Konsumentenschutz</p>";
    private final String appointmentText = "<h1>Terminbestätigung</h1><p>Ihr Termin für den kostenlosen COVID-19 Test wurde gebucht.</p><p>Zum Test bitte mitbringen:</p><li>Gedruckten Laufzettel aus Ihrer Anmeldung oder die Laufzettel-Nummer</li><li>Ausweis</li><p style=\"font-weight: bold;\">Laufzettel-Nummer: plhLaufzettelNr</p><p style=\"font-weight: bold;\">Datum: plhDate</p><p style=\"font-weight: bold;\">Zeit: plhTime</p><p style=\"font-weight: bold;\">Ort: plhAddress - plhPostalCode plhCity</p><a href=\"https://htlgkr-testet.web.app/appointment/auth/{registrationId}\" target=\"_blank\" style=\"padding: 0.6rem 1rem; color: ffffff\">Termin anzeigen</a><p>-------------------------------------</p><p>Bundesministerium</p></p><p>Soziales, Gesundheit, Pflege</p><p>und Konsumentenschutz</p>";
    private final String cancellationText = "<h1>Storno-Bestätigung</h1><p>Ihr Termin am TODO um TODO Uhr für den kostenlosen COVID-19 Test wurde storniert.</p><p>-------------------------------------</p><p>Bundesministerium</p></p><p>Soziales, Gesundheit, Pflege</p><p>und Konsumentenschutz</p>";

    public void sendRegistrationEmail(String recipient, String registrationId) {
        new EmailHandler().sendEmail(recipient, "Österreich testet - Bestätigung", registrationText.replace("{registrationId}", registrationId));
    }

    public void sendAppointmentEmail(String recipient, String laufzettelNr, String date, String time, String address, String postalCode, String city) {
        // Infos zu Termin von Firestore in Email einfuegen
        String readyAppointmentText = appointmentText.replace("plhAddress", address);
        readyAppointmentText = readyAppointmentText.replace("plhPostalCode", postalCode);
        readyAppointmentText = readyAppointmentText.replace("plhCity", city);
        readyAppointmentText = readyAppointmentText.replace("plhDate", date);
        readyAppointmentText = readyAppointmentText.replace("plhTime", time);
        readyAppointmentText = readyAppointmentText.replace("plhLaufzettelNr", laufzettelNr);

        // Email senden
        new EmailHandler().sendEmail(recipient, "Österreich testet - Bestätigung", readyAppointmentText);
    }

    public void sendCancelEmail(String recipient) {
        new EmailHandler().sendEmail(recipient, "Österreich testet - Storno-Bestätigung", cancellationText);
    }


    private void sendEmail(String to, String subject, String content) {
        String from = "htlgrieskirchentestet@gmail.com";

        // Assuming you are sending email from through gmails smtp
        String host = "smtp.gmail.com";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        //properties.put("mail.smtp.host", "smtp.1und1.de");
        properties.put("mail.smtp.auth", true);
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        // Get the Session object.// and pass username and password
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, "Htlgkr21"); // Unser Program kann emails derzeit von einem Gmail-Konto aus versenden
            }
        });

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            //message.setSubject("This is the Subject Line!");
            message.setSubject(subject);


            // Now set the actual message
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(content, "text/html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);

            // Send message
            Transport.send(message);
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
