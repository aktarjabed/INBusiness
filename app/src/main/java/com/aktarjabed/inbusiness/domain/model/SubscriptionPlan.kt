package com.aktarjabed.inbusiness.domain.model

enum class SubscriptionPlan(
    val displayName: String,
    val priceINR: Int,
    val features: List<String>
) {
    BASIC(
        "Basic Plan",
        99,
        listOf(
            "Unlimited invoices",
            "PDF export",
            "Email support",
            "Basic templates",
            "No watermark"
        )
    ),
    PRO(
        "Pro Plan",
        149,
        listOf(
            "Everything in Basic",
            "AI anomaly detection",
            "Bank-grade encryption",
            "NIC IRN generation",
            "Priority support",
            "Custom branding"
        )
    ),
    ENTERPRISE(
        "Enterprise Plan",
        499,
        listOf(
            "Everything in Pro",
            "Multi-user (5 users)",
            "API access",
            "White-label option",
            "Dedicated support",
            "Custom integrations",
            "Advanced analytics"
        )
    );

    val priceInPaise: Int
        get() = priceINR * 100
}