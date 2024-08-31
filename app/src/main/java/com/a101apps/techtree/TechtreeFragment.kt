package com.a101apps.techtree

import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.UUID

class TechtreeFragment : Fragment() {

    private lateinit var fabScrollToBottom: FloatingActionButton
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var myTechTreesRecyclerView: RecyclerView
    private lateinit var existingTechTreesRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var addButton: MaterialButton
    private lateinit var viewModel: TechtreeViewModel
    private lateinit var progressBarMyTechTrees: ProgressBar
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }
    private val techTreeDao by lazy { database.techTreeDao() }
    private var areDetailsVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_techtree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)

        viewModel = ViewModelProvider(this)[TechtreeViewModel::class.java]

        if (viewModel.getTechTreeData().value.isNullOrEmpty()) {
            loadJsonTechTrees()
        }

        addButton.setOnClickListener {
            navigateToMakeTreeFragment()
        }

        viewModel.getTechTreeData().observe(viewLifecycleOwner) { techTreeList ->
            if (techTreeList.isNotEmpty()) {
                updateJsonUI(techTreeList)
            }
        }
        // Find the menu button
        val menuButton = view.findViewById<ImageButton>(R.id.menuButton)

        // Set an OnClickListener to navigate to MenuFragment
        menuButton.setOnClickListener {
            navigateToMenuFragment()
        }
        setupFab()
        loadTechTreesFromDb() // Load tech trees from the database
    }

    private fun navigateToMenuFragment() {
        findNavController().navigate(R.id.action_techtreeFragment_to_menuFragment)
    }

    private fun navigateToMakeTreeFragment(techTreeId: UUID? = null) {
        val bundle = Bundle()
        // Only put the string in the bundle if the UUID is not null
        techTreeId?.let {
            bundle.putString("techTreeId", it.toString())
        }

        findNavController().navigate(R.id.action_techtreeFragment_to_maketreeFragment, bundle)
    }

    private fun initView(view: View) {
        myTechTreesRecyclerView = view.findViewById(R.id.myTechTreesRecyclerView)
        existingTechTreesRecyclerView = view.findViewById(R.id.existingTechTreesRecyclerView)
        progressBar = view.findViewById(R.id.progressBarTechTrees)
        addButton = view.findViewById(R.id.btnMakeTechTree)

        nestedScrollView = view.findViewById(R.id.nestedScrollView)

        progressBarMyTechTrees = view.findViewById(R.id.progressBarMyTechTrees)

        myTechTreesRecyclerView.layoutManager = LinearLayoutManager(context)
        existingTechTreesRecyclerView.layoutManager = LinearLayoutManager(context)

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
        myTechTreesRecyclerView.addItemDecoration(SpacingItemDecoration(spacingInPixels))
        existingTechTreesRecyclerView.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        addButton.setOnClickListener {
            // TODO: Handle the click event for adding new tech trees
        }

         fabScrollToBottom = view.findViewById(R.id.fabScrollToBottom)
    }
    
    private fun loadJsonTechTrees() {
        CoroutineScope(Dispatchers.Main).launch {
            showLoadingUI()
            val techTree = withContext(Dispatchers.IO) { loadTechTree() }
            val sortedTechTreeList = techTree.entries
                .map { it.key to it.value }
                .sortedBy { it.first }
            viewModel.setTechTreeData(sortedTechTreeList)
        }
    }

    private fun showLoadingUI() {
        progressBar.visibility = View.VISIBLE
         existingTechTreesRecyclerView.visibility = View.GONE
    }

    private fun updateJsonUI(sortedTechTreeList: List<Pair<String, Map<String, String>>>) {
        progressBar.visibility = View.GONE
        existingTechTreesRecyclerView.visibility = View.VISIBLE
        existingTechTreesRecyclerView.adapter = JsonTechTreeAdapter(sortedTechTreeList)
    }

    private fun loadTechTree(): Map<String, Map<String, String>> {
        return resources.openRawResource(R.raw.techtree).use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                Gson().fromJson(reader, type)
            }
        }
    }

    private fun loadTechTreesFromDb() {
        toggleLoadingMyTechTrees(true)  // Show the loading indicator for 'My TechTrees'
        CoroutineScope(Dispatchers.Main).launch {
            val techTrees = withContext(Dispatchers.IO) {
                techTreeDao.getAll().sortedBy { it.details.keys.firstOrNull() ?: "" }
            }
            updateMyTechTreeUI(techTrees)
        }
    }

    private fun updateMyTechTreeUI(techTrees: List<TechTree>) {
        toggleLoadingMyTechTrees(false)
        myTechTreesRecyclerView.visibility = View.VISIBLE
        myTechTreesRecyclerView.adapter = MyTechtreeAdapter(techTrees) { techTree ->
            showOptions(techTree)  // Pass the method to show options as a lambda
        }
    }

    // Separate method to show/hide loading indicator for 'My TechTrees'
    private fun toggleLoadingMyTechTrees(isLoading: Boolean) {
        progressBarMyTechTrees.visibility = if (isLoading) View.VISIBLE else View.GONE
        myTechTreesRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showOptions(techTree: TechTree) {
        // Inflate the custom dialog view
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.tech_tree_options_dialog, null)

        // Create the dialog before setting click listeners
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Now find TextViews in the inflated layout
        val editTextView: TextView = dialogView.findViewById(R.id.tvEditTechTree)
        val deleteTextView: TextView = dialogView.findViewById(R.id.tvDeleteTechTree)

        // Configure click listeners for the TextViews
        editTextView.setOnClickListener {
            // Handle the edit action
            navigateToMakeTreeFragment(techTree.id)
            dialog.dismiss()  // Dismiss the dialog when an action is taken
        }

        deleteTextView.setOnClickListener {
            // Handle the delete action
            showDeleteConfirmation(techTree)
            dialog.dismiss()  // Dismiss the dialog when an action is taken
        }

        // Show the dialog
        dialog.show()
    }

    private fun showDeleteConfirmation(techTree: TechTree) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Tech Tree")
            .setMessage("Are you sure you want to delete this tech tree?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTechTree(techTree)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTechTree(techTree: TechTree) {
        CoroutineScope(Dispatchers.IO).launch {
            techTreeDao.delete(techTree)
            withContext(Dispatchers.Main) {
                // Now the UI update is explicitly on the main thread.
                loadTechTreesFromDb() // Reload data to reflect the deletion
            }
        }
    }

    private fun setupFab() {
        fabScrollToBottom.setOnClickListener {
            areDetailsVisible = !areDetailsVisible
            toggleDetailsVisibility(areDetailsVisible)
            // Update the FAB icon based on the visibility state
            val iconResId = if (areDetailsVisible)
                R.drawable.ic_collapse
            else
                R.drawable.ic_expand
            fabScrollToBottom.setImageResource(iconResId)
        }

    }



    private fun toggleDetailsVisibility(visible: Boolean) {
        (existingTechTreesRecyclerView.adapter as? JsonTechTreeAdapter)?.toggleAllDetails(visible)
        (myTechTreesRecyclerView.adapter as? MyTechtreeAdapter)?.toggleAllDetails(visible)
    }

}

