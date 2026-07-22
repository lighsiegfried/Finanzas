package com.kratt.finanzas.presentation.common

import androidx.annotation.StringRes
import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.AccountType

// nombres visibles de los tipos de cuenta y su orden en el selector
object AccountTypeLabels {

    val ordered = listOf(
        AccountType.CASH,
        AccountType.BANK_ACCOUNT,
        AccountType.CREDIT_CARD,
        AccountType.SAVINGS,
        AccountType.HOUSEHOLD,
        AccountType.DIGITAL_WALLET,
        AccountType.OTHER,
    )

    @StringRes
    fun labelOf(type: AccountType): Int = when (type) {
        AccountType.CASH -> R.string.account_type_cash
        AccountType.BANK_ACCOUNT -> R.string.account_type_bank
        AccountType.CREDIT_CARD -> R.string.account_type_credit
        AccountType.SAVINGS -> R.string.account_type_savings
        AccountType.HOUSEHOLD -> R.string.account_type_household
        AccountType.DIGITAL_WALLET -> R.string.account_type_wallet
        AccountType.OTHER -> R.string.account_type_other
    }
}
