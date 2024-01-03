package com.example.jeudifferences

import android.content.Intent
import android.os.Bundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatActivityTest {
    private lateinit var chatActivity: ChatActivity

    @Before
    fun setUp() {
        val intent = Intent().apply {
            putExtra("name", "Name")
        }
        val savedInstanceState = Bundle()
        chatActivity =
            Robolectric.buildActivity(ChatActivity::class.java, intent, savedInstanceState)
                .create()
                .start()
                .resume()
                .get()
    }

    @Test
    fun testSendMessage() {

        // Call the sendMessage method
        chatActivity.sendMessage("Name")
        // Assert that the message is added to the RecyclerView
        assert(chatActivity.messageArray.size == 1)
        assert(chatActivity.messageArray[0].senderName == "Name")
    }

    //when sendMessage is not called should not change messageArray size
    @Test
    fun testMessageArray() {

        // Call the sendMessage method
        //chatActivity.sendMessage
        assert(chatActivity.messageArray.size == 0)
    }
}
