# QDVC Countdowns

An Android app that counts the days to the dates in a CSV file you choose. The
file stays where it is, in your own storage, and the app only ever reads it.

Built to the conventions in
[qdvc-android-app-specification](https://github.com/qdvc-apps/qdvc-android-app-specification),
with the deviations listed under [Deviations from the specification](#deviations-from-the-specification).

## The CSV file

Three columns are read. Any others are kept and shown on a countdown's detail
screen, so there is no need to strip a file down before pointing the app at it.

| Column | Required | Notes |
| --- | --- | --- |
| `date` | Yes | `yyyy-mm-dd`. Rows with any other format are skipped and counted. |
| `name` | No | The description shown in the list. Falls back to the date. |
| `category` | No | `Event`, `Deadline (internal)` or `Deadline (external)`. |

Column order does not matter, and header matching ignores case. `name` also
matches a `description` or `title` header; `category` also matches `type`.
Category values are matched leniently, so `deadline (internal)`,
`Deadline [Internal]` and `Internal deadline` all land in the same place. A
value that matches nothing becomes *Uncategorised* rather than dropping the row.

Quoting follows RFC 4180: double quotes protect commas and newlines inside a
field, and a doubled quote is a literal quote. A byte-order mark and CRLF line
endings are both handled.

`sample/countdowns.csv` is a small file to try it with.

## What the app does

Three tabs, each with its own root:

1. **Countdowns** — everything happening today or later, soonest first. Tapping a
   row opens the big view: the day count at display size, the category named in
   its own colour, the full date, and every other column from that row.
2. **Past** — everything from yesterday backwards, most recent first. Tapping a
   row opens the same big view, counting up instead of down.
3. **Settings** — the file picker first, then notifications and appearance.

### Notifications

Both kinds are off until a file is chosen, and both can be switched off entirely.

**Daily digest** — a nudge to open the app, at as many times of day as you like
(9 AM and 3 PM to begin with). What it says widens until it has something to
report: how many countdowns fall in the next week; failing that, the next
fortnight; failing that, the next month; and failing all three, a reminder to
check the file is up to date. It also speaks up when the file has gone missing
or can no longer be read.

**Reminders for single countdowns** — one notification per countdown as its date
approaches, at 10, 7, 3, 2 and 1 days remaining and on the day itself. The set of
days is yours to change and applies to every countdown. They arrive at one
configurable time (9 AM to begin with). Each countdown gets each reminder once.

Each kind has a **Send a test** button on its Settings page, which posts one
straight away using your real countdowns. It works whether or not the kind is
switched on, and it neither changes the schedule nor uses up a reminder.

Delivery is deliberately inexact: these are scheduled with `WorkManager`, which
can drift by minutes, and longer if the device is dozing. That is the right
trade for a daily nudge, and it avoids asking you for the exact-alarm permission.

### Appearance

Automatic, light or dark, with an independent colour theme for light and for dark
mode. Five themes ship, including a pure-black **OLED Black**. Adding another is
a data change: drop a JSON file into `app/src/main/assets/themes/` and it appears
in the relevant list. Category colours are read from the active theme, so a new
theme restyles the list badges, the detail ring and the chips for free.

Text size is a single setting applied to the content that carries meaning.

Short vibrations confirm that an action landed, honouring the system's own
touch-feedback setting — turn haptics off in Android settings and the app is
silent. Feedback marks a change rather than a touch, so navigating, toggling and
committing all buzz, while scrolling, a disabled control, and a tab tap that moves
nothing stay quiet. Resetting a setting and discarding a chosen file feel heavier
than an ordinary tap, because they throw something away.

## Building

Requires JDK 17 and the Android SDK (compile and target 34, minimum 26).

**The Gradle wrapper JAR is not committed.** Generate it once, or let Android
Studio do it when you open the project:

```
gradle wrapper --gradle-version 8.7
```

Then:

```
./gradlew assembleDebug          # build
./gradlew test                   # unit tests
./gradlew installDebug           # build and install on a connected device
```

Versions are pinned as a known-good set: Kotlin 1.9.24 with Compose compiler
extension 1.5.14, Compose BOM 2024.06.00, AGP 8.5.2, Gradle 8.7. On Kotlin 1.9.x
the compiler extension is a separate pinned version; if you move to Kotlin 2.0 or
later, delete the `composeOptions` block and apply the
`org.jetbrains.kotlin.plugin.compose` plugin instead.

Application ID and namespace: `qdvc.countdowns.android.app`.

## Deviations from the specification

The spec describes a folder-backed editor with a four-item bottom bar. This app
opens one read-only file, so several of its parts have no work to do here.

- **Three bottom-bar items, not four**, and Settings is one of them rather than
  living behind an overflow menu on tab 1. There is no multitasking switcher: a
  countdown is something you glance at, not something you keep open and edit, so
  a switcher would list one thing at a time and never earn its tab.
- **One file, not a folder.** `OpenDocument` with a persistable read grant, shown
  as the first Settings row, in place of `OpenDocumentTree` and a workspace list.
- **No search index.** The spec's Room and FTS4 machinery exists because walking
  a folder over the Storage Access Framework is expensive. One CSV is a single
  cheap read, and there is no free-text corpus to search.
- **No custom fonts.** The four-slot font pattern serves a text editor. There is
  no editing surface here, so a single text-size setting covers it.

Kept as specified: Kotlin with Jetpack Compose and Material 3, a single Activity,
one `AppViewModel` owning all state with repositories behind it, DataStore for
preferences, JSON colour themes as data, system-bar colour matching, the
horizontal slide for every hierarchical step, and the rule that the toolbar back
arrow and the Android system back button always do the same thing.

See `MAINTENANCE.md` for the parts of the code that are load-bearing in
non-obvious ways.
