package com.smartattendance.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val FACE_ENROLL = "face_enroll"
    const val QR_SCAN = "qr_scan"
    const val FACE_VERIFY = "face_verify/{sessionToken}/{courseCode}"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val RESULT = "result/{outcome}/{message}"

    fun faceVerify(sessionToken: String, courseCode: String): String {
        val encodedToken = java.net.URLEncoder.encode(sessionToken, "UTF-8")
        val encodedCourse = java.net.URLEncoder.encode(courseCode, "UTF-8")
        return "face_verify/$encodedToken/$encodedCourse"
    }

    fun result(outcome: String, message: String): String {
        val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
        return "result/$outcome/$encodedMessage"
    }
}
