# HelpDesk

A native Android help desk app where students report campus issues and staff resolve them. Built with Kotlin, Jetpack Compose and Firebase for the Mobile App Development module.

Package: `com.example.helpdesk`

## Features

- **Three roles:** Student, Staff and Admin, each with its own screens
- **Tickets:** subject, description, category (Electrical, Plumbing, IT/Wifi, Furniture, Other) and an optional photo from the camera or gallery
- **Claiming:** a staff member claims a ticket, and only they can edit it; other staff see it as read-only
- **Live sync:** all data updates in real time across devices through Firestore listeners
- **Chat:** a per-ticket conversation between the student and the staff member
- **Ratings:** students rate resolved tickets from 1 to 5 stars
- **Notifications:** an in-app notification list with a bell badge, plus Android system notifications
- **Dashboard stats:** an animated donut chart and resolution rate
- **Admin tools:** create users, enable or disable accounts, send password reset emails
- **Themes:** dark mode and five accent colours, saved on the device
- **Permissions:** camera, storage and notifications, each with an explanation dialog and a Settings shortcut if blocked

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Auth | Firebase Authentication (email and password) |
| Database | Cloud Firestore |
| Async | Kotlin coroutines and Flow |
| Build | Gradle (Kotlin DSL) with a version catalog |

Build settings in use: `minSdk 25`, `targetSdk 36`, `compileSdk 37`.

## Project structure

```
app/src/main/java/com/example/helpdesk/
├── data/
│   ├── AuthRepository.kt          Login, sign up, roles, admin user management
│   ├── Ticket.kt                  Ticket model
│   ├── TicketRepository.kt        Ticket and chat reads/writes (Flow-based)
│   ├── Notification.kt            Notification model
│   ├── NotificationRepository.kt  Notification reads/writes
│   └── ChatMessage.kt             Chat message model
├── LoginActivity.kt               Entry point, Enter-key navigation
├── SignUpActivity.kt              Account creation (Student or Staff)
├── MainActivity.kt                Student app: Home, My Issues, Report, Profile tabs
├── StaffDashboardActivity.kt      Staff ticket list, stats, category filters
├── TicketDetailActivity.kt        Claim, status, reply (staff)
├── ChatActivity.kt                Ticket conversation
├── NotificationsActivity.kt       Notification list
├── AdminDashboardActivity.kt      User management
├── UiComponents.kt                Shared UI: header, cards, pills, charts, tab bar
├── ThemeManager.kt                Theme state, palettes, theme picker
├── ImageUtils.kt                  Photo compression and Base64 conversion
└── NotificationWatcher.kt         Turns new Firestore notifications into system notifications
```

## Setup

### 1. Open the project

Open the project folder in Android Studio and let Gradle sync.

### 2. Create the Firebase project

