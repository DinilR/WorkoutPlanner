# WorkoutPlanner — Design Document

An offline Android app that gives step-by-step workout plans and lets the user complete each set with an interactive, satisfying tap instead of a plain checkbox.

| | |
|---|---|
| **Platform** | Android (phone) |
| **Stack** | Kotlin · Jetpack Compose (Material 3) · Room · Navigation Compose · ViewModel + StateFlow |
| **Connectivity** | 100% offline — no server, no account, no internet permission |
| **Min / Target SDK** | 26 (Android 8.0) / 35 |
| **Theme** | Red Orange `#E8452C` · Snowy White `#FFFAFA` · Yellow Punk `#FFD000` (light + dark) |
| **Visual style** | Sports-poster look: Red Orange → orange gradient top bars with diagonal stripes and a Yellow Punk underline; peach (Red Orange tint) page background instead of white; pale-yellow cards; Anton condensed font for headings and numbers (bundled, SIL OFL — see `licenses/`); cut-corner shapes on cards, dialogs and buttons |
| **Status** | v1 implemented |

---

## 1. Goals

1. Ship with a few **default workout plans** that work on first launch.
2. Let users **create and edit plans inside the app using a table** (Exercise · Sets · Reps · Weight · Rest) — no JSON or file editing.
3. Show an **exact plan** for each exercise: sets, reps, weight, rest time and how-to instructions.
4. Make completing a set **fun and fast** (set bubbles, rings, rest timer, PR badges) instead of a checkbox.
5. **Never lose progress** — every tap is saved to the database immediately; an unfinished workout resumes on reopen.

### Non-goals (for now)
Cloud sync, accounts, social features, AI-generated plans, exercise videos.

---

## 2. Architecture

Single-activity app, MVVM, unidirectional data flow.

```
 UI (Compose screens)
   │  observes StateFlow / sends events
   ▼
 ViewModels  ──────────────►  Repositories  ──────────────►  Room DAOs  ──►  SQLite (on device)
                                   ▲
                                   └── DefaultPlans.kt (seeded once on DB creation)
```

### Package layout

```
com.workoutplanner
├── data
│   ├── db           AppDatabase, entities, DAOs, converters
│   ├── seed         DefaultPlans.kt, DatabaseSeeder
│   └── repo         PlanRepository, SessionRepository
├── ui
│   ├── plans        PlanListScreen, PlanDetailScreen
│   ├── editor       PlanEditorScreen (table input)
│   ├── workout      WorkoutScreen, SetBubble, ProgressRing, RestTimer
│   ├── finish       FinishScreen
│   ├── history      HistoryScreen
│   └── theme        colors, typography, dark mode
├── reminder         ReminderScheduler, ReminderReceiver, ReminderNotifications
├── timer            RestTimerService (foreground service, v2)
└── AppContainer.kt  manual dependency wiring (no Hilt, keeps it simple)
```

---

## 3. Data model (Room)

```
WorkoutPlan 1 ──── * PlanExercise * ──── 1 Exercise
     │
     1
     │
     * WorkoutSession 1 ──── * SetLog
```

### `Exercise` — the exercise library
| Column | Type | Notes |
|---|---|---|
| id | Long PK | auto |
| name | String | unique, e.g. "Push-up" |
| muscleGroup | String? | e.g. "Chest" |
| instructions | String? | numbered how-to steps, one per line |
| isDefault | Boolean | true for built-in exercises |

### `WorkoutPlan`
| Column | Type | Notes |
|---|---|---|
| id | Long PK | auto |
| name | String | e.g. "Beginner Full Body" |
| description | String? | |
| level | Enum | BEGINNER / INTERMEDIATE / ADVANCED |
| isDefault | Boolean | built-in plans can be copied but not deleted |
| createdAt | Long | epoch millis |

### `PlanExercise` — one row of the plan table
| Column | Type | Notes |
|---|---|---|
| id | Long PK | auto |
| planId | Long FK | → WorkoutPlan, cascade delete |
| exerciseId | Long FK | → Exercise |
| orderIndex | Int | row order in the table |
| sets | Int | 1–10 |
| target | Int | reps, or seconds if `targetType = TIME` |
| targetType | Enum | REPS / TIME (e.g. Plank = 45 s) |
| weightKg | Double? | null = bodyweight |
| restSeconds | Int | default 90 |

### `WorkoutSession`
| Column | Type | Notes |
|---|---|---|
| id | Long PK | auto |
| planId | Long FK | → WorkoutPlan |
| startedAt | Long | |
| finishedAt | Long? | **null = in progress → resume on launch** |
| rating | Enum? | AWESOME / GOOD / TOUGH (set on finish screen) |

