package ani.dantotsu.media

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ItemChipBinding
import ani.dantotsu.databinding.ItemSearchHeaderBinding
import ani.dantotsu.saveData
import com.google.android.material.checkbox.MaterialCheckBox.*


class SearchAdapter(private val activity: SearchActivity) : RecyclerView.Adapter<SearchAdapter.SearchHeaderViewHolder>() {
    private val itemViewType = 6969
    var search: Runnable? = null
    var requestFocus: Runnable? = null
    private var textWatcher: TextWatcher? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHeaderViewHolder {
        val binding = ItemSearchHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchHeaderViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: SearchHeaderViewHolder, position: Int) {
        val binding = holder.binding


        val imm: InputMethodManager = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager

        when (activity.style) {
            0 -> {
                binding.searchResultGrid.alpha = 1f
                binding.searchResultList.alpha = 0.33f
            }
            1 -> {
                binding.searchResultList.alpha = 1f
                binding.searchResultGrid.alpha = 0.33f
            }
        }

        binding.searchBar.hint = activity.result.type
        var adult = activity.result.isAdult
        var listOnly = activity.result.onList

        binding.searchBarText.removeTextChangedListener(textWatcher)
        binding.searchBarText.setText(activity.result.search)

        binding.searchAdultCheck.isChecked = adult
        binding.searchList.isChecked = listOnly == true

        binding.searchChipRecycler.adapter = SearchChipAdapter(activity).also {
            activity.updateChips = { it.update() }
        }
        binding.searchChipRecycler.layoutManager = LinearLayoutManager(binding.root.context, HORIZONTAL, false)

        binding.searchFilter.setOnClickListener {
            SearchFilterBottomDialog.newInstance().show(activity.supportFragmentManager, "dialog")
        }

        fun searchTitle() {
            activity.result.apply {
                search = if (binding.searchBarText.text.toString() != "") binding.searchBarText.text.toString() else null
                onList = listOnly
                isAdult = adult
            }
            activity.search()
        }

        textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                searchTitle()
            }
        }
        binding.searchBarText.addTextChangedListener(textWatcher)

        binding.searchBarText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    searchTitle()
                    binding.searchBarText.clearFocus()
                    imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
                    true
                }
                else                         -> false
            }
        }
        binding.searchBar.setEndIconOnClickListener { searchTitle() }

        binding.searchResultGrid.setOnClickListener {
            it.alpha = 1f
            binding.searchResultList.alpha = 0.33f
            activity.style = 0
            saveData("searchStyle", 0)
            activity.recycler()
        }
        binding.searchResultList.setOnClickListener {
            it.alpha = 1f
            binding.searchResultGrid.alpha = 0.33f
            activity.style = 1
            saveData("searchStyle", 1)
            activity.recycler()
        }

        if (Anilist.adult) {
            binding.searchAdultCheck.visibility = View.VISIBLE
            binding.searchAdultCheck.isChecked = adult
            binding.searchAdultCheck.setOnCheckedChangeListener { _, b ->
                adult = b
                searchTitle()
            }
        } else binding.searchAdultCheck.visibility = View.GONE
        binding.searchList.apply {
            if (Anilist.userid != null) {
                visibility = View.VISIBLE
                checkedState = when(listOnly){
                    null -> STATE_UNCHECKED
                    true -> STATE_CHECKED
                    false -> STATE_INDETERMINATE
                }

                addOnCheckedStateChangedListener { _, state ->
                    listOnly = when (state) {
                        STATE_CHECKED       -> true
                        STATE_INDETERMINATE -> false
                        STATE_UNCHECKED     -> null
                        else                -> null
                    }
                }

                setOnTouchListener { _, event ->
                    (event.actionMasked == MotionEvent.ACTION_DOWN).also {
                        if (it) checkedState = (checkedState + 1) % 3
                        searchTitle()
                    }
                }
            } else visibility = View.GONE
        }

        search = Runnable { searchTitle() }
        requestFocus = Runnable { binding.searchBarText.requestFocus() }
    }


    override fun getItemCount(): Int = 1

    inner class SearchHeaderViewHolder(val binding: ItemSearchHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return itemViewType
    }


    class SearchChipAdapter(val activity: SearchActivity) : RecyclerView.Adapter<SearchChipAdapter.SearchChipViewHolder>() {
        private var chips = activity.result.toChipList()

        inner class SearchChipViewHolder(val binding: ItemChipBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchChipViewHolder {
            val binding = ItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SearchChipViewHolder(binding)
        }


        override fun onBindViewHolder(holder: SearchChipViewHolder, position: Int) {
            val chip = chips[position]
            holder.binding.root.apply {
                text = chip.text
                setOnClickListener {
                    activity.result.removeChip(chip)
                    update()
                    activity.search()
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun update() {
            chips = activity.result.toChipList()
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = chips.size
    }
}

