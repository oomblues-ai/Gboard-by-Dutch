# Use Local AI with Gboard

Local AI adds an optional on-device route for writing help, image text and dictation. A separate Local Model Host app owns the models, so Gboard and other integrating apps can share them. Its installation and inference do not require the Play Store, AICore, a Google account or Google Play services.

The original Gboard backends remain available. This is not a switch that removes all Google traffic from the keyboard: GIF searches, synchronization and explicitly selected Google features retain their own behavior.

## Install the companion

1. Apply **AI Writing Tools**, **Enable OCR / Scan Text** and, for shared dictation, **Parakeet voice typing** in Morphe. Keep your existing package name and signing key when updating Gboard.
2. Open the patched Gboard's **Patches → AI & Voice → Local AI** settings. Use **Get Local Model Host** or **Update Local Model Host**. Gboard downloads the pinned signed APK and verifies it before opening Android's installer.
3. Approve installation from Gboard if Android requests it. Keep the existing companion's data when updating.
4. Open **Model setup**. Choose Gemma 4 E2B or the optional E4B profile if it is offered for your hardware. Download the selected model and enable Local AI and text/image processing. Enable image support for image tasks.
5. Wait for the text/image model to say **Ready**. Return to Gboard and enable **Use Local AI**.

The current companion requires **Android 11 or later and 64-bit ARM**. E4B needs more memory than E2B. A model being selectable does not guarantee that it can load under the current memory conditions. The setup screen distinguishes download, verification, loading, Ready and a memory pause.

For a standalone installation without Gboard, use the [companion project](https://gitlab.com/johninnld/local-model-host) and [verified installation guide](../developers/local-ai/install.md).

## Write in the current keyboard theme

Open Gboard's **Writing Tools** from the toolbar. Local actions use the keyboard's applied theme. Try Proofread, Rephrase or Professional on a short draft, review the preview and apply or cancel. Keep normal editor undo available.

Additional actions include shortening, summaries, replies, style examples and a saved instruction where offered. A reply needs supplied conversation context; it does not read another app automatically. Use **Read this screen** explicitly and approve Android's screen-capture consent when asked. Apps that block capture cannot be read through that workflow. Nothing is sent as a message for you.

**AI Translate** uses the destination selected under Local AI's **Writing preferences → Translation language**. Choose that destination first. Experimental Smart Compose and inline grammar have their own Writing preferences switches; suggestions still need review.

## Read text from an image

With Local AI selected and vision Ready, open **Scan Text** and choose an image or use the camera entry. Review the recognized text before selecting or inserting it. A screenshot or photograph can contain sensitive information, so choose only the image you intend to process.

The local image path returns text generated from the image. Check names and numbers. It is distinct from barcode/QR scanning and does not provide OCR word boxes or confidence scores. The existing Google/bundled OCR route has its own setup when Local AI is not selected.

## Set up local dictation

Open **Voice typing → Local AI voice typing → Open or install Local AI**. Choose and download a speech profile in **Local AI → Speech recognition**, enable speech and wait for its Ready state. Then enable **Use shared local speech** in Gboard.

| Choice | What to check |
| --- | --- |
| Parakeet TDT v3 or Ultra | Selected profile's declared languages and download |
| Nemotron Streaming | Streaming profile's language and readiness information |
| Whisper Tiny | The supplied integration uses an English prefix |

Downloads are managed in the companion, not separately in Gboard. Text/image processing can be off if you only want speech. The microphone and insertion UI remain Gboard's. Selected local speech reports an unavailable model or memory pause instead of silently switching to Google.

The shared adapters do not provide the old embedded Parakeet decoder's acoustic voice-dictionary bias. Google/Rambler settings apply to those selected backends; they do not add the same commands or dictionary behavior to every local recognizer.

## Memory, storage and privacy

**Always ready** keeps a foreground notification and reduces cold-start delay. On-demand mode saves residency between uses. Android may still reclaim a process, and genuine memory pressure can pause loading. A client app cannot override your Stop action.

Only selected managed model downloads are retained. Switching removes the previous managed download after its active use ends. Original files you imported from Downloads remain yours to manage.

The writing baseline covers 38 languages. Other languages remain available with a clear experimental-quality caveat; quality and speed vary by task and model. Speech and voice providers have separate language lists.

Text, image and speech inference stay on the device. Model/APK setup needs downloads. Search tools make network requests only when enabled by you and permitted by the consuming app. Review every generated edit before applying it.

[All Gboard patches](gboard.md) · [Developer integration](../developers/local-ai/index.md)
