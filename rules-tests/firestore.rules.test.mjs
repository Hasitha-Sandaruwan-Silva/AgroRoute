import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from "@firebase/rules-unit-testing";
import { deleteDoc, doc, getDoc, setDoc, updateDoc, writeBatch } from "firebase/firestore";
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
    await setDoc(doc(db, "users/supplier-1"), {
      userId: "supplier-1",
      role: "SUPPLIER"
    });
    await setDoc(doc(db, "users/supplier-2"), {
      userId: "supplier-2",
      role: "SUPPLIER"
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
    await setDoc(doc(db, "listings/listing-fixed"), {
      listingId: "listing-fixed",
      farmerId: "farmer-1",
      cropName: "Rice",
      quantity: 10,
      availableQuantity: 10,
      soldQuantity: 0,
      reservePrice: 8,
      status: "OPEN",
      biddingEnabled: false,
      isDraft: false
    });
    // Expected (future-harvest) listing — no actual stock yet.
    await setDoc(doc(db, "listings/listing-expected"), {
      listingId: "listing-expected",
      farmerId: "farmer-1",
      cropName: "Wheat",
      quantity: 0,
      availableQuantity: 0,
      soldQuantity: 0,
      estimatedQuantity: 20,
      reservePrice: 5,
      status: "OPEN",
      biddingEnabled: false,
      harvestStatus: "EXPECTED",
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

// ── Harvest lifecycle fields on existing listings ─────────────────────────────

test("farmer can update harvestStatus on their listing", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertSucceeds(updateDoc(doc(db, "listings/listing-expected"), {
    harvestStatus: "READY",
    availableQuantity: 18,
    quantity: 18,
    soldQuantity: 0,
    harvestUpdatedAt: Date.now()
  }));
});

test("farmer can update estimatedQuantity on an expected listing", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertSucceeds(updateDoc(doc(db, "listings/listing-expected"), {
    estimatedQuantity: 25,
    harvestUpdatedAt: Date.now()
  }));
});

test("farmer can set harvestFailureReason on their listing", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertSucceeds(updateDoc(doc(db, "listings/listing-expected"), {
    harvestStatus: "FAILED",
    harvestFailureReason: "Flood",
    biddingEnabled: false,
    harvestUpdatedAt: Date.now()
  }));
});

// ── Create EXPECTED (future-harvest) listing ──────────────────────────────────

test("farmer can create an expected-harvest listing with no actual stock", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertSucceeds(setDoc(doc(db, "listings/listing-new-expected"), {
    listingId: "listing-new-expected",
    farmerId: "farmer-1",
    cropName: "Barley",
    quantity: 0,
    availableQuantity: 0,
    soldQuantity: 0,
    estimatedQuantity: 15,
    reservePrice: 6,
    status: "OPEN",
    biddingEnabled: false,
    harvestStatus: "EXPECTED",
    isDraft: false
  }));
});

test("farmer cannot create an expected-harvest listing with zero estimatedQuantity", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertFails(setDoc(doc(db, "listings/listing-bad-expected"), {
    listingId: "listing-bad-expected",
    farmerId: "farmer-1",
    cropName: "Barley",
    quantity: 0,
    availableQuantity: 0,
    soldQuantity: 0,
    estimatedQuantity: 0,
    reservePrice: 6,
    status: "OPEN",
    biddingEnabled: false,
    harvestStatus: "EXPECTED",
    isDraft: false
  }));
});

// ── Order requests ───────────────────────────────────────────────────────────

function orderRequestDoc(requestId, overrides = {}) {
  return {
    requestId,
    listingId: "listing-fixed",
    farmerId: "farmer-1",
    farmerName: "Farmer One",
    supplierId: "supplier-1",
    supplierName: "Supplier One",
    cropName: "Rice",
    requestedQuantity: 5,
    quantityUnit: "KG",
    listedPrice: 8,
    status: "PENDING",
    isPreHarvest: false,
    createdAt: 1,
    updatedAt: 1,
    ...overrides
  };
}

