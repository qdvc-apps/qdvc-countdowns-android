# Maintenance notes

Things in this codebase that are load-bearing for reasons the code alone does not
make obvious. Read the relevant entry before rewriting the part it describes.

## Where to look for what

*What state exists* is answered by `AppViewModel`: it owns every `StateFlow` and
exposes plain callbacks. *How something is stored or read* is answered by the
matching repository. Screens are stateless projections — they hold only transient
UI state such as which dialog is open, never anything that must survive a tab
switch or process death.

```
MainActivity.kt        host; edge-to-edge; scaffold; back handling; pickers
AppViewModel.kt        all UI state; navigation state machine
model/                 plain data types, no Android dependencies
data/
  SettingsRepository    DataStore preferences and the fired-reminder record
  CountdownRepository   the single SAF document read
  ThemeRepository       JSON themes from assets
notify/
  ReminderScheduler     turns settings into WorkManager jobs
  ReminderWorker        one scheduled slot's worth of work
  Notifications         channels and posting
  BootReceiver          re-anchors the schedule to the wall clock
util/                   CSV parser, digest cascade, date helpers
ui/                     theme, shared components, one package per surface
```

`model/` and `util/` have no Android dependencies, which is why the unit tests
live there — they are the cheapest and most valuable things to cover.

## Navigation and back

There is exactly one back implementation: `AppViewModel.goBack()`. The toolbar
arrow's `onClick` and the `BackHandler` both call it, so they cannot diverge.
Never give a screen its own `BackHandler` that does something different from its
arrow.

`NavState.canGoBack` is false only at the Countdowns list, which is the app's
home root and the one place the system back button closes the app. From the Past
or Settings root, back returns to the Countdowns tab — standard tab behaviour, and
the reason those roots show no toolbar arrow while back still does something.

Re-tapping the current tab returns that tab to its own root. This is handled in
`selectTab`, by comparing the tapped tab with the current one.

## The horizontal slide (do not remove the SizeTransform)

`ui/components/Motion.kt` holds the app's one animation vocabulary, used for both
the list-to-detail step and the Settings root-to-sub-page step.

`SizeTransform { _, _ -> snap() }` is not decoration. `AnimatedContent`'s default
size transform animates the container's height at the same time as the horizontal
slide, so when two screens differ in height the content drifts in diagonally from
a corner instead of sliding straight across. Snapping the size makes the height
change instantly and leaves only the slide moving. The bug only shows up between
screens of different heights, which is why it is easy to reintroduce.

`AnimatedContent` is keyed on the destination but the direction is derived from
**depth**, so adding a Settings page needs no change to the animation.

The function builds its `ContentTransform` through the constructor rather than the
more idiomatic `(enter togetherWith exit).using(sizeTransform)`. That is not a
style choice: `using` is declared inside `AnimatedContentTransitionScope`, so it
only resolves inside a `transitionSpec` lambda and fails with *Unresolved
reference* from a shared top-level function. `ContentTransform.sizeTransform` is
not an escape route either — it is a `var` with an `internal set`. If you want the
idiomatic form, the function has to become an extension on
`AnimatedContentTransitionScope<*>`; the constructor keeps it callable from
anywhere.

## Storage Access Framework

The app holds a **document URI**, not a path. All access goes through
`ContentResolver`; a `java.io.File` built from the URI carries no permission and
will not read. `takePersistableUriPermission` is called the moment a file is
picked, so the grant survives a reboot.

A content-provider read is IPC, not a local read, so it happens on
`Dispatchers.IO` and every failure mode is caught and turned into a
`CountdownsState.Failed` with a sentence the user can act on. `DocumentFile
.fromSingleUri(...).exists()` distinguishes "moved or deleted" from "unreadable"
before the read is attempted.

The file is re-read on launch and on every resume, and by each notification
worker. Nothing about the user's countdowns is cached, so what the app shows can
never be stale.

## The CSV parser

`util/CsvParser.kt` is the only place a file the app does not control becomes the
app's own data, so it is deliberately forgiving and deliberately pure Kotlin.

