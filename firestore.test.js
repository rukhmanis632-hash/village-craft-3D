const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Explorer Leo",
    coins: 24180,
    rubies: 142,
    selectedWorld: 0,
    selectedChar: "leo",
    selectedBoard: "solar",
    challengeDist: 1200,
    challengeGems: 0,
    magnetLevel: 2,
    shieldLevel: 2,
    boostLevel: 1,
    multiplierLevel: 3,
    highScoreW0: 98420,
    highScoreW1: 64200,
    highScoreW2: 51800,
    highScoreW3: 42900,
    createdAt: now,
    updatedAt: now,
  };
}

function validRunRecordPayload(uid, now) {
  return {
    userId: uid,
    runnerName: "Explorer Leo",
    worldId: 0,
    characterId: "leo",
    score: 25000,
    distanceMeters: 1620,
    coinsCollected: 142,
    rubiesCollected: 2,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write profiles or run records", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("run_records").get());
});

test("Authenticated user: can create, read, and update their own PlayerProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 30000,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read or modify another user's PlayerProfile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID, now));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ coins: 0, updatedAt: new Date() }));
});

test("Shadow Update & Value Poisoning: rejects ghost fields and invalid types on PlayerProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const withGhost = Object.assign({}, validProfilePayload(ALICE_UID, now), { isAdmin: true });
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(withGhost));

  const badType = Object.assign({}, validProfilePayload(ALICE_UID, now), { coins: "tons" });
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badType));
});

test("Authenticated user: can create and query own RunRecords with matching userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("run_records").doc("run_1").set(validRunRecordPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("run_records").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails RunRecords query without userId filter and cannot read Bob's RunRecord", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("run_records").doc("bob_run").set(validRunRecordPayload(BOB_UID, now));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("run_records").get());
  await assertFails(aliceDb.collection("run_records").doc("bob_run").get());
});
