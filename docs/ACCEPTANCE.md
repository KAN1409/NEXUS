# NEXUS Milestone Acceptance

An update is not complete merely because it compiles.

## Required gates
1. Build is green.
2. App installs as an update over the currently installed NEXUS.
3. Existing data remains available.
4. No package/signing regression.
5. No crash on launch.
6. Main changed flow is tested on-device.
7. UI matches the approved NEXUS visual language.
8. No placeholder copy or dead controls remain in the milestone scope.

## Update 5 device acceptance
Verify all of the following:
- Ready count reflects actions waiting for approval.
- Approve changes action state.
- Later defers without immediately regenerating the same action.
- Dismiss rejects and keeps that decision.
- Approved action can enter Start / in-progress state.
- In-progress action can Complete or Fail.
- Activity shows the lifecycle state clearly.
- Existing Memory and Interests remain intact after install.
