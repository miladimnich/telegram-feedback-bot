package com.example.telegram_bot.update;

import com.example.telegram_bot.service.GoogleSheetsService;
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

    private final Map<Long, String> userDepartments = new HashMap<>();
    private final Map<Long, String> userPositions = new HashMap<>();


    public UpdateConsumer(
            @Value("${telegram.bot.token}") String telegramToken,
            @Value("${openai.api.key}") String openAiKey,
            GoogleSheetsService googleSheetsService) {
        this.telegramClient = new OkHttpTelegramClient(telegramToken);
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(openAiKey)
                .build();
        this.googleSheetsService = googleSheetsService;
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
                sendText(chatId, "Будь ласка, спершу натисніть /start, щоб обрати філію та посаду.");
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "ФІЛІЯ_КИЇВ":
                    userDepartments.put(chatId, "СТО Київ");
                    sendPositionMenu(chatId, "СТО Київ");
                    break;
                case "ФІЛІЯ_ЛЬВІВ":
                    userDepartments.put(chatId, "СТО Львів");
                    sendPositionMenu(chatId, "СТО Львів");
                    break;
                case "ПОСАДА_МЕХАНІК":
                    userPositions.put(chatId, "Механік");
                    sendText(chatId, "Ви обрали посаду: Механік.");
                    break;
                case "ПОСАДА_ЕЛЕКТРИК":
                    userPositions.put(chatId, "Електрик");
                    sendText(chatId, "Ви обрали посаду: Електрик.");
                    break;
                case "ПОСАДА_МЕНЕДЖЕР":
                    userPositions.put(chatId, "Менеджер");
                    sendText(chatId, "Ви обрали посаду: Менеджер.");
                    break;
                default:
                    sendText(chatId, "Невідома опція. Спробуйте ще раз.");
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
                .text("Оберіть посаду у " + department)
                .chatId(chatId)
                .build();

        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (department.equals("СТО Київ")) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Механік")
                            .callbackData("ПОСАДА_МЕХАНІК")
                            .build()));

            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Електрик")
                            .callbackData("ПОСАДА_ЕЛЕКТРИК")
                            .build()));
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Менеджер")
                            .callbackData("ПОСАДА_МЕНЕДЖЕР")
                            .build()));
        } else if (department.equals("СТО Львів")) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Механік")
                            .callbackData("ПОСАДА_МЕХАНІК")
                            .build()));

            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Електрик")
                            .callbackData("ПОСАДА_ЕЛЕКТРИК")
                            .build()));
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Менеджер")
                            .callbackData("ПОСАДА_МЕНЕДЖЕР")
                            .build()));
        }


        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        message.setReplyMarkup(markup);
        telegramClient.execute(message);
    }

    @SneakyThrows
    private void analyzeFeedback(String feedback, long chatId) {
        String prompt = """
                Проаналізуй повідомлення українською.
                1) Визначи, чи це негативний/нейтральний/позитивний відгук.
                2) Визначи рівень критичності за шкалою від 1 до 5.
                3) порадь як можна вирішити дане питання.
                
                Фідбек: "%s
                
                Відповідь повинна бути у форматі JSON:
                {
                  "емоція": "...",
                  "критичність": ...,
                  "рішення": "..."
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

        String sentiment = root.path("емоція").asText("Невідомо");
        int criticality = root.path("критичність").asInt(0);
        String solution = root.path("рішення").asText("Рішення не надано.");

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

        String reply = String.format("Емоція: %s\nКритичність: %d/5\nПропозиція рішення: %s",
                sentiment, criticality, solution);
        sendText(chatId, reply);
    }


    @SneakyThrows
    private void sendMainMenu(long chatId) {
        SendMessage message = SendMessage.builder()
                .text("Вибери філію")
                .chatId(chatId)
                .build();
        InlineKeyboardButton button_1 = InlineKeyboardButton.builder()
                .text("СТО Київ")
                .callbackData("ФІЛІЯ_КИЇВ")
                .build();
        InlineKeyboardButton button_2 = InlineKeyboardButton.builder()
                .text("СТО Львів")
                .callbackData("ФІЛІЯ_ЛЬВІВ")
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
