package com.aktarjabed.inbusiness.domain.model

enum class PaymentMethod(val displayName: String) {
    UPI("UPI (GPay, PhonePe, Paytm, BHIM)"),
    CARD("Credit & Debit Cards (Visa, Master, RuPay)"),
    NETBANKING("Net Banking (50+ banks)"),
    WALLET("Wallets (Paytm, Amazon Pay, Mobikwik)"),
    EMI("Card EMI"),
    PAYLATER("Pay Later (LazyPay, Simpl)")
}