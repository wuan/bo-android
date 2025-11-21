package org.blitzortung.android.dialogs.changelog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.blitzortung.android.app.R

class ChangelogDialog(
    context: Context,
) : android.app.AlertDialog(context) {
    init {
        setCancelable(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.changelog_dialog)
    }

    override fun onStart() {
        super.onStart()

        val releases = ChangelogParser().readChangeLog(context, R.xml.changelog_master)
        val items = createItems(releases)
        val changeLogAdapter = ChangeLogAdapter(context, items)

        with(findViewById<RecyclerView>(R.id.changelog)) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = changeLogAdapter
        }
    }

    private fun createItems(releases: List<Release>): List<ViewItem> {
        val items = mutableListOf<ViewItem>()
        for (release in releases) {
            items.add(ReleaseEntry(release.versionName, release.versionCode))
            for (change in release.changes) {
                items.add(ChangeEntry(change.description))
            }
        }
        return items
    }
}

interface ViewItem {
    val viewType: ChangeLogAdapter.Type
    val layoutId: Int
}

class ReleaseEntry(
    val versionName: String,
    val versionCode: Int,
) : ViewItem {
    override val viewType = ChangeLogAdapter.Type.HEADER
    override val layoutId = R.layout.changelog_header
}

class ChangeEntry(
    val description: String,
) : ViewItem {
    override val viewType = ChangeLogAdapter.Type.ENTRY
    override val layoutId = R.layout.changelog_entry
}

class HeaderView(
    view: View,
) : RecyclerView.ViewHolder(view) {
    val header = view.findViewById<TextView>(R.id.changelog_header)
}

class EntryView(
    view: View,
) : RecyclerView.ViewHolder(view) {
    val description = view.findViewById<TextView>(R.id.changelog_description)
}

class ChangeLogAdapter(
    context: Context,
    private val items: List<ViewItem>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val inflater = LayoutInflater.from(context)

    enum class Type { ENTRY, HEADER }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            Type.HEADER.ordinal -> HeaderView(inflater.inflate(R.layout.changelog_header, parent, false))
            Type.ENTRY.ordinal -> EntryView(inflater.inflate(R.layout.changelog_entry, parent, false))
            else -> throw IllegalArgumentException("Type not handled: $viewType")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val viewItem = items[position]

        when (viewItem.viewType) {
            Type.HEADER -> (holder as HeaderView).header.text = (viewItem as ReleaseEntry).versionName
            Type.ENTRY -> (holder as EntryView).description.text = (viewItem as ChangeEntry).description
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType.ordinal
    }
}
