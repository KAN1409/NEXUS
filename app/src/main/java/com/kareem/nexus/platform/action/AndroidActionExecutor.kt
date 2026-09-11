package com.kareem.nexus.platform.action

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.kareem.nexus.core.model.ContextAction
import com.kareem.nexus.core.model.NexusActionKind
import com.kareem.nexus.core.model.OpenLoop

data class AndroidActionResult(
    val success: Boolean,
    val message: String,
)

object AndroidActionExecutor {
    fun execute(context: Context, loop: OpenLoop, action: ContextAction): AndroidActionResult = runCatching {
        when (action.kind) {
            NexusActionKind.OPEN_SOURCE, NexusActionKind.REPLY, NexusActionKind.RETRY -> openSource(context, loop, action)
            NexusActionKind.ADD_TO_CALENDAR -> addToCalendar(context, loop)
            NexusActionKind.NAVIGATE -> navigate(context, action.payload ?: loop.detail)
            NexusActionKind.CALL -> call(context, action.payload ?: loop.detail)
            NexusActionKind.COPY -> copy(context, action.payload ?: loop.detail)
            NexusActionKind.TRACK -> track(context, loop, action.payload)
            NexusActionKind.REMIND -> AndroidActionResult(false, "Reminder scheduling is handled separately")
            NexusActionKind.MARK_RESOLVED -> AndroidActionResult(false, "Resolution is handled inside NEXUS")
        }
    }.getOrElse { AndroidActionResult(false, it.message ?: "Could not open this action") }

    private fun openSource(context: Context, loop: OpenLoop, action: ContextAction): AndroidActionResult {
        val packageName = loop.source ?: action.payload
        val intent = packageName?.let(context.packageManager::getLaunchIntentForPackage)
            ?: return AndroidActionResult(false, "The source app cannot be opened directly")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return AndroidActionResult(true, "Opened source app")
    }

    private fun addToCalendar(context: Context, loop: OpenLoop): AndroidActionResult {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, loop.title)
            .putExtra(CalendarContract.Events.DESCRIPTION, loop.detail)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        loop.dueAt?.let { due ->
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, due)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, due + 60 * 60_000L)
        }
        context.startActivity(intent)
        return AndroidActionResult(true, "Opened calendar")
    }

    private fun navigate(context: Context, payload: String): AndroidActionResult {
        val uri = if (payload.startsWith("geo:")) Uri.parse(payload)
        else Uri.parse("geo:0,0?q=${Uri.encode(payload)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return AndroidActionResult(true, "Opened navigation")
    }

    private fun call(context: Context, payload: String): AndroidActionResult {
        val phone = Regex("\\+?[0-9][0-9 -]{5,}").find(payload)?.value?.replace(" ", "")
            ?: return AndroidActionResult(false, "No phone number was found")
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return AndroidActionResult(true, "Opened dialer")
    }

    private fun copy(context: Context, payload: String): AndroidActionResult {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("NEXUS", payload))
        return AndroidActionResult(true, "Copied")
    }

    private fun track(context: Context, loop: OpenLoop, payload: String?): AndroidActionResult {
        val uri = payload?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (uri != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return AndroidActionResult(true, "Opened tracking link")
        }
        return openSource(context, loop, ContextAction(NexusActionKind.OPEN_SOURCE, "Open source", loop.source))
    }
}
