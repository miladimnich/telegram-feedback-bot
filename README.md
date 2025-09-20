# Feedback Bot

Це Spring Boot застосунок, який інтегрується з **Telegram Bot API**, **OpenAI** та **Google Sheets**.  
Бот отримує відгуки від користувачів у Telegram, аналізує їх за допомогою OpenAI та зберігає результати у Google Таблицю.

---

## Функціонал
Обов’язковий (реалізований)

- ✅ Приймає повідомлення від користувачів у Telegram
- ✅ Використовує OpenAI (ChatGPT) для аналізу фідбеку:
    - Визначає емоцію (позитивна/негативна/нейтральна)
    - Визначає критичність (1–5)
    - Пропонує рішення
- ✅ Автоматично записує результати в Google Sheets
- ✅ Повертає користувачу відповідь з аналізом

## 🛠️ Технології

- **Spring Boot** – основа застосунку
- **TelegramBots (client, springboot-longpolling-starter)** – інтеграція з Telegram Bot API
- **OpenAI Java SDK** – взаємодія з GPT-моделями
- **Google API Client** – робота з Google API
- **Google Sheets API v4** – збереження відгуків у таблиці
- **Google Auth Library** – авторизація через service account

---

## ⚙️ Налаштування

### 1. Telegram Bot
1. Створи бота через [BotFather]
2. Отримай токен
3. Додай у `application.properties`:
   ```properties
   telegram.bot.token=YOUR_TELEGRAM_BOT_TOKEN
   
### 2. OpenAI API
1. Зареєструйся або увійди на [OpenAI Platform](https://platform.openai.com/).
2. Перейди в https://platform.openai.com/settings/organization/api-keys **API Keys** → створи новий ключ.
3. Поповни баланс акаунта (мінімум $5) — без цього ключ не буде працювати.
4. Скопіюй ключ.
4. Додай у `application.properties`:
   ```properties
   openai.api.key=YOUR_OPENAI_API_KEY

### 3. Google Sheets API
1. Перейди в [Google Cloud Console](https://console.cloud.google.com/).
2. Створи новий проект або вибери існуючий.
3. Увімкни API **Google Sheets API** для цього проєкту.
4. Створи **Service Account** (обліковий запис служби).
5. Згенеруй JSON-ключ (credentials) та збережи його в `src/main/resources/`, наприклад:
   src/main/resources/service-account.json
6. Додай сервісний акаунт у налаштуваннях доступу Google Sheets (поділись таблицею як з e-mail користувача service account).
7. У `application.properties` додай:
```properties
 google.sheets.id=YOUR_SPREADSHEET_ID
YOUR_SPREADSHEET_ID — це частина URL після /d/ та до /edit.
Наприклад: https://docs.google.com/spreadsheets/d/1AbCDefGhIJklMNopQRstuVWxyz12345/edit
ID буде 1AbCDefGhIJklMNopQRstuVWxyz12345
```
## 🚀 Запуск застосунку
1. запусти IntelliJ IDEA
2. Бот автоматично підключиться до Telegram API.
3. Коли користувач напише /start → отримає меню з вибором філії.
4. Далі можна обрати посаду → бот почне приймати фідбек.
5. Відгук буде проаналізований OpenAI (емоція, критичність, рішення).
6. Результати збережуться в Google Sheets.

## 📌 Автоматичне створення Trello-карт для критичних відгуків

Якщо критичність відгуку >= 4, бот автоматично створює картку на Trello:
1. Перейти на Trello Power-Up Admin (https://trello.com/power-ups/admin/)
2. Натиснути New → New Power-Up or Integration.

Заповнити форму:

Power-Up or Integration name – назва вашого Power-Up, можна змінити пізніше.

Workspace – оберіть Workspace, до якого належить Power-Up.

Email – ваш робочий email для Trello.

Support contact – email або посилання для підтримки користувачів.

Author – ім’я автора або компанії.
3. Натиснути Create.
4. Generate New API Key – скопіюй цей ключ.
5. Зліва натисни Generate a Token – згенеруй токен для доступу вашого бота до Trello.
6. Збережи API Key та Token – вони знадобляться у вашому Spring Boot застосунку для створення карток у Trello.
7. Відкриваєш потрібну карту на дошці Trello.
8. Натискаєш три крапки (меню) → Share → Export JSON.
9. У JSON знайди поле "id" – це і є List ID, тобто ідентифікатор списку, куди має додаватися нова картка.
4. Додай дані у `application.properties`:
```properties
trello.api=YOUR_TRELLO_API_KEY
trello.token=YOUR_TRELLO_TOKEN
trello.listId=YOUR_LIST_ID
```
## 🚀Використання
У коді є клас TrelloService:
1. Метод додає картку на Trello-дошку при критичних відгуках (4–5).
2. Параметри name та description можна налаштовувати на свій смак.(назва картки, яка буде створена в Trello. Наприклад: "Критичний відгук"),
description — опис картки, що з’явиться всередині картки. Там можна помістити текст відгуку, пропозиції рішення тощо.)
3. У методі analyzeFeedback додана перевірка критичності:Якщо критичність відгуку 4 або 5, викликається метод createCard з TrelloService.

---

## 🗄️ Інтеграція з PostgreSQL

Тепер бот зберігає всі відгуки не тільки у **Google Sheets**, але й у **PostgreSQL**.

### 1. Залежності
1. У `pom.xml` додано відповідні залежності
2. У application.properties додано налаштування для бази
3. Створено Entity, Service та Repository класи для збереження даних у таблиці:

