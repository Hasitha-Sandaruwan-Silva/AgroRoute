import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from "@firebase/rules-unit-testing";
import { doc, setDoc, updateDoc } from "firebase/firestore";
import { after, before, beforeEach, test } from "node:test";

const projectId = "demo-agroroute-rules";
const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
let testEnvironment;

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: await readFile(path.join(rootDir, "firestore.rules"), "utf8")
    }
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "users/farmer-1"), {
      userId: "farmer-1",
      role: "FARMER"
    });
    await setDoc(doc(db, "users/farmer-2"), {
      userId: "farmer-2",
      role: "FARMER"
    });
    await setDoc(doc(db, "listings/listing-1"), {
      listingId: "listing-1",
      farmerId: "farmer-1",
      cropName: "Rice",
      quantity: 10,
      availableQuantity: 10,
      soldQuantity: 0,
      reservePrice: 8,
      status: "OPEN",
      biddingEnabled: true,
      isDraft: false
    });
  });
});

after(async () => {
  await testEnvironment?.cleanup();
});

test("listing owner can edit cropName on their existing listing", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertSucceeds(updateDoc(doc(db, "listings/listing-1"), {
    cropName: "Brown rice"
  }));
});

test("another Farmer cannot edit the listing", async () => {
  const db = testEnvironment.authenticatedContext("farmer-2").firestore();

  await assertFails(updateDoc(doc(db, "listings/listing-1"), {
    cropName: "Changed by another Farmer"
  }));
});

test("listing owner cannot change listing ownership through edit", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertFails(updateDoc(doc(db, "listings/listing-1"), {
    farmerId: "farmer-2"
  }));
});