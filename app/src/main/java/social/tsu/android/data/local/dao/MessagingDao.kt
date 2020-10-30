package social.tsu.android.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import io.reactivex.Maybe
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.network.model.Message

@Dao
interface MessagingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveRecentContacts(vararg recentContact: RecentContact)

    @Query("SELECT COUNT(*) FROM recentcontact WHERE isRead=0 AND senderId!=:currentUserId")
    fun countUnreadContacts(currentUserId: Int): LiveData<Int>

    @Query("SELECT * FROM recentcontact WHERE recipientId=:recipientId OR senderId=:recipientId")
    fun getRecentContact(recipientId: Int): LiveData<RecentContact>

    @Query("SELECT * FROM recentcontact ORDER BY createdAt DESC")
    fun getRecentContacts(): DataSource.Factory<Int, RecentContact>

    @Query("SELECT * FROM recentcontact ORDER BY createdAt DESC")
    fun getRecentContactsList(): List<RecentContact>

    @Delete
    fun removeRecentContacts(vararg recentContact: RecentContact)

    @Query("DELETE FROM recentcontact")
    fun removeAll()

    @Transaction
    fun updateRecents(list: List<RecentContact>) {
        removeAll()
        saveRecentContacts(*list.toTypedArray())
        val oldList = getRecentContactsList().map { it.otherUser?.id }.toHashSet()
        val newList = list.map { it.otherUser?.id }.toHashSet()
        // On some reason RecentContact.id can differ for same conversation
        val difference = oldList - newList
        if (difference.isNotEmpty()) {
            val convToDelete = difference.filterNotNull().toIntArray()
            removeMessagesOfRecipient(*convToDelete)
        }
    }

    @Transaction
    fun removeContactWithMessages(recentContact: RecentContact) {
        removeRecentContacts(recentContact)
        recentContact.otherUser?.id?.let {
            removeMessagesOfRecipient(it)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveMessage(message: Message)

    @Query("SELECT * FROM ChatMessages WHERE recipientId=:recipientId OR senderId=:recipientId ORDER BY createdAt DESC LIMIT 1")
    fun getLastMessage(recipientId: Int): Maybe<Message>

    @Query("SELECT * FROM ChatMessages WHERE recipientId=:recipientId OR senderId=:recipientId ORDER BY createdAt DESC")
    fun getMessagesWithUser(recipientId: Int): DataSource.Factory<Int, Message>

    @Query("SELECT * FROM ChatMessages WHERE recipientId=:recipientId OR senderId=:recipientId ORDER BY createdAt DESC")
    fun getMessageListWithUser(recipientId: Int): List<Message>

    @Query("DELETE FROM ChatMessages WHERE recipientId IN (:recipientId) OR senderId IN (:recipientId)")
    fun removeMessagesOfRecipient(vararg recipientId: Int)

    @Query("SELECT COUNT(*) FROM ChatMessages WHERE recipientId=:recipientId")
    fun getMessagesCount(recipientId: Int): Int

    @Delete
    fun removeMessages(vararg message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveMessageList(list: List<Message>)

}