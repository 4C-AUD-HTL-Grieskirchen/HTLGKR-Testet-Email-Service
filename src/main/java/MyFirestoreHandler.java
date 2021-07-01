import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MyFirestoreHandler {

    public static void main(String[] args) throws IOException {
        FileInputStream service = null;
        try {
            service = new FileInputStream(new File(MyFirestoreHandler.class.getResource("/key.json").toURI()));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(service))
                .setDatabaseUrl("https://htlgkr-testet-default-rtdb.firebaseio.com/")
                .build();

        FirebaseApp.initializeApp(options);
        Firestore firestore = FirestoreClient.getFirestore();

        // Auf Aenderungen in Firestore reagieren
        firestore.collection("Registrations")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirestoreException e) {
                        if (e != null || queryDocumentSnapshots == null) return;

                        // Datensaetze abfragen
                        for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {

                            // Datensatz information
                            String email = doc.getDocument().getString("email");
                            boolean emailSent = doc.getDocument().getBoolean("emailSent");
                            DocumentReference selectedFacility = (DocumentReference) doc.getDocument().get("selectedFacility");
                            boolean appointmentEmailSent = doc.getDocument().getBoolean("appointmentEmailSent");
                            boolean isCanceled = doc.getDocument().getBoolean("isCanceled");
                            boolean resultEmailSent = doc.getDocument().getBoolean("resultEmailSent");
                            boolean cancelEmailSent = doc.getDocument().getBoolean("cancelEmailSent");
                            String result = (String) doc.getDocument().get("result");

                            // Wenn registriert => registration-email senden
                            if (!emailSent) {
                                System.out.println("registration-email sent to: " + email + "\n");
                                new EmailHandler().sendRegistrationEmail(email, doc.getDocument().getId());

                                // set emailSent == true
                                Map<String, Object> update = new HashMap<>();
                                update.put("emailSent", true);
                                doc.getDocument().getReference().set(update, SetOptions.merge());
                            }



                            // Wenn Termin gebucht => appointment-email senden
                            if (selectedFacility != null && !appointmentEmailSent) {
                                DocumentSnapshot docScreeningStation = null;
                                try {
                                    docScreeningStation = selectedFacility.get().get();
                                } catch (InterruptedException | ExecutionException interruptedException) {
                                    interruptedException.printStackTrace();
                                }

                                if(!docScreeningStation.exists()) {
                                    System.err.println("Screeningstation doesn't exist anymore");
                                }

                                // Termin Informationen
                                String laufzettelNr = doc.getDocument().getId();
                                String address = docScreeningStation.getString("address");
                                String postalCode = docScreeningStation.getString("postalCode");
                                String city = docScreeningStation.getString("city");

                                String date = "";
                                String time = "";

                                try {
                                    date = ((DocumentReference) (doc.getDocument().get("selectedTimeDay"))).get().get().getString("date");
                                    time = ((DocumentReference) (doc.getDocument().get("selectedTimeslot"))).get().get().getString("time");
                                } catch (InterruptedException | ExecutionException interruptedException) {
                                    interruptedException.printStackTrace();
                                }

                                new EmailHandler().sendAppointmentEmail(email, laufzettelNr, date, time, address, postalCode, city);
                                System.out.println("appointment-email sent to: " + email + "\n");

                                // set appointmentEmailSent == true
                                Map<String, Object> update = new HashMap<>();
                                update.put("appointmentEmailSent", true);
                                doc.getDocument().getReference().set(update, SetOptions.merge());
                            }

                            if (!result.equals("unknown") && !resultEmailSent){
                                new EmailHandler().sendResultEmail(email, result);
                                System.out.println("result-email sent to: " + email + "\n");

                                // set appointmentEmailSent == true
                                Map<String, Object> update = new HashMap<>();
                                update.put("resultEmailSent", true);
                                doc.getDocument().getReference().set(update, SetOptions.merge());
                            }

                            // Termin storniert => cancelEmail senden
                            if (isCanceled && !cancelEmailSent) {
                                System.out.println("cancel-email sent to: " + email + "\n");
                                new EmailHandler().sendCancelEmail(email);

                                // set cancelEmailSent == true
                                Map<String, Object> update = new HashMap<>();
                                update.put("cancelEmailSent", true);
                                doc.getDocument().getReference().set(update, SetOptions.merge());
                            }
                        }
                    }
                });
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
