package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {

    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "account_balance" -> Icons.Default.AccountBalance
            "payments", "cash" -> Icons.Default.Payments
            "savings" -> Icons.Default.Savings
            "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
            "pie_chart" -> Icons.Default.PieChart
            "trending_up", "stock" -> Icons.Default.TrendingUp
            "public", "global" -> Icons.Default.Public
            "monetization_on", "gold" -> Icons.Default.MonetizationOn
            "diamond" -> Icons.Default.Diamond
            "receipt_long", "bonds" -> Icons.Default.ReceiptLong
            "lock", "fd" -> Icons.Default.Lock
            "account_tree" -> Icons.Default.AccountTree
            "assured_workload", "epf" -> Icons.Default.AssuredWorkload
            "security", "nps" -> Icons.Default.Security
            "child_care", "child_plan" -> Icons.Default.ChildCare
            "shield", "ppf" -> Icons.Default.Shield
            "elderly", "pension" -> Icons.Default.Elderly
            "umbrella" -> Icons.Default.Umbrella
            "shopping_cart", "grocery", "groceries", "local_grocery_store" -> Icons.Default.ShoppingCart
            "storefront", "store" -> Icons.Default.Storefront
            "restaurant", "food", "dining" -> Icons.Default.Restaurant
            "local_cafe", "coffee", "cafe" -> Icons.Default.LocalCafe
            "fastfood" -> Icons.Default.Fastfood
            "home", "rent", "housing" -> Icons.Default.Home
            "bolt", "utilities", "electricity" -> Icons.Default.Bolt
            "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
            "flight", "travel", "holiday" -> Icons.Default.Flight
            "directions_car", "transport", "fuel", "car" -> Icons.Default.DirectionsCar
            "directions_bus", "bus", "commute" -> Icons.Default.DirectionsBus
            "school", "education", "courses" -> Icons.Default.School
            "local_hospital", "medical", "health", "hospital" -> Icons.Default.LocalHospital
            "health_and_safety", "insurance" -> Icons.Default.HealthAndSafety
            "fitness_center", "gym", "fitness" -> Icons.Default.FitnessCenter
            "pets", "pet" -> Icons.Default.Pets
            "movie", "entertainment" -> Icons.Default.Movie
            "family_restroom", "family" -> Icons.Default.FamilyRestroom
            "subscriptions", "software" -> Icons.Default.Subscriptions
            "card_giftcard", "gift" -> Icons.Default.CardGiftcard
            "credit_card" -> Icons.Default.CreditCard
            "phone_android", "mobile", "recharge" -> Icons.Default.PhoneAndroid
            "build", "maintenance", "repair" -> Icons.Default.Build
            "local_laundry_service", "laundry" -> Icons.Default.LocalLaundryService
            else -> Icons.Default.Category
        }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF6366F1)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorInt = if (cleanHex.length == 6) {
                ("FF$cleanHex").toLong(16)
            } else {
                cleanHex.toLong(16)
            }
            Color(colorInt)
        } catch (e: Exception) {
            fallback
        }
    }
}

