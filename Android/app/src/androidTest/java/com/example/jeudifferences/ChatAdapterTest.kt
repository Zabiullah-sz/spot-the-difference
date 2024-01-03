package com.example.jeudifferences

import android.content.Context
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner


/*@RunWith(MockitoJUnitRunner::class)
class ChatAdapterTest {

    private lateinit var adapter: ChatAdapter

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val messageArray = arrayListOf(
            message("Hello", "user1"),
            message("Bye", "user2"),
        )
        val timeArray = arrayListOf(
            "10:00:43",
            "10:01:22",
        )
        val currentUserName = "user1"

        //adapter = ChatAdapter(context, messageArray, currentUserName, timeArray)
    }

    //get item test
    @Test
    fun testItemCount() {
        assert(adapter.itemCount == expectedSize)
    }


    //test getItemViewType fot sent messages
    @Test
    fun testGetItemViewType_sentMessage() {
        val position = 0
        val viewType = adapter.getItemViewType(position)
        assert(viewType == adapter.sent)
    }

    //test getItemViewType fot received messages
    @Test
    fun testGetItemViewType_receivedMessage() {
        val position = 1
        val viewType = adapter.getItemViewType(position)
        assert(viewType == adapter.received)
    }


    companion object {
        const val expectedSize = 2
        val parent: ViewGroup = mock(ViewGroup::class.java)
    }
}*/
