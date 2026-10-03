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

async function seedRequest(requestId, overrides = {}) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      doc(context.firestore(), "order_requests", requestId),
      orderRequestDoc(requestId, overrides)
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