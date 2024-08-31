package com.a101apps.techtree

import android.graphics.Typeface
import android.os.Bundle
import android.text.Selection.moveUp
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MaketreeFragment : Fragment() {

    private lateinit var etTechCategory: EditText
    private lateinit var layoutTechInputContainer: LinearLayout
    private lateinit var btnAddMoreTech: Button
    private lateinit var btnSubmitTechTree: Button
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }
    private val techTreeDao by lazy { database.techTreeDao() }
    private var editingTechTreeId: String? = null
    private lateinit var toolbar: Toolbar
    private var techTreeName: String = "Make Tech Tree"
    private var editingCategoryName: String? = null  // Add this line
    private lateinit var scrollView: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_maketree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scrollView = view.findViewById(R.id.scrollView)
        etTechCategory = view.findViewById(R.id.etTechCategory)
        layoutTechInputContainer = view.findViewById(R.id.layoutTechInputContainer)
        btnAddMoreTech = view.findViewById(R.id.btnAddMoreTech)
        btnSubmitTechTree = view.findViewById(R.id.btnSubmitTechTree)
        toolbar = view.findViewById(R.id.fragment_maketree_toolbar)
        setupToolbar()

        // Retrieve the tech tree ID from arguments (if any)
        editingTechTreeId = arguments?.getString("techTreeId")
        if (editingTechTreeId != null) {
            loadTechTreeForEditing(UUID.fromString(editingTechTreeId))
        }

        btnAddMoreTech.setOnClickListener { addTechDetailInputField() }
        btnSubmitTechTree.setOnClickListener { submitTechTree() }
        toolbar.title = "Create New Tech Tree"  // Default title for new tech trees
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
            }

            // Set title color to white
            toolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.toolbar_text_color))
            // Set the custom title
            updateToolbarTitle(techTreeName)

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
    }

    private fun updateToolbarTitle(category: String) {
        val maxLength = 30  // Maximum characters to display in the title
        val shortenedCategory = if (category.length > maxLength) {
            category.take(maxLength) + "..."
        } else {
            category
        }

        // Update the ActionBar title directly if you're using an AppCompatActivity.
        // It will automatically reflect on the toolbar since it's set as the ActionBar.
        (activity as? AppCompatActivity)?.supportActionBar?.title = shortenedCategory
    }

    private fun loadTechTreeForEditing(techTreeId: UUID) {
        CoroutineScope(Dispatchers.IO).launch {
            techTreeDao.getById(techTreeId)?.let { tree ->
                // Set the original category name for comparison during submission
                editingCategoryName = tree.details.keys.firstOrNull()
                updateUIForEditing(tree)
            }
        }
    }

    private suspend fun updateUIForEditing(techTree: TechTree) {
        withContext(Dispatchers.Main) {
            // Populate the UI with tech tree details for editing
            etTechCategory.setText(techTree.details.keys.firstOrNull() ?: "New Category")
            techTree.details.values.firstOrNull()?.forEach { (techName, techDetail) ->
                addTechDetailInputField(techName, techDetail)
            }
            // Update the toolbar title with the category name
            updateToolbarTitle(techTree.details.keys.firstOrNull() ?: "New Category")
        }
    }

    private fun addTechDetailInputField(techName: String = "", techDetail: String = "") {
        val level = (layoutTechInputContainer.childCount / 6) + 1  // Adjusted for the additional views
        // Container for each tech level section
        val techContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            tag = level  // Tag to identify the level
        }

        // Horizontal layout to contain the title and up button
        val techTitleContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
        }

        // Tech Level Title
        val tvTechLevelTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Tech Level $level"
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, Typeface.BOLD)
        }

        // Up Button
        val btnUp = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setImageResource(android.R.drawable.arrow_up_float)  // Use a drawable icon for the up arrow
            setOnClickListener {
                moveUp(techContainer)  // Function to handle moving the section up
            }
        }

        // Tech Name Subtitle
        val tvTechNameSubtitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = "Tech Name"
            setTypeface(null, Typeface.BOLD)
        }

        // Tech Name Input
        val etTechName = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            hint = "Enter tech name"
            setText(techName)
        }

        // Tech Details Subtitle
        val tvTechDetailSubtitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = "Tech Details"
            setTypeface(null, Typeface.BOLD)
        }

        // Tech Details Input
        val etTechDetail = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            hint = "Enter tech details"
            setText(techDetail)
        }

        // Add title and up button to the title container
        techTitleContainer.addView(tvTechLevelTitle)
        techTitleContainer.addView(btnUp)

        // Add all views to the tech container
        techContainer.addView(techTitleContainer)
        techContainer.addView(tvTechNameSubtitle)
        techContainer.addView(etTechName)
        techContainer.addView(tvTechDetailSubtitle)
        techContainer.addView(etTechDetail)

        // Add the tech container to the main layout
        layoutTechInputContainer.addView(techContainer)
        // Update titles for all tech levels
        updateTechLevelTitles()
    }
    private fun moveUp(currentContainer: LinearLayout) {
        val index = layoutTechInputContainer.indexOfChild(currentContainer)

        if (index > 0) {
            // Remove the current container
            layoutTechInputContainer.removeView(currentContainer)

            // Add the container above the previous one
            layoutTechInputContainer.addView(currentContainer, index - 1)

            // Update titles for all tech levels
            updateTechLevelTitles()
        }
    }

    private fun updateTechLevelTitles() {
        for (i in 0 until layoutTechInputContainer.childCount) {
            val container = layoutTechInputContainer.getChildAt(i) as LinearLayout
            val titleContainer = container.getChildAt(0) as LinearLayout
            val title = titleContainer.getChildAt(0) as TextView
            title.text = "Tech Level ${i + 1}"
        }
    }

    private fun submitTechTree() {
        val newCategoryName = etTechCategory.text.toString().trim()
        val techDetails = mutableMapOf<String, String>()

        // Extract tech details from the dynamic input fields
        layoutTechInputContainer.children.forEach { containerView ->
            if (containerView is LinearLayout) { // Ensure we are working with the correct LinearLayout
                var tempTechName: String? = null
                var tempTechDetail: String? = null

                containerView.children.forEach { view ->
                    when (view) {
                        is EditText -> {
                            val text = view.text.toString().trim()
                            if (tempTechName == null) {
                                tempTechName = text  // Assume first EditText is the tech name
                            } else {
                                tempTechDetail = text  // Assume second EditText is the tech detail
                            }
                        }
                    }
                }

                // Only add valid entries to the map
                if (!tempTechName.isNullOrBlank() && !tempTechDetail.isNullOrBlank()) {
                    techDetails[tempTechName!!] = tempTechDetail!!
                }
            }
        }

        if (newCategoryName.isNotBlank() && techDetails.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val techTreeId = editingTechTreeId?.let { UUID.fromString(it) } ?: UUID.randomUUID()

                val techTree = techTreeDao.getById(techTreeId)
                if (techTree != null) {
                    val updatedDetails = techTree.details.toMutableMap()
                    editingCategoryName?.let {
                        if (it != newCategoryName) {
                            updatedDetails.remove(it)
                        }
                    }

                    updatedDetails[newCategoryName] = techDetails
                    val updatedTechTree = techTree.copy(details = updatedDetails)
                    techTreeDao.update(updatedTechTree)
                } else {
                    val newTechTree = TechTree(id = techTreeId, details = mapOf(newCategoryName to techDetails))
                    techTreeDao.insert(newTechTree)
                    editingTechTreeId = techTreeId.toString()
                }

                withContext(Dispatchers.Main) {
                    updateToolbarTitle(newCategoryName)
                    editingCategoryName = newCategoryName
                    // Show a toast message upon successful submission
                    Toast.makeText(context, "Saved successfully.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Handle error or empty input case
            Toast.makeText(context, "Category or tech details are blank.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        saveTechTree()
    }

    private fun saveTechTree() {
        val newCategoryName = etTechCategory.text.toString().trim()
        val techDetails = extractTechDetails()

        if (newCategoryName.isNotBlank() && techDetails.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val techTreeId = editingTechTreeId?.let { UUID.fromString(it) } ?: UUID.randomUUID()

                val techTree = techTreeDao.getById(techTreeId)
                if (techTree != null) {
                    updateExistingTechTree(techTree, techTreeId, newCategoryName, techDetails)
                } else {
                    saveNewTechTree(techTreeId, newCategoryName, techDetails)
                }

                withContext(Dispatchers.Main) {
                    // Check if the fragment is still added to its activity
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Tech tree saved.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Make sure to also check here
            if (isAdded) {
                Toast.makeText(requireContext(), "Category or tech details are blank.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun extractTechDetails(): MutableMap<String, String> {
        val techDetails = mutableMapOf<String, String>()
        var tempTechName: String? = null
        layoutTechInputContainer.children.forEach { view ->
            when (view) {
                is EditText -> {
                    val text = view.text.toString().trim()
                    if (tempTechName == null) {
                        tempTechName = text
                    } else {
                        tempTechName?.let { techName ->
                            if (techName.isNotBlank() && text.isNotBlank()) {
                                techDetails[techName] = text
                            }
                        }
                        tempTechName = null
                    }
                }
            }
        }
        return techDetails
    }

    private suspend fun updateExistingTechTree(techTree: TechTree, techTreeId: UUID, newCategoryName: String, techDetails: Map<String, String>) {
        val updatedDetails = techTree.details.toMutableMap()

        // Update or remove the old category name if it has changed
        editingCategoryName?.let {
            if (it != newCategoryName) {
                updatedDetails.remove(it)
            }
        }

        // Add or update the new category details
        updatedDetails[newCategoryName] = techDetails

        // Create the updated tech tree
        val updatedTechTree = techTree.copy(details = updatedDetails)

        // Update the tech tree in the database
        techTreeDao.update(updatedTechTree)
    }

    private suspend fun saveNewTechTree(techTreeId: UUID, newCategoryName: String, techDetails: Map<String, String>) {
        // Create a new tech tree with the given details
        val newTechTree = TechTree(id = techTreeId, details = mapOf(newCategoryName to techDetails))

        // Insert the new tech tree into the database
        techTreeDao.insert(newTechTree)

        // Update the editingTechTreeId to reflect the new tech tree's ID
        editingTechTreeId = techTreeId.toString()
    }


}
