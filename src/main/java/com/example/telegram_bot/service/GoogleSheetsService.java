package com.example.telegram_bot.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class GoogleSheetsService {
    private final Sheets sheets;
    @Value("${google.sheets.id}")
    private String spreadsheetId;

    public GoogleSheetsService() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("src/main/resources/service-account-472620-2ef8cbbd851b.json"))
                .createScoped(List.of(SheetsScopes.SPREADSHEETS));

        sheets = new Sheets.Builder(
                com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Feedback Bot")
                .build();

    }

    public void appendFeedback(String department, String position, String message,
                               String emotion, int criticality, String solution) throws IOException {
        long timestamp = System.currentTimeMillis();
        Date date = new Date(timestamp);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedTime = sdf.format(date);

        List<Object> row = Arrays.asList(
                department,
                position,
                message,
                emotion,
                String.valueOf(criticality),
                solution,
                formattedTime
        );
        ValueRange body = new ValueRange().setValues(List.of(row));

        sheets.spreadsheets().values()
                .append(spreadsheetId, "A1", body)
                .setValueInputOption("RAW")
                .execute();

    }
    @SneakyThrows
    public void createHeaderIfNotExists() {
        ValueRange response = sheets.spreadsheets().values()
                .get(spreadsheetId, "A1:G1")
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            List<Object> header = List.of("Філія", "Посада", "Відгук", "Емоція", "Критичність", "Рішення", "Час");
            ValueRange body = new ValueRange().setValues(List.of(header));
            sheets.spreadsheets().values()
                    .update(spreadsheetId, "A1:G1", body)
                    .setValueInputOption("RAW")
                    .execute();
        }
    }

}