    package com.kelompok2.petaan

    import android.content.Context
    import io.appwrite.Client
    import io.appwrite.services.Storage

    class AppWriteHelper {
        fun getClient(context: Context): Client {
            return Client(context)
                .setEndpoint("https://cloud.appwrite.io/v1")
                .setProject(BuildConfig.APP_WRITE_PROJECT_ID)
        }

        fun getStorage(client: Client): Storage {
            return Storage(client)
        }

    }