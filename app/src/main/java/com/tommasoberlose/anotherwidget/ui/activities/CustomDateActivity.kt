package com.tommasoberlose.anotherwidget.ui.activities

import android.app.Activity
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.blockingBulk
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomDateBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.DateHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.CustomDateViewModel
import com.tommasoberlose.anotherwidget.utils.getCapWordString
import com.tommasoberlose.anotherwidget.utils.openURI
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.android.synthetic.main.activity_custom_date.*
import kotlinx.android.synthetic.main.activity_custom_location.action_back
import kotlinx.android.synthetic.main.activity_custom_location.list_view
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class CustomDateActivity  : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomDateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(CustomDateViewModel::class.java)
        val binding = DataBindingUtil.setContentView<ActivityCustomDateBinding>(this, R.layout.activity_custom_date)


        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_date_example_item) { item, injector ->
                injector
                    .text(R.id.custom_date_example_format, item)
                    .text(R.id.custom_date_example_value, SimpleDateFormat(item, Locale.getDefault()).format(DATE.time))
            }
            .attachTo(list_view)

        adapter.updateData(
            listOf(
                "d", "dd", "EE", "EEEE", "MM", "MMM", "MMMM", "yy", "yyyy"
            )
        )

        setupListener()
        subscribeUi(binding, viewModel)

        date_format.requestFocus()

    }
    private var formatJob: Job? = null

    private fun subscribeUi(binding: ActivityCustomDateBinding, viewModel: CustomDateViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.dateInput.observe(this, Observer { dateFormat ->
            formatJob?.cancel()
            formatJob = lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    loader.visibility = View.VISIBLE
                }

                delay(200)
                var text = if (dateFormat != "") {
                    try {
                        SimpleDateFormat(dateFormat, Locale.getDefault()).format(DATE.time)
                    } catch (e: Exception) {
                        ERROR_STRING
                    }
                } else {
                    ERROR_STRING
                }

                if (viewModel.isDateCapitalize.value == true) {
                    text = text.getCapWordString()
                }

                if (viewModel.isDateUppercase.value == true) {
                    text = text.toUpperCase(Locale.getDefault())
                }

                withContext(Dispatchers.Main) {
                    loader.visibility = View.INVISIBLE
                    date_format_value.text = text
                }

            }
        })

        viewModel.isDateCapitalize.observe(this, Observer {
            viewModel.dateInput.value = viewModel.dateInput.value
            updateCapitalizeUi()
        })

        viewModel.isDateUppercase.observe(this, Observer {
            viewModel.dateInput.value = viewModel.dateInput.value
            updateCapitalizeUi()
        })
    }

    private fun updateCapitalizeUi() {
        when {
            viewModel.isDateUppercase.value == true -> {
                action_capitalize.setImageDrawable(ContextCompat.getDrawable(this@CustomDateActivity, R.drawable.round_publish))
                action_capitalize.alpha = 1f
            }
            viewModel.isDateCapitalize.value == true -> {
                action_capitalize.setImageDrawable(ContextCompat.getDrawable(this@CustomDateActivity, R.drawable.ic_capitalize))
                action_capitalize.alpha = 1f
            }
            else -> {
                action_capitalize.setImageDrawable(ContextCompat.getDrawable(this@CustomDateActivity, R.drawable.round_publish))
                action_capitalize.alpha = 0.3f
            }
        }
    }

    private fun setupListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }

        action_capitalize.setOnClickListener {
            when {
                viewModel.isDateUppercase.value == true -> {
                    viewModel.isDateCapitalize.value = false
                    viewModel.isDateUppercase.value = false
                }
                viewModel.isDateCapitalize.value == true -> {
                    viewModel.isDateCapitalize.value = false
                    viewModel.isDateUppercase.value = true
                }
                else -> {
                    viewModel.isDateCapitalize.value = true
                    viewModel.isDateUppercase.value = false
                }
            }
        }

        action_capitalize.setOnLongClickListener {
            toast(getString(R.string.action_capitalize_the_date))
            true
        }

        action_date_format_info.setOnClickListener {
            openURI("https://developer.android.com/reference/java/text/SimpleDateFormat")
        }
    }

    override fun onBackPressed() {
        Preferences.blockingBulk {
            dateFormat = viewModel.dateInput.value ?: ""
            isDateCapitalize = viewModel.isDateCapitalize.value ?: true
            isDateUppercase = viewModel.isDateUppercase.value ?: false
        }
        super.onBackPressed()
    }

    companion object {
        const val ERROR_STRING = "--"
        val DATE: Calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, 10)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, 1993)
        }
    }
}
