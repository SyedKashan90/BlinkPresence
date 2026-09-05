# Face embedding model

Drop a MobileFaceNet-style TensorFlow Lite model here as `mobilefacenet.tflite`
(112x112 RGB input, ~192-d float embedding output; the widely-used
`sirius-ai/MobileFaceNet_TF` export is a common source for FYP projects).

`FaceEmbedder` (see `face/FaceEmbedder.kt`) loads this asset lazily and
degrades gracefully — if the file is missing, embedding-based matching is
skipped and the app falls back to ML Kit's on-device liveness/quality gate
only. Enrollment and verification screens surface this state so it's obvious
during a demo whether the model is loaded.