function preHarvestRequestDoc(requestId, overrides = {}) {
  return {
    requestId,
    listingId: "listing-expected",
    farmerId: "farmer-1",
    farmerName: "Farmer One",
    supplierId: "supplier-1",
    supplierName: "Supplier One",
    cropName: "Wheat",
    requestedQuantity: 12,
    quantityUnit: "KG",
    listedPrice: 5,
    status: "PENDING",
    isPreHarvest: true,
    createdAt: 1,
    updatedAt: 1,
    ...overrides
  };
}

function listingFixedAfterSale(availableQuantity, soldQuantity) {
  return {
    listingId: "listing-fixed",
    farmerId: "farmer-1",
    cropName: "Rice",
    quantity: 10,
    availableQuantity,
    soldQuantity,
    reservePrice: 8,
    status: "OPEN",
    biddingEnabled: false,
    isDraft: false
  };
}

function orderFromRequest(request, overrides = {}) {
  return {
    orderId: request.requestId,
    listingId: request.listingId,
    bidId: "",
    farmerId: request.farmerId,
    supplierId: request.supplierId,
    cropName: request.cropName,
    quantity: request.requestedQuantity,
    quantityUnit: request.quantityUnit,
    agreedPrice: request.listedPrice,
    status: "AWAITING_PICKUP",
    pickupLocation: "North Farm",
    deliveryLocation: "",
    createdAt: 1,
    updatedAt: 1,
    ...overrides
  };
}

function listingExpectedAfterSale(availableQuantity, soldQuantity) {
  return {
    listingId: "listing-expected",
    farmerId: "farmer-1",
    cropName: "Wheat",
    quantity: availableQuantity + soldQuantity,
    availableQuantity,
    soldQuantity,
    reservePrice: 5,
    status: "OPEN",
    biddingEnabled: false,
    isDraft: false,
    harvestStatus: "READY"
  };
}

function buildAcceptBatchForExpected(db, request, resultingAvailable, resultingSold) {
  const batch = writeBatch(db);
  batch.set(
    doc(db, "listings", request.listingId),
    listingExpectedAfterSale(resultingAvailable, resultingSold)
  );
  batch.set(doc(db, "orders", request.requestId), orderFromRequest(request));
  batch.set(doc(db, "order_requests", request.requestId), {
    ...request,
    status: "ACCEPTED",
    updatedAt: 2
  });
  return batch;
}

async function seedRequest(requestId, overrides = {}) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      doc(context.firestore(), "order_requests", requestId),
      orderRequestDoc(requestId, overrides)
    );
  });
}

async function seedPreHarvestRequest(requestId, overrides = {}) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      doc(context.firestore(), "order_requests", requestId),
      preHarvestRequestDoc(requestId, overrides)
    );
  });
}

function buildAcceptBatch(db, request) {
  const batch = writeBatch(db);
  batch.set(
    doc(db, "listings", request.listingId),
    listingFixedAfterSale(
      10 - request.requestedQuantity,
      request.requestedQuantity
    )
  );
  batch.set(doc(db, "orders", request.requestId), orderFromRequest(request));
  batch.set(doc(db, "order_requests", request.requestId), {
    ...request,
    status: "ACCEPTED",
    updatedAt: 2
  });
  return batch;
}

test("supplier can create an order request on a fixed-price listing", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertSucceeds(
    setDoc(doc(db, "order_requests/request-1"), orderRequestDoc("request-1"))
  );
});

test("supplier cannot create an order request on a bidding-enabled listing", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(
    setDoc(
      doc(db, "order_requests/request-1"),
      orderRequestDoc("request-1", { listingId: "listing-1" })
    )
  );
});

test("supplier cannot request more than the available stock", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(
    setDoc(
      doc(db, "order_requests/request-1"),
      orderRequestDoc("request-1", { requestedQuantity: 11 })
    )
  );
});

test("supplier cannot request a non-positive quantity", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(
    setDoc(
      doc(db, "order_requests/request-1"),
      orderRequestDoc("request-1", { requestedQuantity: 0 })
    )
  );
});

