-keepattributes *Annotation*,InnerClasses,EnclosingMethod

-keep,allowoptimization @kotlinx.serialization.Serializable class com.savatech.chimelauncher.** { *; }

-keep class com.savatech.chimelauncher.data.db.AppDatabase_Impl { *; }

-keep class com.savatech.chimelauncher.service.work.DigestWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.savatech.chimelauncher.service.work.CheckInWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.savatech.chimelauncher.service.work.TaskReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.savatech.chimelauncher.service.work.NudgeWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class com.savatech.chimelauncher.ChimeApp { *; }
-keep class com.savatech.chimelauncher.MainActivity { *; }
-keep class com.savatech.chimelauncher.service.GrantExpiryReceiver { *; }
-keep class com.savatech.chimelauncher.service.SessionCompleteReceiver { *; }
-keep class com.savatech.chimelauncher.service.DigestListenerService { *; }
-keep class com.savatech.chimelauncher.service.ForegroundWatcherService { *; }
