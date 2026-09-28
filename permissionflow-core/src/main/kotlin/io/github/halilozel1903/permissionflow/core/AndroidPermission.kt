package io.github.halilozel1903.permissionflow.core

/**
 * Names of Android runtime permissions, so pure Kotlin code can use them without `android.Manifest`.
 * The values are identical to `Manifest.permission.*`.
 */
public object AndroidPermission {
    public const val CAMERA: String = "android.permission.CAMERA"
    public const val RECORD_AUDIO: String = "android.permission.RECORD_AUDIO"
    public const val ACCESS_FINE_LOCATION: String = "android.permission.ACCESS_FINE_LOCATION"
    public const val ACCESS_COARSE_LOCATION: String = "android.permission.ACCESS_COARSE_LOCATION"
    public const val ACCESS_BACKGROUND_LOCATION: String = "android.permission.ACCESS_BACKGROUND_LOCATION"
    public const val POST_NOTIFICATIONS: String = "android.permission.POST_NOTIFICATIONS"
    public const val READ_EXTERNAL_STORAGE: String = "android.permission.READ_EXTERNAL_STORAGE"
    public const val WRITE_EXTERNAL_STORAGE: String = "android.permission.WRITE_EXTERNAL_STORAGE"
    public const val READ_MEDIA_IMAGES: String = "android.permission.READ_MEDIA_IMAGES"
    public const val READ_MEDIA_VIDEO: String = "android.permission.READ_MEDIA_VIDEO"
    public const val READ_MEDIA_AUDIO: String = "android.permission.READ_MEDIA_AUDIO"
    public const val READ_MEDIA_VISUAL_USER_SELECTED: String = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    public const val ACCESS_MEDIA_LOCATION: String = "android.permission.ACCESS_MEDIA_LOCATION"
    public const val READ_CONTACTS: String = "android.permission.READ_CONTACTS"
    public const val READ_CALENDAR: String = "android.permission.READ_CALENDAR"
    public const val READ_PHONE_STATE: String = "android.permission.READ_PHONE_STATE"
    public const val READ_PHONE_NUMBERS: String = "android.permission.READ_PHONE_NUMBERS"
    public const val ANSWER_PHONE_CALLS: String = "android.permission.ANSWER_PHONE_CALLS"
    public const val ACCEPT_HANDOVER: String = "android.permission.ACCEPT_HANDOVER"
    public const val BODY_SENSORS: String = "android.permission.BODY_SENSORS"
    public const val BODY_SENSORS_BACKGROUND: String = "android.permission.BODY_SENSORS_BACKGROUND"
    public const val ACTIVITY_RECOGNITION: String = "android.permission.ACTIVITY_RECOGNITION"
    public const val BLUETOOTH_SCAN: String = "android.permission.BLUETOOTH_SCAN"
    public const val BLUETOOTH_CONNECT: String = "android.permission.BLUETOOTH_CONNECT"
    public const val BLUETOOTH_ADVERTISE: String = "android.permission.BLUETOOTH_ADVERTISE"
    public const val NEARBY_WIFI_DEVICES: String = "android.permission.NEARBY_WIFI_DEVICES"
    public const val UWB_RANGING: String = "android.permission.UWB_RANGING"
}
