# Little Steps

A simple Android app for the small things you want to do more often: read a few pages, go for a walk, drink water, or practise a language.

Add an activity, mark what you did today, and look back at your progress in the calendar. Start with just a name. Add a goal or a reminder when you need one.

**Works offline · No account needed · English and Russian · Android 10+**

[Download the test APK](https://github.com/sturivny/little-steps/raw/refs/heads/main/downloads/LittleSteps-MVP-debug.apk) · [Русская версия](README.ru.md) · [Full picture guide](docs/walkthrough.md)

## What does it look like?

These are real screens from the app with example activities and demonstration history.

| Your day at a glance | Look back over the month | Keep track of a quantity |
|:---:|:---:|:---:|
| <img src="docs/screenshots/today.png" alt="Today with reading, walking, water and language activities; one of four complete" width="240"> | <img src="docs/screenshots/calendar.png" alt="Expanded monthly calendar with colored dots showing completed activities" width="240"> | <img src="docs/screenshots/quantity.png" alt="Reading quantity dialog showing a total of 12 pages for the selected day" width="240"> |
| Tap the circle beside an activity to mark it done. Tap again to undo. | Tap a date to see that day's activities. Colored dots show completed activities. | Hold a measured activity's circle to enter pages, glasses, minutes, or another amount. |

## Start simple

1. Tap **+**, type something like “Go for a walk”, then tap **Add**. The app picks an icon and color.
2. Do your activity and tap its circle. That's enough to start tracking it.
3. Want more detail? Tap the activity's name, then open **Advanced settings** for a daily goal, units, days of the week, or a reminder.

| Add only a name | Set a goal when you want one | Switch to Russian |
|:---:|:---:|:---:|
| <img src="docs/screenshots/add-activity.png" alt="New activity dialog with only the name Stretch for 5 minutes entered" width="240"> | <img src="docs/screenshots/goals.png" alt="Activity settings with a daily reading goal and custom units" width="240"> | <img src="docs/screenshots/russian.png" alt="Today screen with the interface translated into Russian" width="240"> |
| No schedule or goal is required to create an activity. | For example, aim for 20 pages a day and enter your actual total. | Open **Settings → Language → Русский**. Your activity names stay as you wrote them. |

A measured activity can be partly done: **12 / 20 pages** records progress without marking the goal complete. A quick tap sets the total to the goal; another tap clears it.

Use **Reorder** to drag activities into your preferred order. The order saves when you let go. Expand the calendar to switch from a week to a month, or use the arrows to browse earlier dates. Future days are read-only.

[See more pictures: past days, reordering, and reminders →](docs/walkthrough.md)

## Try it on your phone

1. Download [LittleSteps-MVP-debug.apk](https://github.com/sturivny/little-steps/raw/refs/heads/main/downloads/LittleSteps-MVP-debug.apk) on an Android 10+ phone.
2. Open the file. If Android asks, allow your browser or file manager to install this app.
3. Open **Little Steps** and add your first activity.

This is an early test build, signed with an Android debug certificate. It is not a store release. The APK was built on October 31, 2026; the screenshot examples were captured on November 8, 2026.

## What to expect from this version

- Activities and daily totals are stored on your device. No account, backend, or cloud sync is required. Android's system backup remains enabled.
- Reminders are approximate. Android battery settings may delay them, and Android 13+ asks for notification permission. Physical vibration and timed reminders still need checking on a real phone.
- Changing a goal, unit, or schedule also affects how past entries are shown and evaluated. The recorded quantities remain stored.
- Archiving removes an activity from today onward and keeps earlier records. There is no unarchive screen yet.

For the technical details, see [building and testing](docs/development.md) and [recorded verification results](docs/verification.md).
