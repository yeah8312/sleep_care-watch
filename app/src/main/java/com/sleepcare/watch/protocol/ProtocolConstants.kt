package com.sleepcare.watch.protocol

object ProtocolConstants {
    const val SOURCE_WATCH = "watch"
    const val SOURCE_PHONE = "phone"

    const val PATH_CTL_START = "/sc/v1/ctl/start"
    const val PATH_CTL_STOP = "/sc/v1/ctl/stop"
    const val PATH_CTL_FLUSH_POLICY = "/sc/v1/ctl/flush_policy"
    const val PATH_CTL_BACKFILL_REQ = "/sc/v1/ctl/backfill_req"
    const val PATH_ALERT_VIBRATE = "/sc/v1/alert/vibrate"

    const val PATH_HR_LIVE = "/sc/v1/hr/live"
    const val PATH_HR_BATCH = "/sc/v1/hr/batch"
    const val PATH_HR_ACK = "/sc/v1/hr/ack"

    const val PATH_SESSION_CURRENT = "/sc/v1/session/current"

    fun sessionCursorPath(sessionId: String): String = "/sc/v1/session/$sessionId/cursor"
}

