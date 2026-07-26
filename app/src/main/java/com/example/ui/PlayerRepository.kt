package com.example.ui

import androidx.media3.common.Player
import androidx.media3.session.MediaSession

object PlayerRepository {
    var player: Player? = null
    var session: MediaSession? = null
}
