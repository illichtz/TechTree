package com.a101apps.techtree

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MenuFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the toolbar
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarMenu)
        (activity as? AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
            }

            // Set title color to white
            toolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.toolbar_text_color))

            // Set navigation icon (back button) to white using DrawableCompat
            val upArrow = ContextCompat.getDrawable(activity, R.drawable.ic_back) // Ensure you have 'ic_back' drawable
            upArrow?.let {
                DrawableCompat.setTint(it, ContextCompat.getColor(activity, R.color.toolbar_icon_color))
                toolbar.navigationIcon = it
            }

            // Handle back navigation
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        progressBar = view.findViewById(R.id.progressBar) // Initialize ProgressBar

        view.findViewById<Button>(R.id.exportButton).setOnClickListener {
            showLoading(true)
            exportTechTreesToJson()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun exportTechTreesToJson() {
        coroutineScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val techTrees = withContext(Dispatchers.IO) {
                db.techTreeDao().getAll()
            }
            val jsonData = Gson().toJson(techTrees)

            writeToFile(requireContext(), "Mytechtree.json", jsonData)
        }
    }

    private fun writeToFile(context: Context, fileName: String, data: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Check if the Downloads directory is available or create it if it doesn't exist
                val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsPath.exists() && !downloadsPath.mkdirs()) {
                    throw IOException("Cannot create or access Downloads directory")
                }

                val file = File(downloadsPath, fileName)
                file.writeText(data)

                withContext(Dispatchers.Main) {
                    showLoading(false) // Hide loading icon
                    Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showLoading(false) // Hide loading icon
                    Toast.makeText(context, "Failed to export data. Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancel the scope when the fragment is destroyed
    }
}
