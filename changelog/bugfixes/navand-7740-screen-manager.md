- Fixed Android Auto `MapboxScreenManager` crashing with `CarScreenFactory was not found` when a
  `MapboxCarContext` is created after the car `Session` is already created (for example, lazily in
  `Session.onCreateScreen`) while a previous session's screen transition is still replayed. Screen
  transitions to a key without a registered `MapboxScreenFactory` are now logged and ignored.
