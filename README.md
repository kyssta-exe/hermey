# HERMEY

Native Android client for [Hermes Agent](https://github.com/nesquena/hermes-agent) — connect to your self-hosted Hermes agent from your phone.

Built with Kotlin + Jetpack Compose + Material 3.

## Features

- Chat with your agent (real-time SSE streaming, tool calls, thinking)
- Sessions — list, search, pin, archive, branch
- Tasks — manage scheduled cron jobs
- Skills — browse and toggle agent skills
- Workspace — browse server files
- Memory — read agent memory
- Insights — usage analytics
- Kanban — board view
- Multi-server support with session restore

## Building

```bash
./gradlew assembleDebug
```

Requires JDK 21, Android SDK 35.

## Releases

APK builds are attached to [GitHub Releases](https://github.com/kyssta-exe/hermey/releases). CI builds debug APKs on every push to main.

## License

MIT