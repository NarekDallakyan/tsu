package social.tsu.android.ui.user_profile.insights.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import social.tsu.android.*
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.network.model.GraphItemResponse
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.user_profile.insights.analytics.graph.GraphMarkerView
import social.tsu.android.ui.user_profile.insights.analytics.graph.GraphXAxisValueFormatter
import social.tsu.android.ui.user_profile.insights.analytics.graph.GraphYAxisValueFormatter
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import javax.inject.Inject

class UserAnalyticsFragment : Fragment(), OnChartValueSelectedListener {
    private val TAG = UserAnalyticsFragment::class.java.simpleName
    private lateinit var filtersRecyclerView: RecyclerView
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var sortButton: ImageButton
    private lateinit var timelineTextView: TextView
    private lateinit var periodChoiceLabel: TextView

    private lateinit var lineChart: LineChart
    private lateinit var dataSet: LineDataSet
    private lateinit var mappedDataSet: List<Entry>

    private var currentDate =
        ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate().minusWeeks(1)
    private var sortOrder = UserAnalyticsSortOrder.DESC
    private var filterBy = FilterType.VIEWS

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    private val filtersAdapter = UserAnalyticsFilterAdapter { filterType, position ->
        filtersRecyclerView.smoothScrollToPosition(getPosition(filterType, position))
        filterBy = filterType

        mappedDataSet = when (filterBy) {
            FilterType.VIEWS -> mapResponseToDataSet(viewModel.analyticsLiveData.value!!.views)
            FilterType.LIKES -> mapResponseToDataSet(viewModel.analyticsLiveData.value!!.likes)
            FilterType.COMMENTS -> mapResponseToDataSet(viewModel.analyticsLiveData.value!!.comments)
            FilterType.SHARES -> mapResponseToDataSet(viewModel.analyticsLiveData.value!!.shares)
        }

        dataSet = LineDataSet(mappedDataSet, "")
        setupDataSet(dataSet, filterBy)
        setupLineChart(lineChart, dataSet)

        when (filterType) {
            FilterType.VIEWS -> {
                val sortedByViews = when (sortOrder) {
                    UserAnalyticsSortOrder.ASC -> postAdapter.itemsList.sortedBy { it.view_count }
                    UserAnalyticsSortOrder.DESC -> postAdapter.itemsList.sortedByDescending { it.view_count }
                }
                postAdapter.filterType = FilterType.VIEWS
                postAdapter.submitList(sortedByViews)
            }
            FilterType.LIKES -> {
                val sortedByLikes = when (sortOrder) {
                    UserAnalyticsSortOrder.ASC -> postAdapter.itemsList.sortedBy { it.like_count }
                    UserAnalyticsSortOrder.DESC -> postAdapter.itemsList.sortedByDescending { it.like_count }
                }
                postAdapter.filterType = FilterType.LIKES
                postAdapter.submitList(sortedByLikes)
            }
            FilterType.COMMENTS -> {
                val sortedByComments = when (sortOrder) {
                    UserAnalyticsSortOrder.ASC -> postAdapter.itemsList.sortedBy { it.comment_count }
                    UserAnalyticsSortOrder.DESC -> postAdapter.itemsList.sortedByDescending { it.comment_count }
                }
                postAdapter.filterType = FilterType.COMMENTS
                postAdapter.submitList(sortedByComments)
            }
            FilterType.SHARES -> {
                val sortedByShares = when (sortOrder) {
                    UserAnalyticsSortOrder.ASC -> postAdapter.itemsList.sortedBy { it.share_count }
                    UserAnalyticsSortOrder.DESC -> postAdapter.itemsList.sortedByDescending { it.share_count }
                }
                postAdapter.filterType = FilterType.SHARES
                postAdapter.submitList(sortedByShares)
            }
        }
    }