Two behaviours matter and are covered by tests: a row with an unusable date is
skipped and **counted**, and the count is surfaced in the list rather than
silently swallowed; and a missing `name` or `category` column degrades (the date
stands in for a name, the category becomes *Uncategorised*) rather than failing
the whole file.

`rowNumber` is the line number in the file, counting the header and starting from
one, because that is the number the user will see in their spreadsheet.

## Countdown identity

A CSV row has no id, so `Countdown.key` is derived from its content
(`date|name`). Editing a row's date or name therefore produces a *new* countdown
as far as the app is concerned, which is what we want: its reminders should fire
again against the new date.

This is why `LazyColumn` keys on `key + "#" + rowNumber` rather than `key` alone —
two rows with the same date and name would otherwise collide and crash the list.

## Notification scheduling

`WorkManager`, not `AlarmManager`. A digest at "about 9 AM" is what WorkManager is
for; it survives reboots on its own and needs no exact-alarm permission. Delivery
can drift by minutes, and longer in Doze. **This is a deliberate trade, not a
defect** — do not "fix" it by reaching for `setExactAndAllowWhileIdle`.

One periodic job per digest time, plus one for the reminders. `reschedule()`
cancels everything tagged and re-enqueues, so it is safe to call on every launch
and on every settings change; `AppViewModel` calls it only when the notification
settings actually change, compared as a signature string.

`BootReceiver` exists even though WorkManager restores its own jobs: a restored
job's remaining delay is a *duration*, not a wall-clock time, so after a reboot or
a timezone change a "9 AM" digest can land at some other hour. The receiver
re-anchors it.

The two **Send a test** buttons in Settings bypass the scheduler and post
directly, so the notification arrives while the user is still looking at the
button. They ignore the master switch on purpose — the point of a preview is to
see the thing before deciding to turn it on — and they never write to the
fired-reminder record, so sending a test cannot suppress a real reminder later.
Both take their wording from `NotificationContent`, shared with the worker; a
preview that showed different copy from the real notification would be worse than
no preview at all.

Reminders are delivered once per countdown per threshold. The record lives in
DataStore as a set of `date|name|days` keys, pruned by comparing the leading ISO
date with today — which works only because the date comes **first** in the key and
ISO dates sort lexicographically. Keep it that way.

## Theming and the system bars

Each theme is a JSON file in `assets/themes/`. A malformed file is skipped rather
than crashing the app, and the list is cached after the first read. Adding a theme
is a data change; nothing in the code enumerates them.

The top app bar and the bottom navigation bar both use `surface` with zero tonal
elevation. That single shared colour is what lets one value match both the status
bar and the navigation bar, making the whole frame of the screen read as one
continuous surface. If you give the two bars different colours, the system-bar
matching in `Theme.kt` no longer holds.

`onSurface` is pointed at the theme's `onBackground` so text reads correctly on
both roles without every theme file having to specify it twice.

Category colours come from `MaterialTheme.colorScheme` (`primary`, `secondary`,
`error`) rather than a fixed palette, so a new theme restyles every badge, chip
and ring without touching that code.

## Haptic feedback

Everything goes through `View.performHapticFeedback` in `ui/components/Haptics.kt`,
never `Vibrator`/`VibrationEffect`. That honours the system's own touch-feedback
setting (a user who turned haptics off is not overridden), needs no `VIBRATE`
permission, and no-ops on a device with no vibrator. `FLAG_IGNORE_GLOBAL_SETTING`
is deliberately not passed.

The three intensity rungs are tabulated in a comment above the class. **That table
is the dial.** If a sensation feels wrong on real hardware, moving it one rung is a
one-word change, and the table exists so nobody re-derives it. In particular, taps
use `VIRTUAL_KEY`, not `CONTEXT_CLICK` — the latter looks like the obvious choice
for a light tick and is the platform's *faintest* effect, which reads as broken
haptics next to every other app on the phone.

`CONFIRM`, `REJECT`, `GESTURE_START` and `GESTURE_END` are API 30+ and go through
`api30()` with a named fallback. Check any constant you add against `minSdk 26`;
the compiler will not.

Three rules shape where the calls live:

- **Feedback marks a change, not a touch.** `selectTab` returns whether anything
  moved, and the bottom bar buzzes only when it did — so re-tapping a tab collapses
  a detail view with a tap, and does nothing at all when the tab was already at its
  root.
