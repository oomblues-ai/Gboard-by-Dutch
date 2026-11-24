# Native Gboard Local AI entry and screen context implementation plan

> **For agentic workers:** Use superpowers:executing-plans for inline implementation. Owner batching and frozen validation-tooling rules take precedence.

**Goal:** Retain the original Writing Tools and Scan Text access points and run their requested work through the selected shared Gemma backend, including user-invoked conversation context for replies.

**Architecture:** Keep native access-point identity/order/icon/theme and the original seven-action Writing Tools backend adapter. Intercept one native camera shutter capture for local image OCR; show selectable text using native OCR styles and Insert. Obtain screen context through one Android-consented MediaProjection capture, stop projection immediately, extract text locally and pass context plus explicit saved style preferences to the existing reply task. No host protocol/model/runtime change is required.

**Tech stack:** Existing Java/Kotlin extension and typed 18.8 bytecode adapters; Android API26+, PixelCopy, MediaProjection, existing shared host SDK.

**Spec:** Owner corrections on 24 November 2026 to the approved shared-local-model-host design: retain original buttons; no duplicate Local AI entry; Scan Text must use Gemma when selected; My Style/replies must be able to use what is visible in Slack/WhatsApp. Preserve the approved native keyboard theme.

## Global constraints

- No root, Accessibility-service dependency or silent screen monitoring. Android confirms each screen capture; one virtual display per permission token and a short mediaProjection foreground service.
- Preserve Google backends and bundled OCR when Local AI/shared OCR is not selected. Keep Parakeet recognition unchanged.
- Use only the current visible screenshot and explicit user-authored style preferences; do not attribute all conversation messages to the user, learn ambient typing history or log recognised content.
- Keep input text, selection and editor identity stable across picker/capture handoffs; reject private/password editors, unknown changed connections and stale results. Apply/Insert remains an explicit action.
- Build/check in disposable copies, retain compact evidence and clean owned scratch. Do not alter fixtures, runners or validation frameworks.

## Review focus

1. Original disabled/empty-draft Writing Tools entry must use actual shared readiness while preserving native suppression and protected editors.
2. Capture must stop and free projection, surfaces, image readers and bitmaps on every exit.
3. A chooser/capture must never transfer context or insert text into a different app or changed draft.
4. Native OCR has no Gemma word boxes: present selectable recognised text, never invent image-space bounding boxes.
5. A slow or cancelled image request must keep the keyboard responsive, show progress and discard its eventual result.

## Task 1: Native entry and camera/image OCR

**Files:** `SharedNativeWritingEntry.java`, `SharedCameraOcr.java`, `SharedImageInputActivity.java`, `EditorSnapshot.java`, `GboardSharedToolsContribution.java`, OCR settings/text resources and shared model bytecode/manifest/runtime-ABI files.

- [x] Trace original 18.8 `aadv.b`, `ppq.l`, `psi.u`, `twm.e/h/b/j` and the `tvy` shutter listener; preserve original bodies in separate wrapper frames.
- [x] Implement native jarvis availability and remove the duplicate shared_local_tools catalog entry.
- [x] Route explicit camera snapshots and selected images to shared OCR when Local AI or the OCR override is selected; retain bundled fallback otherwise.
- [x] Add native-styled selectable text and original Insert action; retain retry/back and avoid fabricated geometry.
- [x] Add product tests for readiness/privacy, range insertion and explicit editor handoff boundaries.
- [x] Grouped Android build/tests/lint passed in disposable batch46; source snapshot `c89b7d8a` and cleanup retained.
- [ ] Both real original ABI emissions for the final immutable review.
- [ ] Bounded phone camera/image recognition, selection/Insert/Undo, theme and cancellation acceptance.

## Task 2: Screen-context replies/My Style

**Files:** `SharedScreenCaptureActivity.java` with private `CaptureService`, `SharedScreenContext.java`, native entry/open hook, extra-action routing, shared text request and manifest.

- [x] Add private normal Android capture consent and a one-shot mediaProjection foreground service with callback/timeout cleanup.
- [x] Restore only the original editor after the explicit handoff; show native-themed progress and Cancel while reading the screenshot locally.
- [x] Route empty-draft native Writing Tools and AI Reply/My Style actions through captured conversation context. Use the current draft as reply intention and saved style preferences when provided; do not claim automatic learning from all visible messages.
- [x] Reuse the existing native preview/Apply/Undo path and operation-local styles.
- [x] Add product tests for image bounds, conversation/style separation and handoff identity/staleness.
- [ ] Grouped checkpoint and fresh read-only review.
- [ ] Bounded Slack/WhatsApp or owner-approved test-chat acceptance: consent, capture teardown, missing/secure context, empty/current draft, cancellation, stale app/selection, Apply/Undo.

## Delivery boundary

The owner selected **Continue offline for now** after ADB disconnected. Do not attempt phone interaction again until it is available/authorized. Source/check success is not runtime acceptance. Keep all earlier passing native Proofread/translation/style evidence for unchanged paths, and identify these newly changed routes as pending phone validation. No main promotion or production publication is authorized.

## Final review fixes and rulings

The fresh whole-delta reviewer identified six important/critical issues; the connected fix pass addresses them before the grouped checkpoint:

- Require the original live InputConnection and EditorInfo, immutable editor metadata, exact text/selection and composing bounds across handoff. Matching resource IDs/names or empty drafts never identify a conversation. Replacement connections fail closed, including View.NO_ID; real-device compatibility remains unproved.
- Keep the validated editor snapshot through asynchronous native activation and recheck immediately before preparing/reading/submitting. Never adopt whichever editor happens to be current later.
- Show the actual captured image and require **Read this screen** before image inference. Ruling: Android's normal capture chooser does not attest that the chosen source is the original chat; explicit confirmation of the actual image supplies source intent — costs an extra confirmation, avoids silently labelling another app's pixels as that conversation.
- Bind every picker callback, bitmap, timeout and editor to one request object. Keep the pending ticket until delayed delivery and discard stale results; clear failed launches.
- Close display/reader/projection independently, retain both readers until resize/surface swap succeeds and clear failed capture launches. Product checks cover exception-continuing cleanup and retry after launch failure.
- Normalize reversed OCR selection endpoints; invalid ranges insert nothing. Product checks cover reversed, collapsed, absent and invalid ranges.

No second review dispatch or per-edit build loop is used; the affected checks run together. Native capture/cleanup, same-editor restoration, OCR quality and reply quality still require phone acceptance after the owner reconnects it.

Grouped checkpoint: the complete affected Android aggregate, patch tests/list generation and Gboard lint passed in batch46 with terminal exit0 and owned scratch removed. Additional request-lifetime regression coverage and the matching delayed-capture ownership guard are included in the final review-artifact checkpoint; do not relabel the phone gates as passed.

Version-contract correction before immutable emission: 18.8 `aadv.b(I)` stores an integer state, so the unrelated older split-keyboard field mapping is not reused. The shared override now runs at `ppq.l`'s exact `j:I` assignment, deriving receiver/state registers from that typed write. It retains native `p/h/e` editor/privacy policy and substitutes shared readiness for Google's model provisioning requirement. Both original ABIs must resolve these exact fields and wrappers before delivery.

Ruling: commit the complete reviewed build input to derive a unique review identity, then run the final build/affected checks and original-APK emissions before pushing that feature head. An unsuccessful review build is not delivered or promoted.
