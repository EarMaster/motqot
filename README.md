# motqot - Daily Motivational Quotes for Developers

motqot is an Android application that delivers daily motivational quotes for programmers and developers using any OpenAI-compatible API (OpenAI, OpenRouter, Anthropic, Mistral, Perplexity, or a custom endpoint).

## Features

- **Daily Motivational Quotes**: Receive a new motivational quote every day to inspire your coding journey
- **OpenAI-Compatible Integration**: Call your favorite OpenAI-compatible provider via configurable presets or a custom endpoint
- **Multiple Languages**: Supports English, German, French, and Spanish
- **Customizable Notifications**: Set your preferred time to receive daily quote notifications
- **Share Functionality**: Easily share your favorite quotes with friends and colleagues

## Technical Details

- **Architecture**: MVVM (Model-View-ViewModel)
- **API Integration**: Retrofit for network calls to any OpenAI-compatible provider
- **Background Processing**: WorkManager for scheduling daily quote generation
- **Data Storage**: SharedPreferences for storing user settings and quotes
- **UI Components**: Material Design components for a modern look and feel

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app on your device or emulator
4. On first launch, select a preset (or keep the default) and enter the matching API key in the settings

## Provider Setup

The app works with OpenAI-compatible APIs that authenticate via API key headers. A few presets are built in:

- **OpenAI** – `https://api.openai.com/v1/`
- **OpenRouter** – `https://openrouter.ai/api/v1/`
- **Anthropic** – `https://api.anthropic.com/v1/`
- **Mistral** – `https://api.mistral.ai/v1/`
- **Perplexity** – `https://api.perplexity.ai/`

Choose a preset to autofill the base URL and model, paste the corresponding API key, or switch to the *Custom* preset to point at any other compatible endpoint.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- OpenAI, OpenRouter, Anthropic, Mistral, and Perplexity for their OpenAI-compatible APIs
- Material Design for UI components
- Android Jetpack libraries