- **The call goes at the interaction site, never in the ViewModel.** `reload()` is
  reached from the toolbar button *and* from `onResume()`; a haptic in the ViewModel
  would buzz every time the app came to the foreground.
- **A disabled control is silent.** The text-size steppers need no special case for
  this: `enabled = false` means `onClick` never runs.

Most taps pass through `ListRow`, so that is where the default lives, via a
`sensation` parameter. Two things to know when editing it:

- `SwitchRow` and `CheckboxRow` pass `sensation = null` and fire their own. Tapping
  the control itself never runs the row's `clickable`, so both paths have to go
  through one toggle or the feedback is inconsistent depending on where you land.
- `Reject` marks a tap that throws something away — the three **Reset** rows and the
  commit of **Forget this file** — and `Confirm` marks a commit: the two dialogs'
  accept buttons and **Send a test**, which is the only in-app receipt for something
  that then appears outside the app. A row that merely *opens* a confirmation takes
  an ordinary tap, so the weight tells the user which side of the decision they are on.

`Step`, `PickUp` and `Drop` have no call site: they serve drag and swipe gestures
and there are none here. They are kept so the vocabulary stays enumerable.

## Lists keep their place

`LazyListState` for both countdown lists is hoisted into `AppRoot`, above the tab
`when` and above the list-to-detail `AnimatedContent`, and passed down.

This is not tidiness. Both of those structures **discard the outgoing
composition**, so a state remembered inside the list screen is rebuilt at zero on
every return and the list silently loses its place — very visible on a long CSV.
`rememberSaveable` is not a fix: it survives configuration change and process
death, not *leaving composition*.

Two fixed lists means two hoisted states are enough; there is no need for a keyed
map or key pruning here. Settings and the detail view deliberately reset to the
top, which is the right behaviour for each — they are left with their own state
rather than exempted by accident.

## Insets

`enableEdgeToEdge()` is on. The `TopAppBar` consumes the status-bar inset itself,
so the `Scaffold` takes **only** the bottom inset
(`WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)`). Applying the top
inset in both places doubles the top padding.

## Composable scope in lazy lists

`LazyColumn`'s content lambda is a `LazyListScope`, not a composable scope. A
`@Composable` call there will not compile, which is why the parse-warning string
in `CountdownListScreen` is computed *before* the `LazyColumn` and only rendered
inside an `item { }`.

## Worth exercising by hand

The paths where the decisions above actually bite:

- Pick a file, then move, rename or delete it outside the app and reopen the app —
  the failure should be a readable sentence, not a blank screen.
- A file with quoted commas, embedded newlines, CRLF endings and a few bad dates:
  the good rows load, the bad ones are counted in the notice.
- A file with no `category` column at all, and one with a category value that
  matches nothing.
- Drill into a countdown and come back using **both** the toolbar arrow and the
  Android system back button; confirm they behave identically. Then the same in
  Settings.
- Back from the Past and Settings roots (should land on Countdowns) and back from
  the Countdowns list (should close the app).
- Re-tap each tab while already on it — nothing should move, and with haptics on,
  nothing should buzz either. Then re-tap a tab from inside a detail view or a
  Settings sub-page, which should collapse to the root *and* buzz.
- Scroll a long list well down, open a countdown, and come back: the list must be
  where you left it. Then switch to another tab and back, same expectation. Settings
  and the detail view should still open at the top.
- With haptics on, check that Reset and Forget feel heavier than an ordinary row,
  that the text-size steppers go silent at both bounds, and that returning to the
  app from the background does not buzz.
- Both **Send a test** buttons, with a file chosen and without one; then with the
  notification permission denied, where they should prompt for it rather than
  appear to do nothing.
- Set a digest time a minute or two ahead and wait for it; then with no file
  chosen, to see the "no file" digest.
- Cross midnight with the app open, then resume it: a countdown at one day
  remaining should move to zero and, the next day, to the Past tab.
- Switch themes and appearance modes, and check the status and navigation bars
  follow — especially with **OLED Black**, where a mismatch is obvious.
- Revoke the notification permission in Android settings and return to a
  notification page; the notice should appear.