class MyTechtreeAdapter(
    private val techTreeList: List<TechTree>,
    private val onOptionsClicked: (TechTree) -> Unit
) : RecyclerView.Adapter<MyTechtreeAdapter.TechCategoryViewHolder>() {

    private val expandedItems = mutableSetOf<Int>()
    private var expandAll = false

    fun toggleAllDetails(visible: Boolean) {
        expandAll = visible
        expandedItems.clear() // Clear the current expanded items
        if (visible) {
            expandedItems.addAll(techTreeList.indices) // Add all indices to expanded set
        }
        notifyDataSetChanged() // Refresh all views
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TechCategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tech_category, parent, false)
        return TechCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: TechCategoryViewHolder, position: Int) {
        val techTree = techTreeList[position]
        val category = techTree.details.keys.firstOrNull() ?: "Unknown Category"
        val details = techTree.details[category] ?: emptyMap()

        holder.tvCategoryTitle.text = category
        holder.layoutDetails.visibility = if (expandedItems.contains(position) || expandAll) View.VISIBLE else View.GONE

        if (expandedItems.contains(position) || expandAll) {
            populateDetails(holder.layoutDetails, details)
        } else {
            holder.layoutDetails.removeAllViews()
        }

        holder.itemView.setOnClickListener {
            if (expandAll) return@setOnClickListener
            if (expandedItems.contains(position)) {
                expandedItems.remove(position)
            } else {
                expandedItems.add(position)
            }
            notifyItemChanged(position)
        }

        holder.itemView.setOnLongClickListener {
            onOptionsClicked(techTree)
            true
        }
    }

    override fun getItemCount() = techTreeList.size

    private fun populateDetails(layout: LinearLayout, details: Map<String, String>) {
        layout.removeAllViews()
        var counter = 1
        details.forEach { (tech, description) ->
            addDetailToLayout(layout, "$counter. $tech", true)
            addDetailToLayout(layout, description, false)
            counter++
        }
    }

    private fun addDetailToLayout(layout: LinearLayout, text: String, isTitle: Boolean) {
        val textView = TextView(layout.context).apply {
            this.text = text
            textSize = if (isTitle) 18f else 16f
            setTypeface(null, if (isTitle) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (!isTitle) setPadding(0, 0, 0, 16)
        }
        layout.addView(textView)
    }

    class TechCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryTitle: TextView = view.findViewById(R.id.tvCategoryTitle)
        val layoutDetails: LinearLayout = view.findViewById(R.id.layoutDetails)
    }
}

