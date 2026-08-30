package com.elementeracoast.app.feature.gate

enum class MailboxEntryPage {
    Choices,
    Login,
    Register
}

data class MailboxEntryState(
    val visible: Boolean = false,
    val page: MailboxEntryPage = MailboxEntryPage.Choices
) {
    fun open(): MailboxEntryState = copy(visible = true, page = MailboxEntryPage.Choices)
    fun close(): MailboxEntryState = MailboxEntryState()
    fun show(page: MailboxEntryPage): MailboxEntryState = copy(visible = true, page = page)
}