`planId` is nullable (`SET_NULL` on plan delete) and `planName` is copied in, so history survives a deleted plan.

### `SetLog` — one bubble on the workout screen
| Column | Type | Notes |
|---|---|---|
| id | Long PK | auto |
| sessionId | Long FK | → WorkoutSession, cascade delete |
| exerciseId, exerciseName, exerciseOrder | | copied from the plan row (no FK to `PlanExercise`, so editing a plan never touches history) |
| setNumber | Int | 1..sets |
| targetType, restSeconds | | copied from the plan row |
| targetValue | Int | copied from plan at session start |
| actualValue | Int? | what the user really did (editable) |
| weightKg | Double? | |
| completedAt | Long? | **null = not done**, set on tap |

**Why snapshot the plan into `SetLog` at session start?** If the user edits a plan later, past history stays accurate.

---

## 4. Default plans (seeded from Kotlin, not JSON)

Default data lives in `DefaultPlans.kt` as plain Kotlin lists — the same shape as the in-app table. `DatabaseSeeder` runs from `RoomDatabase.Callback.onOpen()` on every launch and inserts only the exercises and default plans that are missing (matched by name), so plans added in an app update appear on existing installs without touching user data.

```kotlin
val beginnerFullBody = PlanSeed(
    name = "Beginner Full Body", level = BEGINNER,
    rows = listOf(
        Row("Bodyweight Squat", sets = 3, reps = 12, rest = 60),
        Row("Push-up",          sets = 3, reps = 10, rest = 60),
        Row("Glute Bridge",     sets = 3, reps = 15, rest = 45),
        Row("Plank",            sets = 3, seconds = 30, rest = 45),
    )
)
```

Rules:
- Default rows have `isDefault = true`. Users can't delete them, only **Duplicate → edit**.
- User data is never wiped (no destructive migration). Schema changes use Room migrations; schemas are exported to `app/schemas/`.

### Starter plans

**Beginner Full Body** (no equipment)
| Exercise | Sets | Reps / Time | Rest |
|---|---|---|---|
| Bodyweight Squat | 3 | 12 | 60 s |
| Push-up (knees allowed) | 3 | 10 | 60 s |
| Glute Bridge | 3 | 15 | 45 s |
| Plank | 3 | 30 s | 45 s |

**Push Day** (gym)
| Exercise | Sets | Reps | Weight | Rest |
|---|---|---|---|---|
| Bench Press | 4 | 8 | 40 kg | 120 s |
| Overhead Press | 3 | 10 | 25 kg | 90 s |
| Incline Dumbbell Press | 3 | 10 | 14 kg | 90 s |
| Tricep Pushdown | 3 | 12 | 20 kg | 60 s |

**Pull Day** (gym)
| Exercise | Sets | Reps | Weight | Rest |
|---|---|---|---|---|
| Lat Pulldown | 4 | 10 | 40 kg | 90 s |
| Seated Cable Row | 3 | 10 | 35 kg | 90 s |
| Dumbbell Curl | 3 | 12 | 10 kg | 60 s |
| Face Pull | 3 | 15 | 15 kg | 60 s |

**Leg Day** (gym)
| Exercise | Sets | Reps | Weight | Rest |
|---|---|---|---|---|
| Barbell Squat | 4 | 8 | 50 kg | 120 s |
| Romanian Deadlift | 3 | 10 | 40 kg | 120 s |
| Leg Press | 3 | 12 | 80 kg | 90 s |
| Calf Raise | 3 | 15 | — | 45 s |

**20-min Core** (no equipment)
| Exercise | Sets | Reps / Time | Rest |
|---|---|---|---|
| Crunch | 3 | 15 | 30 s |
| Leg Raise | 3 | 12 | 30 s |
| Russian Twist | 3 | 20 | 30 s |
| Side Plank (each side) | 2 | 30 s | 30 s |

### Dumbbell-only plans (no bench, ~30 min each)

Chosen for training at home with dumbbells only. The floor press replaces the bench press, and the floor limits elbow depth, which is easier on the shoulders. Each plan opens with its heaviest exercise (longer rest) and moves to isolation work (shorter rest). Weights are per hand, aimed at beginner to early intermediate, and can be changed mid-workout by holding a set bubble.

**Dumbbell Chest** (~27 min)
| Exercise | Sets | Reps | kg | Rest |
|---|---|---|---|---|
| Dumbbell Floor Press | 4 | 10 | 10 | 90 s |
| Dumbbell Floor Fly | 3 | 12 | 6 | 60 s |
| Dumbbell Pullover (on floor) | 3 | 12 | 8 | 60 s |
| Decline Push-up (feet raised) | 3 | 10 | — | 45 s |
| Incline Push-up (hands raised) | 3 | 15 | — | 45 s |

