package com.example.taskflow

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class IntroActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: WormDotsIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        viewPager = findViewById(R.id.viewPager)
        dotsIndicator = findViewById(R.id.dotsIndicator)

        val adapter = IntroAdapter(this)
        viewPager.adapter = adapter
        dotsIndicator.setViewPager2(viewPager)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == adapter.itemCount - 1) {
                }
            }
        })
    }
}
