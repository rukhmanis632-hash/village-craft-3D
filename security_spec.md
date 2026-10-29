# Horizon Dash Security Specification (Payload-First Security TDD)

## 1. Data Invariants
1. **PlayerProfile (`/users/{userId}`)**:
   - A player profile document ID (`userId`) must strictly equal `request.auth.uid` and pass `isValidId(userId)`.
   - `userId` and `createdAt` are immutable after creation.
   - Numeric progression fields (`coins`, `rubies`, `selectedWorld`, `challengeDist`, `challengeGems`, upgrade levels, and world high scores) must be bounded integers.
   - String fields (`displayName`, `selectedChar`, `selectedBoard`) must be bounded strings.
2. **RunRecord (`/run_records/{recordId}`)**:
   - Every run record must belong to the authenticated caller (`userId == request.auth.uid`).
   - `list` queries on `/run_records` must enforce `resource.data.userId == request.auth.uid`.
   - `userId` and `createdAt` are immutable on update.

## 2. The "Dirty Dozen" Payloads
1. Unauthenticated read on `/users/alice_123` -> REJECT
2. Unauthenticated list on `/run_records` -> REJECT
3. Cross-user profile read (`alice_123` reading `/users/bob_456`) -> REJECT
4. Cross-user run record read (`alice_123` reading Bob's `/run_records/rec_bob`) -> REJECT
5. Unfiltered list query (`alice_123` querying `/run_records` without `where("userId", "==", "alice_123")`) -> REJECT
6. Identity spoofing on create (`alice_123` creating `/run_records/rec_1` with `userId: "bob_456"`) -> REJECT
7. Ghost field injection (`isVerified: true` on `/users/alice_123`) -> REJECT
8. Value poisoning (`coins: "million"` string instead of integer) -> REJECT
9. Boundary overflow (`displayName` > 50 chars or `coins` < 0) -> REJECT
10. Immutable field mutation (modifying `createdAt` or `userId` during update) -> REJECT
11. Future timestamp spoofing (`createdAt` > `request.time`) -> REJECT
12. Invalid path ID characters (`recordId` with spaces or special characters) -> REJECT
