package de.felixnuesse.extract.notifications.implementations

import android.content.Context
import ca.pkay.rcloneexplorer.Items.FileItem
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.notifications.prototypes.WorkerNotification
import ca.pkay.rcloneexplorer.notifications.support.StatusObject

class DownloadWorkerNotification(var context: Context) : WorkerNotification(context) {

    override val CHANNEL_ID = "de.felixnuesse.extract.download_service"


    override val initialTitle = string(R.string.worker_download_initialtitle)
    override val serviceOngoingTitle = initialTitle
    override val serviceFailed = string(R.string.worker_download_failed)
    override val serviceCancelled = string(R.string.worker_download_cancelled)
    override val serviceSuccess = string(R.string.worker_download_complete)



    override val channel_ongoing_title = string(R.string.download_service_ongoing_notification_title)
    override val channel_ongoing_description = string(R.string.download_service_ongoing_notification_description)
    override val channel_success_title = string(R.string.download_service_success_notification_title)
    override val channel_success_description = string(R.string.download_service_success_notification_description)
    override val channel_failed_title = string(R.string.download_service_failed_notification_title)
    override val channel_failed_description = string(R.string.download_service_failed_notification_description)

    override val PERSISTENT_NOTIFICATION_ID = 389

    override val SUMMARY_ID = 390

    override fun generateSuccessMessage(statusObject: StatusObject, fileItem: FileItem): String {
        val totalTransfers = statusObject.getTotalTransfers()
        val transfers = if (totalTransfers > 0) totalTransfers else if (!fileItem.isDir) 1 else 0
        val message = if (totalTransfers <= 1 && !fileItem.isDir) {
            fileItem.name
        } else {
            statusObject.getTransfers().toString()
        }
        return mContext.resources.getQuantityString(
                R.plurals.worker_download_success_message,
                transfers,
                message
        )
    }

}