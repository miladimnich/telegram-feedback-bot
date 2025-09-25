# Feedback Bot

This is a Spring Boot application that integrates with Telegram Bot API, OpenAI, and Google Sheets.
The bot receives feedback from users on Telegram, analyzes it using OpenAI, and stores the results in a Google Sheet.
---

## Functionality

✅ Receives messages from users on Telegram

✅ Uses OpenAI (ChatGPT) to analyze feedback:

Determines the emotion (positive/negative/neutral)

Determines criticality (1–5)

Suggests a solution

✅ Automatically saves results to Google Sheets

✅ Returns the analysis result to the user


## 🛠️ Technologies

Spring Boot – application framework

TelegramBots (client, springboot-longpolling-starter) – Telegram Bot API integration

OpenAI Java SDK – interaction with GPT models

Google API Client – working with Google APIs

Google Sheets API v4 – saving feedback in spreadsheets

Google Auth Library – service account authentication

---

## ⚙️ Setup

### 1. Telegram Bot
 
1. Create a bot via [BotFather]
2. Get the token 
3. Add it to `application.properties`:
   ```properties
   telegram.bot.token=YOUR_TELEGRAM_BOT_TOKEN
   
### 2. OpenAI API
1. Sign up or log in to [OpenAI Platform](https://platform.openai.com/).
2. Go to https://platform.openai.com/settings/organization/api-keys **API Keys** → create a new API key.
3. Add some funds to your account (minimum $5) – the key won’t work without it.
4. Copy the key and add it to `application.properties`:
   ```properties
   openai.api.key=YOUR_OPENAI_API_KEY

### 3. Google Sheets API
1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project or select an existing one.
3. Enable Google Sheets API for this project.
4. Create a Service Account.
5. Generate a JSON key (credentials) and save it in src/main/resources/, e.g.:
   src/main/resources/service-account.json
6. Share the Google Sheet with the service account email.
7. Add the spreadsheet ID to `application.properties` 
```properties
google.sheets.id=YOUR_SPREADSHEET_ID
YOUR_SPREADSHEET_ID — is the part of the URL between /d/ and /edit.
Example:
https://docs.google.com/spreadsheets/d/1AbCDefGhIJklMNopQRstuVWxyz12345/edit
Spreadsheet ID = 1AbCDefGhIJklMNopQRstuVWxyz12345
```
## 🚀 Running the Application
1. Open the project in IntelliJ IDEA

2. The bot will automatically connect to the Telegram API

3. When a user types /start, they will receive a menu to select a department

4. Then they can select a role → the bot will start receiving feedback

5. Feedback will be analyzed by OpenAI (emotion, criticality, solution)

6. Results will be saved in Google Sheets

## 📌 Automatic Trello Card Creation for Critical Feedback

If feedback criticality >= 4, the bot automatically creates a Trello card:

Go to Trello Power-Up Admin: https://trello.com/power-ups/admin/

Click New → New Power-Up or Integration

Fill in the form:

Power-Up or Integration name – name of your Power-Up

Workspace – select your workspace

Email – your work email

Support contact – email or link for user support

Author – author or company name

Click Create

Generate a New API Key and copy it

Generate a Token for your bot to access Trello

Save the API Key and Token – you will use them in Spring Boot to create cards

Open the desired Trello board list → click three dots → Share → Export JSON

Find "id" in the JSON – this is the List ID for new cards
Add the Trello details to `application.properties`
```properties
trello.api=YOUR_TRELLO_API_KEY
trello.token=YOUR_TRELLO_TOKEN
trello.listId=YOUR_LIST_ID
```
## 🚀Using TrelloService
The service adds a Trello card for critical feedback (4–5)

You can configure name and description (e.g., card title: "Critical Feedback", description: feedback + solution)

In analyzeFeedback, if criticality is 4 or 5, createCard is called automatically

---

## 🗄️ PostgreSQL Integration

Now the bot stores all feedback not only in Google Sheets, but also in PostgreSQL.

Dependencies

Added dependencies to pom.xml

Configured database settings in application.properties

Created Entity, Service, and Repository classes for saving data in the table