test("supplier cannot create a request with a mismatched listed price", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(
    setDoc(
      doc(db, "order_requests/request-1"),
      orderRequestDoc("request-1", { listedPrice: 7 })
    )
  );
});

test("farmer cannot create an order request", async () => {
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertFails(
    setDoc(doc(db, "order_requests/request-1"), orderRequestDoc("request-1"))
  );
});

test("order request is readable by the supplier who placed it and the owning farmer only", async () => {
  await seedRequest("request-1");

  await assertSucceeds(
    getDoc(doc(testEnvironment.authenticatedContext("supplier-1").firestore(), "order_requests/request-1"))
  );
  await assertSucceeds(
    getDoc(doc(testEnvironment.authenticatedContext("farmer-1").firestore(), "order_requests/request-1"))
  );
  await assertFails(
    getDoc(doc(testEnvironment.authenticatedContext("farmer-2").firestore(), "order_requests/request-1"))
  );
  await assertFails(
    getDoc(doc(testEnvironment.authenticatedContext("supplier-2").firestore(), "order_requests/request-1"))
  );
});

test("owning farmer can reject a pending order request without touching stock", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertSucceeds(
    updateDoc(doc(db, "order_requests/request-1"), {
      status: "REJECTED",
      updatedAt: 2
    })
  );
});

test("non-owning farmer cannot reject an order request", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-2").firestore();

  await assertFails(
    updateDoc(doc(db, "order_requests/request-1"), {
      status: "REJECTED",
      updatedAt: 2
    })
  );
});

test("owning supplier can cancel a pending order request", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertSucceeds(
    updateDoc(doc(db, "order_requests/request-1"), {
      status: "CANCELLED",
      updatedAt: 2
    })
  );
});

test("another supplier cannot cancel someone else's order request", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("supplier-2").firestore();

  await assertFails(
    updateDoc(doc(db, "order_requests/request-1"), {
      status: "CANCELLED",
      updatedAt: 2
    })
  );
});

test("farmer cannot change the requested quantity while rejecting", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertFails(
    updateDoc(doc(db, "order_requests/request-1"), {
      status: "REJECTED",
      requestedQuantity: 1,
      updatedAt: 2
    })
  );
});

test("owning farmer can atomically accept a request, consume stock, and create the order", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();
  const request = orderRequestDoc("request-1");

  await assertSucceeds(buildAcceptBatch(db, request).commit());

  const snap = await getDoc(doc(db, "listings/listing-fixed"));
  const listing = snap.data();
  if (listing.availableQuantity !== 5 || listing.soldQuantity !== 5) {
    throw new Error(`Unexpected stock after acceptance: ${JSON.stringify(listing)}`);
  }
  const requestSnap = await getDoc(doc(db, "order_requests/request-1"));
  if (requestSnap.data().status !== "ACCEPTED") {
    throw new Error(`Request was not marked accepted: ${JSON.stringify(requestSnap.data())}`);
  }
  const orderSnap = await getDoc(doc(db, "orders/request-1"));
  const order = orderSnap.data();
  if (order.status !== "AWAITING_PICKUP" || order.bidId !== "" || order.agreedPrice !== 8) {
    throw new Error(`Unexpected order after acceptance: ${JSON.stringify(order)}`);
  }
});

test("supplier cannot run the accept batch", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(buildAcceptBatch(db, orderRequestDoc("request-1")).commit());
});

test("accept batch fails when the request is no longer pending", async () => {
  await seedRequest("request-1", { status: "ACCEPTED" });
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertFails(buildAcceptBatch(db, orderRequestDoc("request-1")).commit());
});

