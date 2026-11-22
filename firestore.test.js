const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_learner_01";
const BOB_UID = "bob_learner_02";
const ADMIN_UID = "admin_founder_01";
// Admin access is granted via the admins/{uid} registry, not via any email.
const ADMIN_EMAIL = "founder@example.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "أحمد",
    activeLanguage: "EN",
    englishDialect: "en-US",
    spanishDialect: "es-ES",
    englishLevel: "A1",
    spanishLevel: "A1",
    englishSubLevel: 1,
    spanishSubLevel: 1,
    xp: 0,
    streakDays: 1,
    dailyGoalMinutes: 15,
    listeningMinutes: 0,
    completedLessonsCount: 0,
    role: "learner",
    createdAt: now,
    updatedAt: now,
  };
}

function validReviewCard(now = new Date(Date.now() - 2000)) {
  return {
    sentenceText: "Could I have a table near the window, please?",
    arabicTranslation: "هل يمكنني الحصول على طاولة بجانب النافذة، من فضلك؟",
    language: "EN",
    dialect: "en-US",
    cefrLevel: "A2",
    grammarNote: "استخدام Could I have للطلب المهذب في المطاعم.",
    masteryScore: 20,
    stabilityDays: 1.0,
    difficulty: 5.0,
    reviewCount: 0,
    lapseCount: 0,
    pronunciationScore: 0,
    nextReviewEpochMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
}

function validCmsItem(authorId, published = true, now = new Date(Date.now() - 2000)) {
  return {
    language: "ES",
    dialect: "es-ES",
    cefrLevel: "A1",
    unitNumber: 1,
    contentType: "sentence",
    title: "تحية الصباح في مدريد",
    bodyText: "Buenos días, ¿cómo está usted hoy?",
    arabicTranslation: "صباح الخير، كيف حالك اليوم؟",
    grammarNote: "استخدام usted للصيغة الرسمية المهذبة.",
    youtubeVideoId: "",
    published,
    authorId,
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("cmsContent").where("published", "==", true).get());
});

test("UserProfile: owner can create, read, and partially update own profile; cannot escalate role or spoof userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Reject self-assigned admin role on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID, created),
      role: "admin",
    })
  );

  // Reject extra ghost field (Shadow Update test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID, created),
      isVerified: true,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validProfile(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, created))
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 120,
      englishLevel: "A2",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject role escalation on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user isolation: Bob cannot read or modify Alice's profile
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ xp: 999, updatedAt: new Date(Date.now() - 1000) }));
});

test("Per-user subcollections: reviewCards, tutorMessages, examAttempts, pronunciationLogs, lessonProgress enforce owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  // ReviewCard create, list, partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reviewCards").doc("card_1").set(validReviewCard(now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reviewCards").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reviewCards").doc("card_1").update({
      masteryScore: 65,
      stabilityDays: 3.5,
      reviewCount: 1,
      nextReviewEpochMs: Date.now() + 86400000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reviewCards").get()
  );

  // TutorMessage append-only create & list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tutorMessages").doc("msg_1").set({
      scenarioId: "airport",
      language: "EN",
      sender: "user",
      text: "Where is boarding gate B12?",
      arabicExplanation: "",
      deepThinking: false,
      createdAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tutorMessages").doc("msg_1").update({
      text: "Modified",
    })
  );

  // ExamAttempt create & read
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("examAttempts").doc("exam_1").set({
      language: "EN",
      cefrLevel: "A1",
      examType: "placement",
      listeningScore: 85,
      speakingScore: 80,
      readingScore: 90,
      writingScore: 75,
      totalScore: 82,
      passed: true,
      aiFeedback: "أداء ممتاز يؤهلك للانتقال.",
      createdAt: now,
    })
  );

  // PronunciationLog create & read
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pronunciationLogs").doc("pron_1").set({
      language: "ES",
      dialect: "es-ES",
      targetSentence: "Necesito una habitación individual para dos noches.",
      recognizedText: "Necesito una habitación individual para dos noches",
      accuracyScore: 88,
      stressScore: 82,
      rhythmScore: 85,
      fluencyScore: 84,
      clarityScore: 90,
      arabicCoaching: "نطق ممتاز لحرف الراء وحروف العلة.",
      createdAt: now,
    })
  );

  // LessonProgress create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lessonProgress").doc("en_a1_u1_l1").set({
      language: "EN",
      cefrLevel: "A1",
      unitNumber: 1,
      lessonNumber: 1,
      contentType: "lesson",
      completed: true,
      score: 95,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lessonProgress").doc("en_a1_u1_l1").update({
      score: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("lessonProgress").doc("en_a1_u1_l1").get()
  );
});

test("CMS Content: admins-registry member can manage; email alone grants nothing; learners only query published content", async () => {
  // Admin membership is seeded in the admins/{uid} registry (console-managed
  // in production), bypassing rules like the Firebase console does.
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({ role: "admin" });
  });

  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  // A token carrying an email (even a verified one) is NOT an admins-registry
  // member, so it must get no admin access.
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Email-based spoof without admins-registry membership MUST fail
  await assertFails(
    spoofedAdminDb.collection("cmsContent").doc("cms_spoof").set(validCmsItem("spoof_uid", true))
  );

  // Regular learner cannot write CMS content
  await assertFails(
    aliceDb.collection("cmsContent").doc("cms_learner").set(validCmsItem(ALICE_UID, true))
  );

  // Verified admin creates published and draft items
  await assertSucceeds(
    adminDb.collection("cmsContent").doc("cms_pub_1").set(validCmsItem(ADMIN_UID, true))
  );
  await assertSucceeds(
    adminDb.collection("cmsContent").doc("cms_draft_1").set(validCmsItem(ADMIN_UID, false))
  );

  // Learner filtered query (published == true) succeeds; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("cmsContent").where("published", "==", true).get()
  );
  await assertFails(aliceDb.collection("cmsContent").get());
  await assertFails(aliceDb.collection("cmsContent").doc("cms_draft_1").get());

  // Admin can list all CMS content including drafts
  await assertSucceeds(adminDb.collection("cmsContent").get());
});

test("SavedStories & Opt-In Leaderboard: enforce owner isolation, opt-in write ownership, and public signed-in ranking read", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // SavedStories: Alice can save and read her own AI story; Bob cannot read or write it
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedStories").doc("story_1").set({
      language: "EN",
      cefrLevel: "B1",
      genreAr: "السفر • مولّدة بالذكاء الاصطناعي",
      titleAr: "موقف في مطار لندن",
      subtitleTarget: "Delayed Flight at Heathrow",
      storyJson: JSON.stringify({ turns: [{ speakerName: "Agent", text: "Hello", arabicTranslation: "مرحبا" }] }),
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedStories").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("savedStories").get()
  );

  // Leaderboard: unauthenticated denied; Alice can opt-in & update own entry; Bob can read leaderboard but cannot spoof Alice
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "أحمد",
      activeLanguage: "EN",
      activeLevel: "B1",
      xp: 420,
      streakDays: 5,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("leaderboard").get());
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "مخترق",
      activeLanguage: "EN",
      activeLevel: "C2",
      xp: 99999,
      streakDays: 99,
      updatedAt: now,
    })
  );
  // Alice can opt out (delete her own entry)
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).delete());
});