**Dumbbell Shoulders** (~31 min)
| Exercise | Sets | Reps | kg | Rest |
|---|---|---|---|---|
| Standing Dumbbell Shoulder Press | 4 | 10 | 8 | 90 s |
| Arnold Press | 3 | 10 | 6 | 60 s |
| Lateral Raise | 3 | 15 | 4 | 45 s |
| Bent-over Reverse Fly | 3 | 15 | 4 | 45 s |
| Front Raise | 3 | 12 | 4 | 45 s |
| Dumbbell Shrug | 3 | 15 | 12 | 45 s |

**Dumbbell Arms** (~29 min, biceps and triceps alternate)
| Exercise | Sets | Reps | kg | Rest |
|---|---|---|---|---|
| Dumbbell Curl | 3 | 12 | 8 | 60 s |
| Overhead Triceps Extension | 3 | 12 | 8 | 60 s |
| Hammer Curl | 3 | 12 | 8 | 60 s |
| Diamond Push-up | 3 | 10 | — | 60 s |
| Concentration Curl (each arm) | 3 | 10 | 6 | 45 s |
| Triceps Kickback (each arm) | 3 | 12 | 4 | 45 s |

**Core Strength** (~28 min, no equipment: dumbbells balanced on the floor were impractical, and bodyweight core work is just as effective)
| Exercise | Sets | Reps / Time | Rest |
|---|---|---|---|
| Bicycle Crunch | 3 | 20 | 45 s |
| Reverse Crunch | 3 | 15 | 45 s |
| Dead Bug (each side) | 3 | 10 | 45 s |
| Hollow Body Hold | 3 | 30 s | 45 s |
| Mountain Climber | 3 | 30 s | 45 s |
| Plank | 3 | 45 s | 45 s |
| Side Plank (each side) | 2 | 30 s | 30 s |