    private val postAdapter = UserAnalyticsPostAdapter {
        findNavController().navigate(R.id.singlePostFragment, bundleOf("postId" to it))
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    val viewModel by viewModels<UserAnalyticsViewModel> { viewModelFactory }

    companion object {
        fun newInstance() = UserAnalyticsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        viewModel.currentPeriodLabel.value = getString(R.string.this_week)
        viewModel.currentTimelineLabel.value =
            "${currentDate.formatDateToEEEdd()} - ${currentDate.plusWeeks(1).formatDateToEEEdd()}"
        viewModel.isLineChartVisible.value = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_analytics, container, false)
        lineChart = view.findViewById(R.id.line_chart)

        if (requireActivity().isInternetAvailable())
            viewModel.loadAnalytics(currentDate.formatDateToQueryArgument())
        else
            requireActivity().internetSnack()

        filtersRecyclerView = view.findViewById(R.id.filters_recyclerView)
        filtersRecyclerView.adapter = filtersAdapter

        postsRecyclerView = view.findViewById(R.id.posts_recyclerView)
        postsRecyclerView.adapter = postAdapter

        sortButton = view.findViewById(R.id.sort_button)
        sortButton.apply { setSortIcon(this, sortOrder) }
        sortButton.setOnClickListener {
            when (sortOrder) {
                UserAnalyticsSortOrder.ASC -> {
                    sortOrder = UserAnalyticsSortOrder.DESC
                    val sortedData = when (filterBy) {
                        FilterType.VIEWS -> postAdapter.itemsList.sortedByDescending { it.view_count }
                        FilterType.LIKES -> postAdapter.itemsList.sortedByDescending { it.like_count }
                        FilterType.COMMENTS -> postAdapter.itemsList.sortedByDescending { it.comment_count }
                        FilterType.SHARES -> postAdapter.itemsList.sortedByDescending { it.share_count }
                    }
                    postAdapter.filterType = filterBy
                    postAdapter.submitList(sortedData)
                }
                UserAnalyticsSortOrder.DESC -> {
                    sortOrder = UserAnalyticsSortOrder.ASC
                    val sortedData = when (filterBy) {
                        FilterType.VIEWS -> postAdapter.itemsList.sortedBy { it.view_count }
                        FilterType.LIKES -> postAdapter.itemsList.sortedBy { it.like_count }
                        FilterType.COMMENTS -> postAdapter.itemsList.sortedBy { it.comment_count }
                        FilterType.SHARES -> postAdapter.itemsList.sortedBy { it.share_count }
                    }
                    postAdapter.filterType = filterBy
                    postAdapter.submitList(sortedData)
                }
            }

            setSortIcon(it, sortOrder)
        }

        timelineTextView = view.findViewById(R.id.timeline_textview)
        periodChoiceLabel = view.findViewById(R.id.period_choice_label)

        viewModel.currentTimelineLabel.observe(viewLifecycleOwner, Observer {
            timelineTextView.text = it
        })

        viewModel.currentPeriodLabel.observe(viewLifecycleOwner, Observer {
            periodChoiceLabel.text = it
        })

        viewModel.isLineChartVisible.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> lineChart.show()
                false -> lineChart.hide()
            }
        })

        view.findViewById<ImageButton>(R.id.analytics_calendar_button).setOnClickListener {
            showPeriodDialog(R.layout.dialog_bottom_analytics_calendar_actions)
        }

        timelineTextView.setOnClickListener {
            showPeriodDialog(R.layout.dialog_bottom_analytics_calendar_actions)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.filtersLiveData.observe(viewLifecycleOwner, Observer { filtersList ->
            filtersList.map { it.isSelected = it.filterType == filterBy }
            filtersAdapter.submitList(filtersList)
        })

        analyticsHelper.logEvent("insights_viewed")
        viewModel.analyticsLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            mappedDataSet = when (filterBy) {
                FilterType.VIEWS -> mapResponseToDataSet(it.views)
                FilterType.LIKES -> mapResponseToDataSet(it.likes)
                FilterType.COMMENTS -> mapResponseToDataSet(it.comments)
                FilterType.SHARES -> mapResponseToDataSet(it.shares)
            }

            dataSet = LineDataSet(mappedDataSet, "")
            setupDataSet(dataSet, filterBy)
            setupLineChart(lineChart, dataSet)

            when (sortOrder) {
                UserAnalyticsSortOrder.DESC -> {
                    val sortedData = when (filterBy) {
                        FilterType.VIEWS -> it.posts.sortedByDescending { it.view_count }
                        FilterType.LIKES -> it.posts.sortedByDescending { it.like_count }
                        FilterType.COMMENTS -> it.posts.sortedByDescending { it.comment_count }
                        FilterType.SHARES -> it.posts.sortedByDescending { it.share_count }
                    }
                    postAdapter.filterType = filterBy
                    postAdapter.submitList(sortedData)
                }
                UserAnalyticsSortOrder.ASC -> {
                    val sortedData = when (filterBy) {
                        FilterType.VIEWS -> it.posts.sortedBy { it.view_count }
                        FilterType.LIKES -> it.posts.sortedBy { it.like_count }
                        FilterType.COMMENTS -> it.posts.sortedBy { it.comment_count }
                        FilterType.SHARES -> it.posts.sortedBy { it.share_count }
                    }
                    postAdapter.filterType = filterBy
                    postAdapter.submitList(sortedData)
                }
            }
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            it?.let { snack(it) }
        })
    }

    private fun setSortIcon(imageButton: View, sortOrder: UserAnalyticsSortOrder) {
        when (sortOrder) {
            UserAnalyticsSortOrder.ASC -> (imageButton as ImageButton).setImageResource(R.drawable.ic_sort_low)
            UserAnalyticsSortOrder.DESC -> (imageButton as ImageButton).setImageResource(R.drawable.ic_sort_top)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPeriodDialog(@LayoutRes layoutRes: Int): BottomSheetDialog {
        val sheetView = activity?.layoutInflater?.inflate(layoutRes, null)
        val actionSheet = BottomSheetDialog(context as Context)
        actionSheet.setContentView(sheetView as View)
        actionSheet.show()

        val dateNow = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate()

        //Removing today label for now
        /* sheetView.findViewById<View>(R.id.dialog_today_button)?.setOnClickListener {
             viewModel.isLineChartVisible.value = false
             currentDate = dateNow
             viewModel.currentPeriodLabel.value = getString(R.string.analytics_period_label_today)
             viewModel.currentTimelineLabel.value = dateNow.formatDateToEEEdd()
             viewModel.loadAnalytics(currentDate.formatDateToQueryArgument())
             actionSheet.dismiss()
         }*/

        sheetView.findViewById<View>(R.id.dialog_week_button)?.setOnClickListener {
            viewModel.isLineChartVisible.value = true
            currentDate = dateNow.minusWeeks(1)
            viewModel.currentPeriodLabel.value = getString(R.string.analytics_period_label_week)
            viewModel.currentTimelineLabel.value = dateNow.getWeekTimelineLabel()
            if (requireActivity().isInternetAvailable())
                viewModel.loadAnalytics(currentDate.formatDateToQueryArgument())
            else
                requireActivity().internetSnack()
            actionSheet.dismiss()
        }

        sheetView.findViewById<View>(R.id.dialog_month_button)?.setOnClickListener {
            viewModel.isLineChartVisible.value = true
            lineChart.visibility = View.VISIBLE
            currentDate = dateNow.minusMonths(1)
            viewModel.currentPeriodLabel.value = getString(R.string.analytics_period_label_month)
            viewModel.currentTimelineLabel.value = dateNow.getMonthTimelineLabel(1)
            if (requireActivity().isInternetAvailable())
                viewModel.loadAnalytics(currentDate.formatDateToQueryArgument())
            else
                requireActivity().internetSnack()
            actionSheet.dismiss()
        }

        sheetView.findViewById<View>(R.id.dialog_three_month_button)?.setOnClickListener {
            viewModel.isLineChartVisible.value = true
            lineChart.visibility = View.VISIBLE
            currentDate = dateNow.minusMonths(3)
            viewModel.currentPeriodLabel.value =
                getString(R.string.analytics_period_label_three_month)
            viewModel.currentTimelineLabel.value = dateNow.getMonthTimelineLabel(3)
            if (requireActivity().isInternetAvailable())
                viewModel.loadAnalytics(currentDate.formatDateToQueryArgument())
            else
                requireActivity().internetSnack()
            actionSheet.dismiss()
        }

        sheetView.findViewById<View>(R.id.dialog_cancel_button)?.setOnClickListener {
            actionSheet.dismiss()
        }

        return actionSheet
    }

    private fun getPosition(filterType: FilterType, position: Int): Int {
        return when (filterType) {
            FilterType.VIEWS -> position
            FilterType.LIKES -> position - 1
            FilterType.COMMENTS -> position + 1
            FilterType.SHARES -> position
        }
    }

    private fun getDateTimestamp(originTimestamp: Long): Float {
        val epochDaysLong = ZonedDateTime
            .ofInstant(Instant.ofEpochSecond(originTimestamp), ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
        return epochDaysLong.toFloat()
    }

    private fun setupDataSet(dataSet: LineDataSet, filterType: FilterType) {
        // customize vertical highlight line
        dataSet.enableDashedHighlightLine(20f, 20f, 0f)

        // remove circles form line
        dataSet.setDrawCircles(false)

        // disable horizontal highlight
        dataSet.setDrawHorizontalHighlightIndicator(false)

        // remove values from line
        dataSet.setDrawValues(false)

        // set with of chart line
        dataSet.lineWidth = 2f

        // round graph line
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER

        val lineColor = when (filterType) {
            FilterType.VIEWS -> ContextCompat.getColor(requireContext(), R.color.tsu_yellow)
            FilterType.LIKES -> ContextCompat.getColor(requireContext(), R.color.tsu_red)
            FilterType.COMMENTS -> ContextCompat.getColor(requireContext(), R.color.tsu_blue)
            FilterType.SHARES -> ContextCompat.getColor(requireContext(), R.color.tsu_cine)
        }

        val gradientDrawable = when (filterType) {
            FilterType.VIEWS -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.graph_gradient_views
            )
            FilterType.LIKES -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.graph_gradient_likes
            )
            FilterType.COMMENTS -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.graph_gradient_comments
            )
            FilterType.SHARES -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.graph_gradient_shares
            )
        }
        // set line color
        dataSet.color = lineColor

        // set fill gradient
        dataSet.fillDrawable = gradientDrawable

        // enable filled draw
        dataSet.setDrawFilled(true)

        // remove highlight indicator
        removeSelectionsFormDataSet(dataSet)
    }

    private fun setupLineChart(lineChart: LineChart, dataSet: LineDataSet) {
        // remove highlight line
        lineChart.highlightValues(null)

        resetGraph(lineChart)

        // set data to graph
        if (dataSet.values.isEmpty())
            lineChart.setNoDataText("No chart data available.")
        else
            lineChart.data = LineData(dataSet)

        // remove right axis
        lineChart.axisRight.isEnabled = false

        // get X axis
        val xAxis = lineChart.xAxis
        // move X axis to bottom
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        // set value formatter
        xAxis.valueFormatter = GraphXAxisValueFormatter()

        // get Y axis
        val yAxis = lineChart.axisLeft
        // set value formatter
        yAxis.valueFormatter = GraphYAxisValueFormatter()
        yAxis.axisMinimum = 0f
        yAxis.isGranularityEnabled = true

        // remove zoom
        lineChart.setScaleEnabled(false)

        // set click listener
        lineChart.setOnChartValueSelectedListener(this)

        // set granularity to X axis
        lineChart.xAxis.granularity = 1.0f
        lineChart.xAxis.isGranularityEnabled = true

        // set colors to axis values
        lineChart.xAxis.textColor =
            ContextCompat.getColor(requireContext(), R.color.secondaryDarkGray)
        lineChart.axisLeft.textColor =
            ContextCompat.getColor(requireContext(), R.color.secondaryDarkGray)

        // remove line marker
        lineChart.legend.isEnabled = false

        // remove vertical grid
        lineChart.xAxis.setDrawGridLines(false)

        // add scroll
        lineChart.setVisibleXRangeMaximum(8f)

        // add touch
        lineChart.setTouchEnabled(true)

        // remove description
        lineChart.description.text = ""

        // add animation
        lineChart.animateX(500, Easing.EaseInExpo)

        // add marker
        val markerView = GraphMarkerView(requireContext(), R.layout.graph_marker_view)
        lineChart.marker = markerView
    }

    override fun onNothingSelected() {
        dataSet.setDrawIcons(false)
    }

    override fun onValueSelected(e: Entry, h: Highlight?) {
        mappedDataSet.forEachIndexed { index, _ ->
            dataSet.getEntryForIndex(index).icon = null
        }
        dataSet.setDrawIcons(true)

        val index = getIndexOfValue(e)

        val icon = when (filterBy) {
            FilterType.VIEWS -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_graph_marker_views
            )
            FilterType.LIKES -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_graph_marker_likes
            )
            FilterType.COMMENTS -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_graph_marker_comments
            )
            FilterType.SHARES -> ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_graph_marker_shares
            )
        }

        dataSet.getEntryForIndex(index).icon = icon

    }

    private fun getIndexOfValue(entry: Entry): Int {
        return mappedDataSet.indexOf(entry)
    }

    private fun removeSelectionsFormDataSet(dataSet: LineDataSet) {
        mappedDataSet.forEachIndexed { index, _ ->
            dataSet.getEntryForIndex(index).icon = null
        }
    }

    private fun mapResponseToDataSet(data: List<GraphItemResponse>): List<Entry> {
        return data.map { Entry(getDateTimestamp(it.timestamp), it.count?.toFloat() ?: 0.0f) }
    }

    private fun resetGraph(lineChart: LineChart) {
        lineChart.fitScreen()
        lineChart.data?.clearValues()
        lineChart.xAxis.valueFormatter = null
        lineChart.notifyDataSetChanged()
        lineChart.clear()
        val pixels = dpToPx(requireContext(), 16f)
        lineChart.setViewPortOffsets(pixels, pixels, 0f, pixels)
        lineChart.invalidate()
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}