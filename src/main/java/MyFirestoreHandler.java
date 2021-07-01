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
import java.util.List;
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
                        if (e != null || queryDocumentSnapshots == null)
                            return;


                      List<DocumentChange> temp =   queryDocumentSnapshots.getDocumentChanges();

                        // Datensaetze abfragen
                        for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {

                            // Datensatz information
                            String email = (String) doc.getDocument().get("email");
                            boolean emailSent = (boolean) doc.getDocument().get("emailSent");
                            String selectedFacility = (String) doc.getDocument().get("selectedFacility");
                            boolean appointmentEmailSent = (boolean) doc.getDocument().get("appointmentEmailSent");
                            boolean isCanceled = (boolean) doc.getDocument().get("isCanceled");
                            boolean resultEmailSent = (boolean) doc.getDocument().get("resultEmailSent");
                            boolean cancelEmailSent = (boolean) doc.getDocument().get("cancelEmailSent");
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
                            if (selectedFacility != "" && !appointmentEmailSent) {
//
                                DocumentSnapshot docScreeningStation = getFirebaseDocument(firestore, "ScreeningStations", selectedFacility);

                                if (docScreeningStation.exists()) {

                                    // Termin Informationen
                                    String laufzettelNr = (String) doc.getDocument().get("laufzettelNr");
                                    String address = (String) docScreeningStation.get("address");
                                    int postalCode = docScreeningStation.getDouble("postalCode").intValue();
                                    String city = (String) docScreeningStation.get("city");

                                    String timeDayId = (String) doc.getDocument().get("selectedTimeDay");
                                    String date = getAppointmentDate(docScreeningStation, timeDayId);

                                    String time = getAppointmentTime(docScreeningStation, timeDayId, doc.getDocument());

                                    System.out.println("LaufzettelNr: " + laufzettelNr);
                                    System.out.println("Datum: " + date);
                                    System.out.println("Zeit: " + time);
                                    System.out.println("Address: " + address);
                                    System.out.println("PostalCode: " + postalCode);
                                    System.out.println("city: " + city);

                                    new EmailHandler().sendAppointmentEmail(email, laufzettelNr, date, time, address, String.valueOf(postalCode), city);
                                    System.out.println("appointment-email sent to: " + email + "\n");

                                    // set appointmentEmailSent == true
                                    Map<String, Object> update = new HashMap<>();
                                    update.put("appointmentEmailSent", true);
                                    doc.getDocument().getReference().set(update, SetOptions.merge());
                                } else {
                                    System.err.println("Screening Station does not exist!");
                                }
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

    private static DocumentSnapshot getFirebaseDocument(Firestore firestore, String collectionName, String documentName) {
        DocumentReference docRef = firestore.collection(collectionName).document(documentName);

        // asynchronously retrieve the document
        ApiFuture<DocumentSnapshot> future = docRef.get();

        // future.get() blocks on response
        DocumentSnapshot document = null;
        try {
            document = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return document;
    }

    private static String getAppointmentDate(DocumentSnapshot docScreeningStation, String timeDayId) {
        DocumentReference timeDayFuture = docScreeningStation.getReference().collection("timeDays").document(timeDayId);
        ApiFuture<DocumentSnapshot> timeDaySnapshot = timeDayFuture.get();
        try {
            DocumentSnapshot timeDay = timeDaySnapshot.get();
            return (String) timeDay.get("date");
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }

        return "Date not found!";
    }

    private static String getAppointmentTime(DocumentSnapshot docScreeningStation, String timeDayId, DocumentSnapshot doc) {
        ApiFuture<DocumentSnapshot> timeSlotFuture = docScreeningStation.getReference().collection("timeDays").document(timeDayId).collection("slots").document((String) doc.get("selectedTimeSlot")).get();
        try {
            DocumentSnapshot timeSlot = timeSlotFuture.get();
            return (String) timeSlot.get("time");
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
        return "Time not found!";
    }
}
