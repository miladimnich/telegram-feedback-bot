package com.example.telegram_bot.update;

import com.example.telegram_bot.entity.Feedback;
import com.example.telegram_bot.repository.FeedbackRepository;
import com.example.telegram_bot.service.GoogleSheetsService;
import com.example.telegram_bot.service.TrelloService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final OpenAIClient openAiClient;
    private final GoogleSheetsService googleSheetsService;
    private final TrelloService trelloService;
    private final FeedbackRepository feedbackRepository;

    private final Map<Long, String> userDepartments = new HashMap<>();
    private final Map<Long, String> userPositions = new HashMap<>();


    public UpdateConsumer(
            @Value("${telegram.bot.token}") String telegramToken,
            @Value("${openai.api.key}") String openAiKey,
            GoogleSheetsService googleSheetsService, TrelloService trelloService, FeedbackRepository feedbackRepository) {
        this.telegramClient = new OkHttpTelegramClient(telegramToken);
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(openAiKey)
                .build();
        this.googleSheetsService = googleSheetsService;
        this.trelloService = trelloService;
        this.feedbackRepository = feedbackRepository;
        this.googleSheetsService.createHeaderIfNotExists();
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equalsIgnoreCase("/start")) {
                sendMainMenu(chatId);
            } else if (userDepartments.containsKey(chatId) && userPositions.containsKey(chatId)) {
                analyzeFeedback(messageText, chatId);
            } else {
                sendText(chatId, "Please press /start first to select your branch and role.");
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "DEPARTMENT_KYIV":
                    userDepartments.put(chatId, "Service Center Kyiv");
                    sendPositionMenu(chatId, "Service Center Kyiv");
                    break;
                case "DEPARTMENT_LVIV":
                    userDepartments.put(chatId, "Service Center Lviv");
                    sendPositionMenu(chatId, "Service Center Lviv");
                    break;
                case "ROLE_MECHANIC":
                    userPositions.put(chatId, "Mechanic");
                    sendText(chatId, "You selected the role: Mechanic.");
                    break;
                case "ROLE_ELECTRICIAN":
                    userPositions.put(chatId, "Electrician");
                    sendText(chatId, "You selected the role: Electrician");
                    break;
                case "ROLE_MANAGER":
                    userPositions.put(chatId, "Manager");
                    sendText(chatId, "You selected the role: Manager.");
                    break;
                default:
                    sendText(chatId, "Unknown option. Please try again.");
            }
        }
    }

    @SneakyThrows
    private void sendText(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .build();
        telegramClient.execute(message);
    }

    @SneakyThrows
    private void sendPositionMenu(long chatId, String department) {
        SendMessage message = SendMessage.builder()
                .text("Select your role in " + department)
                .chatId(chatId)
                .build();

        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (department.equals("Service Center Kyiv")) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Mechanic")
                            .callbackData("ROLE_MECHANIC")
                            .build()));

            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Electrician")
                            .callbackData("ROLE_ELECTRICIAN")
                            .build()));
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Manager")
                            .callbackData("ROLE_MANAGER")
                            .build()));
        } else if (department.equals("Service Center Lviv")) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Mechanic")
                            .callbackData("ROLE_MECHANIC")
                            .build()));

            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Electrician")
                            .callbackData("ROLE_ELECTRICIAN")
                            .build()));
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Manager")
                            .callbackData("ROLE_MANAGER")
                            .build()));
        }


        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        message.setReplyMarkup(markup);
        telegramClient.execute(message);
    }

    @SneakyThrows
    private void analyzeFeedback(String feedback, long chatId) {
        String prompt = """
                Analyze the message in English.
                               1) Determine whether the feedback is negative, neutral, or positive.
                               2) Determine the criticality level on a scale from 1 to 5.
                               3) Suggest how this issue could be resolved.
                
                               Feedback: "%s
                
                               The response should be in JSON format:
                               {
                                 "emotion": "...",
                                 "criticality": ...,
                                 "solution": "..."
                               }
                """.formatted(feedback);


        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_3_5_TURBO)
                .addUserMessage(prompt)
                .build();

        ChatCompletion completion = openAiClient.chat().completions().create(params);
        String content = completion.choices().get(0).message().content().orElse("{}");


        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);

        String sentiment = root.path("emotion").asText("Unknown");
        int criticality = root.path("criticality").asInt(0);
        String solution = root.path("solution").asText("No solution provided.");


        try {
            googleSheetsService.appendFeedback(
                    userDepartments.get(chatId),
                    userPositions.get(chatId),
                    feedback,
                    sentiment,
                    criticality,
                    solution
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        String reply = String.format("Emotion: %s\nCriticality: %d/5\nSuggested solution: %s",
                sentiment, criticality, solution);
        if (criticality >= 4) {
            try {
                trelloService.createCard("Critical Feedback", feedback + "\nSuggested solution: " + solution);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        Feedback fb = new Feedback();
        fb.setDepartment(userDepartments.get(chatId));
        fb.setPosition(userPositions.get(chatId));
        fb.setMessage(feedback);
        fb.setEmotion(sentiment);
        fb.setCriticality(criticality);
        fb.setSolution(solution);
        feedbackRepository.save(fb);

        sendText(chatId, reply);
    }


    @SneakyThrows
    private void sendMainMenu(long chatId) {
        SendMessage message = SendMessage.builder()
                .text("Select a department")
                .chatId(chatId)
                .build();
        InlineKeyboardButton button_1 = InlineKeyboardButton.builder()
                .text("Service Center Kyiv")
                .callbackData("DEPARTMENT_KYIV")
                .build();
        InlineKeyboardButton button_2 = InlineKeyboardButton.builder()
                .text("Service Center Lviv")
                .callbackData("DEPARTMENT_LVIV")
                .build();
        List<InlineKeyboardRow> row = List.of(
                new InlineKeyboardRow(button_1),
                new InlineKeyboardRow(button_2)
        );
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(row);
        message.setReplyMarkup(markup);
        telegramClient.execute(message);
    }
}
