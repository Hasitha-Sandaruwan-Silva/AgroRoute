# AgroRoute - Supplier Module Technical & Functional Specification

Welcome to the comprehensive documentation for the **Supplier Module** of the AgroRoute application. This document outlines the screen hierarchy, functional workflows, permission models, and UI/UX guidelines required for implementation. 

---

## 📌 Executive Overview

The AgroRoute Supplier Module enables registered buyers and suppliers to browse agricultural produce, submit competitive bids, track bid statuses, and manage post-acceptance orders. While sharing a unified, clean visual identity with the Farmer interface, the Supplier module maintains strict operational, data visibility, and permission boundaries within Firebase and application logic.

---

## 📱 Screen-by-Screen Specifications

### 1. Authentication Screen (`AuthScreen`)
Handles user authentication and role onboarding.

* **Supported Methods:**
  * Standard Email & Password credentials.
  * Google OAuth Sign-In.
* **Features & Actions:**
  * Login / Registration toggle.
  * Password reset ("Forgot Password") workflow.
  * Real-time validation, error handling, and loading state indicators.
* **Role Assignment Logic:**
  * On initial Google Sign-In, first-time users must select their role (**Farmer** or **Supplier**).
  * Returning users previously registered as **Supplier** bypass role selection and route directly to the **Supplier Home Screen**.

---

### 2. Supplier Home Screen (`SupplierHomeScreen`)
The central dashboard displaying active operations and quick access shortcuts.

* **Key Elements:**
  * Personalized header displaying the Supplier's name.
  * **Browse Marketplace** call-to-action button.
  * Active Pending Bids counter widget (bound to live state).
  * Quick shortcut to **Open Orders**.
  * Recent activity summary (sourced strictly from existing backend data).
* **UI States:** Full support for Loading (shimmer), Empty, and Network Error states.
* **Permission Constraints:** Strictly hides Farmer actions (e.g., creating listings, accepting bids).

---

### 3. Marketplace Screen (`MarketplaceScreen`)
The main discovery hub where Suppliers browse available produce listings.

* **Item Card Details:**
  * Crop image with fallback placeholder image logic.
  * Crop title / name.
  * Stock quantity and unit of measurement (e.g., kg, tons, bags).
  * Listed price (if specified by the Farmer).
  * Metadata badges: Category, quality grade, harvest date.
  * Bidding availability badge ("Bidding Open" / "Closed").
* **Behavior:**
  * Tapping a card opens the **Item Details Screen**.
  * Filters and sorts only active, published listings authorized for Supplier access.

---

### 4. Item Details Screen (`ItemDetailsScreen`)
Detailed view of a selected produce listing.

* **Displayed Information:**
  * High-resolution crop image banner / gallery.
  * Crop name, available quantity, unit, price, location, quality grade, harvest date, and description.
* **Actions:**
  * **Place Bid** button.
* **Validation Rules:**
  * The "Place Bid" action must be dynamically disabled if:
    * Bidding is toggled off by the Farmer.
    * The listing status is closed/unpublished.
    * Stock quantity equals 0.
  * Displays informative status messages for disabled states rather than leaving buttons unresponsive.

---

### 5. Place Bid Screen (`PlaceBidScreen`)
Form screen for constructing and submitting price offers.

* **Form Fields:**
  * Numeric Price Offer input.
  * Quantity & Message input fields (if supported by schema).
* **Summary & Actions:**
  * Pre-submission summary breakdown confirming offer details.
  * **Submit Bid** action button.
  * Submission loading overlay, success toast/banner, and error handling.
* **State Behavior:** Newly created bids default immediately to `Pending` state.

---

### 6. My Bids Screen (`MyBidsScreen`)
Historical and live tracking view of all bids submitted by the logged-in Supplier.

* **Displayed Metrics:**
  * Crop name and thumbnail.
  * Offered price and quantity.
  * Submission timestamp.
  * Status badge: `Pending`, `Accepted`, `Rejected`, or `Withdrawn`.
* **Actions:**
  * **Withdraw Bid** button (active only for `Pending` bids, subject to system rules).
* **Data Isolation:**
  * Displays **strictly the logged-in Supplier's bids**.
  * Suppliers cannot view bids placed by competing buyers, edit external bids, or self-accept their own offers.

---

### 7. Order List Screen (`OrderListScreen`)
Fulfillment tracking screen containing generated orders.

* **Order Card Properties:**
  * Crop title and image.
  * Accepted quantity and agreed final price.
  * Order status badge.
  * Creation date.
* **Business Rule:**
  * Bids in `Pending` state **are not orders**. An entry appears here **only after** a Farmer explicitly accepts a bid.

---

### 8. Order Details Screen (`OrderDetailsScreen`)
In-depth view of a specific confirmed order.

* **Displayed Information:**
  * Complete item summary, quantities, total cost, and Unique Order ID.
  * Live status timeline (e.g., `Pending Pickup` $\rightarrow$ `In Transit` $\rightarrow$ `Delivered`).
  * Farmer contact details and pickup/delivery logistics details.
* **State Transitions:**
  * Order status updates strictly follow system-defined sequential transitions without exposing invalid state controls.

---

### 9. Supplier Profile Screen (`SupplierProfileScreen`)
User profile management and session control.

* **Fields & Actions:**
  * Full Name
  * Email Address
  * Contact Phone Number
  * Business / Delivery Address
  * Profile Photo upload/change
  * **Logout** button (invalidates session token and returns to Auth).
* **Isolation:** Excludes Farmer farm configuration tools, listing management options, or access to other user profiles.

---

### 10. Navigation & System Consistency

* **Bottom Navigation Bar Tabs:**
  1. 🏠 **Home**
  2. 🛒 **Marketplace**
  3. 🏷️ **Bids**
  4. 📦 **Orders**
  5. 👤 **Profile**
* **System Principles:**
  * Predictable back-stack navigation handling across all screens.
  * Unified design language (typography, colors, loading shimmers, offline alerts) matching the Farmer module.
  * Clear operational separation: Placing bids is a **Supplier** action; accepting/rejecting bids is a **Farmer** action.