1. Go to the [Firebase Console](https://console.firebase.google.com) and create a project.
2. Add an **Android app** with the package name `com.example.helpdesk`.
3. Download `google-services.json` and place it in the `app/` folder.
4. Do not publish `google-services.json` in a public repository if you want to keep your project private.

### 3. Enable the Firebase services

- **Authentication:** Sign-in method, enable **Email/Password**.
- **Firestore Database:** create a database in any region.

### 4. Publish the security rules

In Firestore, open **Rules**, paste the following and publish:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() { return request.auth != null; }
    function userDoc() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
    }
    function isStaff() { return signedIn() && userDoc().role == 'STAFF'; }
    function isAdmin() { return signedIn() && userDoc().role == 'ADMIN'; }

    match /users/{uid} {
      allow read: if signedIn();
      allow create: if (signedIn() && request.auth.uid == uid
                        && request.resource.data.role in ['STUDENT', 'STAFF'])
                    || isAdmin();
      allow update: if isAdmin();
      allow delete: if false;
    }

    match /tickets/{ticketId} {
      allow read: if isStaff() || isAdmin()
                  || resource.data.studentId == request.auth.uid;
      allow create: if signedIn()
                    && request.resource.data.studentId == request.auth.uid;
      allow update: if
        (isStaff() && (
           resource.data.get('assignedStaffEmail', null) == null
           || resource.data.assignedStaffEmail == request.auth.token.email))
        || (resource.data.studentId == request.auth.uid
            && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['rating']));
      allow delete: if false;

      match /messages/{messageId} {
        allow read, create: if isStaff()
          || get(/databases/$(database)/documents/tickets/$(ticketId)).data.studentId == request.auth.uid;
        allow update, delete: if false;
      }
    }

    match /notifications/{id} {
      allow read: if signedIn() && resource.data.userId == request.auth.uid;
      allow create: if signedIn();
      allow update: if signedIn() && resource.data.userId == request.auth.uid
                    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['read']);
      allow delete: if false;
    }
  }
}
```

### 5. Create the Firestore indexes

Two queries need composite indexes:

| Collection | Fields |
|---|---|
| `tickets` | `studentId` ascending, `createdAt` descending |
| `notifications` | `userId` ascending, `createdAt` descending |

The easiest way is to run the app. When Firestore needs an index, the Logcat error contains a link that creates it in one click. Wait until the index status shows **Enabled** before testing again.

### 6. Create the first Admin

Public sign-up only creates Students and Staff. To create your first Admin:

1. Sign up normally in the app.
2. In the Firebase Console, open Firestore, then `users`, then your document.
3. Change the `role` field to `ADMIN`.

After that, the Admin can create other users from the app.

### 7. Run

Run the app on a physical device or an emulator. On a phone, uninstall any older build first so the manifest permissions register correctly.

## Firestore data model

```
tickets/{ticketId}
  studentId, subject, description, category, status (OPEN | IN_PROGRESS | RESOLVED),
  attachmentUrl (Base64 photo), staffReply, assignedStaffEmail, rating, createdAt
  └── messages/{messageId}
        senderId, senderLabel, text, createdAt

users/{uid}
  uid, email, role (STUDENT | STAFF | ADMIN), active

notifications/{id}
  userId, message, ticketId, read, createdAt
```

## Permissions

| Permission | Used for |
|---|---|
| `CAMERA` | Taking a photo of the issue |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (API 32 and below) | Attaching an existing photo |
| `POST_NOTIFICATIONS` (Android 13 and above) | System notifications for ticket activity |

## Design decisions

- **Firestore instead of SQLite.** The project started on local SQLite with Room. Two phones cannot share a local database, so it moved to Firestore for shared, real-time data.
- **Repository pattern.** Screens never call Firebase directly. They use `TicketRepository`, `AuthRepository` and `NotificationRepository`, which expose Kotlin `Flow`s.
- **Photos as Base64.** Images are resized to 800 px wide and compressed to JPEG, then stored inside the ticket document. This avoids Firebase Storage, which needs the paid Blaze plan.
- **Notifications without FCM.** `NotificationWatcher` listens to the signed-in user's notification documents and posts a system notification for each new one. Real push needs a server, which also needs the Blaze plan.
- **Admin creating users.** `AuthRepository.createUserAsAdmin` uses a second, temporary `FirebaseApp` instance, so creating an account does not sign the Admin out.

## Known limitations

- Admins can disable accounts but cannot delete a login. Deleting a Firebase Auth user needs the Admin SDK on a server.
- System notifications are delivered while the app is running or in the background, not after a force-close.
- Photos are limited by Firestore's 1 MB document size.
- `firebase-messaging` is declared in `build.gradle.kts` but not used yet. It is reserved for a future FCM push implementation.

## Troubleshooting

| Problem | Fix |
|---|---|
| `PERMISSION_DENIED` in Logcat | Check that the rules are published, the user has a `users/{uid}` document, and the account is not disabled |
| "The query requires an index" | Open the link in the error message and create the index, then wait for it to finish building |
| Camera permission missing in Settings | Uninstall the app and reinstall it so the manifest is re-registered |
| Build says "Kotlin daemon crashed" | Run the build again, or close and reopen Android Studio |
| Warning about compileSdk 37.1 | Harmless. The Android Gradle plugin has not been tested against that version yet |
| Notifications stop when minimised | Set the app to run in the background in the phone's battery settings |

## Testing with two devices

1. Install the same build on two phones.
2. Sign in as a Student on one and Staff on the other.
3. Submit a ticket as the student and check that staff receive a notification.
4. Claim the ticket, chat, change the status and resolve it.
5. Rate the ticket as the student.

Changes should appear on both devices without a manual refresh.