class JsonTechTreeAdapter(private val techTreeList: List<Pair<String, Map<String, String>>>) :
    RecyclerView.Adapter<JsonTechTreeAdapter.TechCategoryViewHolder>() {

    private val expandedItems = mutableSetOf<Int>()
    private var expandAll = false

    fun toggleAllDetails(visible: Boolean) {
        expandAll = visible
        expandedItems.clear() // Clear the current expanded items
        if (visible) {
            expandedItems.addAll(techTreeList.indices) // Add all indices to expanded set
        }
        notifyDataSetChanged() // Refresh all views
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TechCategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tech_category, parent, false)
        return TechCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: TechCategoryViewHolder, position: Int) {
        val (category, details) = techTreeList[position]

        holder.tvCategoryTitle.text = category
        holder.layoutDetails.visibility = if (expandedItems.contains(position) || expandAll) View.VISIBLE else View.GONE

        if (expandedItems.contains(position) || expandAll) {
            populateDetails(holder.layoutDetails, details)
        } else {
            holder.layoutDetails.removeAllViews()
        }

        holder.itemView.setOnClickListener {
            if (expandAll) return@setOnClickListener
            if (expandedItems.contains(position)) {
                expandedItems.remove(position)
            } else {
                expandedItems.add(position)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = techTreeList.size

    private fun populateDetails(layout: LinearLayout, details: Map<String, String>) {
        layout.removeAllViews()
        var counter = 1
        details.forEach { (tech, description) ->
            addDetailToLayout(layout, "$counter. $tech", true)
            addDetailToLayout(layout, description, false)
            counter++
        }
    }

    private fun addDetailToLayout(layout: LinearLayout, text: String, isTitle: Boolean) {
        val textView = TextView(layout.context).apply {
            this.text = text
            textSize = if (isTitle) 18f else 16f
            setTypeface(null, if (isTitle) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (!isTitle) setPadding(0, 0, 0, 16)
        }
        layout.addView(textView)
    }

    class TechCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryTitle: TextView = view.findViewById(R.id.tvCategoryTitle)
        val layoutDetails: LinearLayout = view.findViewById(R.id.layoutDetails)
    }
}

class SpacingItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.bottom = verticalSpaceHeight
    }
}

class TechtreeViewModel : ViewModel() {
    // Define LiveData for your data
    private val techTreeData = MutableLiveData<List<Pair<String, Map<String, String>>>>()

    fun getTechTreeData(): LiveData<List<Pair<String, Map<String, String>>>> {
        return techTreeData
    }

    fun setTechTreeData(data: List<Pair<String, Map<String, String>>>) {
        techTreeData.value = data
    }
}