Each default exercise also ships with 4 numbered instruction steps (e.g. Push-up: *1. Hands under shoulders. 2. Body in a straight line. 3. Lower chest to a fist's height from the floor. 4. Push back up, breathe out.*).

---

## 5. Screens

### 5.1 Plan list (home)
```
┌───────────────────────────────┐
│ WorkoutPlanner          [＋]  │
│ ▶ Resume: Push Day (7/15)     │  ← only if a session is unfinished
│───────────────────────────────│
│ Beginner Full Body   ★ default│
│ 4 exercises · ~25 min         │
│ Push Day             ★ default│
│ My Arm Day                    │
└───────────────────────────────┘
```
Tap a plan → Plan detail. `＋` → Plan editor (new plan). Long-press → Duplicate / Edit / Delete.

### 5.2 Plan detail
The plan shown as a read-only table, with each exercise expandable to show its instructions, and a big **Start workout** button.

### 5.3 Plan editor — table input
The main way to put plans into the app. It looks and behaves like a spreadsheet.

```
┌──────────────────────────────────────────────────┐
│ Plan name: [ My Arm Day                     ]    │
│ Level:     ( Beginner ▾ )                        │
├────┬───────────────────┬──────┬───────┬─────┬────┤
│ ≡  │ Exercise          │ Sets │ Reps  │ kg  │Rest│
├────┼───────────────────┼──────┼───────┼─────┼────┤
│ ≡  │ Dumbbell Curl   ▾ │ − 3 +│ − 12 +│ 10  │ 60 │
│ ≡  │ Hammer Curl     ▾ │ − 3 +│ − 10 +│ 10  │ 60 │
│ ≡  │ Plank           ▾ │ − 3 +│ ⏱ 45s │  —  │ 45 │
├────┴───────────────────┴──────┴───────┴─────┴────┤
│            [ ＋ Add exercise row ]               │
└──────────────────────────────────────────────────┘
                                   [ Save plan ]
```

Behaviour:
- **Exercise cell**: a text field with autocomplete from the `Exercise` library. Typing a new name creates a new exercise, with optional instructions.
- **Sets / Reps cells**: `−` / `+` steppers so the keyboard isn't needed. Tapping the number lets you type it directly.
- **Reps ↔ Time toggle**: tapping the ⏱ icon switches the row to seconds, e.g. for planks.
- **kg cell**: decimal keyboard. Empty means bodyweight.
- **Rest cell**: picker with 30 / 45 / 60 / 90 / 120 / 180 s.
- **Row actions** (last column): ▲ / ▼ to reorder, 🗑 to delete with an Undo snackbar.
- **Validation**: the plan needs a name and at least one row, and sets/reps must be ≥ 1. Invalid cells get a red outline.
- On phone width, the table scrolls horizontally if needed, and the Exercise column stays pinned.

### 5.4 Workout screen — the core experience
```
┌───────────────────────────────┐
│ Push Day          ⏱ 18:42     │
│        ╭───────────╮          │
│        │   7 / 15  │          │  ← progress ring
│        │   sets    │          │
│        ╰───────────╯          │
│ Volume 1,170 kg · 2 PRs       │
│───────────────────────────────│
│ ✓ Bench Press — 4 sets done   │  ← collapsed card
│───────────────────────────────│
│ Overhead Press   25 kg        │  ← current card, expanded
│ (10)  (10)  (  )              │
│  ●✓    ●✓    ○                │  ← set bubbles
│ ▸ How to do it                │
│───────────────────────────────│
│ Incline DB Press  (next)      │
└───────────────────────────────┘
```

**Set bubble interactions**
| Gesture | Result |
|---|---|
| Tap empty bubble | Fill animation from the centre, ✓ draws in, haptic tick, `SetLog.completedAt = now`, rest timer starts |
| Long-press bubble | Opens a stepper to edit actual reps and weight, e.g. "did 8, not 10" |
| Tap filled bubble | Undo, with a confirm snackbar |
| PR beaten | Gold 🏆 badge pops onto the bubble |

**Progress**
- Top ring = completed sets ÷ total sets (`Canvas.drawArc`, animated).
- When an exercise's last set is done, its card **collapses** into a green summary, and the next card expands and scrolls into view (`AnimatedVisibility`, `animateContentSize`).
- Live stats: elapsed time, total volume (Σ reps × kg) and PR count.

**Rest timer**
- After a set, the next empty bubble becomes a countdown ring (e.g. 90 → 0).
- When it reaches 0: vibration, an optional sound, and the bubble pulses.
- "+30 s" and "Skip" buttons.
- v1: timer runs while the app is open. v2: `RestTimerService` (foreground service + ongoing notification) keeps it running in the background.
- The screen stays on during a workout (`FLAG_KEEP_SCREEN_ON`).

### 5.5 Finish screen
Confetti, then a summary card: duration, sets, volume, PRs, current streak. The user picks a rating (Awesome / Good / Tough) and taps **Save** to set `finishedAt`.

### 5.6 History
Opened from the yellow **HISTORY** button in the home header. Every workout saved on the finish screen is listed; discarded workouts are deleted and unfinished ones stay in the Resume banner.

- **Stats**: total workouts, current streak (consecutive days ending today or yesterday), and workouts this week (since Monday).
- **Heatmap**: the last 12 weeks, one column per week, Monday at the top. Red Orange = 1 workout that day, Yellow Punk = 2+, today outlined.
- **Past workouts**: newest first, with date and time, plan, duration, sets done/total, volume and rating emoji.
- **Workout details** (tap a workout): summary tiles, then each exercise as a table of Set · Planned · Done · kg. Skipped sets are marked, and reps below plan show in Red Orange. ⋮ → *Delete from history*.
- **"Last time" hints**: during a workout, each unfinished exercise card shows what was done for that exercise in the most recent earlier workout, e.g. "Last time: 3×10 @ 40 kg" or "10, 10, 8 @ 40 kg". Matched by exercise, so it carries across different plans.

### 5.7 Sounds
Each milestone has its own sound, so you can tell what happened without looking. Tones are synthesised in code (`sound/SoundPlayer.kt`: sine partials with a fast attack and exponential decay), so there are no audio files or licences, and everything works offline. They play on the **media** stream, following media volume and mixing over music.

| Moment | Sound |
|---|---|
| Set ticked | quick bright "tr-ing" (grace note → E6) |
| Every set of an exercise done | rising chime, C6 → G6 |
| Each second of rest | soft woodblock clock tick |
| Last 3 seconds of rest | short beeps, "3, 2, 1" |
| Rest over | woody "ding-ding" (A5 twice) plus vibration |
| Last set of the workout done | fanfare: C5 E5 G5 → C6+E6 chord |

- One completion sound per tap; the biggest milestone wins (workout > exercise > set). Skipping rest is silent.
- **Sound settings** (⚙ on the home header, or ⋮ → Sound settings during a workout) has an *All sounds* switch, plus a switch and a ▶ preview for each sound. Settings are saved in SharedPreferences.
- The app doesn't count individual reps, so a finished set is the smallest moment that gets a sound.

### 5.8 Reminders
Opened from the 🔔 button on the home header. Both reminders are off by default and save immediately (SharedPreferences). The daily alarm rings every day; the smart reminder is skipped on a day a workout has already been finished. Each has a **Test in 1 min** button that fires it through the real alarm path, so it can be checked on the phone with the app closed.

| Reminder | When | How it looks |
|---|---|---|
| **Daily alarm** | A time the user picks (Material time picker, default 6:00 PM), every day | `AlarmService` (a short foreground service) loops the phone's alarm tone with `MediaPlayer` on the **alarm** stream, with vibration, so it rings even on silent or vibrate. It stops on **Snooze 10 min**, **Dismiss**, tapping the notification (opens the app), or after 10 min. Shows the alarm-clock icon in the status bar (`setAlarmClock`). |
| **Smart reminder** | Every day at **1 hour before the time of day the last finished workout started** (last workout 5:30 PM → 4:30 PM), until the next workout moves it | A normal notification: "Workout in an hour?" with the current streak, if any. The dialog shows the next time it will fire. |

Rules:
- **Why the app plays the tone itself:** a notification channel's sound can be muted by silent mode, phone-maker rules or Android 15's notification cooldown, so the alarm channel is silent and `AlarmRinger` plays the tone.
- Android only lets a background app start a foreground service from an **exact** alarm. Without exact-alarm permission, the tone plays from the receiver for 45 s instead (the most Android allows a receiver), with the same notification.
- `ReminderScheduler` sets alarms with `AlarmManager`. They are exact when the user allows *Alarms & reminders* (Android 12+). That permission starts **off** for new installs on Android 14+, so turning the daily alarm on opens the setting. Without it, alarms use `setWindow` and land within 10 minutes of the set time (a plain inexact alarm a day ahead can be hours late).
- The dialog shows the next time each reminder will fire, and warns (with a shortcut) when notifications are blocked, exact alarms are off, or battery optimisation is on. Phone makers' battery savers (Samsung, Xiaomi, Oppo, Vivo, Huawei) can drop alarms once the app is swiped away.
- Finishing or deleting a workout reschedules the smart reminder and clears any reminder still on screen.
- Alarms are lost on reboot, so `ReminderReceiver` sets them again after boot, an app update, a clock or time-zone change, and when exact-alarm permission changes.
- On Android 13+, turning a reminder on asks for notification permission. If notifications are blocked, the dialog says so and links to the app's notification settings.

---

## 6. Persistence and resume rules

1. **Start workout**: insert a `WorkoutSession`, plus one `SetLog` per set copied from the plan.
2. **Every tap or edit**: a single `UPDATE` on that `SetLog`, done immediately on a background coroutine. There is no "save" button mid-workout.
3. **App killed or closed**: on launch, a query for `WorkoutSession WHERE finishedAt IS NULL` shows the **Resume** banner.
4. **Only one active session at a time.** Starting a new one while another is unfinished asks the user to resume or discard it.
5. **Finish**: set `finishedAt`.

---

## 7. Permissions

| Permission | When | Why |
|---|---|---|
| `VIBRATE` | v1 | haptics, rest-timer alert |
| `POST_NOTIFICATIONS` | v3 ✅ | reminders, later the rest-timer notification (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | v3 ✅ | reminders ring on time (user can deny; falls back to inexact) |
| `RECEIVE_BOOT_COMPLETED` | v3 ✅ | set reminders again after a reboot |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | v3 ✅ | keep the daily alarm ringing until it is dismissed |
| `FOREGROUND_SERVICE` (+ type) | v2 | background rest timer |
| `health.WRITE_EXERCISE` | v4 (optional) | Health Connect export |

No `INTERNET` permission. The app is fully offline.

---

## 8. Roadmap

| Version | Scope |
|---|---|
| **v1** ✅ | Room schema, Kotlin seed with 9 default plans, plan list, plan detail, **table plan editor**, workout screen with set bubbles + ring + collapsing cards, in-app rest timer, finish screen, resume |
| **v2** | ✅ History (list, details, heatmap, streak), ✅ "last time" hints, ✅ sounds + sound settings. Still to do: background rest timer service, PR detection |
| **v3** | ✅ Reminders (daily alarm + smart reminder). Still to do: weekly schedule + "Today" screen |
| **v4** | Charts (weight over time), Health Connect export, backup/restore to a file, kg/lb setting, polish |

---

## 9. Open questions

- Should default plans be editable in place, or only via Duplicate? (Current design: Duplicate only.)
- Should reps be allowed to differ per set in the editor (e.g. 12/10/8), or only during the workout? (Current design: only during the workout.)
- Should exercises have images in v1? Text instructions are enough for v1; images could come from the public-domain [free-exercise-db](https://github.com/yuhonas/free-exercise-db) later.
