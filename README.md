# Notification Detox

An Android application for notification management.

## Setup

### Prerequisites
- Android Studio
- Android SDK
- OpenAI API Key

### Configuration

1. Clone the repository
2. Create or update `local.properties` file in the root directory
3. Add your OpenAI API key:
   ```
   OPENAI_API_KEY=your_openai_api_key_here
   ```
4. Update `app/src/main/java/com/aura/di/OpenAIModule.kt` to use the API key from configuration
5. Build and run the application

## Security Note

⚠️ **Never commit your API keys to version control.** The `local.properties` file is already included in `.gitignore` to prevent accidental commits of sensitive data.

## License

[Add your license here]
