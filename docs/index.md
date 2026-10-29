---
  layout: default.md
  title: ""
---

# CoordiMate

[![CI Status](https://github.com/AY2627S1-CS2103T-W12-3/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-W12-3/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-W12-3/tp/branch/master/graph/badge.svg)](https://codecov.io/gh/AY2627S1-CS2103T-W12-3/tp)

**Your CCA committee's contacts, events, and attendance, in one place.**

CoordiMate is a desktop app for university CCA EXCO members. Keep everyone your committee works with, plan events
and their members, and take attendance, all by typing short commands. A visual interface shows the results as you
work, and everything is saved on your own computer.

![CoordiMate main window](images/Ui.png)

<div class="text-center">

[**Download the latest release**](https://github.com/AY2627S1-CS2103T-W12-3/tp/releases) ·
[**Get started**](UserGuide.html#quick-start) · [**See all commands**](UserGuide.html#command-summary)

</div>

---

## Why CoordiMate?

**Everyone your committee works with**<br>
Save members, sponsors, and university staff with their roles, organisations, birthdays, and notes. Group them with
tags, find them with `search` and `filter`, and see at a glance who is [on leave](UserGuide.html#marking-a-contact-as-on-leave).

**Events, members, and attendance**<br>
Create events, assign members, and mark each member present or absent. The **Events** tab shows who has and hasn't
been marked, and [reminders](UserGuide.html#automatic-event-reminders) appear the day before an event starts.

**Built for handovers**<br>
Archive past events and former contacts instead of deleting them, so the next committee keeps the history. Data is
stored in a single, human-readable file that you can back up or pass on.

**Fast for people who type**<br>
Every action is a short command, and you can move through cards, switch views, and recall earlier commands without
touching the mouse. New users can follow the [built-in tutorial](UserGuide.html#taking-the-tutorial-tutorial), which
opens on the first launch.

---

## Get started

1. Make sure Java `25` or later is installed on your computer.
1. Download the latest `.jar` file from the [releases page](https://github.com/AY2627S1-CS2103T-W12-3/tp/releases).
1. Run it from a terminal with `java -jar` followed by the file name, and follow the tutorial.

The [**User Guide**](UserGuide.html) explains every command, and its [_Quick start_](UserGuide.html#quick-start)
section has step-by-step setup instructions.

## For developers

Interested in how CoordiMate works, or in contributing? The [**Developer Guide**](DeveloperGuide.html) describes its
design and implementation, and the source code is on
[GitHub](https://github.com/AY2627S1-CS2103T-W12-3/tp). Meet the team on the [About Us](AboutUs.html) page.

**Acknowledgements**

* CoordiMate is based on [AddressBook-Level3](https://se-education.org/addressbook-level3/), created by the
  [SE-EDU initiative](https://se-education.org).
* Libraries used: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson),
  [JUnit 5](https://github.com/junit-team/junit5)