test("accept batch fails when remaining stock cannot cover the requested quantity", async () => {
  await seedRequest("request-1", { requestedQuantity: 9 });
  // Another sale reduced the stock before the farmer responded.
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "listings/listing-fixed"), {
      listingId: "listing-fixed",
      farmerId: "farmer-1",
      cropName: "Rice",
      quantity: 10,
      availableQuantity: 2,
      soldQuantity: 8,
      reservePrice: 8,
      status: "OPEN",
      biddingEnabled: false,
      isDraft: false
    });
  });
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();
  const request = orderRequestDoc("request-1", { requestedQuantity: 9 });
  const batch = writeBatch(db);
  batch.set(doc(db, "listings/listing-fixed"), listingFixedAfterSale(-7, 9));
  batch.set(doc(db, "orders/request-1"), orderFromRequest(request));
  batch.set(doc(db, "order_requests/request-1"), {
    ...request,
    status: "ACCEPTED",
    updatedAt: 2
  });

  await assertFails(batch.commit());
});

test("accept batch fails when the order total price does not match the listed price", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();
  const request = orderRequestDoc("request-1");
  const batch = writeBatch(db);
  batch.set(doc(db, "listings/listing-fixed"), listingFixedAfterSale(5, 5));
  batch.set(
    doc(db, "orders/request-1"),
    orderFromRequest(request, { agreedPrice: 7 })
  );
  batch.set(doc(db, "order_requests/request-1"), {
    ...request,
    status: "ACCEPTED",
    updatedAt: 2
  });

  await assertFails(batch.commit());
});

test("accept batch fails when the request is marked accepted without the coupled order", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();
  const request = orderRequestDoc("request-1");
  const batch = writeBatch(db);
  batch.set(doc(db, "listings/listing-fixed"), listingFixedAfterSale(5, 5));
  batch.set(doc(db, "order_requests/request-1"), {
    ...request,
    status: "ACCEPTED",
    updatedAt: 2
  });

  await assertFails(batch.commit());
});

test("order request cannot be deleted", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  await assertFails(deleteDoc(doc(db, "order_requests/request-1")));
});

// ── Pre-harvest order request scenarios ──────────────────────────────────────

test("supplier can create a pre-harvest request on an expected listing within estimated quantity", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertSucceeds(
    setDoc(doc(db, "order_requests/pre-req-1"), preHarvestRequestDoc("pre-req-1"))
  );
});

test("supplier cannot create a pre-harvest request with isPreHarvest=false on an expected listing", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(
    setDoc(
      doc(db, "order_requests/pre-req-1"),
      preHarvestRequestDoc("pre-req-1", { isPreHarvest: false })
    )
  );
});

test("supplier cannot create a pre-harvest request exceeding the estimated quantity", async () => {
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(
    setDoc(
      doc(db, "order_requests/pre-req-1"),
      preHarvestRequestDoc("pre-req-1", { requestedQuantity: 25 })
    )
  );
});

test("supplier cannot create a non-pre-harvest request on an expected listing", async () => {
  // A normal order request (isPreHarvest=false) must not be allowed on an EXPECTED listing
  // because there is no real stock. Already covered by the rule: isPreHarvest must be true.
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertFails(
    setDoc(
      doc(db, "order_requests/req-bad"),
      {
        requestId: "req-bad",
        listingId: "listing-expected",
        farmerId: "farmer-1",
        farmerName: "Farmer One",
        supplierId: "supplier-1",
        supplierName: "Supplier One",
        cropName: "Wheat",
        requestedQuantity: 5,
        quantityUnit: "KG",
        listedPrice: 5,
        status: "PENDING",
        isPreHarvest: false,
        createdAt: 1,
        updatedAt: 1
      }
    )
  );
});

test("pre-harvest request is readable by the placing supplier and owning farmer only", async () => {
  await seedPreHarvestRequest("pre-req-1");

  await assertSucceeds(
    getDoc(doc(testEnvironment.authenticatedContext("supplier-1").firestore(), "order_requests/pre-req-1"))
  );
  await assertSucceeds(
    getDoc(doc(testEnvironment.authenticatedContext("farmer-1").firestore(), "order_requests/pre-req-1"))
  );
  await assertFails(
    getDoc(doc(testEnvironment.authenticatedContext("supplier-2").firestore(), "order_requests/pre-req-1"))
  );
  await assertFails(
    getDoc(doc(testEnvironment.authenticatedContext("farmer-2").firestore(), "order_requests/pre-req-1"))
  );
});

