# MotQot - Daily Motivational Quotes for Developers

MotQot is an Android application that delivers daily motivational quotes for programmers and developers using the Perplexity AI API.

## Features

- **Daily Motivational Quotes**: Receive a new motivational quote every day to inspire your coding journey
- **Perplexity AI Integration**: Uses the Perplexity AI API to generate unique and relevant quotes
- **Multiple Languages**: Supports English, German, French, and Spanish
- **Customizable Notifications**: Set your preferred time to receive daily quote notifications
- **Share Functionality**: Easily share your favorite quotes with friends and colleagues

## Technical Details

- **Architecture**: MVVM (Model-View-ViewModel)
- **API Integration**: Retrofit for network calls to Perplexity AI
- **Background Processing**: WorkManager for scheduling daily quote generation
- **Data Storage**: SharedPreferences for storing user settings and quotes
- **UI Components**: Material Design components for a modern look and feel

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app on your device or emulator
4. On first launch, you'll need to enter your Perplexity API key in the settings

## API Key

To use this app, you need a valid Perplexity AI API key:
1. Sign up at [Perplexity AI](https://www.perplexity.ai/)
2. Generate an API key from your [account dashboard](https://www.perplexity.ai/account/api)
3. Enter the API key in the app settings

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Perplexity AI for providing the API
- Material Design for UI components
- Android Jetpack libraries
