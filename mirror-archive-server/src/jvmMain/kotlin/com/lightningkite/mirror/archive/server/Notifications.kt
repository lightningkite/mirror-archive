package com.lightningkite.mirror.archive.server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.lightningkite.kommon.exception.stackTraceString
import com.lightningkite.koolui.UIPlatform
import com.lightningkite.koolui.notification.Notification
import com.lightningkite.koolui.notification.PushNotificationToken
import kotlinx.serialization.json.Json
import java.lang.Exception


object Notifications {

    var firebaseEnabled = false

    init {
        try {
            val options = FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build()

            FirebaseApp.initializeApp(options)
            firebaseEnabled = true
        } catch (e: Exception) {
            println("Firebase disabled because of ${e.stackTraceString()}")
        }
    }

    fun send(toTokens: List<PushNotificationToken>, notification: Notification) {
        val perPlatform = toTokens.groupBy { it.platform }
        if (firebaseEnabled) {
            val fcmTokens = perPlatform[UIPlatform.Android] ?: listOf()
            for (batch in fcmTokens.asSequence().map { it.token }.chunked(100)) {

                FirebaseMessaging.getInstance().sendMulticast(
                        MulticastMessage.builder()
                                .putData("n_id", notification.id.toString())
                                .putData("n_priority", notification.priority.toString())
                                .putData("n_title", notification.title)
                                .putData("n_content", notification.content)
                                .putData("n_image", notification.image)
                                .putData("n_action", notification.action)
                                .putData("n_actions", notification.actions.entries.joinToString("|") { it.key + "=" + it.value })
                                .addAllTokens(batch)
                                .build()
                )
            }
        }
    }
}