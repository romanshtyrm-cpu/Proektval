# AudioStreamer (Java) - minimal TCP audio prototype

This is a minimal Android project (Java) demonstrating sending raw PCM audio from one phone to another over local Wi‑Fi.

How to test:
- On receiver phone: implement AudioServer usage (Activity) or use separate app to receive TCP on port 50005 and play.
- On sender phone: enter server IP and press Start Client.

Notes:
- This is a simple prototype. For production use RTP/Opus and implement jitter buffers, NAT traversal, and security.
- GitHub Actions will build APK automatically.
