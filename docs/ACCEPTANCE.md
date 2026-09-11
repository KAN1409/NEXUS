# NEXUS 3.0 ACCEPTANCE

NEXUS is not accepted because it compiles or looks polished. It is accepted only if it provides useful personal intelligence on the real device.

## Automated gates

All must pass on the final commit:

- Unit tests.
- Debug APK build.
- Android test APK build.
- Room migration tests for `1 → 3` and `2 → 3` with legacy data preserved.
- Repository regression tests including resolved/snoozed open-loop persistence.
- Emulator UI acceptance.
- CI validation artifact upload.

## Product-value gates

### 1. Direct request
Capture:

`Ahmed — Please send the quotation today`

Expected:
- Appears under **Needs you**.
- Reads as a reply/action required, not a generic theme.
- Shows source evidence.
- Offers useful next action(s), reminder and Done.

### 2. Payment + amount
Capture:

`CIB — payment of 5672 EGP is due today`

Expected:
- Payment open loop.
- CIB and amount are visible in useful context.
- Not presented as an “upcoming commitment” or generic percentage.

### 3. Related evidence → one Situation
Add another related CIB payment signal.

Expected:
- One CIB Situation connects the related evidence.
- Situation shows current state, what changed and evidence count.

### 4. Vague memory recall
Search:

`الحاجة اللي كان فيها 5672 جنيه`

Expected:
- Returns the CIB evidence containing 5672 EGP.
- Exact original wording is not required.

### 5. Resolution
Mark an open loop Done.

Expected:
- It immediately leaves **Needs you**.
- It remains searchable in Memory.
- It appears in finished/history state.
- Rebuilding intelligence must not resurrect it.

### 6. Reminder
Schedule a reminder from an open loop.

Expected:
- Android notification permission is requested only when needed.
- A local reminder is actually scheduled.
- The loop becomes snoozed until the wake time.
- Reminder delivery opens NEXUS when tapped.

### 7. Real action
Use an available contextual action.

Expected:
- Source-app/calendar/dialer/maps/copy action genuinely executes where Android supports it.
- Success/failure outcome appears in Activity.
- NEXUS must not claim an action succeeded when Android could not execute it.

### 8. Quiet state
When nothing unresolved is important:

Expected:
- Home says nothing important needs the user.
- It does not fill Home with themes, usage percentages or generic insights merely to avoid an empty screen.

## Final real-device gate

Install the candidate over the existing signed NEXUS using `UPDATE_LOCAL.sh` with no uninstall and no data clear. Confirm:

- version `3.0.0 (300)`;
- signer is unchanged;
- existing Memory/evidence is still present;
- no startup/crash regression;
- Home, Situations, Memory, Activity and Settings all operate on the migrated database;
- at least one real notification or saved item produces a useful result.

Only after these gates pass may the release be merged to `main` and called Done.
