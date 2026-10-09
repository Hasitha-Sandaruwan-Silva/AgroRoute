package com.github.thelacspace.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** AgroRoute outline family. 24-unit viewport, 1.65-unit round strokes, no fills.
 * Existing tint, icon size, touch targets, selection styling and callbacks belong to callers.
 * Vectors are lazily cached; no icon dependency or bitmap allocation is required.
 */
object Lucide {
    private fun outline(name: String, data: String, mirrored: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = "AgroRoute.$name", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f, autoMirror = mirrored
        ).addPath(
            pathData = addPathNodes(data), fill = null, stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.65f, strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()

    val Home: ImageVector by lazy { outline("Home", "M3 10 L12 3 L21 10 M5 9 V19 Q5 21 7 21 H10 V14 H14 V21 H17 Q19 21 19 19 V9") }
    val ShoppingBag: ImageVector by lazy { outline("ShoppingBag", "M6 7 H18 Q20 7 20 9 L19 19 Q19 21 17 21 H7 Q5 21 5 19 L4 9 Q4 7 6 7 Z M9 9 V6 C9 2 15 2 15 6 V9") }
    val FileText: ImageVector by lazy { outline("FileText", "M14 3 H6 Q4 3 4 5 V19 Q4 21 6 21 H18 Q20 21 20 19 V9 L14 3 Z M14 3 V9 H20 M8 13 H16 M8 17 H14") }
    val Receipt: ImageVector by lazy { outline("Receipt", "M5 3 L7 4 L9 3 L12 4 L15 3 L17 4 L19 3 V21 L17 20 L15 21 L12 20 L9 21 L7 20 L5 21 Z M9 8 H15 M9 12 H15 M9 16 H13") }
    val User: ImageVector by lazy { outline("User", "M16 7 A4 4 0 1 1 8 7 A4 4 0 1 1 16 7 Z M4 21 V19 C4 13 20 13 20 19 V21") }
    val Users: ImageVector by lazy { outline("Users", "M14 7 A3 3 0 1 1 8 7 A3 3 0 1 1 14 7 Z M3 21 V19 C3 13 19 13 19 19 V21 M18 4 C22 4 22 10 18 10 M21 15 Q23 17 23 20") }
    val Search: ImageVector by lazy { outline("Search", "M18 10 A7 7 0 1 1 4 10 A7 7 0 1 1 18 10 Z M16 16 L21 21") }
    val Filter: ImageVector by lazy { outline("Filter", "M4 6 H20 M7 12 H17 M10 18 H14") }
    val MapPin: ImageVector by lazy { outline("MapPin", "M19 10 C19 15 12 21 12 21 C12 21 5 15 5 10 A7 7 0 0 1 19 10 Z M14.5 10 A2.5 2.5 0 1 1 9.5 10 A2.5 2.5 0 1 1 14.5 10 Z") }
    val MyLocation: ImageVector by lazy { outline("MyLocation", "M19 12 A7 7 0 1 1 5 12 A7 7 0 1 1 19 12 Z M14.5 12 A2.5 2.5 0 1 1 9.5 12 A2.5 2.5 0 1 1 14.5 12 Z M12 2 V5 M12 19 V22 M2 12 H5 M19 12 H22") }
    val Navigation: ImageVector by lazy { outline("Navigation", "M21 3 L15 21 L11 13 L3 9 L21 3 Z") }
    val Truck: ImageVector by lazy { outline("Truck", "M3 5 H14 V17 H3 Z M14 9 H18 L21 13 V17 H14 M9 18 A2 2 0 1 1 5 18 A2 2 0 1 1 9 18 Z M19 18 A2 2 0 1 1 15 18 A2 2 0 1 1 19 18 Z") }
    val Pickup: ImageVector by lazy { outline("Pickup", "M4 8 L12 4 L20 8 V18 L12 22 L4 18 Z M4 8 L12 12 L20 8 M12 12 V22 M8 6 L16 10 M12 2 V7 M10 5 L12 7 L14 5") }
    val Edit: ImageVector by lazy { outline("Edit", "M14 5 L19 10 M4 20 L9 19 L21 7 Q22 6 21 5 L19 3 Q18 2 17 3 L5 15 L4 20 Z") }
    val Delete: ImageVector by lazy { outline("Delete", "M3 6 H21 M9 6 V3 H15 V6 M5 6 L6 19 Q6 21 8 21 H16 Q18 21 18 19 L19 6 M10 10 V17 M14 10 V17") }
    val Plus: ImageVector by lazy { outline("Plus", "M12 5 V19 M5 12 H19") }
    val Camera: ImageVector by lazy { outline("Camera", "M8 6 L10 3 H14 L16 6 H20 Q22 6 22 8 V18 Q22 20 20 20 H4 Q2 20 2 18 V8 Q2 6 4 6 Z M16 13 A4 4 0 1 1 8 13 A4 4 0 1 1 16 13 Z") }
    val Image: ImageVector by lazy { outline("Image", "M5 3 H19 Q21 3 21 5 V19 Q21 21 19 21 H5 Q3 21 3 19 V5 Q3 3 5 3 Z M3 17 L8 12 L12 16 L16 11 L21 16 M9 8 A1.5 1.5 0 1 1 6 8 A1.5 1.5 0 1 1 9 8 Z") }
    val ImagePlus: ImageVector by lazy { outline("ImagePlus", "M11 3 H5 Q3 3 3 5 V19 Q3 21 5 21 H19 Q21 21 21 19 V13 M3 17 L8 12 L12 16 L16 12 L21 17 M9 8 A1.5 1.5 0 1 1 6 8 A1.5 1.5 0 1 1 9 8 Z M18 2 V8 M15 5 H21") }
    val Bell: ImageVector by lazy { outline("Bell", "M6 9 C6 1 18 1 18 9 V14 L20 17 H4 L6 14 Z M10 21 H14 M12 2 V3") }
    val Settings: ImageVector by lazy { outline("Settings", "M9 3 H15 L16 6 L19 7 L21 12 L19 17 L16 18 L15 21 H9 L8 18 L5 17 L3 12 L5 7 L8 6 Z M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12 Z") }
    val ArrowLeft: ImageVector by lazy { outline("ArrowLeft", "M20 12 H4 M10 6 L4 12 L10 18", mirrored = true) }
    val ArrowRight: ImageVector by lazy { outline("ArrowRight", "M4 12 H20 M14 6 L20 12 L14 18", mirrored = true) }
    val ArrowUpRight: ImageVector by lazy { outline("ArrowUpRight", "M5 19 L19 5 M7 5 H19 V17") }
    val ChevronRight: ImageVector by lazy { outline("ChevronRight", "M9 5 L16 12 L9 19", mirrored = true) }
    val Close: ImageVector by lazy { outline("Close", "M6 6 L18 18 M18 6 L6 18") }
    val Check: ImageVector by lazy { outline("Check", "M4 12 L9 17 L20 6") }
    val CheckCircle: ImageVector by lazy { outline("CheckCircle", "M21 11 V12 A9 9 0 1 1 15 3.5 M8 11 L12 15 L21 5") }
    val MoreVertical: ImageVector by lazy { outline("MoreVertical", "M13 5 A1 1 0 1 1 11 5 A1 1 0 1 1 13 5 Z M13 12 A1 1 0 1 1 11 12 A1 1 0 1 1 13 12 Z M13 19 A1 1 0 1 1 11 19 A1 1 0 1 1 13 19 Z") }
    val Calendar: ImageVector by lazy { outline("Calendar", "M5 5 H19 Q21 5 21 7 V19 Q21 21 19 21 H5 Q3 21 3 19 V7 Q3 5 5 5 Z M7 3 V7 M17 3 V7 M3 11 H21 M8 15 H10 M14 15 H16") }
    val DollarSign: ImageVector by lazy { outline("DollarSign", "M12 2 V22 M17 6 C15 3 7 3 7 8 C7 13 17 11 17 16 C17 21 9 21 6 18") }
    val Banknote: ImageVector by lazy { outline("Banknote", "M4 5 H20 Q22 5 22 7 V17 Q22 19 20 19 H4 Q2 19 2 17 V7 Q2 5 4 5 Z M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12 Z M5 9 V15 M19 9 V15") }
    val Package: ImageVector by lazy { outline("Package", "M3 7 L12 3 L21 7 V17 L12 21 L3 17 Z M3 7 L12 12 L21 7 M12 12 V21 M7.5 5 L16.5 9") }
    val Phone: ImageVector by lazy { outline("Phone", "M7 3 H4 Q2 3 3 7 C5 14 10 19 17 21 Q21 22 21 20 V17 L16 15 L14 18 C10 16 8 14 6 10 L9 8 L7 3 Z") }
    val Mail: ImageVector by lazy { outline("Mail", "M4 4 H20 Q22 4 22 6 V18 Q22 20 20 20 H4 Q2 20 2 18 V6 Q2 4 4 4 Z M3 6 L12 13 L21 6") }
    val Logout: ImageVector by lazy { outline("Logout", "M9 3 H5 Q3 3 3 5 V19 Q3 21 5 21 H9 M10 12 H21 M17 8 L21 12 L17 16", mirrored = true) }
    val Leaf: ImageVector by lazy { outline("Leaf", "M20 3 C20 3 9 2 5 7 C1 12 5 20 11 19 C17 18 20 10 20 3 Z M4 21 L14 11") }
    val Sprout: ImageVector by lazy { outline("Sprout", "M12 21 V12 M12 15 C12 8 3 7 3 7 C3 7 2 15 12 15 Z M12 11 C12 4 21 3 21 3 C21 3 22 11 12 11 Z") }
    val Wheat: ImageVector by lazy { outline("Wheat", "M12 22 V7 M12 7 Q9 4 12 2 Q15 4 12 7 Z M12 12 C7 12 5 9 5 6 C9 6 12 8 12 12 Z M12 12 C17 12 19 9 19 6 C15 6 12 8 12 12 Z M12 18 C7 18 5 15 5 12 C9 12 12 14 12 18 Z M12 18 C17 18 19 15 19 12 C15 12 12 14 12 18 Z") }
    val Store: ImageVector by lazy { outline("Store", "M4 9 V21 H20 V9 M9 21 V15 H15 V21 M3 3 H21 L22 8 C22 12 17 12 17 8 C17 12 12 12 12 8 C12 12 7 12 7 8 C7 12 2 12 2 8 L3 3 Z") }
    val Gavel: ImageVector by lazy { outline("Gavel", "M14 3 L21 10 L18 13 L11 6 Z M10 7 L17 14 L14 17 L7 10 Z M12 12 L4 20 M14 21 H22") }
    val Handshake: ImageVector by lazy { outline("Handshake", "M3 8 L7 4 L12 6 L17 4 L21 8 L18 12 L16 10 L13 11 L9 8 L7 10 L11 14 L14 17 Q15 18 16 17 L18 15 M3 8 L2 14 L6 17 L9 20 Q10 21 11 20 L14 17 M21 8 L22 14 L18 17") }
    val Inbox: ImageVector by lazy { outline("Inbox", "M5 4 H19 L22 14 V19 Q22 21 20 21 H4 Q2 21 2 19 V14 Z M2 14 H8 L10 17 H14 L16 14 H22") }
    val Clock: ImageVector by lazy { outline("Clock", "M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 Z M12 7 V12 L16 14") }
    val Lock: ImageVector by lazy { outline("Lock", "M6 10 H18 Q20 10 20 12 V19 Q20 21 18 21 H6 Q4 21 4 19 V12 Q4 10 6 10 Z M8 10 V7 C8 1 16 1 16 7 V10 M12 14 V17") }
    val Info: ImageVector by lazy { outline("Info", "M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 Z M12 11 V17 M12 7 V7.1") }
    val AlertCircle: ImageVector by lazy { outline("AlertCircle", "M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 Z M12 7 V13 M12 17 V17.1") }
    val Eye: ImageVector by lazy { outline("Eye", "M2 12 C6 3 18 3 22 12 C18 21 6 21 2 12 Z M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12 Z") }
    val EyeOff: ImageVector by lazy { outline("EyeOff", "M3 3 L21 21 M9 5 C14 3 19 7 22 12 Q21 15 18 17 M14 19 C9 22 4 17 2 12 Q3 9 6 7 M10 10 L14 14") }
    val Heart: ImageVector by lazy { outline("Heart", "M12 21 L4 13 C-2 7 6 0 12 7 C18 0 26 7 20 13 Z") }
    val Star: ImageVector by lazy { outline("Star", "M12 3 L15 9 L21 10 L16.5 15 L18 21 L12 18 L6 21 L7.5 15 L3 10 L9 9 Z") }
    val Trophy: ImageVector by lazy { outline("Trophy", "M7 3 H17 V9 C17 17 7 17 7 9 Z M7 5 H3 V8 Q3 12 8 12 M17 5 H21 V8 Q21 12 16 12 M12 15 V21 M8 21 H16") }
    val Award: ImageVector by lazy { outline("Award", "M17 8 A5 5 0 1 1 7 8 A5 5 0 1 1 17 8 Z M8 12 L6 21 L12 18 L18 21 L16 12") }
    val Bookmark: ImageVector by lazy { outline("Bookmark", "M6 3 H18 V21 L12 17 L6 21 Z") }
    val Save: ImageVector by lazy { outline("Save", "M4 3 H17 L21 7 V19 Q21 21 19 21 H5 Q3 21 3 19 V5 Q3 3 4 3 Z M7 3 V9 H16 V3 M7 21 V14 H17 V21") }
    val RefreshCw: ImageVector by lazy { outline("RefreshCw", "M20 7 A9 9 0 0 0 4 7 M20 3 V7 H16 M4 17 A9 9 0 0 0 20 17 M4 21 V17 H8") }
    val TrendingUp: ImageVector by lazy { outline("TrendingUp", "M3 17 L9 11 L13 15 L21 7 M15 7 H21 V13", mirrored = true) }
    val ShoppingCart: ImageVector by lazy { outline("ShoppingCart", "M2 3 H5 L8 16 H19 L22 7 H6 M10 21 A1.5 1.5 0 1 1 7 21 A1.5 1.5 0 1 1 10 21 Z M20 21 A1.5 1.5 0 1 1 17 21 A1.5 1.5 0 1 1 20 21 Z") }
    val Tag: ImageVector by lazy { outline("Tag", "M3 3 H12 L21 12 L12 21 L3 12 Z M8 7 A1 1 0 1 1 6 7 A1 1 0 1 1 8 7 Z") }
    val Archive: ImageVector by lazy { outline("Archive", "M3 3 H21 V7 H3 Z M5 7 V21 H19 V7 M10 12 H14") }
    val Sparkles: ImageVector by lazy { outline("Sparkles", "M12 3 L14.5 9.5 L21 12 L14.5 14.5 L12 21 L9.5 14.5 L3 12 L9.5 9.5 Z M20 2 V6 M18 4 H22") }
    val Quote: ImageVector by lazy { outline("Quote", "M4 5 H10 V12 Q10 18 4 19 M4 5 V12 H10 M14 5 H20 V12 Q20 18 14 19 M14 5 V12 H20") }
    val List: ImageVector by lazy { outline("List", "M8 6 H21 M8 12 H21 M8 18 H21 M3 6 H3.1 M3 12 H3.1 M3 18 H3.1") }

    // Semantic aliases preserve existing imports and feature symbols.
    val Description: ImageVector get() = FileText
    val CalendarDays: ImageVector get() = Calendar
    val Box: ImageVector get() = Package
    val Label: ImageVector get() = Tag
    val X: ImageVector get() = Close
    val BadgeDollarSign: ImageVector get() = DollarSign
    val Farmer: ImageVector get() = Sprout
    val Supplier: ImageVector get() = Store
    val Orders: ImageVector get() = Receipt
    val Requests: ImageVector get() = Inbox
    val Quantity: ImageVector get() = Package
    val Gallery: ImageVector get() = Image
    val ImageUpload: ImageVector get() = ImagePlus
    val Delivery: ImageVector get() = Truck
    val Directions: ImageVector get() = Navigation
    val Marketplace: ImageVector get() = ShoppingBag
    val Listings: ImageVector get() = FileText
}
