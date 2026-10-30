# ai-multimodal

All four modalities using Spring AI.

## Features

- **Image generation** — Generate images from text prompts
- **Vision** — Answer questions about images using multimodal chat
- **Text-to-speech** — Synthesize speech from text
- **Transcription** — Transcribe audio to text

## APIs

- `POST /api/multimodal/image` — Generate image
- `POST /api/multimodal/vision` — Describe/analyze image
- `POST /api/multimodal/speech` — Text-to-speech
- `POST /api/multimodal/transcribe` — Speech-to-text
- OpenAPI docs at `/v3/api-docs`

## Notes

Image/TTS/transcription typically require providers like OpenAI; vision works with multimodal chat models. Switch profiles as needed.

## Run

```bash
mvn -pl ai-multimodal -am spring-boot:run
```