test("farmer can cancel a pending pre-harvest request when marking harvest failed", async () => {
  await seedPreHarvestRequest("pre-req-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  // Simulate the harvest-failure cancellation (farmer cancels their own pre-harvest request).
  await assertSucceeds(
    updateDoc(doc(db, "order_requests/pre-req-1"), {
      status: "CANCELLED",
      updatedAt: 2
    })
  );
});

test("farmer cannot cancel a non-pre-harvest request directly (must use reject flow)", async () => {
  await seedRequest("request-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();

  // Farmer trying to CANCEL a normal (non-pre-harvest) request is not allowed.
  await assertFails(
    updateDoc(doc(db, "order_requests/request-1"), {
      status: "CANCELLED",
      updatedAt: 2
    })
  );
});

test("owning supplier can cancel their own pre-harvest request", async () => {
  await seedPreHarvestRequest("pre-req-1");
  const db = testEnvironment.authenticatedContext("supplier-1").firestore();

  await assertSucceeds(
    updateDoc(doc(db, "order_requests/pre-req-1"), {
      status: "CANCELLED",
      updatedAt: 2
    })
  );
});

test("farmer cannot accept a pre-harvest request on an expected listing (harvest not ready)", async () => {
  // An order cannot be created from a request when the listing's harvestStatus is EXPECTED.
  // This is enforced by the orders/create rule: harvestStatus must be READY.
  await seedPreHarvestRequest("pre-req-1");
  const db = testEnvironment.authenticatedContext("farmer-1").firestore();
  const request = preHarvestRequestDoc("pre-req-1");

  const batch = writeBatch(db);
  batch.set(doc(db, "order_requests/pre-req-1"), {
    ...request,
    status: "ACCEPTED",
    updatedAt: 2
  });
  batch.set(doc(db, "orders/pre-req-1"), {
    orderId: "pre-req-1",
    listingId: "listing-expected",
    bidId: "",
    farmerId: "farmer-1",
    supplierId: "supplier-1",
    cropName: "Wheat",
    quantity: 12,
    quantityUnit: "KG",
    agreedPrice: 5,
    status: "AWAITING_PICKUP",
    pickupLocation: "",
    deliveryLocation: "",
    createdAt: 1,
    updatedAt: 2
  });
  // listing stays as EXPECTED (harvest not ready) — the order create rule must reject this.
  await assertFails(batch.commit());
});

test("multiple pending pre-harvest requests may exist but only accepted up to available stock", async () => {
  // Create two pre-harvest requests whose total exceeds the estimate (estimate = 20)
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "order_requests", "pre-req-A"), preHarvestRequestDoc("pre-req-A", { requestedQuantity: 12 }));
    await setDoc(doc(db, "order_requests", "pre-req-B"), preHarvestRequestDoc("pre-req-B", { requestedQuantity: 10 }));
  });

  // Farmer marks listing expected -> READY with actual availableQuantity = 15
  const dbFarmer = testEnvironment.authenticatedContext("farmer-1").firestore();
  await assertSucceeds(updateDoc(doc(dbFarmer, "listings/listing-expected"), {
    harvestStatus: "READY",
    availableQuantity: 15,
    quantity: 15,
    soldQuantity: 0,
    harvestUpdatedAt: Date.now()
  }));

  // Accept first request (12) -> should succeed
  const requestA = preHarvestRequestDoc("pre-req-A", { requestedQuantity: 12 });
  const batchA = buildAcceptBatchForExpected(dbFarmer, requestA, 3, 12);
  await assertSucceeds(batchA.commit());

  // Accept second request (10) -> should fail because only 3 remain
  const requestB = preHarvestRequestDoc("pre-req-B", { requestedQuantity: 10 });
  const batchB = buildAcceptBatchForExpected(dbFarmer, requestB, -7, 22);
  await assertFails(batchB.commit());
});