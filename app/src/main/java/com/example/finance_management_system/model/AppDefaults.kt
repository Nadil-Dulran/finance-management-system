package com.example.finance_management_system.model

object AppDefaults {
    const val DEFAULT_INCOME_SOURCE = "Salary"
    const val DEFAULT_CURRENCY = "LKR"
    const val DEFAULT_SPENDING_TYPE = "Committed"
    const val DEFAULT_PAYMENT_METHOD = "Card"
    const val DEFAULT_ACCOUNT_NAME = "Main account"

    const val INCOME_HELPER_TEXT = "Salary, freelance, AdSense, or crypto income"
    const val EXPENSE_HELPER_TEXT =
        "Capture category, amount, currency, payment method, and spending type quickly"

    const val ERROR_INVALID_INCOME = "Enter a valid income amount."
    const val ERROR_INVALID_EXPENSE = "Enter a valid expense amount."
    const val ERROR_EXPENSE_EXCEEDS_FREE_CASH = "Expense exceeds available free cash"
    const val SUCCESS_INCOME_SAVED = "Income saved successfully."
    const val SUCCESS_EXPENSE_SAVED = "Expense saved successfully."
    const val SUCCESS_TRANSACTION_UPDATED = "Transaction updated."
    const val SUCCESS_TRANSACTION_DELETED = "Transaction deleted."
    const val SUCCESS_CURRENCY_UPDATED = "Display currency updated."
    const val ERROR_INCOME_SAVE = "Could not save income."
    const val ERROR_EXPENSE_SAVE = "Could not save expense."
    const val ERROR_TRANSACTION_UPDATE = "Could not update transaction."
    const val ERROR_TRANSACTION_DELETE = "Could not delete transaction."
    const val ERROR_SIGN_IN = "Sign in failed."
    const val ERROR_LOGIN_REQUIRED = "Please enter email or password"
    const val ERROR_REGISTER = "Registration failed."
    const val ERROR_RESET_PASSWORD = "Could not send reset email."
    const val ERROR_RESET_PASSWORD_EMAIL_REQUIRED = "Enter your account email first."
    const val SUCCESS_RESET_PASSWORD =
        "Password reset email sent. Check your inbox and spam folder."
    const val ERROR_AUTH_TIMEOUT =
        "Authentication timed out. Please check your connection and try again."
    const val ERROR_AUTH_NETWORK =
        "Authentication could not reach Firebase. Check your network and try again."
    const val ERROR_AUTH_TOO_MANY_REQUESTS =
        "Too many authentication attempts. Please wait a moment and try again."
    const val ERROR_AUTH_INVALID_CREDENTIALS =
        "Your email or password is incorrect."
}
